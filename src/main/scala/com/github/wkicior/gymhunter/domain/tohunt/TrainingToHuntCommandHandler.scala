package com.github.wkicior.gymhunter.domain.tohunt

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntCommandHandler.{CreateTrainingToHuntCommand, DeleteTrainingToHuntCommand}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntId.OptionalTrainingToHunt
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntPersistence.{GetTrainingToHuntAggregate, StoreEvents}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps


object TrainingToHuntCommandHandler {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new TrainingToHuntCommandHandler(trainingToHuntEventStore))
  case class CreateTrainingToHuntCommand(externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime)
  case class DeleteTrainingToHuntCommand(id: TrainingToHuntId)
}

class TrainingToHuntCommandHandler(trainingToHuntEventStore: ActorRef) extends Actor with ActorLogging {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def receive: PartialFunction[Any, Unit] = {
    case tr: CreateTrainingToHuntCommand =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      val trainingToHunt = new TrainingToHuntAggregate(TrainingToHuntId(), tr.externalSystemId, tr.clubId, tr.huntingEndTime)
      ask(trainingToHuntEventStore, StoreEvents(trainingToHunt.id, trainingToHunt.pendingEventsList())).mapTo[OptionalTrainingToHunt[TrainingToHuntAggregate]]
        .map(ttha => ttha.toOption.get)
        .map(ttha => ttha())
        .pipeTo(sender())

    case DeleteTrainingToHuntCommand(id) =>
      implicit val timeout: Timeout = Timeout(2 seconds)
      ask(trainingToHuntEventStore, GetTrainingToHuntAggregate(id)).mapTo[OptionalTrainingToHunt[TrainingToHuntAggregate]]
        .flatMap {
          case ot@Left(_) => Future(ot)
          case Right(trainingToHunt) =>
            trainingToHunt.delete()
            ask(trainingToHuntEventStore, StoreEvents(trainingToHunt.id, trainingToHunt.pendingEventsList())).mapTo[OptionalTrainingToHunt[TrainingToHuntAggregate]]
              .map(_ => Right(trainingToHunt()))
        }
        .pipeTo(sender())

    case x =>
      log.error(s"Unrecognized message: $x")
  }
}