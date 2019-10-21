package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

import scala.language.postfixOps

object GymHunterApp extends App {
  val system: ActorSystem = ActorSystem("GymHunter")
  val scheduler = QuartzSchedulerExtension(system)
  val supervisor: ActorRef = system.actorOf(GymHunterSupervisor.props, "GymHunterSupervisor")
  scheduler.schedule("GymHunterSupervisorScheduler", supervisor, RunGymHunting())

}
