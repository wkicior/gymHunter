package com.github.wkicior.gymhunter.domain.notification


import akka.actor.{Actor, ActorLogging, Props}
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHunt
import com.github.wkicior.gymhunter.domain.training.Training

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps

object SlotsAvailableNotificationSender {
  def props(): Props = Props[SlotsAvailableNotificationSender]
  final case class SendNotification(trainingToHunt: TrainingToHunt, training: Training)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
}

class SlotsAvailableNotificationSender() extends Actor with ActorLogging {

  def receive: PartialFunction[Any, Unit] = {
    case SendNotification(trainingToHunt, training) =>
      log.info(s"will send IFTT notification for ${trainingToHunt.id}")
      context.system.eventStream.publish(SlotsAvailableNotificationSentEvent(trainingToHunt.id))


//        .foreach(optionalTrainingToHunt => optionalTrainingToHunt {
//        case ot@Left(ex) => log.error(s"Could not load trainingToHunt ${ex}")
//        case Right(trainingToHunt) => log.info(s"will send IFTT notification for $id")
//      })
  }

}