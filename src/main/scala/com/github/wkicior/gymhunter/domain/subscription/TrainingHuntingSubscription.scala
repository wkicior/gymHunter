package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime

import akka.event.jul.Logger
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionErrors.TrainingHuntingSubscriptionNotFound

import scala.collection.mutable.ListBuffer

object OptionalTrainingHuntingSubscription {
  type OptionalTrainingHuntingSubscription[+A] = Either[TrainingHuntingSubscriptionNotFound, A]
}

case class TrainingHuntingSubscription(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime, notificationOnSlotsAvailableSentDateTime: Option[OffsetDateTime] = None, autoBookingDeadline: Option[OffsetDateTime] = None) {
  def hasNotificationBeenSent: Boolean = this.notificationOnSlotsAvailableSentDateTime.isDefined
  def isContemporary: Boolean = this.huntingEndTime.isAfter(OffsetDateTime.now)
  def isActive: Boolean = !hasNotificationBeenSent && isContemporary
}

case class TrainingHuntingSubscriptionAggregate(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long) {
  val logger = Logger("name")
  var huntingEndTime: OffsetDateTime = OffsetDateTime.now()
  var notificationOnSlotsAvailableSentTime: Option[OffsetDateTime] = None
  var autoBookingDeadline: Option[OffsetDateTime] = None

  private var pendingEvents = ListBuffer[TrainingHuntingSubscriptionEvent]()

  def this(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime, autoBookingDeadline: Option[OffsetDateTime] = None) {
    this(id, externalSystemId, clubId)
    this.huntingEndTime = huntingEndTime
    this.autoBookingDeadline = autoBookingDeadline
    this.notificationOnSlotsAvailableSentTime = None
    pendingEvents += TrainingHuntingSubscriptionAddedEvent(id, externalSystemId, clubId, huntingEndTime, autoBookingDeadline)
  }

  def this(trainingHuntingSubscriptionAddedEvent: TrainingHuntingSubscriptionAddedEvent) {
    this(trainingHuntingSubscriptionAddedEvent.id, trainingHuntingSubscriptionAddedEvent.externalSystemId, trainingHuntingSubscriptionAddedEvent.clubId)
    this.huntingEndTime = trainingHuntingSubscriptionAddedEvent.huntingEndTime
    this.autoBookingDeadline = trainingHuntingSubscriptionAddedEvent.autoBookingDeadline
  }

  private def applyPendingEvent(event: TrainingHuntingSubscriptionEvent): Unit = {
    apply(event)
    pendingEvents += event
  }

  def apply(trainingHuntingSubscriptionEvent: TrainingHuntingSubscriptionEvent): TrainingHuntingSubscriptionAggregate = {
    trainingHuntingSubscriptionEvent match {
      case deleted: TrainingHuntingSubscriptionDeletedEvent => logger.info(s"TrainingHuntingSubscription deleted ${deleted.id}")
      case notificationSent: TrainingHuntingSubscriptionNotificationSentEvent => this.notificationOnSlotsAvailableSentTime = Some(notificationSent.createdDateTime)
      case event => logger.warning(s"unrecognized event: $event")
    }
    this
  }

  def apply(): TrainingHuntingSubscription = {
    TrainingHuntingSubscription(id, externalSystemId, clubId, huntingEndTime, notificationOnSlotsAvailableSentTime, autoBookingDeadline)
  }

  def pendingEventsList(): List[TrainingHuntingSubscriptionEvent] = pendingEvents.toList

  def delete(): Unit = {
    applyPendingEvent(TrainingHuntingSubscriptionDeletedEvent(id))
  }

  def notifyOnSlotsAvailable(): Unit = {
    applyPendingEvent(TrainingHuntingSubscriptionNotificationSentEvent(id))
  }
}


