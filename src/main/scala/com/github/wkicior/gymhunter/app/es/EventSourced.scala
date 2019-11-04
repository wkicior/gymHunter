package com.github.wkicior.gymhunter.app.es

import java.time.OffsetDateTime
import java.util.UUID

trait EventSourced {
  def eventId: UUID = UUID.randomUUID()
  def createdDateTime: OffsetDateTime = OffsetDateTime.now()
}
