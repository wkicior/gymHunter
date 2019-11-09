package com.github.wkicior.gymhunter.infrastructure.iftt

import java.time.{OffsetDateTime, ZoneOffset}

import com.github.wkicior.gymhunter.domain.notification.Notification
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId
import org.scalatest._

import scala.language.postfixOps

class IFTTNotificationSpec extends WordSpec with Matchers {

  "An IFTTNotification " should {
    "convert notification into IFTTNotification" in {
      val date = OffsetDateTime.of(2019, 10, 10, 7, 15, 0, 0, ZoneOffset.of("+02:00"))
      val notification = Notification(date, 8L, TrainingHuntingSubscriptionId())
      val ifttNotification = new IFTTNotification(notification)
      ifttNotification shouldBe IFTTNotification("10.10.2019, 07:15:00", "8")
    }
  }
}

