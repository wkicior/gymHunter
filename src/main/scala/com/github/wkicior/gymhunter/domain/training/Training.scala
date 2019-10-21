package com.github.wkicior.gymhunter.domain.training

import java.time.LocalDateTime

case class Training(id: Long, slotsAvailable: Int, bookings_open_at: String, start_date: String) {
  def canBeBooked(): Boolean = {
    this.slotsAvailable > 0 //&& bookings_open_at.compareTo(LocalDateTime.now()) > 1
  }
}
