package com.github.wkicior.gymhunter.domain.notification

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotification

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object SlotsAvailableNotificationSender {
  def props(ifttNotifiationSender: ActorRef): Props = Props(new SlotsAvailableNotificationSender(ifttNotifiationSender))
  final case class SendNotification(notification: Notification)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
}

class SlotsAvailableNotificationSender(ifttNotificationSender: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global


  def receive: PartialFunction[Any, Unit] = {
    case SendNotification(notification) =>
      log.info(s"will send IFTT notification for ${notification.trainingToHuntId}")
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(ifttNotificationSender, new IFTTNotification(notification)).onComplete {
        case Success(_) => {
          log.info("will send eventStream")
          context.system.eventStream.publish(SlotsAvailableNotificationSentEvent(notification))}
        case Failure(ex) => log.error("Error occured on sending IFTT notification", ex)
      }


        //case Success =>

      //}


      //context.system.eventStream.publish(SlotsAvailableNotificationSentEvent(notification))


//        .foreach(optionalTrainingToHunt => optionalTrainingToHunt {
//        case ot@Left(ex) => log.error(s"Could not load trainingToHunt ${ex}")
//        case Right(trainingToHunt) => log.info(s"will send IFTT notification for $id")
//      })
  }

}