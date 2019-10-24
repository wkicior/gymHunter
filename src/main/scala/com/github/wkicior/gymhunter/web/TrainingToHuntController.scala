package com.github.wkicior.gymhunter.web

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object TrainingToHuntController extends TrainingToHuntController {


}

trait TrainingToHuntController {
  lazy val trainingToHuntRoutes: Route = pathPrefix("trainings-to-hunt") {
    get {
      complete {
        "Hello World from GymHunter"
      }
    }
  }
}
