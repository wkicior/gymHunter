package com.github.wkicior.gymhunter.infrastructure.gymsteer

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.app.{Settings, SettingsImpl}
import com.github.wkicior.gymhunter.domain.training.{GetTraining, Training}
import spray.json.DeserializationException

import scala.concurrent.Future
import scala.language.postfixOps

case class TrainingResponse(training: Training)

object GymsteerTrainingFetcher {
  def props(hostname: String): Props = Props(new GymsteerTrainingFetcher(hostname))
}


final case class GymsteerTrainingFetcherException(msg: String) extends RuntimeException(msg)

class GymsteerTrainingFetcher(hostname: String) extends Actor with ActorLogging {
  import akka.pattern.pipe
  import com.github.wkicior.gymhunter.app.JsonProtocol._
  import context.dispatcher

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()(context)

  def receive: PartialFunction[Any, Unit] = {
    case GetTraining(id) =>
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = s"$hostname/api/clubs/8/trainings/$id"))
      responseFuture
        .flatMap {
          case response@HttpResponse(StatusCodes.OK, _, _, _) =>
              Unmarshal(response).to[TrainingResponse].recoverWith {
                case ex: DeserializationException =>
                  val msg = s"Could not deserialize the response: ${ex.getMessage}"
                  log.error(msg)
                  Future.failed(GymsteerTrainingFetcherException(msg))
              }

          case x => {
            val msg = s"Something is wrong on getting the training $id: $x"
            log.error(msg)
            Future.failed(GymsteerTrainingFetcherException(msg))
          }
        }
        .map(tr => tr.training)
        .pipeTo(sender())
    case _ =>
      log.error("unrecognized message")
  }
}