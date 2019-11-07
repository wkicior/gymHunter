package com.github.wkicior.gymhunter.app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.github.wkicior.gymhunter.domain.tohunt.TrainingSlotsAvailableEventHandler
import com.github.wkicior.gymhunter.domain.training.TrainingHunter
import com.github.wkicior.gymhunter.domain.training.TrainingHunter.Hunt

object GymHunterSupervisor {
  def props(trainingToHuntRepository: ActorRef, gymsteerTrainingFetcher: ActorRef): Props = Props(new GymHunterSupervisor(trainingToHuntRepository, gymsteerTrainingFetcher))
  final case class RunGymHunting()
}

class GymHunterSupervisor(trainingToHuntEventStore: ActorRef, trainingFetcher: ActorRef) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("GymHunter Application started")
  override def postStop(): Unit = log.info("GymHunter Application stopped")

  val trainingHunter: ActorRef = context.actorOf(TrainingHunter.props(trainingToHuntEventStore, trainingFetcher), "trainingHunter")
  val trainingSlotsAvailableNotificationHandler: ActorRef = context.actorOf(TrainingSlotsAvailableEventHandler.props(trainingToHuntEventStore), "trainingSlotsAvailableNotificationHandler")

  def receive: PartialFunction[Any, Unit] = {
    case RunGymHunting() => trainingHunter ! Hunt()
    case x => log.error(s"Unrecognized message: $x")
  }
}