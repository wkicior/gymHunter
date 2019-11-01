package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntRepository.{AddTrainingToHunt, GetAllTrainingsToHunt}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


object TrainingToHuntProvider {
  def props(trainingToHuntRepository: ActorRef): Props = Props(new TrainingToHuntProvider(trainingToHuntRepository))
  final case class GetTrainingsToHunt()
}

class TrainingToHuntProvider(trainingToHuntRepository: ActorRef) extends Actor with ActorLogging {
  import TrainingToHuntProvider._
  implicit val ec = ExecutionContext.global

  def receive = {
    case GetTrainingsToHunt() =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingToHuntRepository, GetAllTrainingsToHunt()).pipeTo(sender())
    case tr: TrainingToHuntRequest =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingToHuntRepository, AddTrainingToHunt(tr)).pipeTo(sender())
    case _ =>
      log.error("Unrecognized message")
  }
}