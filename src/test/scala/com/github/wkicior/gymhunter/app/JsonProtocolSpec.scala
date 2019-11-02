package com.github.wkicior.gymhunter.app

import java.time.{OffsetDateTime, ZoneOffset}

import com.github.wkicior.gymhunter.domain.training.tohunt.{TrainingToHunt, TrainingToHuntId}
import org.scalatest._
import spray.json.{JsBoolean, JsNumber, JsObject, JsString, JsonFormat}

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
      val trainingToHuntId = TrainingToHuntId()
      val trainingToHuntJson = JsString(trainingToHuntId.toString)
      val jf = implicitly[JsonFormat[TrainingToHuntId]]
      jf.write(trainingToHuntId) shouldBe trainingToHuntJson
    }

    "write TrainingToHunt value to JSON" in {
      val trainingToHunt = new TrainingToHunt(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())
      val trainingToHuntJson = JsObject(
        "id" -> JsString(trainingToHunt.id.toString),
        "externalSystemId" -> JsNumber(trainingToHunt.externalSystemId),
        "clubId" -> JsNumber(trainingToHunt.clubId),
        "huntingEndTime" -> OffsetDateTimeFormat.write(trainingToHunt.huntingEndTime)
      )
      val jf = implicitly[JsonFormat[TrainingToHunt]]
      jf.write(trainingToHunt) shouldBe trainingToHuntJson
    }
  }
}
