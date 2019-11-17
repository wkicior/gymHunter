package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.wkicior.gymhunter.domain.notification.Notification
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.CreateTrainingHuntingSubscriptionCommand
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.GetTrainingHuntingSubscriptionAggregate
import com.github.wkicior.gymhunter.domain.subscription._
import com.github.wkicior.gymhunter.domain.training.Training
import com.github.wkicior.gymhunter.infrastructure.gymsteer.{GymsteerTrainingFetcher, TrainingResponse}
import com.github.wkicior.gymhunter.infrastructure.iftt.{IFTTNotification, IFTTNotificationSender}
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
  private val trainingFetcher = system.actorOf(GymsteerTrainingFetcher.props("http://localhost:8081"), "GymsteerTrainingFetcher")
  private val ifttNotificationSender = system.actorOf(IFTTNotificationSender.props, "IFTTNotificationSender")
  private val thsCommandHandler = system.actorOf(TrainingHuntingSubscriptionCommandHandler.props(thsEventStore))


  private val gymHunterSupervisor = system.actorOf(GymHunterSupervisor.props(thsEventStore, trainingFetcher, ifttNotificationSender), "GymHunterSupervisorIntegrationTest")
  val postNotificationPath = "/trigger/gymhunter/with/key/test-key"


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
        post(urlPathEqualTo(postNotificationPath))
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
      Try(awaitCond(hasIfttBeenNotified(training))).orElse(Try.apply(verifyIfttBeenNotified(training))).get

      thsEventStore.tell(GetTrainingHuntingSubscriptionAggregate(tth.id), probe.ref)
      val updateTthAggregate = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
      updateTthAggregate.toOption.get.notificationOnSlotsAvailableSentTime.get should be <= OffsetDateTime.now
    }
  }

  private def hasIfttBeenNotified(training: Training) = {
    !wireMockServer.findAll(ifttPost(training)).isEmpty
  }

  private def verifyIfttBeenNotified(training: Training) = {
    wireMockServer.verify(ifttPost(training))
  }

  private def ifttPost(training: Training) = {
    val body: String = ifttNotificationFormat.write(new IFTTNotification(Notification(training.start_date, 8L, TrainingHuntingSubscriptionId()))).toString()
    postRequestedFor(urlEqualTo(postNotificationPath))
      .withHeader("Content-Type", equalTo("application/json"))
      .withRequestBody(equalToJson(body))
  }
}