//#full-example
package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting

import scala.io.StdIn
import scala.language.postfixOps

object GymHunterApp extends App {
  val system: ActorSystem = ActorSystem("GymHunter")
    try {
    val supervisor: ActorRef = system.actorOf(GymHunterSupervisor.props, "GymHunterSupervisor")
    supervisor ! RunGymHunting()
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}
