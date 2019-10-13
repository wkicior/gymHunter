package com.github.wkicior.gymhunter.domain.training


import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.language.postfixOps


object TrainingTracker {
  def props: Props = Props[TrainingTracker]
  final case class GetTrackedTrainings()
  final case class TrackedTrainingIds(ids: List[Long])

}

class TrainingTracker extends Actor with ActorLogging {
  import TrainingTracker._
  implicit val system: ActorSystem = ActorSystem("GymHunter")
  val trainingFetcher: ActorRef = system.actorOf(TrainingFetcher.props, "trainingFetcher")


  def receive = {
    case GetTrackedTrainings() =>
      val trainingids = List(550633L, 699157, 699176, 699158, 550634, 550635)
      //TODO: use source? val trainingIdsSource: Source[Long, NotUsed] = Source(List(550633, 550634))
      sender() ! TrackedTrainingIds(trainingids)
  }
}