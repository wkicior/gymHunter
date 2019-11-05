package com.github.wkicior.gymhunter.domain.tohunt

import java.time.OffsetDateTime
import java.util.UUID

import akka.event.jul.Logger
import com.github.wkicior.gymhunter.app.es.EventSourced
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntAggregate.{TrainingToHuntAdded, TrainingToHuntDeleted, TrainingToHuntEvent, TrainingToHuntNotificationSent}

import scala.collection.mutable.ListBuffer

class TrainingToHuntId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

final case class TrainingToHuntNotFound(id: TrainingToHuntId) extends RuntimeException(s"Training to hunt not found with id $id")

object TrainingToHuntId {
  def apply(): TrainingToHuntId = new TrainingToHuntId(UUID.randomUUID())
  def apply(id: UUID): TrainingToHuntId = new TrainingToHuntId(id)
  def apply(id: String): TrainingToHuntId = new TrainingToHuntId(UUID.fromString(id))

  type OptionalTrainingToHunt[+A] = Either[TrainingToHuntNotFound, A]
}

case class TrainingToHunt(id: TrainingToHuntId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime, notificationOnSlotsAvailableSentTime: Option[OffsetDateTime] = None)

object TrainingToHuntAggregate {
  sealed trait TrainingToHuntEvent extends EventSourced {
    val id: TrainingToHuntId
    override def createdDateTime: OffsetDateTime = {
      super.createdDateTime
    }
  }
  final case class TrainingToHuntAdded(id: TrainingToHuntId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime) extends TrainingToHuntEvent
  final case class TrainingToHuntDeleted(id: TrainingToHuntId) extends TrainingToHuntEvent
  final case class TrainingToHuntNotificationSent(id: TrainingToHuntId) extends TrainingToHuntEvent {

  }
}

case class TrainingToHuntAggregate(id: TrainingToHuntId, externalSystemId: Long, clubId: Long) {
  val logger = Logger("name")
  var huntingEndTime: OffsetDateTime = OffsetDateTime.now()
  var notificationOnSlotsAvailableSentTime: OffsetDateTime = _

  private var pendingEvents = ListBuffer[TrainingToHuntEvent]()

  def this(id: TrainingToHuntId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime) {
    this(id, externalSystemId, clubId)
    this.huntingEndTime = huntingEndTime
    pendingEvents += TrainingToHuntAdded(id, externalSystemId, clubId, huntingEndTime)
  }

  def this(trainingToHuntAddedEvent: TrainingToHuntAdded) {
    this(trainingToHuntAddedEvent.id, trainingToHuntAddedEvent.externalSystemId, trainingToHuntAddedEvent.clubId)
    this.huntingEndTime = trainingToHuntAddedEvent.huntingEndTime
  }

  private def applyPendingEvent(event: TrainingToHuntEvent): Unit = {
    apply(event)
    pendingEvents += event
  }

  def apply(trainingToHuntEvent: TrainingToHuntEvent): TrainingToHuntAggregate = {
    trainingToHuntEvent match {
      case deleted: TrainingToHuntDeleted => logger.info(s"TrainingToHunt deleted ${deleted.id}")
      case notificationSent: TrainingToHuntNotificationSent => this.notificationOnSlotsAvailableSentTime = notificationSent.createdDateTime
      case event => logger.warning(s"unrecognized event: $event")
    }
    this
  }

  def apply(): TrainingToHunt = {
    TrainingToHunt(id, externalSystemId, clubId, huntingEndTime)
  }

  def pendingEventsList(): List[TrainingToHuntEvent] = pendingEvents.toList

  def delete(): Unit = {
    applyPendingEvent(TrainingToHuntDeleted(id))
  }

  def notifyOnSlotsAvailable(): Unit = {
    applyPendingEvent(TrainingToHuntNotificationSent(id))
  }
}


