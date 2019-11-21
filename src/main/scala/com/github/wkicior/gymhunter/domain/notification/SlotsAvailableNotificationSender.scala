package com.github.wkicior.gymhunter.domain.notification

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object SlotsAvailableNotificationSender {
  def props(ifttNotificationSender: ActorRef): Props = Props(new SlotsAvailableNotificationSender(ifttNotificationSender))
  final case class SendNotification(notification: SlotsAvailableNotification)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
}

class SlotsAvailableNotificationSender(ifttNotificationSender: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global


  def receive: PartialFunction[Any, Unit] = {
    case SendNotification(notification) =>
      log.info(s"will send IFTT notification for ${notification.trainingHuntingSubscriptionId}")
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(ifttNotificationSender, notification).onComplete {
        case Success(_) => context.system.eventStream.publish(SlotsAvailableNotificationSentEvent(notification))
        case Failure(ex) => log.error("Error occurred on sending IFTT notification", ex)
      }
    case x => log.error(s"Unrecognized message $x")
  }

}