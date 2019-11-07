package com.github.wkicior.gymhunter.domain.notification

import java.time.OffsetDateTime


final case class NotificationFailedException() extends RuntimeException(s"Notification failed")

case class Notification(trainingDate: OffsetDateTime, clubId: Long)
