package com.github.wkicior.gymhunter.domain.subscription

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetTrainingHuntingSubscriptionAggregate, StoreEvents}
import com.github.wkicior.gymhunter.domain.training.TrainingAutoBookingPerformedEvent

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

object TrainingAutoBookingPerformedEventHandler {
  def props(trainingHuntingSubscriptionEventStore: ActorRef): Props = Props(new TrainingAutoBookingPerformedEventHandler(trainingHuntingSubscriptionEventStore))
  def props(trainingHuntingSubscriptionEventStore: ActorRef, slotsAvailableNotificationSenderProps: Props): Props = Props(
    new TrainingSlotsAvailableNotificationSentEventHandler(trainingHuntingSubscriptionEventStore))
}

class TrainingAutoBookingPerformedEventHandler(trainingHuntingSubscriptionEventStore: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[TrainingAutoBookingPerformedEvent])

  def receive: PartialFunction[Any, Unit] = {
    case TrainingAutoBookingPerformedEvent(_, _, thsId, _) =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      ask(trainingHuntingSubscriptionEventStore, GetTrainingHuntingSubscriptionAggregate(thsId))
        .mapTo[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
        .flatMap {
          case ot@Left(_) => Future(ot)
          case Right(trainingHuntingSubscription) =>
            trainingHuntingSubscription.autoBookingPerformed()
            ask(trainingHuntingSubscriptionEventStore, StoreEvents(trainingHuntingSubscription.id, trainingHuntingSubscription.pendingEventsList()))

        }
    case x =>
      log.error(s"Unrecognized message: $x")
  }
}