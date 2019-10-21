package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.VacantTrainingManager.ProcessVacantTraining
import com.github.wkicior.gymhunter.domain.training.TrainingFetcher.GetTraining

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


object TrainingHunter {
  def props: Props = Props[TrainingHunter]
  final case class Hunt()
}

class TrainingHunter extends Actor with ActorLogging {
  import TrainingHunter._
  import TrainingTracker._
  implicit val ec = ExecutionContext.global;

  val trainingTracker: ActorRef = context.actorOf(TrainingTracker.props, "trainingTracker")
  val trainingFetcher: ActorRef = context.actorOf(RoundRobinPool(8).props(TrainingFetcher.props), "trainingFetcher")
  val vacantTrainingManager: ActorRef = context.actorOf(RoundRobinPool(5).props(VacantTrainingManager.props), "vacantTrainingManager")

  def receive = {
    case Hunt() =>
      log.info("hunting begins...")
      getTrackedTrainings()
        .map(trackedTrainingIds => trackedTrainingIds.ids)
        .map(ids => ids.map(id => getTraining(id)))
        .flatMap(trainingFutures => Future.sequence(trainingFutures))
        .foreach(trainings => {
          trainings
            .filter(training => training.canBeBooked())
            .foreach(training => vacantTrainingManager ! ProcessVacantTraining(training))
        })
  }

  private def getTrackedTrainings(): Future[TrackedTrainingIds] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingTracker, GetTrackedTrainings()).mapTo[TrackedTrainingIds]
  }

  private def getTraining(id: Long): Future[Training] = {
    implicit val timeout: Timeout = Timeout(10 seconds)
    ask(trainingFetcher, GetTraining(id)).mapTo[Training]
  }
}