package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.wkicior.gymhunter.domain.training._
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntCommandHandler.CreateTrainingToHuntCommand
import com.github.wkicior.gymhunter.domain.training.tohunt.{TrainingToHunt, TrainingToHuntId}
import spray.json.{JsString, JsValue, _}

object JsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object OffsetDateTimeFormat extends RootJsonFormat[OffsetDateTime] {
    private val offsetDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ssx"
    def write(dateTime: OffsetDateTime) = JsString(dateTime.format(DateTimeFormatter.ofPattern(offsetDateTimeFormat)))
    def read(value: JsValue): OffsetDateTime = value match {
      case JsString(dateTime) => OffsetDateTime.parse(dateTime, DateTimeFormatter.ofPattern(offsetDateTimeFormat))
      case _ => deserializationError("OffsetDateTime expected.")
    }
  }

  implicit object TrainingToHuntIdFormat extends RootJsonFormat[TrainingToHuntId] {
    def write(trainingToHuntId: TrainingToHuntId) = JsString(trainingToHuntId.toString)
    def read(value: JsValue): TrainingToHuntId = value match {
      case JsString(trainingToHuntId) => TrainingToHuntId(trainingToHuntId)
      case _ => deserializationError("TrainingToHuntId expected.")
    }
  }

  implicit val trainingFormat: RootJsonFormat[Training] = jsonFormat4(Training)
  implicit val trainingResponseFormat: RootJsonFormat[TrainingResponse] = jsonFormat1(TrainingResponse)
  implicit val trainingToHuntFormat: RootJsonFormat[TrainingToHunt] = jsonFormat4(TrainingToHunt)
  implicit val trainingToHuntRequestFormat: RootJsonFormat[CreateTrainingToHuntCommand] = jsonFormat3(CreateTrainingToHuntCommand)
}