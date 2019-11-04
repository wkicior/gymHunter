package com.github.wkicior.gymhunter.app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.github.wkicior.gymhunter.domain.notification.IFTTNotifier
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
  val ifttNotifier: ActorRef = context.actorOf(IFTTNotifier.props(trainingToHuntEventStore), "ifttNotifier")

  def receive: PartialFunction[Any, Unit] = {
    case RunGymHunting() => trainingHunter ! Hunt()
    case x => log.error(s"Unrecognized message: $x")
  }
}