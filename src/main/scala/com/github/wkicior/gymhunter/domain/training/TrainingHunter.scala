package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.TrainingFetcher.GetTraining
import com.github.wkicior.gymhunter.domain.training.VacantTrainingManager.ProcessVacantTraining

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


object TrainingHunter {
  def props(): Props = Props(new TrainingHunter(TrainingToHuntProvider.props, TrainingFetcher.props, VacantTrainingManager.props))
  def props(trainingHunterProps: Props, trainingFetcherProps: Props, vacantTrainingManagerProps: Props): Props = Props(
    new TrainingHunter(trainingHunterProps, trainingFetcherProps, vacantTrainingManagerProps)
  )
  final case class Hunt()
}

class TrainingHunter(trainingToHuntProviderProps: Props, trainingFetcherProps: Props, vacantTrainingManagerProps: Props) extends Actor with ActorLogging {
  import TrainingHunter._
  import TrainingToHuntProvider._
  import TrainingToHuntRepository._
  implicit val ec = ExecutionContext.global

  val trainingToHuntProvider: ActorRef = context.actorOf(trainingToHuntProviderProps, "trainingToHuntProvider")
  val trainingFetcher: ActorRef = context.actorOf(RoundRobinPool(8).props(trainingFetcherProps), "trainingFetcher")
  val vacantTrainingManager: ActorRef = context.actorOf(RoundRobinPool(5).props(vacantTrainingManagerProps), "vacantTrainingManager")

  def receive = {
    case Hunt() =>
      log.info("hunting begins...")
      getTrackedTrainings()
        .map(trainingsToHunt => trainingsToHunt.trainings)
        .map(trainings => trainings.map(training => getTraining(training.externalSystemId)))
        .flatMap(trainingFutures => Future.sequence(trainingFutures))
        .foreach(trainings => {
          trainings
            .filter(training => training.canBeBooked())
            .foreach(training =>  vacantTrainingManager ! ProcessVacantTraining(training))
        })
    case _ =>
      log.error("unrecognized message")
  }

  private def getTrackedTrainings(): Future[TrainingsToHunt] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingsToHunt()).mapTo[TrainingsToHunt]
  }

  private def getTraining(id: Long): Future[Training] = {
    implicit val timeout: Timeout = Timeout(10 seconds)
    ask(trainingFetcher, GetTraining(id)).mapTo[Training]
  }
}