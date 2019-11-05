package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.wkicior.gymhunter.domain.notification.{IFTTNotification, Notification}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntCommandHandler.CreateTrainingToHuntCommand
import com.github.wkicior.gymhunter.domain.tohunt._
import com.github.wkicior.gymhunter.domain.training.Training
import com.github.wkicior.gymhunter.infrastructure.gymsteer.{GymsteerTrainingFetcher, TrainingResponse}
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingToHuntEventStore
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps
import scala.util.Try


class GymHunterSupervisorIntegrationSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("GymHunter"))


  import JsonProtocol._

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

  private val trainingToHuntEventStore = system.actorOf(TrainingToHuntEventStore.props, "TrainingToHuntEventStore")
  private val trainingFetcher = system.actorOf(GymsteerTrainingFetcher.props, "GymsteerTrainingFetcher")
  private val trainingToHuntCommandHandler = system.actorOf(TrainingToHuntCommandHandler.props(trainingToHuntEventStore))


  private val gymHunterSupervisor = system.actorOf(GymHunterSupervisor.props(trainingToHuntEventStore, trainingFetcher), "GymHunterSupervisorIntegrationTest")

  "A GymHunterSupervisor Actor" should {
    """get all trainings to hunt
      |and send IFTT notifications if any training has slots available
    """.stripMargin in {

      //given
      val training = Training(44L, 1, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1))
      val trainingResponse = TrainingResponse(training)
      val path = s"/api/clubs/8/trainings/${training.id}"

      wireMockServer.stubFor(
        get(urlPathEqualTo(path))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(trainingResponseFormat.write(trainingResponse).toString())
            .withStatus(200)))

      val probe = TestProbe()
      trainingToHuntCommandHandler.tell(CreateTrainingToHuntCommand(44L, 1L, OffsetDateTime.now().plusDays(1)), probe.ref)
      probe.expectMsgType[TrainingToHunt]

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)
      Try(awaitCond(hasIfttBeenNotified(training))).orElse(Try.apply(verifyIfttBeenNotified(training))).get
    }
  }

  private def hasIfttBeenNotified(training: Training) = {
    !wireMockServer.findAll(ifttPost(training)).isEmpty
  }

  private def verifyIfttBeenNotified(training: Training) = {
    wireMockServer.verify(ifttPost(training))
  }

  private def ifttPost(training: Training) = {
    val body: String = ifttNotificationFormat.write(new IFTTNotification(Notification(training.start_date, 8L))).toString()
    postRequestedFor(urlEqualTo("/trigger/gymhunter/with/key/test-key"))
      .withHeader("Content-Type", equalTo("application/json"))
      .withRequestBody(equalToJson(body))
  }
}