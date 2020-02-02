package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime

import akka.event.jul.Logger
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionErrors.TrainingHuntingSubscriptionNotFound

import scala.collection.mutable.ListBuffer

object OptionalTrainingHuntingSubscription {
  type OptionalTrainingHuntingSubscription[+A] = Either[TrainingHuntingSubscriptionNotFound, A]
}

case class TrainingHuntingSubscription(id: TrainingHuntingSubscriptionId,
                                       externalSystemId: Long,
                                       clubId: Long,
                                       huntingDeadline: OffsetDateTime,
                                       notificationOnSlotsAvailableSentDateTime: Option[OffsetDateTime] = None,
                                       autoBookingDeadline: Option[OffsetDateTime] = None,
                                       autoBookingDateTime: Option[OffsetDateTime] = None,
                                       huntingStartTime: Option[OffsetDateTime] = None) {
  def hasNotificationBeenSent: Boolean = this.notificationOnSlotsAvailableSentDateTime.isDefined
  def isContemporary: Boolean = this.huntingDeadline.isAfter(OffsetDateTime.now) && this.huntingStartTime.forall(s => s.compareTo(OffsetDateTime.now) <= 0)
  def isActive: Boolean = !hasNotificationBeenSent && isContemporary && autoBookingDateTime.isEmpty
  def canBeAutoBooked: Boolean = isContemporary && autoBookingDeadline.exists(d => d.isAfter(OffsetDateTime.now)) && autoBookingDateTime.isEmpty
}

case class TrainingHuntingSubscriptionAggregate(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long) {
  val logger = Logger("name")
  var huntingDeadline: OffsetDateTime = OffsetDateTime.now()
  var notificationOnSlotsAvailableSentTime: Option[OffsetDateTime] = None
  var autoBookingDeadline: Option[OffsetDateTime] = None
  var autoBookingDateTime: Option[OffsetDateTime] = None
  var huntingStartTime: Option[OffsetDateTime] = None

  private var pendingEvents = ListBuffer[TrainingHuntingSubscriptionEvent]()

  def this(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long,
           huntingDeadline: OffsetDateTime, autoBookingDeadline: Option[OffsetDateTime] = None, huntingStartTime: Option[OffsetDateTime] = None) {
    this(id, externalSystemId, clubId)
    this.huntingDeadline = huntingDeadline
    this.autoBookingDeadline = autoBookingDeadline
    this.notificationOnSlotsAvailableSentTime = None
    this.autoBookingDateTime = None
    this.huntingStartTime = huntingStartTime
    pendingEvents += TrainingHuntingSubscriptionAddedEvent(id, externalSystemId, clubId, huntingDeadline, autoBookingDeadline, huntingStartTime)
  }

  def this(trainingHuntingSubscriptionAddedEvent: TrainingHuntingSubscriptionAddedEvent) {
    this(trainingHuntingSubscriptionAddedEvent.id, trainingHuntingSubscriptionAddedEvent.externalSystemId, trainingHuntingSubscriptionAddedEvent.clubId)
    this.huntingDeadline = trainingHuntingSubscriptionAddedEvent.huntingDeadline
    this.autoBookingDeadline = trainingHuntingSubscriptionAddedEvent.autoBookingDeadline
    this.huntingStartTime = trainingHuntingSubscriptionAddedEvent.huntingStartTime
  }

  private def applyPendingEvent(event: TrainingHuntingSubscriptionEvent): Unit = {
    apply(event)
    pendingEvents += event
  }

  def apply(trainingHuntingSubscriptionEvent: TrainingHuntingSubscriptionEvent): TrainingHuntingSubscriptionAggregate = {
    trainingHuntingSubscriptionEvent match {
      case deleted: TrainingHuntingSubscriptionDeletedEvent => logger.info(s"TrainingHuntingSubscription deleted ${deleted.id}")
      case notificationSent: TrainingHuntingSubscriptionNotificationSentEvent => this.notificationOnSlotsAvailableSentTime = Some(notificationSent.createdDateTime)
      case autoBooking: TrainingHuntingSubscriptionAutoBookingPerformedEvent => this.autoBookingDateTime = Some(autoBooking.createdDateTime)
      case event => logger.warning(s"unrecognized event: $event")
    }
    this
  }

  def apply(): TrainingHuntingSubscription = {
    TrainingHuntingSubscription(id, externalSystemId, clubId, huntingDeadline, notificationOnSlotsAvailableSentTime, autoBookingDeadline, autoBookingDateTime, huntingStartTime)
  }

  def pendingEventsList(): List[TrainingHuntingSubscriptionEvent] = pendingEvents.toList

  def delete(): Unit = {
    applyPendingEvent(TrainingHuntingSubscriptionDeletedEvent(id))
  }

  def notifyOnSlotsAvailable(): Unit = {
    applyPendingEvent(TrainingHuntingSubscriptionNotificationSentEvent(id))
  }

  def autoBookingPerformed(): Unit = {
    applyPendingEvent(TrainingHuntingSubscriptionAutoBookingPerformedEvent(id))
  }
}


