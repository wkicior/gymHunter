package com.github.wkicior.gymhunter.domain.notification

import java.time.OffsetDateTime

import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId

case class AutoBookingNotification(trainingDate: OffsetDateTime, clubId: Long, trainingHuntingSubscriptionId: TrainingHuntingSubscriptionId)
