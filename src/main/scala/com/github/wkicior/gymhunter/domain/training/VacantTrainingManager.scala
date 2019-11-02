package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wkicior.gymhunter.domain.notification.MailNotifier
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntProvider

object VacantTrainingManager {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new VacantTrainingManager(trainingToHuntEventStore))
  final case class ProcessVacantTraining(training: Training)
}

class VacantTrainingManager(trainingToHuntEventStore: ActorRef) extends Actor with ActorLogging {
  import VacantTrainingManager._

  val mailNotifier: ActorRef = context.actorOf(MailNotifier.props, "mailNotifier")
  val trainingToHuntProvider: ActorRef = context.actorOf(TrainingToHuntProvider.props(trainingToHuntEventStore))


  def receive = {
    case ProcessVacantTraining(training: Training) =>
      log.info(s"slots available on $training")
      //
  }
}