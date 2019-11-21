package com.github.wkicior.gymhunter.infrastructure.iftt

import java.time.format.{DateTimeFormatter, FormatStyle}

import com.github.wkicior.gymhunter.domain.notification.AutoBookingNotification


object IFTTAutoBookingNotification {
  val name = "gymHunterAutoBooking"
}

case class IFTTAutoBookingNotification(value1: String, value2: String) {
  def this(notification: AutoBookingNotification) = {
    this(notification.trainingDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)), notification.clubId.toString)
  }
}
