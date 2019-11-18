package com.github.wkicior.gymhunter.infrastructure.iftt

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.app.{Settings, SettingsImpl}
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotificationSender.SendIFTTNotification

import scala.concurrent.Future
import scala.language.postfixOps

object IFTTNotificationSender {
  def props: Props = Props[IFTTNotificationSender]

  case class SendIFTTNotification(name: String, notification: IFTTNotification)
}

class IFTTNotificationSender extends Actor with ActorLogging {
  import akka.pattern.pipe
  import com.github.wkicior.gymhunter.infrastructure.json.JsonProtocol._
  import context.dispatcher

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()(context)
  val settings: SettingsImpl = Settings(context.system)

  def receive: PartialFunction[Any, Unit] = {
    case SendIFTTNotification(name, notification) =>
      log.info(s"sending notification $notification...")
      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(method = HttpMethods.POST,
        uri = s"${settings.ifttHost}/trigger/$name/with/key/${settings.ifttKey}",
        entity = HttpEntity(ContentTypes.`application/json`, ifttNotificationFormat.write(notification).toString())))
      responseFuture
        .map {
          case response@HttpResponse(StatusCodes.OK, _, _, _) =>
            response.discardEntityBytes()
            Status.Success()
          case _ => Status.Failure(new RuntimeException("notification sending failed"))
        }
        .pipeTo(sender())
    case _ =>
      log.error("unrecognized message")
  }
}