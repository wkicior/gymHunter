package com.github.wkicior.gymhunter.infrastructure.gymsteer.training

import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.infrastructure.gymsteer.GymsteerException
import com.github.wkicior.gymhunter.infrastructure.gymsteer.training.GymsteerTrainingBooker.BookTraining

import scala.concurrent.Future
import scala.language.postfixOps

object GymsteerTrainingBooker {
  def props(hostname: String): Props = Props(new GymsteerTrainingBooker(hostname))

  case class BookTraining(id: Long, authToken: String)

}

class GymsteerTrainingBooker(hostname: String) extends Actor with ActorLogging {
  import akka.pattern.pipe
  import context.dispatcher

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()(context)

  def receive: PartialFunction[Any, Unit] = {
    case BookTraining(id, authToken) =>
      val responseFuture: Future[HttpResponse] = Http().singleRequest(
        HttpRequest(method = HttpMethods.POST,
          uri = s"$hostname/api/clubs/8/trainings/$id/user-booking",
          headers = Seq(new RawHeader("Access-Token", authToken))))
      responseFuture
        .flatMap {
          case response@HttpResponse(StatusCodes.Created, _, _, _) =>
            response.discardEntityBytes()
            Future.successful(Success)
          case x =>
            val msg = s"Something is wrong on booking the training: $x"
            log.error(msg)
            Future.failed(GymsteerException(msg))
        }
        .pipeTo(sender())
    case _ =>
      log.error("unrecognized message")
  }
}