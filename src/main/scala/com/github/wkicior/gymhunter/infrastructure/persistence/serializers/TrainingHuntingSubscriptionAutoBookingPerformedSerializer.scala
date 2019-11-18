package com.github.wkicior.gymhunter.infrastructure.persistence.serializers

import java.io.NotSerializableException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.serialization.SerializerWithStringManifest
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscriptionAutoBookingPerformedEvent, TrainingHuntingSubscriptionId, TrainingHuntingSubscriptionNotificationSentEvent}
import com.github.wkicior.gymhunter.infrastructure.persistence.{TrainingHuntingSubscriptionAutoBookingPerformedOuterClass, TrainingHuntingSubscriptionNotificationSentOuterClass}


class TrainingHuntingSubscriptionAutoBookingPerformedSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 3333
  final val TrainingHuntingSubscriptionAutoBookingPerformedEventManifest = s"${classOf[TrainingHuntingSubscriptionAutoBookingPerformedEvent].getName}_v1"

  override def manifest(o: AnyRef): String = TrainingHuntingSubscriptionAutoBookingPerformedEventManifest

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: TrainingHuntingSubscriptionAutoBookingPerformedEvent => toProtobuf(e).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case TrainingHuntingSubscriptionAutoBookingPerformedEventManifest =>
        toEvent(TrainingHuntingSubscriptionAutoBookingPerformedOuterClass.TrainingHuntingSubscriptionAutoBookingPerformed.parseFrom(bytes))
      case _ =>
        throw new NotSerializableException("Unable to handle manifest: " + manifest)
    }

  private def toProtobuf(event: TrainingHuntingSubscriptionAutoBookingPerformedEvent) = {
    TrainingHuntingSubscriptionNotificationSentOuterClass.TrainingHuntingSubscriptionNotificationSent.newBuilder()
      .setId(event.eventId.toString)
      .setCreatedDateTime(event.createdDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      .setTrainingHuntingSubscriptionId(event.id.toString)
      .build()
  }

  def toEvent(proto: TrainingHuntingSubscriptionAutoBookingPerformedOuterClass.TrainingHuntingSubscriptionAutoBookingPerformed): TrainingHuntingSubscriptionAutoBookingPerformedEvent = {
    TrainingHuntingSubscriptionAutoBookingPerformedEvent(
      TrainingHuntingSubscriptionId(proto.getTrainingHuntingSubscriptionId),
      UUID.fromString(proto.getId),
      OffsetDateTime.parse(proto.getCreatedDateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    )
  }
}
