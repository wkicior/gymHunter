package com.github.wkicior.gymhunter.web

import java.time.{OffsetDateTime, ZoneOffset}

import akka.http.javadsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.wkicior.gymhunter.domain.training.{TrainingToHunt, TrainingToHuntRequest}
import org.scalatest.{Inside, Matchers, WordSpec}

class TrainingToHuntControllerSpec extends WordSpec with Matchers with ScalatestRouteTest with Inside {
  "TrainingToHuntController" should {
    import com.github.wkicior.gymhunter.app.JsonProtocol._

    "return all trainings to hunt" in {
      Get("/api/trainings-to-hunt") ~> new RestApi(system).routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Seq[TrainingToHunt]].size should be > 0
      }
    }

    "save new training to hunt" in {
      val endOfHuntDatetime = OffsetDateTime.of(2019, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC)
      Post("/api/trainings-to-hunt", TrainingToHuntRequest(123, 8, endOfHuntDatetime)) ~> new RestApi(system).routes ~> check {
        status shouldEqual StatusCodes.CREATED
        val trainingToHunt = responseAs[TrainingToHunt]
        inside(trainingToHunt) { case TrainingToHunt(id, externalSystemId, clubId, huntingEndTime) =>
            id should not be empty
            externalSystemId shouldEqual 123
            clubId shouldEqual 8
            huntingEndTime shouldEqual endOfHuntDatetime
        }
        Get("/api/trainings-to-hunt") ~> new RestApi(system).routes ~> check {
          responseAs[Seq[TrainingToHunt]] should contain (trainingToHunt)
        }
      }

    }
  }
}
