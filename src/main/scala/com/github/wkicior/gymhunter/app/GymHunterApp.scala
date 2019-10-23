package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

import scala.concurrent.Await
import akka.http.scaladsl.server.Directives._

import scala.concurrent.duration.Duration
import scala.language.postfixOps

object GymHunterApp extends App {
  implicit val system: ActorSystem = ActorSystem("GymHunter")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val log = Logging.getLogger(system, this)

  log.info("Initializing GymHunter Supervisor Scheduler...")
  val scheduler = QuartzSchedulerExtension(system)
  val supervisor: ActorRef = system.actorOf(GymHunterSupervisor.props, "GymHunterSupervisor")
  scheduler.schedule("GymHunterSupervisorScheduler", supervisor, RunGymHunting())



 //TODO: Move this to controller. See: https://www.codersbistro.com/blog/restful-apis-with-akka-http/
  lazy val apiRoutes: Route = pathPrefix("api") {
    get {
      complete {
        "Hello World from GymHunter"
      }
    }
  }

  Http().bindAndHandle(apiRoutes, "localhost", 8080)
  log.info("Starting the HTTP server at 8080")
  Await.result(system.whenTerminated, Duration.Inf)
}
