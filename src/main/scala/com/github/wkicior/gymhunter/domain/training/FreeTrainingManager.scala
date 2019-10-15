package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, Props}

object FreeTrainingManager {
  def props: Props = Props[FreeTrainingManager]
  final case class ProcessFreeTraining(training: Training)
}

class FreeTrainingManager extends Actor with ActorLogging {
  import FreeTrainingManager._


  def receive = {
    case ProcessFreeTraining(training: Training) =>
      println(s"slots available on ${training}")
  }
}