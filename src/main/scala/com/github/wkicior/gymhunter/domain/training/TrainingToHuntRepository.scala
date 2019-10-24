package com.github.wkicior.gymhunter.domain.training


import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}

import scala.language.postfixOps


object TrainingToHuntRepository {
  def props: Props = Props[TrainingToHuntRepository]
  final case class GetAllTrainingsToHunt()
  final case class TrainingsToHunt(trainings: Set[TrainingToHunt])
  final case class AddTrainingToHunt(training: TrainingToHunt)
}

class TrainingToHuntRepository extends Actor with ActorLogging {
  import TrainingToHuntRepository._

  private var trainings = Set(
    TrainingToHunt(UUID.randomUUID().toString, 550633L, 8, OffsetDateTime.now()),
    TrainingToHunt(UUID.randomUUID().toString, 550656L, 8, OffsetDateTime.now()),
    TrainingToHunt(UUID.randomUUID().toString, 699176L, 8, OffsetDateTime.now()),
    TrainingToHunt(UUID.randomUUID().toString, 699158L, 8, OffsetDateTime.now()),
    TrainingToHunt(UUID.randomUUID().toString, 550634L, 8, OffsetDateTime.now()),
    TrainingToHunt(UUID.randomUUID().toString, 550635L, 8, OffsetDateTime.now()),
    TrainingToHunt(UUID.randomUUID().toString, 550667L, 8, OffsetDateTime.now()))

  def receive = {
    case GetAllTrainingsToHunt() =>
      //TODO: use source? val trainingIdsSource: Source[Long, NotUsed] = Source(List(550633, 550634))
      sender() ! TrainingsToHunt(trainings)
    case AddTrainingToHunt(training) =>
      this.trainings += training
    case _ =>
      log.error("Unrecognized message")
  }
}