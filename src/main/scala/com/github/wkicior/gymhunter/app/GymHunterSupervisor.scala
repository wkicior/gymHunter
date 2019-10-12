package com.github.wkicior.gymhunter.app

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer

object GymHunterSupervisor {
  def props(): Props = Props(new GymHunterSupervisor)
}

class GymHunterSupervisor extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("GymHunter Application started")
  override def postStop(): Unit = log.info("GymHunter Application stopped")

  // No need to handle any messages
  override def receive = Actor.emptyBehavior
}