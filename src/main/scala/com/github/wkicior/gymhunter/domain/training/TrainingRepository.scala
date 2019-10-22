package com.github.wkicior.gymhunter.domain.training


import akka.actor.{Actor, ActorLogging, Props}

import scala.language.postfixOps


object TrainingRepository {
  def props: Props = Props[TrainingRepository]
  final case class GetTrackedTrainings()
  final case class TrackedTrainingIds(ids: Set[Long])
  final case class AddTrackedTraining(id: Long)

}

class TrainingRepository extends Actor with ActorLogging {
  import TrainingRepository._

  private var trainingIds = Set(550633L, 550656, 699157, 699176, 699158, 550634, 550635, 550667)

  def receive = {
    case GetTrackedTrainings() =>
      //TODO: use source? val trainingIdsSource: Source[Long, NotUsed] = Source(List(550633, 550634))
      sender() ! TrackedTrainingIds(trainingIds)
    case AddTrackedTraining(id) =>
      this.trainingIds += id
    case _ =>
      log.error("Unrecognized message")
  }
}