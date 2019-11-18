package com.github.wkicior.gymhunter.infrastructure.gymsteer.training

import akka.actor.ActorSystem
import akka.actor.Status.{Failure, Success}
import akka.testkit.{TestKit, TestProbe}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.wkicior.gymhunter.infrastructure.gymsteer.training.GymsteerTrainingBooker.BookTraining
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps


class GymsteerTrainingBookerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("GymHunter"))

  override def beforeAll: Unit = {
    wireMockServer.start()
  }

  override def afterAll: Unit = {
    shutdown(system)
    wireMockServer.stop()
  }

  private val port = 8084
  private val hostname = "localhost"
  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))


  private val gymsteerTokenProvider = system.actorOf(GymsteerTrainingBooker.props("http://localhost:8084"), "GymsteerTokenProvider")


  "A GymsteerTrainingBooker Actor" should {
    """book a training using provided access token
      | and return success
    """.stripMargin in {
      //given
      val getBookingPath = s"/api/clubs/8/trainings/1/user-booking"

      wireMockServer.stubFor(
        post(urlPathEqualTo(getBookingPath))
          .withHeader("Access-Token", equalTo("some-token"))
          .willReturn(aResponse()
            .withStatus(201)))

      val probe = TestProbe()

      //when
      gymsteerTokenProvider.tell(BookTraining(1, "some-token"), probe.ref)

      //then
      probe.expectMsg(Success)
    }

    """try to book a training using provided access token
      | and return failure on any error
    """.stripMargin in {
      //given
      val getBookingPath = s"/api/clubs/8/trainings/1/user-booking"

      wireMockServer.stubFor(
        post(urlPathEqualTo(getBookingPath))
          .withHeader("Access-Token", equalTo("some-token"))
          .willReturn(aResponse()
            .withStatus(500)))

      val probe = TestProbe()

      //when
      gymsteerTokenProvider.tell(BookTraining(1, "some-token"), probe.ref)

      //then
      probe.expectMsgType[Failure]
    }
  }
}