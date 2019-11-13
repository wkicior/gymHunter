package com.github.wkicior.gymhunter.web

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.CreateTrainingHuntingSubscriptionCommand
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingHuntingSubscriptionEventStore
import org.scalatest.{Inside, Matchers, WordSpec}


class TrainingHuntingSubscriptionControllerSpec extends WordSpec with Matchers with ScalatestRouteTest with Inside {
  "TrainingHuntingSubscriptionController" should {
    import com.github.wkicior.gymhunter.app.JsonProtocol._

    val thsEventStore = system.actorOf(TrainingHuntingSubscriptionEventStore.props, "TrainingHuntingSubscriptionEventStore")
    val routes = new RestApi(system, thsEventStore).routes
    val validCredentials = BasicHttpCredentials("John", "knight-who-say-ni")


    "require authentication" in {
      Get("/api/training-hunting-subscriptions") ~> routes  ~> check {
        rejection shouldBe a [AuthenticationFailedRejection]
      }
    }

    "reject with invalid authentication" in {
      Get("/api/training-hunting-subscriptions") ~> addCredentials(BasicHttpCredentials("John", "invalidpass")) ~> routes  ~> check {
        rejection shouldBe a [AuthenticationFailedRejection]
      }
    }

    "return empty list of training hunting subscriptions " in {
      Get("/api/training-hunting-subscriptions") ~> addCredentials(validCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Seq[TrainingHuntingSubscription]].size shouldEqual 0
      }
    }

    "save new training hunting subscription and be able to return it" in {
      val endOfHuntDatetime = OffsetDateTime.of(2019, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC)
      Post("/api/training-hunting-subscriptions", CreateTrainingHuntingSubscriptionCommand(123, 8, endOfHuntDatetime)) ~> addCredentials(validCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        val ths = responseAs[TrainingHuntingSubscription]
        inside(ths) { case TrainingHuntingSubscription(id, externalSystemId, clubId, huntingEndTime, notificationOnSlotsAvailableSentTime, autoBookingDeadline) =>
            id.toString should not be empty
            externalSystemId shouldEqual 123
            clubId shouldEqual 8
            huntingEndTime shouldEqual endOfHuntDatetime
            notificationOnSlotsAvailableSentTime shouldBe None
            autoBookingDeadline shouldBe None
        }
        Get("/api/training-hunting-subscriptions") ~> addCredentials(validCredentials) ~> routes ~> check {
          responseAs[Seq[TrainingHuntingSubscription]] should contain (ths)
        }
      }
    }

    "save new training hunting subscription with auto booking option and be able to return it" in {
      val date = OffsetDateTime.of(2019, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC)
      Post("/api/training-hunting-subscriptions", CreateTrainingHuntingSubscriptionCommand(123, 8, date, Some(date))) ~> addCredentials(validCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        val ths = responseAs[TrainingHuntingSubscription]
        inside(ths) { case TrainingHuntingSubscription(id, externalSystemId, clubId, huntingEndTime, notificationOnSlotsAvailableSentTime, autoBookingDeadline) =>
          id.toString should not be empty
          externalSystemId shouldEqual 123
          clubId shouldEqual 8
          huntingEndTime shouldEqual date
          notificationOnSlotsAvailableSentTime shouldBe None
          autoBookingDeadline shouldBe Some(date)
        }
        Get("/api/training-hunting-subscriptions") ~> addCredentials(validCredentials) ~> routes ~> check {
          responseAs[Seq[TrainingHuntingSubscription]] should contain (ths)
        }
      }
    }

    "return already saved training hunting subscriptions" in {
      Get("/api/training-hunting-subscriptions") ~> addCredentials(validCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Seq[TrainingHuntingSubscription]].size shouldEqual 2
      }
    }

    "delete training hunting subscription by and not returning it on a query" in {
      val endOfHuntDatetime = OffsetDateTime.of(2019, 11, 1, 14, 0, 0, 0, ZoneOffset.UTC)
      Post("/api/training-hunting-subscriptions", CreateTrainingHuntingSubscriptionCommand(124, 8, endOfHuntDatetime)) ~> addCredentials(validCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        val ths = responseAs[TrainingHuntingSubscription]
        Delete(s"/api/training-hunting-subscriptions/${ths.id}") ~> addCredentials(validCredentials) ~> routes ~> check {
          status shouldEqual StatusCodes.OK
          val deletedThs = responseAs[TrainingHuntingSubscription]
          inside(deletedThs) { case TrainingHuntingSubscription(id, externalSystemId, clubId, huntingEndTime, notificationOnSlotsAvailableSentTime, autoBookingDeadline) =>
            id.toString should not be empty
            externalSystemId shouldEqual 124
            clubId shouldEqual 8
            huntingEndTime shouldEqual endOfHuntDatetime
            notificationOnSlotsAvailableSentTime shouldBe None
            autoBookingDeadline shouldBe None
          }

          Get("/api/training-hunting-subscriptions") ~> addCredentials(validCredentials) ~> routes ~> check {
            status shouldEqual StatusCodes.OK
            responseAs[Seq[TrainingHuntingSubscription]] should not contain deletedThs
          }
        }
      }
    }

    "delete returns 404 if training hunting subscription is not found" in {
      val id = UUID.randomUUID().toString
      Delete(s"/api/training-hunting-subscriptions/$id") ~> addCredentials(validCredentials) ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

  }
}
