package com.github.wkicior.gymhunter.domain.subscription

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetAllTrainingHuntingSubscriptions, GetTrainingHuntingSubscription}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.postfixOps


object TrainingHuntingSubscriptionProvider {
  def props(trainingHuntingSubscriptionEventStore: ActorRef): Props = Props(new TrainingHuntingSubscriptionProvider(trainingHuntingSubscriptionEventStore))
  final case class GetActiveTrainingHuntingSubscriptionsQuery()
  final case class GetAllTrainingHuntingSubscriptionsQuery()
  final case class GetTrainingHuntingSubscriptionsByTrainingIdQuery(id: Long)
  final case class GetTrainingHuntingSubscriptionQuery(id: TrainingHuntingSubscriptionId)
}

class TrainingHuntingSubscriptionProvider(trainingHuntingSubscriptionEventStore: ActorRef) extends Actor with ActorLogging {
  import TrainingHuntingSubscriptionProvider._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def receive: PartialFunction[Any, Unit] = {

    case GetActiveTrainingHuntingSubscriptionsQuery() =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingHuntingSubscriptionEventStore, GetAllTrainingHuntingSubscriptions()).mapTo[Set[TrainingHuntingSubscription]]
        .map(trainingHuntingSubscriptions => trainingHuntingSubscriptions.filter(t => t.isActive))
        .pipeTo(sender())

    case GetAllTrainingHuntingSubscriptionsQuery() =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingHuntingSubscriptionEventStore, GetAllTrainingHuntingSubscriptions()).mapTo[Set[TrainingHuntingSubscription]]
        .pipeTo(sender())

    case GetTrainingHuntingSubscriptionsByTrainingIdQuery(id) =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingHuntingSubscriptionEventStore, GetAllTrainingHuntingSubscriptions()).mapTo[Set[TrainingHuntingSubscription]]
        .map(trainingHuntingSubscriptions => trainingHuntingSubscriptions.filter(t => t.externalSystemId == id))
        .pipeTo(sender())

    case GetTrainingHuntingSubscriptionQuery(id) =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingHuntingSubscriptionEventStore, GetTrainingHuntingSubscription(id)).mapTo[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]]
        .pipeTo(sender())

    case x =>
      log.error(s"Unrecognized message: $x")
  }
}