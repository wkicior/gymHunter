package com.github.wkicior.gymhunter.domain.training

import java.time.{LocalDateTime, OffsetDateTime}

import com.github.wkicior.gymhunter.app.JsonProtocol

case class Training(id: Long, slotsAvailable: Int, bookings_open_at: OffsetDateTime, start_date: OffsetDateTime) {
  def canBeBooked(): Boolean = {
    this.slotsAvailable > 0 //&& bookings_open_at.compareTo(LocalDateTime.now()) > 1
  }
}
