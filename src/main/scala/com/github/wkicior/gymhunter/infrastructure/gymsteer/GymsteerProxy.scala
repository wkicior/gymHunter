package com.github.wkicior.gymhunter.infrastructure.gymsteer

import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.{BookTraining, GetTraining, Training}
import com.github.wkicior.gymhunter.infrastructure.gymsteer.training.GymsteerTrainingFetcher

import scala.concurrent.duration._
import scala.language.postfixOps

object GymsteerProxy {
  def props(hostname: String): Props = Props(new GymsteerProxy(GymsteerTrainingFetcher.props(hostname)))
}

final case class GymsteerProxyException(msg: String) extends RuntimeException(msg)

class GymsteerProxy(trainingFetcherProps: Props) extends Actor with ActorLogging {
  import akka.pattern.pipe
  import context.dispatcher

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()(context)

  implicit val trainingFetcher: ActorRef = system.actorOf(trainingFetcherProps)

  def receive: PartialFunction[Any, Unit] = {
    case gt@GetTraining =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingFetcher, gt).pipeTo(sender())
    case bt@BookTraining =>
      sender() ! Success
    case _ =>
      log.error("unrecognized message")
  }
}