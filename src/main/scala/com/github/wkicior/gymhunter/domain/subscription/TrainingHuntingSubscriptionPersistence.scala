package com.github.wkicior.gymhunter.domain.subscription

object TrainingHuntingSubscriptionPersistence {
  final case class GetAllTrainingHuntingSubscriptions()
  final case class GetTrainingHuntingSubscriptionAggregate(id: TrainingHuntingSubscriptionId)
  final case class GetTrainingHuntingSubscription(id: TrainingHuntingSubscriptionId)
  final case class StoreEvents(id: TrainingHuntingSubscriptionId, events: List[TrainingHuntingSubscriptionEvent])
}


