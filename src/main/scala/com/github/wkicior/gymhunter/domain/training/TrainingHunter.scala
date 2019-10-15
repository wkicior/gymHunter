package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.FreeTrainingManager.ProcessFreeTraining
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

  def receive = {
    case Hunt() =>
      log.info("hunting start...")
      implicit val timeout: Timeout = Timeout(5 seconds)
      val futureTrackedIds: Future[TrackedTrainingIds] = ask(trainingTracker, GetTrackedTrainings()).mapTo[TrackedTrainingIds]
      futureTrackedIds
        .map(trackedTrainingIds => trackedTrainingIds.ids)
        .map(ids => ids.map(id => getTraining(id)))
        .flatMap(trainingFutures => Future.sequence(trainingFutures))
        .foreach(trainings => {
          trainings
            .filter(training => training.slotsAvailable > 0)
            .foreach(training => sendFreeTrainingForFurtherProcessing(training))
        })
  }

  private def sendFreeTrainingForFurtherProcessing(training: Training) = {
    val freeTrainingManager: ActorRef = context.actorOf(FreeTrainingManager.props, s"freeTrainingManager-${training.id}")
    freeTrainingManager ! ProcessFreeTraining(training)
  }

  private def getTraining(id: Long) = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    val trainingFetcher: ActorRef = context.actorOf(TrainingFetcher.props, s"trainingFetcher-${id}")
    ask(trainingFetcher, GetTraining(id)).mapTo[Training]
  }
}