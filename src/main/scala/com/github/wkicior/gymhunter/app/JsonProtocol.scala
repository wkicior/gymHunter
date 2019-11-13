package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.wkicior.gymhunter.domain.training._
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.CreateTrainingHuntingSubscriptionCommand
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionId}
import com.github.wkicior.gymhunter.infrastructure.gymsteer.TrainingResponse
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotification
import spray.json.{JsString, JsValue, _}

object JsonProtocol extends DefaultJsonProtocol with SprayJsonSupport with NullOptions {

  implicit object OffsetDateTimeFormat extends RootJsonFormat[OffsetDateTime] {
    private val offsetDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ssx"
    def write(dateTime: OffsetDateTime) = JsString(dateTime.format(DateTimeFormatter.ofPattern(offsetDateTimeFormat)))
    def read(value: JsValue): OffsetDateTime = value match {
      case JsString(dateTime) => OffsetDateTime.parse(dateTime, DateTimeFormatter.ofPattern(offsetDateTimeFormat))
      case _ => deserializationError("OffsetDateTime expected.")
    }
  }

  implicit object TrainingHuntingSubscriptionIdFormat extends RootJsonFormat[TrainingHuntingSubscriptionId] {
    def write(trainingHuntingSubscriptionId: TrainingHuntingSubscriptionId) = JsString(trainingHuntingSubscriptionId.toString)
    def read(value: JsValue): TrainingHuntingSubscriptionId = value match {
      case JsString(id) => TrainingHuntingSubscriptionId(id)
      case _ => deserializationError("TrainingHuntingSubscriptionId expected.")
    }
  }


  implicit val trainingFormat: RootJsonFormat[Training] = jsonFormat4(Training)
  implicit val trainingResponseFormat: RootJsonFormat[TrainingResponse] = jsonFormat1(TrainingResponse)
  implicit val trainingHuntingSubscriptionFormat: RootJsonFormat[TrainingHuntingSubscription] = jsonFormat6(TrainingHuntingSubscription)
  implicit val trainingHuntingSubscriptionRequestFormat: RootJsonFormat[CreateTrainingHuntingSubscriptionCommand] = jsonFormat4(CreateTrainingHuntingSubscriptionCommand)
  implicit val ifttNotificationFormat: RootJsonFormat[IFTTNotification] = jsonFormat2(IFTTNotification)
}