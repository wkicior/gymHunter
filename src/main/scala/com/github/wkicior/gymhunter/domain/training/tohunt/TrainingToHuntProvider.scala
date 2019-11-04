package com.github.wkicior.gymhunter.domain.training.tohunt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntEventStore.{GetAllTrainingsToHunt, GetTrainingToHunt, OptionalTrainingToHunt}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps


object TrainingToHuntProvider {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new TrainingToHuntProvider(trainingToHuntEventStore))
  final case class GetTrainingsToHuntQuery()
  final case class GetTrainingsToHuntByTrainingIdQuery(id: Long)
  final case class GetTrainingToHuntQuery(id: TrainingToHuntId)

}

class TrainingToHuntProvider(trainingToHuntEventStore: ActorRef) extends Actor with ActorLogging {
  import TrainingToHuntProvider._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def receive: PartialFunction[Any, Unit] = {

    case GetTrainingsToHuntQuery() =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingToHuntEventStore, GetAllTrainingsToHunt()).mapTo[Set[TrainingToHunt]]
        .pipeTo(sender())

    case GetTrainingsToHuntByTrainingIdQuery(id) =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingToHuntEventStore, GetAllTrainingsToHunt()).mapTo[Set[TrainingToHunt]]
        .map(trainingsToHunt => trainingsToHunt.filter(t => t.externalSystemId == id))
        .pipeTo(sender())

    case GetTrainingToHuntQuery(id) =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingToHuntEventStore, GetTrainingToHunt(id)).mapTo[OptionalTrainingToHunt[TrainingToHunt]]
        .pipeTo(sender())
      
    case x =>
      log.error(s"Unrecognized message: $x")
  }
}