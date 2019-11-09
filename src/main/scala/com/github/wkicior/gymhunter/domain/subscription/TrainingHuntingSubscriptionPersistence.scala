package com.github.wkicior.gymhunter.domain.subscription

import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionAggregate.TrainingHuntingSubscriptionEvent

object TrainingHuntingSubscriptionPersistence {
  final case class GetAllTrainingHuntingSubscriptions()
  final case class GetTrainingHuntingSubscriptionAggregate(id: TrainingHuntingSubscriptionId)
  final case class GetTrainingHuntingSubscription(id: TrainingHuntingSubscriptionId)
  final case class StoreEvents(id: TrainingHuntingSubscriptionId, events: List[TrainingHuntingSubscriptionEvent])
}


