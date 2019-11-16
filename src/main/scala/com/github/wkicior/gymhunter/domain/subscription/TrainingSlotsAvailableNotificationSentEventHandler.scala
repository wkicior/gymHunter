package com.github.wkicior.gymhunter.domain.subscription


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSentEvent
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetTrainingHuntingSubscriptionAggregate, StoreEvents}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps


object TrainingSlotsAvailableNotificationSentEventHandler {
  def props(trainingHuntingSubscriptionEventStore: ActorRef): Props = Props(new TrainingSlotsAvailableNotificationSentEventHandler(trainingHuntingSubscriptionEventStore))
  def props(trainingHuntingSubscriptionEventStore: ActorRef, slotsAvailableNotificationSenderProps: Props): Props = Props(
    new TrainingSlotsAvailableNotificationSentEventHandler(trainingHuntingSubscriptionEventStore))
}

class TrainingSlotsAvailableNotificationSentEventHandler(trainingHuntingSubscriptionEventStore: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[SlotsAvailableNotificationSentEvent])

  def receive: PartialFunction[Any, Unit] = {
    case SlotsAvailableNotificationSentEvent(notification) =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      ask(trainingHuntingSubscriptionEventStore, GetTrainingHuntingSubscriptionAggregate(notification.trainingHuntingSubscriptionId))
        .mapTo[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
        .flatMap {
          case ot@Left(_) => Future(ot)
          case Right(trainingHuntingSubscription) =>
            trainingHuntingSubscription.notifyOnSlotsAvailable()
            ask(trainingHuntingSubscriptionEventStore, StoreEvents(trainingHuntingSubscription.id, trainingHuntingSubscription.pendingEventsList()))
              .mapTo[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
              .map(_ => Right(trainingHuntingSubscription()))
        }
    case x =>
      log.error(s"Unrecognized message: $x")
  }
}