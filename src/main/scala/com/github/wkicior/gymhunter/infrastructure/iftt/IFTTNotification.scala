package com.github.wkicior.gymhunter.infrastructure.iftt

import java.time.format.{DateTimeFormatter, FormatStyle}

import com.github.wkicior.gymhunter.domain.notification.Notification

case class IFTTNotification(value1: String, value2: String) {
  def this(notification: Notification) = {
    this(notification.trainingDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)), notification.clubId.toString)
  }
}
