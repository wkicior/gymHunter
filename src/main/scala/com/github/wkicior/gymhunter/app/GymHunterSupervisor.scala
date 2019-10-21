package com.github.wkicior.gymhunter.app

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
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

  val trainingHunter: ActorRef = context.actorOf(TrainingHunter.props, "trainingHunter")

  def receive = {
    case RunGymHunting() =>
      trainingHunter ! Hunt()
  }
}