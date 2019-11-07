package com.github.wkicior.gymhunter.domain.tohunt


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntId.OptionalTrainingToHunt
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntPersistence.{GetTrainingToHuntAggregate, StoreEvents}
import com.github.wkicior.gymhunter.domain.training.TrainingSlotsAvailableEvent

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps


object TrainingSlotsAvailableEventHandler {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new TrainingSlotsAvailableEventHandler(trainingToHuntEventStore, SlotsAvailableNotificationSender.props()))
  def props(trainingToHuntEventStore: ActorRef, slotsAvailableNotificationSenderProps: Props): Props = Props(new TrainingSlotsAvailableEventHandler(trainingToHuntEventStore, slotsAvailableNotificationSenderProps))
}

class TrainingSlotsAvailableEventHandler(trainingToHuntEventStore: ActorRef, slotsAvailableNotificationSenderProps: Props) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val slotsAvailableNotificationSender: ActorRef = context.actorOf(slotsAvailableNotificationSenderProps, "slotsAvailableNotificationSender")

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[TrainingSlotsAvailableEvent])

  def receive: PartialFunction[Any, Unit] = {
    case TrainingSlotsAvailableEvent(id) =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      ask(trainingToHuntEventStore, GetTrainingToHuntAggregate(id)).mapTo[OptionalTrainingToHunt[TrainingToHuntAggregate]]
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