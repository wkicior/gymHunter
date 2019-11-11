package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

final case class GetTraining(id: Long)

case class Training(id: Long, slotsAvailable: Int, bookings_open_at: Option[OffsetDateTime], start_date: OffsetDateTime) {
  def canBeBooked: Boolean = {
    this.slotsAvailable > 0 &&
      (bookings_open_at.isEmpty || OffsetDateTime.now().compareTo(bookings_open_at.get) >= 0) &&
      OffsetDateTime.now().compareTo(start_date) <= 0
  }
}
