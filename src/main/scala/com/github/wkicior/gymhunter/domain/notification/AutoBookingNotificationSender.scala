package com.github.wkicior.gymhunter.domain.notification

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.github.wkicior.gymhunter.domain.training.TrainingAutoBookingPerformedEvent
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotification
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotificationSender.SendIFTTNotification

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps


object AutoBookingNotificationSender {
  def props(ifttNotifiationSender: ActorRef): Props = Props(new AutoBookingNotificationSender(ifttNotifiationSender))
  final case class SendNotification(notification: Notification)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
}

class AutoBookingNotificationSender(ifttNotificationSender: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[TrainingAutoBookingPerformedEvent])

  def receive: PartialFunction[Any, Unit] = {
    case TrainingAutoBookingPerformedEvent(_, clubId, thsId, trainingDateTime) =>
      log.info(s"will send IFTT auto booking notification for $thsId")
      implicit val timeout: Timeout = Timeout(5 seconds)
      ifttNotificationSender ? SendIFTTNotification("gymHunterAutoBooking", new IFTTNotification(Notification(trainingDateTime, clubId, thsId)))
    case x =>
      log.error(s"Unrecognized message: $x")
  }
}