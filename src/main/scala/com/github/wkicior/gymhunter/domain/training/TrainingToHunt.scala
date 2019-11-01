package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

case class TrainingToHuntRequest(externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime)

case class TrainingToHunt(id: String, externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime)
