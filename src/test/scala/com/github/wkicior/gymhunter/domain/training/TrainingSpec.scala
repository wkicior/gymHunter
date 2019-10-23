package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

import org.scalatest.{Matchers, WordSpec}
import scala.language.postfixOps


class TrainingSpec extends WordSpec with Matchers {

  "A Training" should {
    "not to be booked if there are no slots available" in {
      val training = Training(0, 0, OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(2))
      training.canBeBooked() shouldBe false
    }

    """be booked if there are slots available
      |and bookings are opened from now
      |and start date has not passed yet
    """.stripMargin in {
      val training = Training(0, 1, OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(2))
      training.canBeBooked() shouldBe true
    }

    """not to be booked if bookings are not opened yet
      |even though there are slots available
      |and start date has not passed yet
    """.stripMargin in {
      val training = Training(0, 16, OffsetDateTime.now().plusHours(1), OffsetDateTime.now().plusMinutes(2))
      training.canBeBooked() shouldBe false
    }

    """not to be booked if start date time has passed
      |even though there are slots available
      |and date for bookings opened has passed
    """.stripMargin in {
      val training = Training(0, 16, OffsetDateTime.now(), OffsetDateTime.now().minusSeconds(1))
      training.canBeBooked() shouldBe false
    }
  }
}