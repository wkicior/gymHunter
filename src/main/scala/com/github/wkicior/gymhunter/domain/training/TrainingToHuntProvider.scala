package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntRepository.GetTrackedTrainings

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


object TrainingToHuntProvider {
  def props: Props = Props[TrainingToHuntProvider]
  final case class GetTrainingsToHunt()
}

class TrainingToHuntProvider extends Actor with ActorLogging {
  import TrainingToHuntProvider._
  implicit val ec = ExecutionContext.global
  val trainingToHuntRepository: ActorRef = context.actorOf(TrainingToHuntRepository.props, "trainingToHuntRepository")

  def receive = {
    case GetTrainingsToHunt() =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingToHuntRepository, GetTrackedTrainings()).pipeTo(sender())
    case _ =>
      log.error("Unrecognized message")
  }
}