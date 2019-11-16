package com.github.wkicior.gymhunter.infrastructure.persistence.serializers

import java.io.NotSerializableException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.serialization.SerializerWithStringManifest
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionDeletedEvent
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingHuntingSubscriptionDeletedOuterClass

class TrainingHuntingSubscriptionDeletedSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 2222
  final val TrainingHuntingSubscriptionDeletedEventManifest = s"${classOf[TrainingHuntingSubscriptionDeletedEvent].getName}_v1"

  override def manifest(o: AnyRef): String = TrainingHuntingSubscriptionDeletedEventManifest

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: TrainingHuntingSubscriptionDeletedEvent => toProtobuf(e).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case TrainingHuntingSubscriptionDeletedEventManifest =>
        toEvent(TrainingHuntingSubscriptionDeletedOuterClass.TrainingHuntingSubscriptionDeleted.parseFrom(bytes))
      case _ =>
        throw new NotSerializableException("Unable to handle manifest: " + manifest)
    }

  private def toProtobuf(event: TrainingHuntingSubscriptionDeletedEvent) = {
    TrainingHuntingSubscriptionDeletedOuterClass.TrainingHuntingSubscriptionDeleted.newBuilder()
      .setId(event.eventId.toString)
      .setCreatedDateTime(event.createdDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      .setTrainingHuntingSubscriptionId(event.id.toString)
      .build()
  }

  def toEvent(proto: TrainingHuntingSubscriptionDeletedOuterClass.TrainingHuntingSubscriptionDeleted): TrainingHuntingSubscriptionDeletedEvent = {
    TrainingHuntingSubscriptionDeletedEvent(
      TrainingHuntingSubscriptionId(proto.getTrainingHuntingSubscriptionId),
      UUID.fromString(proto.getId),
      OffsetDateTime.parse(proto.getCreatedDateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    )
  }
}