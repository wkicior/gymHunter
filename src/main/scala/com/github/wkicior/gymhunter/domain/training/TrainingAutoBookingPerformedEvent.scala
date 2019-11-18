package com.github.wkicior.gymhunter.domain.training

import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId

case class TrainingAutoBookingPerformedEvent(trainingId: Long, trainingHuntingSubscriptionId: TrainingHuntingSubscriptionId)
