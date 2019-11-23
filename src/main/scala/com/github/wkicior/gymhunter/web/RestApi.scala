package com.github.wkicior.gymhunter.web

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, RejectionHandler, Route}

import scala.language.postfixOps

class RestApi(actorSystem: ActorSystem, implicit val trainingHuntingSubscriptionEventStore: ActorRef) extends TrainingHuntingSubscriptionRestAPI {
  implicit def system: ActorSystem = actorSystem
  lazy val routes: Route = pathPrefix("api") {
    trainingHuntingSubscriptionRoutes
  }
}