package com.github.wkicior.gymhunter.app

import java.time.{OffsetDateTime, ZoneOffset}

import org.scalatest.{FunSuite, Matchers}
import spray.json.{JsString, JsonFormat}

class JsonProtocolSpecs extends FunSuite with Matchers {
  import JsonProtocol._
  test("OffsetDateTime writes to JSON") {
    val date = OffsetDateTime.of(2019, 10, 10, 7, 15, 0, 0, ZoneOffset.of("+02:00"))
    val dateJson = JsString("2019-10-10T07:15:00+02")
    val jf = implicitly[JsonFormat[OffsetDateTime]]
    jf.write(date) shouldBe dateJson
  }

  test("OffsetDateTime reads from JSON") {
    val date = OffsetDateTime.of(2019, 10, 10, 7, 15, 0, 0, ZoneOffset.of("+02:00"))
    val dateJson = JsString("2019-10-10T07:15:00+0200")
    val jf = implicitly[JsonFormat[OffsetDateTime]]
    jf.read(dateJson) shouldBe date
  }
}
