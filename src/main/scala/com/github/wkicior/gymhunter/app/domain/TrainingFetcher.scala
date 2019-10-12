package com.github.wkicior.gymhunter.app.domain


import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future
import scala.language.postfixOps

case class TrainingResponse(training: Training)

object TrainingFetcher {
  def props: Props = Props[TrainingFetcher]
  final case class GetTraining(id: Long)
}

class TrainingFetcher extends Actor with ActorLogging {
  import TrainingFetcher._
  import akka.pattern.pipe
  import context.dispatcher

  implicit val system: ActorSystem = ActorSystem("GymHunter")
  implicit val mat = ActorMaterializer()(context)
  implicit val trainingFormat = jsonFormat4(Training)
  implicit val trainingResponseFormat = jsonFormat1(TrainingResponse)

  val http = Http(context.system)

  def receive = {
    case GetTraining(id) =>
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "https://api.gymsteer.com/api/clubs/8/trainings/" + id))
      responseFuture
        .flatMap {
          case response@HttpResponse(StatusCodes.OK, _, _, _) =>
            Unmarshal(response).to[TrainingResponse]
          case _ => sys.error("something wrong")
        }
        .pipeTo(sender())
    case _ =>
      log.error("unrecognized message")
  }
}