package com.github.wkicior.gymhunter.domain.notification


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHunt

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps

object SlotsAvailableNotificationSender {
  def props(): Props = Props[SlotsAvailableNotificationSender]
  final case class SendNotification(trainingToHunt: TrainingToHunt)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
}

class SlotsAvailableNotificationSender() extends Actor with ActorLogging {

  def receive: PartialFunction[Any, Unit] = {
    case SendNotification(id) =>
      log.info(s"will send IFTT notification for $id")


//        .foreach(optionalTrainingToHunt => optionalTrainingToHunt {
//        case ot@Left(ex) => log.error(s"Could not load trainingToHunt ${ex}")
//        case Right(trainingToHunt) => log.info(s"will send IFTT notification for $id")
//      })
  }

}