package com.github.wkicior.gymhunter.app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wkicior.gymhunter.app.GymHunterSupervisor.RunGymHunting
import com.github.wkicior.gymhunter.domain.subscription.TrainingSlotsAvailableNotificationSentEventHandler
import com.github.wkicior.gymhunter.domain.training.TrainingHunter
import com.github.wkicior.gymhunter.domain.training.TrainingHunter.Hunt

object GymHunterSupervisor {
  def props(trainingHuntingSubscriptionEventStore: ActorRef, gymsteerProxy: ActorRef, ifttNotificationSender: ActorRef): Props = Props(
    new GymHunterSupervisor(trainingHuntingSubscriptionEventStore, gymsteerProxy, ifttNotificationSender))
  final case class RunGymHunting()
}

class GymHunterSupervisor(trainingHuntingSubscriptionEventStore: ActorRef, gymsteerProxy: ActorRef, ifttNotificationSender: ActorRef) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("GymHunter Application started")
  override def postStop(): Unit = log.info("GymHunter Application stopped")

  val trainingHunter: ActorRef = context.actorOf(TrainingHunter.props(trainingHuntingSubscriptionEventStore, gymsteerProxy, ifttNotificationSender), "trainingHunter")
  val trainingSlotsAvailableNotificationHandler: ActorRef = context.actorOf(
    TrainingSlotsAvailableNotificationSentEventHandler.props(trainingHuntingSubscriptionEventStore), "trainingSlotsAvailableNotificationHandler")

  def receive: PartialFunction[Any, Unit] = {
    case RunGymHunting() => trainingHunter ! Hunt()
    case x => log.error(s"Unrecognized message: $x")
  }
}