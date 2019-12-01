package com.github.wkicior.gymhunter.app

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.github.wkicior.gymhunter.infrastructure.gymsteer.GymsteerProxy
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
  val settings: SettingsImpl = Settings(system)

  val trainingHuntingSubscriptionEventStore = system.actorOf(TrainingHuntingSubscriptionEventStore.props, "TrainingHuntingSubscriptionEventStore")
  val gymsteerProxy = system.actorOf(RoundRobinPool(8).props(GymsteerProxy.props(settings.gymsteerHost, settings.gymsteerUsername, settings.gymsteerPassword)), "GymsteerProxy")
  val ifttNotificationSender = system.actorOf(IFTTNotificationSender.props, "IFTTNotificationSender")
  val supervisor: ActorRef = system.actorOf(GymHunterSupervisor.props(trainingHuntingSubscriptionEventStore, gymsteerProxy, ifttNotificationSender), "GymHunterSupervisor")
  scheduler.schedule("GymHunterSupervisorScheduler", supervisor, RunGymHunting())

  val api = new RestApi(system, trainingHuntingSubscriptionEventStore).routes
  Http().bindAndHandle(api, "0.0.0.0", 8080)
  log.info("Starting the HTTP server at 8080")
  val httpsContext = HttpsContext(settings)
  if (httpsContext.isAvailable) {
    Http().bindAndHandle(api, "0.0.0.0", 8443, connectionContext = httpsContext())
    log.info("Starting the HTTPS server at 8443")
  } else {
    log.warning("HTTPS context is unavailable. Please provide correct values for gymhunter.keystore.password and gymhunter.keystore.path parameters")
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
