package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotification
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.CreateTrainingHuntingSubscriptionCommand
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.GetTrainingHuntingSubscriptionAggregate
import com.github.wkicior.gymhunter.domain.subscription._
import com.github.wkicior.gymhunter.domain.training.Training
import com.github.wkicior.gymhunter.infrastructure.gymsteer.GymsteerProxy
import com.github.wkicior.gymhunter.infrastructure.gymsteer.auth.GymsteerLoginRequest
import com.github.wkicior.gymhunter.infrastructure.gymsteer.training.TrainingResponse
import com.github.wkicior.gymhunter.infrastructure.iftt.{IFTTAutoBookingNotification, IFTTNotificationSender, IFTTSlotsAvailableNotification}
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingHuntingSubscriptionEventStore
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps
import scala.util.Try


class GymHunterSupervisorIntegrationSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("GymHunter"))

  import com.github.wkicior.gymhunter.infrastructure.json.JsonProtocol._

  override def beforeAll: Unit = {
    wireMockServer.start()
  }

  override def afterAll: Unit = {
    shutdown(system)
    wireMockServer.stop()
  }

  private val port = 8081
  private val hostname = "localhost"
  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))

  private val thsEventStore = system.actorOf(TrainingHuntingSubscriptionEventStore.props, "TrainingHuntingSubscriptionEventStore")
  private val gymsteerProxy = system.actorOf(GymsteerProxy.props("http://localhost:8081", "root", "Z10N0101"), "GymsteerProxy")
  private val ifttNotificationSender = system.actorOf(IFTTNotificationSender.props, "IFTTNotificationSender")
  private val thsCommandHandler = system.actorOf(TrainingHuntingSubscriptionCommandHandler.props(thsEventStore))


  private val gymHunterSupervisor = system.actorOf(GymHunterSupervisor.props(thsEventStore, gymsteerProxy, ifttNotificationSender), "GymHunterSupervisorIntegrationTest")
  val postIFTTSlotsAvailableNotificationPath = s"/trigger/${IFTTSlotsAvailableNotification.name}/with/key/test-key"
  val postIFTTAutoBookingNotificationPath = s"/trigger/${IFTTAutoBookingNotification.name}/with/key/test-key"
  val gymsteerLoginPath = "/api/login"


  "A GymHunterSupervisor Actor" should {
    """get all training hunting subscriptions
      |and send IFTT notifications if any training has slots available
    """.stripMargin in {

      //given
      val training = Training(44L, 1, Some(OffsetDateTime.now().minusDays(1)), OffsetDateTime.now().plusDays(1))
      val trainingResponse = TrainingResponse(training)
      val getTrainingPath = s"/api/clubs/8/trainings/${training.id}"


      wireMockServer.stubFor(
        get(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(trainingResponseFormat.write(trainingResponse).toString())
            .withStatus(200)))

      wireMockServer.stubFor(
        post(urlPathEqualTo(postIFTTSlotsAvailableNotificationPath))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("OK")
            .withStatus(200)))

      val probe = TestProbe()
      thsCommandHandler.tell(CreateTrainingHuntingSubscriptionCommand(44L, 8L, OffsetDateTime.now().plusDays(1)), probe.ref)
      val tth = probe.expectMsgType[TrainingHuntingSubscription]

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      Try(awaitCond(hasIfttBeenNotified(training, postIFTTSlotsAvailableNotificationPath))).orElse(Try.apply(verifyIfttBeenNotified(training, postIFTTSlotsAvailableNotificationPath))).get

      thsEventStore.tell(GetTrainingHuntingSubscriptionAggregate(tth.id), probe.ref)
      val updateTthAggregate = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
      updateTthAggregate.toOption.get.notificationOnSlotsAvailableSentTime.get should be <= OffsetDateTime.now
    }

    """get all training hunting subscriptions
      |and auto book subscriptions with booking deadline if training has slots available
      |and send IFTT notification about autoBooking performed
    """.stripMargin in {

      //given
      val training = Training(44L, 1, Some(OffsetDateTime.now().minusDays(1)), OffsetDateTime.now().plusDays(1))
      val trainingResponse = TrainingResponse(training)
      val trainingPath = s"/api/clubs/8/trainings/${training.id}"
      val bookingPath = s"$trainingPath/user-booking"


      wireMockServer.stubFor(
        get(urlPathEqualTo(trainingPath))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(trainingResponseFormat.write(trainingResponse).toString())
            .withStatus(200)))

      wireMockServer.stubFor(
        post(urlPathEqualTo(gymsteerLoginPath))
          .withRequestBody(equalTo(gymsteerLoginRequestFormat.write(GymsteerLoginRequest("root", "Z10N0101")).toString()))
          .willReturn(aResponse()
            .withHeader("Access-Token", "sample-access-token")
            .withStatus(200)))

      wireMockServer.stubFor(
        post(urlPathEqualTo(bookingPath))
          .withHeader("Access-Token", equalTo("sample-access-token"))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)))

      wireMockServer.stubFor(
        post(urlPathEqualTo(postIFTTAutoBookingNotificationPath))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("OK")
            .withStatus(200)))

      val probe = TestProbe()
      thsCommandHandler.tell(CreateTrainingHuntingSubscriptionCommand(44L, 8L, OffsetDateTime.now().plusDays(1), Some(OffsetDateTime.now().plusHours(2))), probe.ref)
      val tth = probe.expectMsgType[TrainingHuntingSubscription]

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      Try(awaitCond(hasTrainingBeenBooked(bookingPath))).orElse(Try.apply(verifyTrainingHasBeenBooked(bookingPath))).get

      thsEventStore.tell(GetTrainingHuntingSubscriptionAggregate(tth.id), probe.ref)
      val updateTthAggregate = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
      updateTthAggregate.toOption.get.autoBookingDateTime.get should be <= OffsetDateTime.now

      Try(awaitCond(hasIfttBeenNotified(training, postIFTTAutoBookingNotificationPath))).orElse(Try.apply(verifyIfttBeenNotified(training, postIFTTAutoBookingNotificationPath))).get
    }
  }

  private def hasIfttBeenNotified(training: Training, path: String): Boolean = {
    !wireMockServer.findAll(ifttPost(training, path)).isEmpty
  }

  private def verifyIfttBeenNotified(training: Training, path: String): Unit = {
    wireMockServer.verify(ifttPost(training, path))
  }

  private def ifttPost(training: Training, path: String): RequestPatternBuilder = {
    val body: String = ifttSlotsAvailableNotificationFormat.write(new IFTTSlotsAvailableNotification(SlotsAvailableNotification(training.start_date, 8L, TrainingHuntingSubscriptionId()))).toString()
    postRequestedFor(urlEqualTo(path))
      .withHeader("Content-Type", equalTo("application/json"))
      .withRequestBody(equalToJson(body))
  }

  private def hasTrainingBeenBooked(trainingPath: String) = {
    !wireMockServer.findAll(gymsteerAutobookingPost(trainingPath)).isEmpty
  }

  private def verifyTrainingHasBeenBooked(trainingPath: String): Unit = {
    wireMockServer.verify(gymsteerAutobookingPost(trainingPath))
  }

  private def gymsteerAutobookingPost(trainingPath: String): RequestPatternBuilder = {
    postRequestedFor(urlEqualTo(trainingPath))
      .withHeader("Access-Token", equalTo("sample-access-token"))
  }
}