package com.github.wkicior.gymhunter.domain.training.tohunt

import java.time.OffsetDateTime
import java.util.UUID

import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHunt.{TrainingToHuntAdded, TrainingToHuntEvent}

import scala.collection.mutable.ListBuffer

case class CreateTrainingToHuntCommand(externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime)

class TrainingToHuntId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

object TrainingToHuntId {
  def apply(): TrainingToHuntId = new TrainingToHuntId(UUID.randomUUID())
  def apply(id: UUID): TrainingToHuntId = new TrainingToHuntId(id)
  def apply(id: String): TrainingToHuntId = new TrainingToHuntId(UUID.fromString(id))

}

object TrainingToHunt {
  sealed trait TrainingToHuntEvent {
    val id: TrainingToHuntId
  }
  final case class TrainingToHuntAdded(id: TrainingToHuntId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime) extends TrainingToHuntEvent
}

case class TrainingToHunt(id: TrainingToHuntId, externalSystemId: Long, clubId: Long) {
  var huntingEndTime: OffsetDateTime = OffsetDateTime.now()
  var active: Boolean = true

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

  def apply(trainingToHuntEvent: TrainingToHuntEvent): TrainingToHunt = {
    this
  }

  def pendingEventsList(): List[TrainingToHuntEvent] = pendingEvents.toList
}


