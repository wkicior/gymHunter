package com.github.wkicior.gymhunter.domain.training.tohunt

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntCommandHandler.{CreateTrainingToHuntCommand, DeleteTrainingToHuntCommand}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntEventStore.{GetTraining, OptionalTrainingToHunt, StoreEvents, TrainingToHuntNotFound}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps


object TrainingToHuntCommandHandler {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new TrainingToHuntCommandHandler(trainingToHuntEventStore))
  case class CreateTrainingToHuntCommand(externalSystemId: Long, clubId: Long, huntingEndTime: OffsetDateTime)
  case class DeleteTrainingToHuntCommand(id: TrainingToHuntId)
}

class TrainingToHuntCommandHandler(trainingToHuntEventStore: ActorRef) extends Actor with ActorLogging {
  implicit val ec = ExecutionContext.global

  def receive = {
    case tr: CreateTrainingToHuntCommand =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      val trainingToHunt = new TrainingToHunt(TrainingToHuntId(), tr.externalSystemId, tr.clubId, tr.huntingEndTime)
      ask(trainingToHuntEventStore, StoreEvents(trainingToHunt.id, trainingToHunt.pendingEventsList())).pipeTo(sender())
    case DeleteTrainingToHuntCommand(id) =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingToHuntEventStore, GetTraining(id)).mapTo[OptionalTrainingToHunt[TrainingToHunt]]
        .flatMap {
          case ot@Left(_) => Future(ot)
          case Right(trainingToHunt) =>
            trainingToHunt.deleteBySettingAsInactive()
            ask(trainingToHuntEventStore, StoreEvents(trainingToHunt.id, trainingToHunt.pendingEventsList()))
              .mapTo[TrainingToHunt]
              .map(tth => Right(tth))
        }
        .pipeTo(sender())
    case _ =>
      log.error("Unrecognized message")
  }
}