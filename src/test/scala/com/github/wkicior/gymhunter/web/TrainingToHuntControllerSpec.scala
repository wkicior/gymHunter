package com.github.wkicior.gymhunter.web

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

import akka.http.javadsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntCommandHandler.CreateTrainingToHuntCommand
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHunt
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingToHuntEventStore
import org.scalatest.{Inside, Matchers, WordSpec}

class TrainingToHuntControllerSpec extends WordSpec with Matchers with ScalatestRouteTest with Inside {
  "TrainingToHuntController" should {
    import com.github.wkicior.gymhunter.app.JsonProtocol._

    val trainingToHuntRepository = system.actorOf(TrainingToHuntEventStore.props, "TrainingToHuntEventStore")
    val routes = new RestApi(system, trainingToHuntRepository).routes

    "return empty list of trainings to hunt " in {
      Get("/api/trainings-to-hunt") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Seq[TrainingToHunt]].size shouldEqual 0
      }
    }

    "save new training to hunt and be able to return it" in {
      val endOfHuntDatetime = OffsetDateTime.of(2019, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC)
      Post("/api/trainings-to-hunt", CreateTrainingToHuntCommand(123, 8, endOfHuntDatetime)) ~> routes ~> check {
        status shouldEqual StatusCodes.CREATED
        val trainingToHunt = responseAs[TrainingToHunt]
        inside(trainingToHunt) { case TrainingToHunt(id, externalSystemId, clubId, huntingEndTime, notificationOnSlotsAvailableSentTime) =>
            id.toString should not be empty
            externalSystemId shouldEqual 123
            clubId shouldEqual 8
            huntingEndTime shouldEqual endOfHuntDatetime
            notificationOnSlotsAvailableSentTime shouldBe None
        }
        Get("/api/trainings-to-hunt") ~> routes ~> check {
          responseAs[Seq[TrainingToHunt]] should contain (trainingToHunt)
        }
      }
    }

    "return already saved trainings to hunt " in {
      Get("/api/trainings-to-hunt") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Seq[TrainingToHunt]].size shouldEqual 1
      }
    }

    "delete training to hunt by and not returning it on a query" in {
      val endOfHuntDatetime = OffsetDateTime.of(2019, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC)
      Post("/api/trainings-to-hunt", CreateTrainingToHuntCommand(124, 8, endOfHuntDatetime)) ~> routes ~> check {
        status shouldEqual StatusCodes.CREATED
        val trainingToHunt = responseAs[TrainingToHunt]
        Delete(s"/api/trainings-to-hunt/${trainingToHunt.id}") ~> routes ~> check {
          status shouldEqual StatusCodes.OK
          val deletedTrainingToHunt = responseAs[TrainingToHunt]
          inside(deletedTrainingToHunt) { case TrainingToHunt(id, externalSystemId, clubId, huntingEndTime, notificationOnSlotsAvailableSentTime) =>
            id.toString should not be empty
            externalSystemId shouldEqual 124
            clubId shouldEqual 8
            huntingEndTime shouldEqual endOfHuntDatetime
            notificationOnSlotsAvailableSentTime shouldBe None
          }

          Get("/api/trainings-to-hunt") ~> routes ~> check {
            status shouldEqual StatusCodes.OK
            responseAs[Seq[TrainingToHunt]] should not contain deletedTrainingToHunt
          }
        }
      }
    }

    "delete returns 404 if training to hunt is not found" in {
      val id = UUID.randomUUID().toString
      Delete(s"/api/trainings-to-hunt/$id") ~> routes ~> check {
        status shouldEqual StatusCodes.NOT_FOUND
      }
    }

  }
}
