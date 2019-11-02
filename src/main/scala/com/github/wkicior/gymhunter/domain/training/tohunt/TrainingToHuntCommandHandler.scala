package com.github.wkicior.gymhunter.domain.training.tohunt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntEventStore.{StoreEvents}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


object TrainingToHuntCommandHandler {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new TrainingToHuntCommandHandler(trainingToHuntEventStore))
}

class TrainingToHuntCommandHandler(trainingToHuntEventStore: ActorRef) extends Actor with ActorLogging {
  implicit val ec = ExecutionContext.global

  def receive = {
    case tr: CreateTrainingToHuntCommand =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      val trainingToHunt = new TrainingToHunt(TrainingToHuntId(), tr.externalSystemId, tr.clubId, tr.huntingEndTime)
      ask(trainingToHuntEventStore, StoreEvents(trainingToHunt.id, trainingToHunt.pendingEventsList())).pipeTo(sender())
    case _ =>
      log.error("Unrecognized message")
  }
}