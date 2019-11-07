package com.github.wkicior.gymhunter.domain.tohunt


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSentEvent
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntId.OptionalTrainingToHunt
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntPersistence.{GetTrainingToHuntAggregate, StoreEvents}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps


object TrainingSlotsAvailableNotificationSentEventHandler {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new TrainingSlotsAvailableNotificationSentEventHandler(trainingToHuntEventStore))
  def props(trainingToHuntEventStore: ActorRef, slotsAvailableNotificationSenderProps: Props): Props = Props(new TrainingSlotsAvailableNotificationSentEventHandler(trainingToHuntEventStore))
}

class TrainingSlotsAvailableNotificationSentEventHandler(trainingToHuntEventStore: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[SlotsAvailableNotificationSentEvent])

  def receive: PartialFunction[Any, Unit] = {
    case SlotsAvailableNotificationSentEvent(notification) =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      ask(trainingToHuntEventStore, GetTrainingToHuntAggregate(notification.trainingToHuntId)).mapTo[OptionalTrainingToHunt[TrainingToHuntAggregate]]
        .flatMap {
          case ot@Left(_) => Future(ot)
          case Right(trainingToHunt) =>
            trainingToHunt.notifyOnSlotsAvailable()
            ask(trainingToHuntEventStore, StoreEvents(trainingToHunt.id, trainingToHunt.pendingEventsList())).mapTo[OptionalTrainingToHunt[TrainingToHuntAggregate]]
              .map(_ => Right(trainingToHunt()))
        }
    case x =>
      log.error(s"Unrecognized message: $x")
  }
}