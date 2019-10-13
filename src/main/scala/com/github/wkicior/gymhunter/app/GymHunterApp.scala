//#full-example
package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import com.github.wkicior.gymhunter.domain.training.TrainingHunter
import com.github.wkicior.gymhunter.domain.training.TrainingHunter.Hunt

import scala.io.StdIn
import scala.language.postfixOps

object AkkaQuickstart extends App {
  val system: ActorSystem = ActorSystem("GymHunter")
  val trainingHunter: ActorRef = system.actorOf(TrainingHunter.props, "trainingFetcher")

  trainingHunter ! Hunt()
  try StdIn.readLine()
}
