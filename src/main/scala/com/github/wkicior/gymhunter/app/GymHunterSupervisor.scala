package com.github.wkicior.gymhunter.app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.github.wkicior.gymhunter.domain.training.TrainingHunter
import com.github.wkicior.gymhunter.domain.training.TrainingHunter.Hunt

object GymHunterSupervisor {
  def props(): Props = Props(new GymHunterSupervisor)
  final case class RunGymHunting()
}

class GymHunterSupervisor extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("GymHunter Application started")
  override def postStop(): Unit = log.info("GymHunter Application stopped")

  def receive = {
    case RunGymHunting() =>
      val trainingHunter: ActorRef = context.actorOf(TrainingHunter.props, "trainingHunter")
      trainingHunter ! Hunt()
  }
}