package com.github.wkicior.gymhunter.infrastructure.gymsteer

import akka.actor.Status.Status
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.{BookTraining, GetTraining, Training}
import com.github.wkicior.gymhunter.infrastructure.gymsteer.auth.GymsteerTokenProvider
import com.github.wkicior.gymhunter.infrastructure.gymsteer.auth.GymsteerTokenProvider.GetToken
import com.github.wkicior.gymhunter.infrastructure.gymsteer.training.{GymsteerTrainingBooker, GymsteerTrainingFetcher}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object GymsteerProxy {
  def props(hostname: String, username: String, password: String): Props = Props(new GymsteerProxy(GymsteerTrainingFetcher.props(hostname), GymsteerTokenProvider.props(hostname), GymsteerTrainingBooker.props(hostname), username, password))
  def props(gymsteerTrainingFetcherProps: Props, tokenProviderProps: Props, trainingBookerProps: Props, username: String, password: String): Props = Props(new GymsteerProxy(gymsteerTrainingFetcherProps, tokenProviderProps, trainingBookerProps, username, password))
}

class GymsteerProxy(trainingFetcherProps: Props, tokenProviderProps: Props, trainingBookerProps: Props, username: String, password: String) extends Actor with ActorLogging {
  import context.dispatcher
  implicit val system: ActorSystem = context.system

  val trainingFetcher: ActorRef = system.actorOf(trainingFetcherProps)
  val tokenProvider: ActorRef = system.actorOf(tokenProviderProps)
  val trainingBooker: ActorRef = system.actorOf(trainingBookerProps)

  def bookTraining(id: Long, authToken: String) = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingBooker, GymsteerTrainingBooker.BookTraining(id, authToken))
  }

  def receive: PartialFunction[Any, Unit] = {
    case gt@GetTraining(_) =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(trainingFetcher, gt).mapTo[Training].pipeTo(sender())
    case bt@BookTraining(id) =>
      implicit val timeout: Timeout = Timeout(5 seconds)
      ask(tokenProvider, GetToken(username, password)).mapTo[String]
        .flatMap(token => bookTraining(id, token))
        .pipeTo(sender())
    case _ =>
      log.error("unrecognized message")
  }
}