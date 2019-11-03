package com.github.wkicior.gymhunter.domain.training.tohunt

import java.time.OffsetDateTime
import java.util.UUID

import akka.event.jul.Logger
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntAggregate.{TrainingToHuntAdded, TrainingToHuntDeleted, TrainingToHuntEvent}

import scala.collection.mutable.ListBuffer

class TrainingToHuntId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

object TrainingToHuntId {
  def apply(): TrainingToHuntId = new TrainingToHuntId(UUID.randomUUID())
  def apply(id: UUID): TrainingToHuntId = new TrainingToHuntId(id)
  def apply(id: String): TrainingToHuntId = new TrainingToHuntId(UUID.fromString(id))
}

case class TrainingToHunt(id: TrainingToHuntId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime)

object TrainingToHuntAggregate {
  sealed trait TrainingToHuntEvent {
    val id: TrainingToHuntId
  }
  final case class TrainingToHuntAdded(id: TrainingToHuntId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime) extends TrainingToHuntEvent
  final case class TrainingToHuntDeleted(id: TrainingToHuntId) extends TrainingToHuntEvent
}

case class TrainingToHuntAggregate(id: TrainingToHuntId, externalSystemId: Long, clubId: Long) {

  val logger = Logger("name")
  var huntingEndTime: OffsetDateTime = OffsetDateTime.now()

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
}


