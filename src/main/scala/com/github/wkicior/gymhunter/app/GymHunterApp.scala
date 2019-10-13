//#full-example
package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.TrainingFetcher.GetTraining

import scala.language.postfixOps
import com.github.wkicior.gymhunter.domain.training.{Training, TrainingFetcher, TrainingResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.StdIn

object AkkaQuickstart extends App {
  val system: ActorSystem = ActorSystem("GymHunter")
  val trainingFetcher: ActorRef = system.actorOf(TrainingFetcher.props, "trainingFetcher")

  implicit val timeout = Timeout(5 seconds)

  val future: Future[Training] = ask(trainingFetcher, GetTraining(550633)).mapTo[Training]
  val result = Await.result(future, 5 second)
  println("Got response, body: " + result)
  try StdIn.readLine()
}
