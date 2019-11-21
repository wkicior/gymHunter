package com.github.wkicior.gymhunter.infrastructure.iftt

import java.time.format.{DateTimeFormatter, FormatStyle}

import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotification

object IFTTNotificationName {
  val autoBookingNotificationName = "gymHunterAutoBooking"
}

object IFTTSlotsAvailableNotification {
  val name = "gymhunter"
}

case class IFTTSlotsAvailableNotification(value1: String, value2: String) {
  def this(notification: SlotsAvailableNotification) = {
    this(notification.trainingDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)), notification.clubId.toString)
  }
}
