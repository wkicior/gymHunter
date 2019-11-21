package com.github.wkicior.gymhunter.infrastructure.iftt

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.app.{Settings, SettingsImpl}
import com.github.wkicior.gymhunter.domain.notification.{AutoBookingNotification, SlotsAvailableNotification}
import spray.json.JsValue

import scala.concurrent.Future
import scala.language.postfixOps

object IFTTNotificationSender {
  def props: Props = Props[IFTTNotificationSender]

}

class IFTTNotificationSender extends Actor with ActorLogging {
  import akka.pattern.pipe
  import com.github.wkicior.gymhunter.infrastructure.json.JsonProtocol._
  import context.dispatcher

  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer()(context)
  val settings: SettingsImpl = Settings(context.system)

  def receive: PartialFunction[Any, Unit] = {
    case notification: SlotsAvailableNotification =>
      val msgJson: JsValue = ifttSlotsAvailableNotificationFormat.write(new IFTTSlotsAvailableNotification(notification))
      sendIFTTNotification(IFTTSlotsAvailableNotification.name, msgJson)
    case notification: AutoBookingNotification =>
      val msgJson: JsValue = ifttAutoBookingNotificationFormat.write(new IFTTAutoBookingNotification(notification))
      sendIFTTNotification(IFTTAutoBookingNotification.name, msgJson)
    case _ =>
      log.error("unrecognized message")
  }

  def sendIFTTNotification(name: String, msgJson: JsValue): Future[Status.Status] = {
    log.info(s"sending notification $msgJson...")
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(method = HttpMethods.POST,
      uri = s"${settings.ifttHost}/trigger/$name/with/key/${settings.ifttKey}",
      entity = HttpEntity(ContentTypes.`application/json`, msgJson.toString())))
    responseFuture
      .map {
        case response@HttpResponse(StatusCodes.OK, _, _, _) =>
          response.discardEntityBytes()
          Status.Success()
        case _ => Status.Failure(new RuntimeException("notification sending failed"))
      }
      .pipeTo(sender())
  }
}