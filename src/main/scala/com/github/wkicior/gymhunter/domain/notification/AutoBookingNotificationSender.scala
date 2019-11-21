package com.github.wkicior.gymhunter.domain.notification

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.TrainingAutoBookingPerformedEvent

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps


object AutoBookingNotificationSender {
  def props(notificationSender: ActorRef): Props = Props(new AutoBookingNotificationSender(notificationSender))
  final case class SendNotification(notification: SlotsAvailableNotification)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
}

class AutoBookingNotificationSender(ifttNotificationSender: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[TrainingAutoBookingPerformedEvent])

  def receive: PartialFunction[Any, Unit] = {
    case TrainingAutoBookingPerformedEvent(_, clubId, thsId, trainingDateTime) =>
      log.info(s"will send IFTT auto booking notification for $thsId")
      implicit val timeout: Timeout = Timeout(5 seconds)
      ifttNotificationSender ? AutoBookingNotification(trainingDateTime, clubId, thsId)
    case x =>
      log.error(s"Unrecognized message: $x")
  }
}