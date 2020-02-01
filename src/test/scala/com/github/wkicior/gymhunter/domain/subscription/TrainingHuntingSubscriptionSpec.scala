package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime

import org.scalatest.{Matchers, WordSpec}

import scala.language.postfixOps


class TrainingHuntingSubscriptionSpec extends WordSpec with Matchers {

  "A TrainingHuntingSubscription" should {
    """is active if
      |huntingDeadline has not passed
      |and notification has not been sent yet
      |and auto booking has not been made
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().plusDays(1))
      ths.isActive shouldBe true
    }

    """is active if
      |huntingDeadline has not passed
      |and notification has not been sent yet
      |and auto booking has not been made
      |and huntingStartTime has passed
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().plusDays(1), huntingStartTime = Some(OffsetDateTime.now().minusDays(1)))
      ths.isActive shouldBe true
    }

    """is not active if
      |huntingStartTime has not passed
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().plusDays(1), huntingStartTime = Some(OffsetDateTime.now().plusDays(1)))
      ths.isActive shouldBe false
    }

    """is not active if
      |and notification has been sent yet
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().plusDays(1), Some(OffsetDateTime.now))
      ths.isActive shouldBe false
    }

    """is not active if
      |huntingDeadline has passed
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().minusDays(1))
      ths.isActive shouldBe false
    }

    """is not active if
      |auto booking has been made
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().plusDays(1), None, Some(OffsetDateTime.now.plusDays(1)), Some(OffsetDateTime.now))
      ths.isActive shouldBe false
    }

    """can be auto booked if
      |huntingDeadline has not passed
      |autoBookingDeadline has not passed
      |autoBooking has not been performed yet
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().plusDays(1), None, Some(OffsetDateTime.now.plusDays(1)))
      ths.canBeAutoBooked shouldBe true
    }

    """can not be auto booked if
      |huntingDeadline has passed
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().minusDays(1))
      ths.canBeAutoBooked shouldBe false
    }

    """can be auto booked if
      |autoBookingDeadline has passed
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().plusDays(1), None, Some(OffsetDateTime.now.minusDays(1)))
      ths.canBeAutoBooked shouldBe false
    }

    """can be auto booked if
      |autoBooking has been performed yet
    """.stripMargin in {
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 0, 0, OffsetDateTime.now().plusDays(1), None, Some(OffsetDateTime.now.plusDays(1)), Some(OffsetDateTime.now))
      ths.canBeAutoBooked shouldBe false
    }
  }
}