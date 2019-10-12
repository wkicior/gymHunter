//#full-example
package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.app.domain.TrainingFetcher.GetTraining

import scala.language.postfixOps
import com.github.wkicior.gymhunter.app.domain.{TrainingFetcher, TrainingResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.StdIn

object AkkaQuickstart extends App {
  val system: ActorSystem = ActorSystem("GymHunter")
  val trainingFetcher: ActorRef = system.actorOf(TrainingFetcher.props, "trainingFetcher")

  implicit val timeout = Timeout(5 seconds)

  val future2: Future[TrainingResponse] = ask(trainingFetcher, GetTraining(550633)).mapTo[TrainingResponse]
  val result2 = Await.result(future2, 5 second)
  println("Got response, body: " + result2)
  try StdIn.readLine()


}
