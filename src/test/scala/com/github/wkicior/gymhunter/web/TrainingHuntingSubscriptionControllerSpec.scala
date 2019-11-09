package com.github.wkicior.gymhunter.web

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

import akka.http.javadsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.CreateTrainingHuntingSubscriptionCommand
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscription
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingHuntingSubscriptionEventStore
import org.scalatest.{Inside, Matchers, WordSpec}

class TrainingHuntingSubscriptionControllerSpec extends WordSpec with Matchers with ScalatestRouteTest with Inside {
  "TrainingHuntingSubscriptionController" should {
    import com.github.wkicior.gymhunter.app.JsonProtocol._

    val thsEventStore = system.actorOf(TrainingHuntingSubscriptionEventStore.props, "TrainingHuntingSubscriptionEventStore")
    val routes = new RestApi(system, thsEventStore).routes

    "return empty list of training hunting subscriptions " in {
      Get("/api/training-hunting-subscriptions") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Seq[TrainingHuntingSubscription]].size shouldEqual 0
      }
    }

    "save new training hunting subscription and be able to return it" in {
      val endOfHuntDatetime = OffsetDateTime.of(2019, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC)
      Post("/api/training-hunting-subscriptions", CreateTrainingHuntingSubscriptionCommand(123, 8, endOfHuntDatetime)) ~> routes ~> check {
        status shouldEqual StatusCodes.CREATED
        val ths = responseAs[TrainingHuntingSubscription]
        inside(ths) { case TrainingHuntingSubscription(id, externalSystemId, clubId, huntingEndTime, notificationOnSlotsAvailableSentTime) =>
            id.toString should not be empty
            externalSystemId shouldEqual 123
            clubId shouldEqual 8
            huntingEndTime shouldEqual endOfHuntDatetime
            notificationOnSlotsAvailableSentTime shouldBe None
        }
        Get("/api/training-hunting-subscriptions") ~> routes ~> check {
          responseAs[Seq[TrainingHuntingSubscription]] should contain (ths)
        }
      }
    }

    "return already saved training hunting subscriptions" in {
      Get("/api/training-hunting-subscriptions") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Seq[TrainingHuntingSubscription]].size shouldEqual 1
      }
    }

    "delete training hunting subscription by and not returning it on a query" in {
      val endOfHuntDatetime = OffsetDateTime.of(2019, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC)
      Post("/api/training-hunting-subscriptions", CreateTrainingHuntingSubscriptionCommand(124, 8, endOfHuntDatetime)) ~> routes ~> check {
        status shouldEqual StatusCodes.CREATED
        val ths = responseAs[TrainingHuntingSubscription]
        Delete(s"/api/training-hunting-subscriptions/${ths.id}") ~> routes ~> check {
          status shouldEqual StatusCodes.OK
          val deletedThs = responseAs[TrainingHuntingSubscription]
          inside(deletedThs) { case TrainingHuntingSubscription(id, externalSystemId, clubId, huntingEndTime, notificationOnSlotsAvailableSentTime) =>
            id.toString should not be empty
            externalSystemId shouldEqual 124
            clubId shouldEqual 8
            huntingEndTime shouldEqual endOfHuntDatetime
            notificationOnSlotsAvailableSentTime shouldBe None
          }

          Get("/api/training-hunting-subscriptions") ~> routes ~> check {
            status shouldEqual StatusCodes.OK
            responseAs[Seq[TrainingHuntingSubscription]] should not contain deletedThs
          }
        }
      }
    }

    "delete returns 404 if training hunting subscription is not found" in {
      val id = UUID.randomUUID().toString
      Delete(s"/api/training-hunting-subscriptions/$id") ~> routes ~> check {
        status shouldEqual StatusCodes.NOT_FOUND
      }
    }

  }
}
