package com.github.wkicior.gymhunter.domain.subscription

object TrainingHuntingSubscriptionErrors {
  final case class TrainingHuntingSubscriptionNotFound(id: TrainingHuntingSubscriptionId) extends RuntimeException(s"Training to hunt not found with id $id")
}
