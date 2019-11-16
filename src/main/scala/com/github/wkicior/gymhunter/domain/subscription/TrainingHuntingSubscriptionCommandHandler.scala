package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.{CreateTrainingHuntingSubscriptionCommand, DeleteTrainingHuntingSubscriptionCommand}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetTrainingHuntingSubscriptionAggregate, StoreEvents}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps


object TrainingHuntingSubscriptionCommandHandler {
  def props(trainingHuntingSubscriptionEventStore: ActorRef): Props = Props(new TrainingHuntingSubscriptionCommandHandler(trainingHuntingSubscriptionEventStore))
  case class CreateTrainingHuntingSubscriptionCommand(externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime, autoBookingDeadline: Option[OffsetDateTime] = None)
  case class DeleteTrainingHuntingSubscriptionCommand(id: TrainingHuntingSubscriptionId)
}

class TrainingHuntingSubscriptionCommandHandler(trainingHuntingSubscriptionEventStore: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def receive: PartialFunction[Any, Unit] = {
    case tr: CreateTrainingHuntingSubscriptionCommand =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      val trainingHuntingSubscription = new TrainingHuntingSubscriptionAggregate(TrainingHuntingSubscriptionId(), tr.externalSystemId, tr.clubId, tr.huntingEndTime, tr.autoBookingDeadline)
      ask(trainingHuntingSubscriptionEventStore, StoreEvents(trainingHuntingSubscription.id, trainingHuntingSubscription.pendingEventsList()))
        .mapTo[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
        .map(ttha => ttha.toOption.get)
        .map(ttha => ttha())
        .pipeTo(sender())

    case DeleteTrainingHuntingSubscriptionCommand(id) =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      ask(trainingHuntingSubscriptionEventStore, GetTrainingHuntingSubscriptionAggregate(id))
        .mapTo[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
        .flatMap {
          case ot@Left(_) => Future(ot)
          case Right(trainingHuntingSubscription) =>
            trainingHuntingSubscription.delete()
            ask(trainingHuntingSubscriptionEventStore, StoreEvents(trainingHuntingSubscription.id, trainingHuntingSubscription.pendingEventsList()))
              .mapTo[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
              .map(_ => Right(trainingHuntingSubscription()))
        }
        .pipeTo(sender())

    case x =>
      log.error(s"Unrecognized message: $x")
  }
}