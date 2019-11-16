package com.github.wkicior.gymhunter.infrastructure.persistence.serializers

import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionEvent
import com.google.protobuf.GeneratedMessageV3

trait TrainingHuntingSubscriptionProtobufEvent {
  def proto: GeneratedMessageV3
  def event: TrainingHuntingSubscriptionEvent
}
