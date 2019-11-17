package com.github.wkicior.gymhunter.infrastructure.gymsteer.auth

import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.testkit.{TestKit, TestProbe}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.wkicior.gymhunter.infrastructure.gymsteer.GymsteerException
import com.github.wkicior.gymhunter.infrastructure.gymsteer.auth.GymsteerTokenProvider.GetToken
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps


class GymsteerTokenProviderSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("GymHunter"))

  override def beforeAll: Unit = {
    wireMockServer.start()
  }

  override def afterAll: Unit = {
    shutdown(system)
    wireMockServer.stop()
  }

  private val port = 8083
  private val hostname = "localhost"
  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))


  private val gymsteerTokenProvider = system.actorOf(GymsteerTokenProvider.props("http://localhost:8083"), "GymsteerTokenProvider")


  "A GymsteerTokenProvider Actor" should {
    """provide token for given request with valid username and password
    """.stripMargin in {
      //given
      val getTrainingPath = s"/api/login"

      wireMockServer.stubFor(
        post(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withHeader("Access-Token", "sample-token")
            .withStatus(200)))

      val probe = TestProbe()

      //when
      gymsteerTokenProvider.tell(GetToken("some-username", "some-password"), probe.ref)

      //then
      probe.expectMsg("sample-token")
    }

    """return failure if Access-Token header is not found in 200 response
    """.stripMargin in {
      //given
      val getTrainingPath = s"/api/login"

      wireMockServer.stubFor(
        post(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withStatus(200)))

      val probe = TestProbe()

      //when
      gymsteerTokenProvider.tell(GetToken("some-username", "some-password"), probe.ref)

      //then
      probe.expectMsg(Failure(GymsteerException("could not read access-token header")))
    }

    """return error on wrong credentials
    """.stripMargin in {
      //given
      val getTrainingPath = s"/api/login"

      wireMockServer.stubFor(
        post(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withStatus(401)))

      val probe = TestProbe()

      //when
      gymsteerTokenProvider.tell(GetToken("some-username", "some-password"), probe.ref)

      //then
      probe.expectMsg(Failure(GymsteerException("not authenticated")))
    }

    """return error on server error
    """.stripMargin in {
      //given
      val getTrainingPath = s"/api/login"

      wireMockServer.stubFor(
        post(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withStatus(500)))

      val probe = TestProbe()

      //when
      gymsteerTokenProvider.tell(GetToken("some-username", "some-password"), probe.ref)

      //then
      probe.expectMsgType[Failure]
    }
  }
}