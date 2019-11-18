package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionAutoBookingPerformedEvent}
import akka.pattern.ask
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps


private [training] object TrainingBooker {
  def props(gymsteerProxy: ActorRef): Props = Props(new TrainingBooker(gymsteerProxy))
  final case class ProcessVacantTraining(training: Training)

  case class BookTraining(ths: TrainingHuntingSubscription, training: Training)
}

class TrainingBooker(gymsteerProxy: ActorRef) extends Actor with ActorLogging {
  import TrainingBooker._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def receive: PartialFunction[Any, Unit] = {
    case BookTraining(ths: TrainingHuntingSubscription, training: Training) =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(gymsteerProxy, com.github.wkicior.gymhunter.domain.training.BookTraining(ths.externalSystemId)).onComplete {
        case Success(_) => context.system.eventStream.publish(TrainingAutoBookingPerformedEvent(training.id, ths.clubId, ths.id, training.start_date))
        case Failure(ex) => log.error("Error occurred on sending IFTT notification", ex)
      }
    case x => log.error(s"unrecognized message $x")
  }
}