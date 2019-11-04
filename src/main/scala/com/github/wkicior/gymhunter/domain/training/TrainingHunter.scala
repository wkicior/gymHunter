package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout

import com.github.wkicior.gymhunter.domain.training.VacantTrainingManager.ProcessVacantTraining
import com.github.wkicior.gymhunter.domain.training.tohunt.{TrainingToHunt, TrainingToHuntProvider}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps


object TrainingHunter {
  def props(trainingToHuntEventStore: ActorRef, trainingFetcher: ActorRef): Props = Props(new TrainingHunter(TrainingToHuntProvider.props(trainingToHuntEventStore), trainingFetcher, VacantTrainingManager.props(trainingToHuntEventStore)))
  def props(trainingHunterProps: Props, trainingFetcher: ActorRef, vacantTrainingManagerProps: Props): Props = Props(
    new TrainingHunter(trainingHunterProps, trainingFetcher, vacantTrainingManagerProps)
  )
  final case class Hunt()
}

class TrainingHunter(trainingToHuntProviderProps: Props, trainingFetcher: ActorRef, vacantTrainingManagerProps: Props) extends Actor with ActorLogging {
  import TrainingHunter._
  import TrainingToHuntProvider._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val trainingToHuntProvider: ActorRef = context.actorOf(trainingToHuntProviderProps, "trainingToHuntProvider")
  val vacantTrainingManager: ActorRef = context.actorOf(RoundRobinPool(5).props(vacantTrainingManagerProps), "vacantTrainingManager")

  def receive: PartialFunction[Any, Unit] = {
    case Hunt() =>
      log.info("hunting begins...")
      getTrainingsToHunt
        .map(trainings => trainings.map(training => getTraining(training.externalSystemId)))
        .flatMap(trainingFutures => Future.sequence(trainingFutures))
        .foreach(trainings => {
          trainings
            .filter(training => training.canBeBooked)
            .foreach(training =>  vacantTrainingManager ! ProcessVacantTraining(training))
        })
    case _ =>
      log.error("unrecognized message")
  }

  private def getTrainingsToHunt: Future[Set[TrainingToHunt]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingsToHuntQuery()).mapTo[Set[TrainingToHunt]]
  }

  private def getTraining(id: Long): Future[Training] = {
    implicit val timeout: Timeout = Timeout(10 seconds)
    ask(trainingFetcher, GetTraining(id)).mapTo[Training]
  }
}