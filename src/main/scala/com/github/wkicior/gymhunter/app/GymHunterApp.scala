//#full-example
package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import com.github.wkicior.gymhunter.app.domain.TrainingFetcher
import com.github.wkicior.gymhunter.app.domain.TrainingFetcher.GetTraining
import com.github.wkicior.gymhunter.app.http.HttpGetter
import com.github.wkicior.gymhunter.app.http.HttpGetter.Get

import scala.io.StdIn

object AkkaQuickstart extends App {
  val system: ActorSystem = ActorSystem("GymHunter")
  val httpGetter: ActorRef = system.actorOf(HttpGetter.props, "httpGetter")
  val trainingFetcher: ActorRef = system.actorOf(TrainingFetcher.props(httpGetter), "trainingFetcher")

  trainingFetcher ! GetTraining(550633)
  try StdIn.readLine()


}
