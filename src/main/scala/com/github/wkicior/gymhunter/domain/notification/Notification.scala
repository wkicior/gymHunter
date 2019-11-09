package com.github.wkicior.gymhunter.domain.notification

import java.time.OffsetDateTime

import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId


final case class NotificationFailedException() extends RuntimeException(s"Notification failed")

case class Notification(trainingDate: OffsetDateTime, clubId: Long, trainingHuntingSubscriptionId: TrainingHuntingSubscriptionId)
