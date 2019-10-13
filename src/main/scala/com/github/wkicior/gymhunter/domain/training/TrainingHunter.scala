package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.TrainingFetcher.GetTraining

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps


object TrainingHunter {
  def props: Props = Props[TrainingHunter]
  final case class Hunt()

}

class TrainingHunter extends Actor with ActorLogging {
  import TrainingHunter._
  import TrainingTracker._
  implicit val system: ActorSystem = ActorSystem("GymHunter")
  val trainingFetcher: ActorRef = system.actorOf(TrainingFetcher.props, "trainingFetcher")
  val trainingTracker: ActorRef = system.actorOf(TrainingTracker.props, "trainingTracker")


  def receive = {
    case Hunt() =>
      implicit val timeout = Timeout(5 seconds)
      val futureTrackedIds: Future[TrackedTrainingIds] = ask(trainingTracker, GetTrackedTrainings()).mapTo[TrackedTrainingIds]
      val trackedIds = Await.result(futureTrackedIds, 1 second)
      trackedIds.ids.foreach(id => {
        val future: Future[Training] = ask(trainingFetcher, GetTraining(id)).mapTo[Training]
        val training = Await.result(future, 5 second)
        if (training.slotsAvailable > 0) {
          log.info("Slots available for training: " + training)
          //TODO: notify + auto booking, stop tracking
        } else {
          log.info("No slots available for training: " + training)
        }
      })
  }
}