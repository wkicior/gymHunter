package com.github.wkicior.gymhunter.web

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route


import scala.language.postfixOps

class RestApi(actorSystem: ActorSystem) extends TrainingToHuntController {
  implicit def system = actorSystem
  lazy val routes: Route = pathPrefix("api") {
    trainingToHuntRoutes
  }
}