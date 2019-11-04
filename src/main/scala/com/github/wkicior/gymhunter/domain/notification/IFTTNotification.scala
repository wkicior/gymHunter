package com.github.wkicior.gymhunter.domain.notification

import java.time.format.{DateTimeFormatter, FormatStyle}


case class IFTTNotification(value1: String, value2: String) {
  def this(notification: Notification) = {
    this(notification.trainingDate.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)), notification.clubId.toString)
  }
}
