package com.github.wkicior.gymhunter.infrastructure.gymsteer

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.app.{Settings, SettingsImpl}
import com.github.wkicior.gymhunter.domain.training.{GetTraining, Training}

import scala.concurrent.Future
import scala.language.postfixOps

case class TrainingResponse(training: Training)

object GymsteerTrainingFetcher {
  def props: Props = Props[GymsteerTrainingFetcher]
}

class GymsteerTrainingFetcher extends Actor with ActorLogging {
  import akka.pattern.pipe
  import com.github.wkicior.gymhunter.app.JsonProtocol._
  import context.dispatcher

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()(context)
  val settings: SettingsImpl = Settings(context.system)

  def receive: PartialFunction[Any, Unit] = {
    case GetTraining(id) =>
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = s"${settings.gymsteerHost}/api/clubs/8/trainings/$id"))
      responseFuture
        .flatMap {
          case response@HttpResponse(StatusCodes.OK, _, _, _) =>
            //val x = response.discardEntityBytes()
            Unmarshal(response).to[TrainingResponse]
          case _ => sys.error("something wrong")
        }
        .map(tr => tr.training)
        .pipeTo(sender())
    case _ =>
      log.error("unrecognized message")
  }
}