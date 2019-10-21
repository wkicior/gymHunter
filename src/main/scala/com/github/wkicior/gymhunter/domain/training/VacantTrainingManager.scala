package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, Props}

object VacantTrainingManager {
  def props: Props = Props[VacantTrainingManager]
  final case class ProcessVacantTraining(training: Training)
}

class VacantTrainingManager extends Actor with ActorLogging {
  import VacantTrainingManager._


  def receive = {
    case ProcessVacantTraining(training: Training) =>
      println(s"slots available on ${training}")
  }
}