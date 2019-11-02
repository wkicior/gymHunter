package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.wkicior.gymhunter.domain.training._
import com.github.wkicior.gymhunter.domain.training.tohunt.{CreateTrainingToHuntCommand, TrainingToHunt, TrainingToHuntId}
import spray.json.{JsString, JsValue, _}

object JsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  implicit object OffsetDateTimeFormat extends RootJsonFormat[OffsetDateTime] {
    private val offsetDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ssx"
    def write(dateTime: OffsetDateTime) = JsString(dateTime.format(DateTimeFormatter.ofPattern(offsetDateTimeFormat)))
    def read(value: JsValue) = value match {
      case JsString(dateTime) => OffsetDateTime.parse(dateTime, DateTimeFormatter.ofPattern(offsetDateTimeFormat))
      case _ => deserializationError("OffsetDateTime expected.")
    }
  }

  implicit object TrainingToHuntIdFormat extends RootJsonFormat[TrainingToHuntId] {
    def write(trainingToHuntId: TrainingToHuntId) = JsString(trainingToHuntId.toString)
    def read(value: JsValue) = value match {
      case JsString(trainingToHuntId) => TrainingToHuntId(trainingToHuntId)
      case _ => deserializationError("TrainingToHuntId expected.")
    }
  }


    implicit object ColorJsonFormat extends RootJsonFormat[TrainingToHunt] {
      def write(c: TrainingToHunt) = JsObject(
        "id" -> JsString(c.id.toString),
        "externalSystemId" -> JsNumber(c.externalSystemId),
        "clubId" -> JsNumber(c.clubId),
        "huntingEndTime" -> OffsetDateTimeFormat.write(c.huntingEndTime)
      )
      def read(value: JsValue) = {
        value.asJsObject.getFields("id", "externalSystemId", "clubId", "huntingEndTime") match {
          case Seq(JsString(id), JsNumber(externalSystemId), JsNumber(clubId), huntingEndTime) =>
            new TrainingToHunt(TrainingToHuntId(id), externalSystemId.longValue, clubId.longValue, OffsetDateTimeFormat.read(huntingEndTime))
          case _ => throw new DeserializationException("TrainingToHunt expected")
        }
      }
    }


  implicit val trainingFormat = jsonFormat4(Training)
  implicit val trainingResponseFormat = jsonFormat1(TrainingResponse)
  implicit val trainingToHuntRequestFormat = jsonFormat3(CreateTrainingToHuntCommand)
}