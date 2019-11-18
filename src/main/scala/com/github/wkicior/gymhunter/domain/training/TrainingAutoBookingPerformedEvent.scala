package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId

case class TrainingAutoBookingPerformedEvent(trainingId: Long, clubId: Long, trainingHuntingSubscriptionId: TrainingHuntingSubscriptionId, trainingDateTime: OffsetDateTime)
