package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime
import java.util.UUID

import com.github.wkicior.gymhunter.domain.es.EventSourced


trait TrainingHuntingSubscriptionEvent extends EventSourced {
  val id: TrainingHuntingSubscriptionId
}

final case class TrainingHuntingSubscriptionAddedEvent(id: TrainingHuntingSubscriptionId,
                                                       externalSystemId: Long,
                                                       clubId: Long,
                                                       huntingEndTime: OffsetDateTime,
                                                       override val eventId: UUID = UUID.randomUUID,
                                                       override val createdDateTime: OffsetDateTime = OffsetDateTime.now
                                                      ) extends TrainingHuntingSubscriptionEvent

final case class TrainingHuntingSubscriptionDeletedEvent(id: TrainingHuntingSubscriptionId,
                                                         override val eventId: UUID = UUID.randomUUID,
                                                         override val createdDateTime: OffsetDateTime = OffsetDateTime.now
                                                        ) extends TrainingHuntingSubscriptionEvent

final case class TrainingHuntingSubscriptionNotificationSentEvent(id: TrainingHuntingSubscriptionId,
                                                                  override val eventId: UUID = UUID.randomUUID,
                                                                  override val createdDateTime: OffsetDateTime = OffsetDateTime.now
                                                                 ) extends TrainingHuntingSubscriptionEvent