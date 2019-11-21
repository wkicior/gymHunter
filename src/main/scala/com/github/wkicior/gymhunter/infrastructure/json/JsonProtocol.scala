package com.github.wkicior.gymhunter.infrastructure.json

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.CreateTrainingHuntingSubscriptionCommand
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionId}
import com.github.wkicior.gymhunter.domain.training.Training
import com.github.wkicior.gymhunter.infrastructure.gymsteer.auth.GymsteerLoginRequest
import com.github.wkicior.gymhunter.infrastructure.gymsteer.training.TrainingResponse
import com.github.wkicior.gymhunter.infrastructure.iftt.{IFTTAutoBookingNotification, IFTTSlotsAvailableNotification}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, NullOptions, RootJsonFormat, deserializationError}


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
  implicit val trainingHuntingSubscriptionFormat: RootJsonFormat[TrainingHuntingSubscription] = jsonFormat7(TrainingHuntingSubscription)
  implicit val trainingHuntingSubscriptionRequestFormat: RootJsonFormat[CreateTrainingHuntingSubscriptionCommand] = jsonFormat4(CreateTrainingHuntingSubscriptionCommand)
  implicit val ifttSlotsAvailableNotificationFormat: RootJsonFormat[IFTTSlotsAvailableNotification] = jsonFormat2(IFTTSlotsAvailableNotification.apply)
  implicit val ifttAutoBookingNotificationFormat: RootJsonFormat[IFTTAutoBookingNotification] = jsonFormat2(IFTTAutoBookingNotification.apply)
  implicit val gymsteerLoginRequestFormat: RootJsonFormat[GymsteerLoginRequest] = jsonFormat2(GymsteerLoginRequest)
}
