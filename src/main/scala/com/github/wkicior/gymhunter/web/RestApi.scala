package com.github.wkicior.gymhunter.web

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.language.postfixOps

class RestApi(actorSystem: ActorSystem, trainingToHuntRepositoryRef: ActorRef) extends TrainingToHuntController {
  implicit def system = actorSystem
  implicit def trainingToHuntRepository: ActorRef = trainingToHuntRepositoryRef
  lazy val routes: Route = pathPrefix("api") {
    trainingToHuntRoutes
  }
}