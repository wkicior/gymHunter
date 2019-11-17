package com.github.wkicior.gymhunter.infrastructure.gymsteer.auth

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.domain.training.Training
import com.github.wkicior.gymhunter.infrastructure.gymsteer.GymsteerException
import com.github.wkicior.gymhunter.infrastructure.gymsteer.auth.GymsteerTokenProvider.GetToken

import scala.concurrent.Future
import scala.language.postfixOps

case class TrainingResponse(training: Training)

object GymsteerTokenProvider {
  def props(hostname: String): Props = Props(new GymsteerTokenProvider(hostname))

  final case class GetToken(username: String, password: String)
}

class GymsteerTokenProvider(hostname: String) extends Actor with ActorLogging {
  import akka.pattern.pipe
  import context.dispatcher
  import com.github.wkicior.gymhunter.infrastructure.json.JsonProtocol._

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()(context)

  def receive: PartialFunction[Any, Unit] = {
    case GetToken(username, password) =>
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"$hostname/api/login",
        entity = HttpEntity(ContentTypes.`application/json`, gymsteerLoginRequestFormat.write(GymsteerLoginRequest(username, password)).toString())))
      responseFuture
        .flatMap {
          case response@HttpResponse(StatusCodes.OK, _, _, _) =>
            response.headers
              .find(h => h.name().equals("Access-Token"))
              .map(h => h.value())
              .map(token => Future.successful(token))
              .getOrElse(Future.failed(GymsteerException("could not read access-token header")))
          case response@HttpResponse(StatusCodes.Unauthorized, _, _, _) =>
            Future.failed(GymsteerException("not authenticated"))
          case x =>
            val msg = s"Something is wrong on getting the token: $x"
            log.error(msg)
            Future.failed(GymsteerException(msg))
        }
        .pipeTo(sender())
    case _ =>
      log.error("unrecognized message")
  }
}