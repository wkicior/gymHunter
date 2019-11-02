package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

import java.util.UUID

case class TrainingToHuntRequest(externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime)

class TrainingToHuntId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

object TrainingToHuntId {
  def apply(): TrainingToHuntId = new TrainingToHuntId(UUID.randomUUID())
  def apply(id: UUID): TrainingToHuntId = new TrainingToHuntId(id)
  def apply(id: String): TrainingToHuntId = new TrainingToHuntId(UUID.fromString(id))
}

case class TrainingToHunt(id: TrainingToHuntId, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime, active: Boolean = true)


