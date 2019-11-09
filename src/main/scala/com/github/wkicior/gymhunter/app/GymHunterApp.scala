package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.github.wkicior.gymhunter.infrastructure.gymsteer.GymsteerTrainingFetcher
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotificationSender
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingHuntingSubscriptionEventStore
import com.github.wkicior.gymhunter.web.RestApi
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.postfixOps

object GymHunterApp extends App {
  implicit val system: ActorSystem = ActorSystem("GymHunter")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val log = Logging.getLogger(system, this)

  log.info("Initializing GymHunter Supervisor Scheduler...")
  val scheduler = QuartzSchedulerExtension(system)
  val trainingHuntingSubscriptionEventStore = system.actorOf(TrainingHuntingSubscriptionEventStore.props, "TrainingHuntingSubscriptionEventStore")
  val gymsteerTrainingFetcher = system.actorOf(RoundRobinPool(8).props(GymsteerTrainingFetcher.props), "GymsteerTrainingFetcher")
  val ifttNotificationSender = system.actorOf(IFTTNotificationSender.props, "IFTTNotificationSender")
  val supervisor: ActorRef = system.actorOf(GymHunterSupervisor.props(trainingHuntingSubscriptionEventStore, gymsteerTrainingFetcher, ifttNotificationSender), "GymHunterSupervisor")
  scheduler.schedule("GymHunterSupervisorScheduler", supervisor, RunGymHunting())

  val api = new RestApi(system, trainingHuntingSubscriptionEventStore).routes
  Http().bindAndHandle(api, "localhost", 8080)
  log.info("Starting the HTTP server at 8080")
  Await.result(system.whenTerminated, Duration.Inf)
}
