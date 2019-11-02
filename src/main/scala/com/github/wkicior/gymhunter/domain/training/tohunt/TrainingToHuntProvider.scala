package com.github.wkicior.gymhunter.domain.training.tohunt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntEventStore.{GetAllTrainingsToHunt, StoreEvents}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


object TrainingToHuntProvider {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new TrainingToHuntProvider(trainingToHuntEventStore))
  final case class GetTrainingsToHuntQuery()
}

class TrainingToHuntProvider(trainingToHuntEventStore: ActorRef) extends Actor with ActorLogging {
  import TrainingToHuntProvider._
  implicit val ec = ExecutionContext.global

  def receive = {
    case GetTrainingsToHuntQuery() =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingToHuntEventStore, GetAllTrainingsToHunt()).pipeTo(sender())
    case _ =>
      log.error("Unrecognized message")
  }
}