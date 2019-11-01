package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.wkicior.gymhunter.domain.training.{Training, TrainingResponse, TrainingToHunt, TrainingToHuntRequest}
import spray.json.{JsValue, _}

object JsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object OffsetDateTimeFormat extends RootJsonFormat[OffsetDateTime] {
    private val offsetDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ssx"
    def write(dateTime: OffsetDateTime) = JsString(dateTime.format(DateTimeFormatter.ofPattern(offsetDateTimeFormat)))
    def read(value: JsValue) = value match {
      case JsString(dateTime) => OffsetDateTime.parse(dateTime, DateTimeFormatter.ofPattern(offsetDateTimeFormat))
      case _ => deserializationError("OffsetDateTime expected.")
    }
  }

  implicit val trainingFormat = jsonFormat4(Training)
  implicit val trainingResponseFormat = jsonFormat1(TrainingResponse)
  implicit val trainingsToHuntFormat = jsonFormat4(TrainingToHunt)
  implicit val trainingToHuntRequestFormat = jsonFormat3(TrainingToHuntRequest)
}