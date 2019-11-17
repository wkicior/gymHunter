package com.github.wkicior.gymhunter.infrastructure.gymsteer


import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.actor.Status.Failure
import akka.testkit.{TestKit, TestProbe}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.wkicior.gymhunter.domain.training.{GetTraining, Training}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.JsString

import scala.io.Source
import scala.language.postfixOps


class GymsteerTrainingFetcherSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("GymHunter"))


  import com.github.wkicior.gymhunter.infrastructure.json.JsonProtocol._

  override def beforeAll: Unit = {
    wireMockServer.start()
  }

  override def afterAll: Unit = {
    shutdown(system)
    wireMockServer.stop()
  }

  private val port = 8082
  private val hostname = "localhost"
  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))


  private val gymHunterSupervisor = system.actorOf(GymsteerTrainingFetcher.props("http://localhost:8082"), "GymsteerTrainingFetcher")

  val sampleValidResponse =
    """
      |{
      |  "training": {
      |    "slotsAvailable": 7,
      |    "id": 42,
      |    "club": {
      |      "id": 8,
      |      "name": "XXX"
      |    },
      |    "activity": {
      |      "id": 143,
      |      "name": "WOD",
      |      "description": "XXX",
      |      "burnedCalories": 700,
      |      "difficultyLevel": "intermediate",
      |      "color": "#0184E8",
      |      "only_for_club_members": true,
      |      "order": 1,
      |      "pay_per_entry_activity_config": {
      |        "disable": false
      |      },
      |      "time_before_last_possible_check_out": 3
      |    },
      |    "trainer": {
      |      "id": 90,
      |      "name": "XXX",
      |      "position": "trener",
      |      "positionType": "trainer",
      |      "picture": {
      |        "name": "XXX.jpg",
      |        "mediaUrl": "https:\/\/api.gymsteer.com\/uploads\/media\/employee_picture\/0001\/11\/thumb_11_employee_picture_default.jpeg"
      |      },
      |      "order": 2
      |    },
      |    "room": {
      |      "id": 15,
      |      "name": "XXX sala"
      |    },
      |    "sportsmanLimit": 16,
      |    "start_date": "2019-11-15T16:30:00+0100",
      |    "end_date": "2019-11-15T17:30:00+0100",
      |    "cycle": {
      |      "id": 19967,
      |      "start_date": "2019-07-05T16:30:00+0200",
      |      "infinite": true,
      |      "occurrence_type": "week",
      |      "occurrence_count": 1,
      |      "week_days": [
      |        "friday"
      |      ]
      |    },
      |    "bookings_open_at": "2019-11-08T16:30:00+0100",
      |    "only_for_club_members": false
      |  },
      |  "bookingOption": "pass",
      |  "invoiceDataSet": false
      |}
    """.stripMargin

  val sampleValidResponseWithoutBookingsOpenAt =
    """
      |{
      |  "training": {
      |    "slotsAvailable": 7,
      |    "id": 42,
      |    "club": {
      |      "id": 8,
      |      "name": "XXX"
      |    },
      |    "sportsmanLimit": 16,
      |    "start_date": "2019-11-15T16:30:00+0100",
      |    "end_date": "2019-11-15T17:30:00+0100"
      |  }
      |}
    """.stripMargin


  "A GymsteerTrainingFetcher Actor" should {
    """get training from external service
      |and return it as Training value object
    """.stripMargin in {
      //given
      val training = Training(42, 7, Some(OffsetDateTimeFormat.read(JsString("2019-11-08T16:30:00+0100"))), OffsetDateTimeFormat.read(JsString("2019-11-15T16:30:00+0100")))
      val getTrainingPath = s"/api/clubs/8/trainings/${training.id}"

      wireMockServer.stubFor(
        get(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(sampleValidResponse)
            .withStatus(200)))

      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GetTraining(training.id), probe.ref)

      //then
      probe.expectMsg(training)
    }

    """get training with no bookings_open_at property from external service
      |and return it as Training value object
    """.stripMargin in {
      //given
      val training = Training(42, 7, Option.empty[OffsetDateTime], OffsetDateTimeFormat.read(JsString("2019-11-15T16:30:00+0100")))
      val getTrainingPath = s"/api/clubs/8/trainings/${training.id}"

      wireMockServer.stubFor(
        get(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(sampleValidResponseWithoutBookingsOpenAt)
            .withStatus(200)))

      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GetTraining(training.id), probe.ref)

      //then
      probe.expectMsg(training)
    }

    """get training from external service
      |and fail if response can't be deserialized
    """.stripMargin in {
      //given
      val training = Training(42, 7, Some(OffsetDateTimeFormat.read(JsString("2019-11-08T16:30:00+0100"))), OffsetDateTimeFormat.read(JsString("2019-11-15T16:30:00+0100")))
      val getTrainingPath = s"/api/clubs/8/trainings/${training.id}"

      wireMockServer.stubFor(
        get(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"training\": {\"id\": 7}}")
            .withStatus(200)))

      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GetTraining(training.id), probe.ref)

      //then
      probe.expectMsg(Failure(GymsteerTrainingFetcherException("Could not deserialize the response: Object is missing required member 'slotsAvailable'")))
    }

    """get training from external service
      |and fail on server error
    """.stripMargin in {
      //given
      val getTrainingPath = s"/api/clubs/8/trainings/101"

      wireMockServer.stubFor(
        get(urlPathEqualTo(getTrainingPath))
          .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("Service unavailable")
            .withStatus(500)))

      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GetTraining(101), probe.ref)

      //then
      val fail = probe.expectMsgType[Failure]
      fail.cause shouldBe a[GymsteerTrainingFetcherException]
    }
  }
}