package com.github.wkicior.gymhunter.domain.subscription

import java.util.UUID

class TrainingHuntingSubscriptionId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

object TrainingHuntingSubscriptionId {
  def apply(): TrainingHuntingSubscriptionId = new TrainingHuntingSubscriptionId(UUID.randomUUID())
  def apply(id: UUID): TrainingHuntingSubscriptionId = new TrainingHuntingSubscriptionId(id)
  def apply(id: String): TrainingHuntingSubscriptionId = new TrainingHuntingSubscriptionId(UUID.fromString(id))
}