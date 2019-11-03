package com.github.wkicior.gymhunter.app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.github.wkicior.gymhunter.domain.training.TrainingHunter
import com.github.wkicior.gymhunter.domain.training.TrainingHunter.Hunt

object GymHunterSupervisor {
  def props(trainingToHuntRepository: ActorRef): Props = Props(new GymHunterSupervisor(trainingToHuntRepository))
  final case class RunGymHunting()
}

class GymHunterSupervisor(trainingToHuntRepository: ActorRef) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("GymHunter Application started")
  override def postStop(): Unit = log.info("GymHunter Application stopped")

  val trainingHunter: ActorRef = context.actorOf(TrainingHunter.props(trainingToHuntRepository), "trainingHunter")

  def receive: PartialFunction[Any, Unit] = {
    case RunGymHunting() =>
      trainingHunter ! Hunt()
  }
}