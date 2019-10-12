package com.github.wkicior.gymhunter.app.domain
import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wkicior.gymhunter.app.http.{HttpGetter, Training, TrainingResponse}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContext, Future}
import com.github.wkicior.gymhunter.app.http.HttpGetter.{Get, GetResponse}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}


object TrainingFetcher {
  def props(httpGetter: ActorRef): Props = Props(new TrainingFetcher(httpGetter))
  final case class GetTraining(id: Long)
}

class TrainingFetcher(httpGetter: ActorRef) extends Actor with ActorLogging {
  import TrainingFetcher._
  implicit val timeout = Timeout(5 seconds)

  override def postStop(): Unit = log.info("TrainingFetcher stopped")


  def receive = {
    case GetTraining(id) =>
      log.info("I'm about to get training" + id)
     // val future = httpGetter ? Get("https://api.gymsteer.com/api/clubs/8/trainings/550633")
      //val result = Await.result(future, timeout.duration).asInstanceOf[GetResponse]

      val future2: Future[TrainingResponse] = ask(httpGetter, Get("https://api.gymsteer.com/api/clubs/8/trainings/550633")).mapTo[TrainingResponse]
      val result2 = Await.result(future2, 5 second)
      log.info("Got response, body: " + result2)

      //httpGetter ! Get("https://api.gymsteer.com/api/clubs/8/trainings/550633")
    case _ =>
      log.info("unrecognized message")

  }
}