package com.github.wkicior.gymhunter.infrastructure.gymsteer.training

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.domain.training.{GetTraining, Training}
import com.github.wkicior.gymhunter.infrastructure.gymsteer.GymsteerException
import spray.json.DeserializationException

import scala.concurrent.Future
import scala.language.postfixOps

object GymsteerTrainingBooker {
  def props(hostname: String): Props = Props(new GymsteerTrainingBooker(hostname))

  case class BookTraining(id: Long, authToken: String)

}

class GymsteerTrainingBooker(hostname: String) extends Actor with ActorLogging {
  import akka.pattern.pipe
  import com.github.wkicior.gymhunter.infrastructure.json.JsonProtocol._
  import context.dispatcher

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()(context)

  def receive: PartialFunction[Any, Unit] = {
    case _ =>
      log.error("unrecognized message")
  }
}