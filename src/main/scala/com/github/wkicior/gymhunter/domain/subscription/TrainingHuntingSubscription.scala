package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime
import java.util.UUID

import akka.event.jul.Logger
import com.github.wkicior.gymhunter.domain.es.EventSourced
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionAggregate.{TrainingHuntingSubscriptionAdded, TrainingHuntingSubscriptionDeleted, TrainingHuntingSubscriptionEvent, TrainingHuntingSubscriptionNotificationSent}

import scala.collection.mutable.ListBuffer

class TrainingHuntingSubscriptionId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

final case class TrainingHuntingSubscriptionNotFound(id: TrainingHuntingSubscriptionId) extends RuntimeException(s"Training to hunt not found with id $id")

object TrainingHuntingSubscriptionId {
  def apply(): TrainingHuntingSubscriptionId = new TrainingHuntingSubscriptionId(UUID.randomUUID())
  def apply(id: UUID): TrainingHuntingSubscriptionId = new TrainingHuntingSubscriptionId(id)
  def apply(id: String): TrainingHuntingSubscriptionId = new TrainingHuntingSubscriptionId(UUID.fromString(id))

  type OptionalTrainingHuntingSubscription[+A] = Either[TrainingHuntingSubscriptionNotFound, A]
}

case class TrainingHuntingSubscription(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime, notificationOnSlotsAvailableSentDateTime: Option[OffsetDateTime] = None, autoBookingDeadline: Option[OffsetDateTime] = None) {
  def hasNotificationBeenSent: Boolean = this.notificationOnSlotsAvailableSentDateTime.isDefined
  def isContemporary: Boolean = this.huntingEndTime.isAfter(OffsetDateTime.now)
  def isActive: Boolean = !hasNotificationBeenSent && isContemporary
}

object TrainingHuntingSubscriptionAggregate {
  sealed trait TrainingHuntingSubscriptionEvent extends EventSourced {
    val id: TrainingHuntingSubscriptionId
  }
  final case class TrainingHuntingSubscriptionAdded(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime, autoBookingDeadline: Option[OffsetDateTime] = None) extends TrainingHuntingSubscriptionEvent
  final case class TrainingHuntingSubscriptionDeleted(id: TrainingHuntingSubscriptionId) extends TrainingHuntingSubscriptionEvent
  final case class TrainingHuntingSubscriptionNotificationSent(id: TrainingHuntingSubscriptionId) extends TrainingHuntingSubscriptionEvent
}

case class TrainingHuntingSubscriptionAggregate(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long) {
  val logger = Logger("name")
  var huntingEndTime: OffsetDateTime = OffsetDateTime.now()
  var notificationOnSlotsAvailableSentTime: OffsetDateTime = _
  var autoBookingDeadLine: Option[OffsetDateTime] = None

  private var pendingEvents = ListBuffer[TrainingHuntingSubscriptionEvent]()

  def this(id: TrainingHuntingSubscriptionId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime, autoBookingDeadline: Option[OffsetDateTime] = None) {
    this(id, externalSystemId, clubId)
    this.huntingEndTime = huntingEndTime
    this.autoBookingDeadLine = autoBookingDeadline
    pendingEvents += TrainingHuntingSubscriptionAdded(id, externalSystemId, clubId, huntingEndTime, autoBookingDeadline)
  }

  def this(trainingHuntingSubscriptionAddedEvent: TrainingHuntingSubscriptionAdded) {
    this(trainingHuntingSubscriptionAddedEvent.id, trainingHuntingSubscriptionAddedEvent.externalSystemId, trainingHuntingSubscriptionAddedEvent.clubId)
    this.huntingEndTime = trainingHuntingSubscriptionAddedEvent.huntingEndTime
    this.autoBookingDeadLine = trainingHuntingSubscriptionAddedEvent.autoBookingDeadline
  }

  private def applyPendingEvent(event: TrainingHuntingSubscriptionEvent): Unit = {
    apply(event)
    pendingEvents += event
  }

  def apply(trainingHuntingSubscriptionEvent: TrainingHuntingSubscriptionEvent): TrainingHuntingSubscriptionAggregate = {
    trainingHuntingSubscriptionEvent match {
      case deleted: TrainingHuntingSubscriptionDeleted => logger.info(s"TrainingHuntingSubscription deleted ${deleted.id}")
      case notificationSent: TrainingHuntingSubscriptionNotificationSent => this.notificationOnSlotsAvailableSentTime = notificationSent.createdDateTime
      case event => logger.warning(s"unrecognized event: $event")
    }
    this
  }

  def apply(): TrainingHuntingSubscription = {
    TrainingHuntingSubscription(id, externalSystemId, clubId, huntingEndTime, Option(notificationOnSlotsAvailableSentTime), autoBookingDeadLine)
  }

  def pendingEventsList(): List[TrainingHuntingSubscriptionEvent] = pendingEvents.toList

  def delete(): Unit = {
    applyPendingEvent(TrainingHuntingSubscriptionDeleted(id))
  }

  def notifyOnSlotsAvailable(): Unit = {
    applyPendingEvent(TrainingHuntingSubscriptionNotificationSent(id))
  }
}


