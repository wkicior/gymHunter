package com.github.wkicior.gymhunter.infrastructure.persistence.serializers

import java.io.NotSerializableException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.serialization.SerializerWithStringManifest
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionNotificationSentEvent
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingHuntingSubscriptionNotificationSentOuterClass


class TrainingHuntingSubscriptionNotificationSentSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 3333
  final val TrainingHuntingSubscriptionNotificationSentEventManifest = s"${classOf[TrainingHuntingSubscriptionNotificationSentEvent].getName}_v1"

  override def manifest(o: AnyRef): String = TrainingHuntingSubscriptionNotificationSentEventManifest

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: TrainingHuntingSubscriptionNotificationSentEvent => toProtobuf(e).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case TrainingHuntingSubscriptionNotificationSentEventManifest =>
        toEvent(TrainingHuntingSubscriptionNotificationSentOuterClass.TrainingHuntingSubscriptionNotificationSent.parseFrom(bytes))
      case _ =>
        throw new NotSerializableException("Unable to handle manifest: " + manifest)
    }

  private def toProtobuf(event: TrainingHuntingSubscriptionNotificationSentEvent) = {
    TrainingHuntingSubscriptionNotificationSentOuterClass.TrainingHuntingSubscriptionNotificationSent.newBuilder()
      .setId(event.eventId.toString)
      .setCreatedDateTime(event.createdDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      .setTrainingHuntingSubscriptionId(event.id.toString)
      .build()
  }

  def toEvent(proto: TrainingHuntingSubscriptionNotificationSentOuterClass.TrainingHuntingSubscriptionNotificationSent): TrainingHuntingSubscriptionNotificationSentEvent = {
    TrainingHuntingSubscriptionNotificationSentEvent(
      TrainingHuntingSubscriptionId(proto.getTrainingHuntingSubscriptionId),
      UUID.fromString(proto.getId),
      OffsetDateTime.parse(proto.getCreatedDateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    )
  }
}
