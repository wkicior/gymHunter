package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.ask
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntCommandHandler.NotifyOnSlotsAvailable
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntProvider.GetTrainingsToHuntByTrainingIdQuery
import com.github.wkicior.gymhunter.domain.training.tohunt.{TrainingToHunt, TrainingToHuntCommandHandler, TrainingToHuntProvider}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

object VacantTrainingManager {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new VacantTrainingManager(TrainingToHuntProvider.props(trainingToHuntEventStore), TrainingToHuntCommandHandler.props(trainingToHuntEventStore)))
  def props(trainingToHuntProviderProps: Props, trainingToHuntCommandHandlerProps: Props): Props = Props(new VacantTrainingManager(trainingToHuntProviderProps, trainingToHuntCommandHandlerProps)
  )
  final case class ProcessVacantTraining(training: Training)
}

class VacantTrainingManager(trainingToHuntProviderProps: Props, trainingToHuntCommandHandlerProps: Props) extends Actor with ActorLogging {
  import VacantTrainingManager._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val trainingToHuntProvider: ActorRef = context.actorOf(trainingToHuntProviderProps)
  val trainingToHuntCommandHandler: ActorRef = context.actorOf(trainingToHuntCommandHandlerProps)


  def receive: PartialFunction[Any, Unit] = {
    case ProcessVacantTraining(training: Training) =>
      log.info(s"slots available on $training")
      getTrainingsToHunt(training.id)
        .foreach(trainings =>
          trainings
            .foreach(t => trainingToHuntCommandHandler ! NotifyOnSlotsAvailable(t.id)))
  }

  private def getTrainingsToHunt(trainingId: Long): Future[Set[TrainingToHunt]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingsToHuntByTrainingIdQuery(trainingId)).mapTo[Set[TrainingToHunt]]
  }
}