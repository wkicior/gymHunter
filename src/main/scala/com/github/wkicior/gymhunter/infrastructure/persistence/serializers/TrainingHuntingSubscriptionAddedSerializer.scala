package com.github.wkicior.gymhunter.infrastructure.persistence.serializers
import java.io.NotSerializableException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.serialization.SerializerWithStringManifest
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionAddedEvent
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingHuntingSubscriptionAddedOuterClass


class TrainingHuntingSubscriptionAddedSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 1111
  final val TrainingHuntingSubscriptionAddedEventManifest = s"${classOf[TrainingHuntingSubscriptionAddedEvent].getName}_v1"

  override def manifest(o: AnyRef): String = TrainingHuntingSubscriptionAddedEventManifest

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case e: TrainingHuntingSubscriptionAddedEvent => toProtobuf(e).toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case TrainingHuntingSubscriptionAddedEventManifest =>
        toEvent(TrainingHuntingSubscriptionAddedOuterClass.TrainingHuntingSubscriptionAdded.parseFrom(bytes))
      case _ =>
        throw new NotSerializableException("Unable to handle manifest: " + manifest)
    }

  private def toProtobuf(event: TrainingHuntingSubscriptionAddedEvent) = {
    TrainingHuntingSubscriptionAddedOuterClass.TrainingHuntingSubscriptionAdded.newBuilder()
      .setId(event.eventId.toString)
      .setCreatedDateTime(event.createdDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      .setTrainingHuntingSubscriptionId(event.id.toString)
      .setExternalSystemId(event.externalSystemId)
      .setClubId(event.clubId)
      .setHuntingEndTime(event.huntingEndTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      .build()
  }

  def toEvent(proto: TrainingHuntingSubscriptionAddedOuterClass.TrainingHuntingSubscriptionAdded): TrainingHuntingSubscriptionAddedEvent = {
    TrainingHuntingSubscriptionAddedEvent(
      TrainingHuntingSubscriptionId(proto.getTrainingHuntingSubscriptionId),
      proto.getExternalSystemId,
      proto.getClubId,
      OffsetDateTime.parse(proto.getHuntingEndTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      UUID.fromString(proto.getId),
      OffsetDateTime.parse(proto.getCreatedDateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    )
  }
}

