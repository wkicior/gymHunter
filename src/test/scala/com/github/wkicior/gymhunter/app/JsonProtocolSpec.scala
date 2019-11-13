package com.github.wkicior.gymhunter.app

import java.time.{OffsetDateTime, ZoneOffset}

import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionId}
import com.github.wkicior.gymhunter.domain.training.Training
import org.scalatest._
import spray.json.{JsNull, JsNumber, JsObject, JsString, JsonFormat}

import scala.language.postfixOps

class JsonProtocolSpec extends WordSpec with Matchers {

  import JsonProtocol._

  "A JsonProtocol " should {
    "write OffsetDateTime value to JSON" in {
      val date = OffsetDateTime.of(2019, 10, 10, 7, 15, 0, 0, ZoneOffset.of("+02:00"))
      val dateJson = JsString("2019-10-10T07:15:00+02")
      val jf = implicitly[JsonFormat[OffsetDateTime]]
      jf.write(date) shouldBe dateJson
    }

    "read OffsetDateTime value from JSON" in {
      val date = OffsetDateTime.of(2019, 10, 10, 7, 15, 0, 0, ZoneOffset.of("+02:00"))
      val dateJson = JsString("2019-10-10T07:15:00+0200")
      val jf = implicitly[JsonFormat[OffsetDateTime]]
      jf.read(dateJson) shouldBe date
    }

    "write TrainingToHuntId value to JSON" in {
      val trainingToHuntId = TrainingHuntingSubscriptionId()
      val trainingToHuntJson = JsString(trainingToHuntId.toString)
      val jf = implicitly[JsonFormat[TrainingHuntingSubscriptionId]]
      jf.write(trainingToHuntId) shouldBe trainingToHuntJson
    }

    "write TrainingToHunt value to JSON without notificationDateTime nor autoBookingDeadline" in {
      val trainingToHunt = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now(), None, None)
      val trainingToHuntJson = JsObject(
        "id" -> JsString(trainingToHunt.id.toString),
        "externalSystemId" -> JsNumber(trainingToHunt.externalSystemId),
        "clubId" -> JsNumber(trainingToHunt.clubId),
        "huntingEndTime" -> OffsetDateTimeFormat.write(trainingToHunt.huntingEndTime),
        "notificationOnSlotsAvailableSentDateTime" -> JsNull,
        "autoBookingDeadline" -> JsNull
      )
      val jf = implicitly[JsonFormat[TrainingHuntingSubscription]]
      jf.write(trainingToHunt) shouldBe trainingToHuntJson
    }

    "write TrainingHuntingSubscription value to JSON with notificationDateTime and autoBookingDeadline" in {
      val trainingToHunt = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now, Some(OffsetDateTime.now), Some(OffsetDateTime.now))
      val trainingToHuntJson = JsObject(
        "id" -> JsString(trainingToHunt.id.toString),
        "externalSystemId" -> JsNumber(trainingToHunt.externalSystemId),
        "clubId" -> JsNumber(trainingToHunt.clubId),
        "huntingEndTime" -> OffsetDateTimeFormat.write(trainingToHunt.huntingEndTime),
        "notificationOnSlotsAvailableSentDateTime" -> OffsetDateTimeFormat.write(trainingToHunt.notificationOnSlotsAvailableSentDateTime.get),
        "autoBookingDeadline" -> OffsetDateTimeFormat.write(trainingToHunt.autoBookingDeadline.get)

      )
      val jf = implicitly[JsonFormat[TrainingHuntingSubscription]]
      jf.write(trainingToHunt) shouldBe trainingToHuntJson
    }

    "read full Training value from JSON" in {
      val date = OffsetDateTime.of(2019, 10, 10, 7, 15, 0, 0, ZoneOffset.of("+02:00"))

      val training = Training(1, 2, Some(date), date)
      val trainingJson = JsObject(
        "id" -> JsNumber(1),
        "slotsAvailable" -> JsNumber(2),
        "bookings_open_at" -> JsString("2019-10-10T07:15:00+0200"),
        "start_date" -> JsString("2019-10-10T07:15:00+0200")
      )
      val jf = implicitly[JsonFormat[Training]]
      jf.read(trainingJson) shouldBe training
    }
  }

  "read Training value from JSON without bookings_open_at" in {
    val date = OffsetDateTime.of(2019, 10, 10, 7, 15, 0, 0, ZoneOffset.of("+02:00"))
    val training = Training(1, 2, Option.empty, date)
    val trainingJson = JsObject(
      "id" -> JsNumber(1),
      "slotsAvailable" -> JsNumber(2),
      "start_date" -> JsString("2019-10-10T07:15:00+0200")
    )
    val jf = implicitly[JsonFormat[Training]]
    jf.read(trainingJson) shouldBe training
  }
}
