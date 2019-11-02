package com.github.wkicior.gymhunter.domain.notification


import akka.actor.{Actor, ActorLogging, Props}
import com.github.wkicior.gymhunter.domain.training.Training
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHunt

object MailNotifier {
  def props: Props = Props[VacantTrainingManager]
  final case class Notify(training: Training, trainingToHunt: TrainingToHunt)
}

class VacantTrainingManager extends Actor with ActorLogging {
  import MailNotifier._


  def receive = {
    case Notify(training: Training, trainingToHunt: TrainingToHunt) =>
      log.info(s"sending email to available on $training")
  }
}