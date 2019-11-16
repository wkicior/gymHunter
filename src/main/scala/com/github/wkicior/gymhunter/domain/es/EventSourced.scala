package com.github.wkicior.gymhunter.domain.es

import java.time.OffsetDateTime
import java.util.UUID

trait EventSourced {
  val eventId: UUID = UUID.randomUUID
  val createdDateTime: OffsetDateTime = OffsetDateTime.now()
}
