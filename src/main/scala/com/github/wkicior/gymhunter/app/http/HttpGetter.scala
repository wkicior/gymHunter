package com.github.wkicior.gymhunter.app.http

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._


import scala.concurrent.Future

object HttpGetter {
  def props: Props = Props[HttpGetter]
  final case class Get(url: String)
  final case class GetResponse(body: String)
}

case class Training(id: Long, slotsAvailable: Long)
case class TrainingResponse(training: Training)

class HttpGetter extends Actor with ActorLogging {
  import akka.pattern.pipe
  import context.dispatcher
  implicit val system: ActorSystem = ActorSystem("GymHunter")
  implicit val mat = ActorMaterializer()(context)
  implicit val trainingFormat = jsonFormat2(Training)
  implicit val trainingResponseFormat = jsonFormat1(TrainingResponse)

  val http = Http(context.system)
  import HttpGetter._

  def receive = {
    case Get(url) =>
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = url))
      responseFuture.flatMap {
        case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
           Unmarshal(response).to[TrainingResponse]
        case _ => sys.error("something wrong")
      }
      .pipeTo(sender())
  }
}