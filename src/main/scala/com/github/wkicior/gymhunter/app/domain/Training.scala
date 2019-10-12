package com.github.wkicior.gymhunter.app.domain

import java.util.{Date, TimeZone}

import akka.http.scaladsl.model.DateTime

case class Training(id: Long, slotsAvailable: Long, bookings_open_at: String, start_date: String)
