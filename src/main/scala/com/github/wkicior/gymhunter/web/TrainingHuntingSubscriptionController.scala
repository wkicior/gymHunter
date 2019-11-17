package com.github.wkicior.gymhunter.web

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.app.Settings
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.{CreateTrainingHuntingSubscriptionCommand, DeleteTrainingHuntingSubscriptionCommand}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionProvider.GetAllTrainingHuntingSubscriptionsQuery
import com.github.wkicior.gymhunter.domain.subscription._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


trait TrainingHuntingSubscriptionController {
  implicit def system: ActorSystem
  implicit def trainingHuntingSubscriptionEventStore: ActorRef
  import com.github.wkicior.gymhunter.infrastructure.json.JsonProtocol._

  lazy val thsProvider: ActorRef = system.actorOf(TrainingHuntingSubscriptionProvider.props(trainingHuntingSubscriptionEventStore))
  lazy val thsCommandHandler: ActorRef = system.actorOf(TrainingHuntingSubscriptionCommandHandler.props(trainingHuntingSubscriptionEventStore))
  lazy val basicAuthenticator = BasicAuthenticator(Settings(system))

  def getAllTrainingHuntingSubscriptions: Future[Set[TrainingHuntingSubscription]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(thsProvider, GetAllTrainingHuntingSubscriptionsQuery()).mapTo[Set[TrainingHuntingSubscription]]
  }

  def saveTrainingHuntingSubscription(thsRequest: CreateTrainingHuntingSubscriptionCommand): Future[TrainingHuntingSubscription] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(thsCommandHandler, thsRequest).mapTo[TrainingHuntingSubscription]
  }

  def deleteTrainingHuntingSubscription(id: UUID): Future[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(thsCommandHandler, DeleteTrainingHuntingSubscriptionCommand(TrainingHuntingSubscriptionId(id))).mapTo[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]]
  }

  lazy val trainingHuntingSubscriptionRoutes: Route = pathPrefix("training-hunting-subscriptions") {
    authenticateBasic(realm = "gymhunter", basicAuthenticator.userPassAuthenticator) { _ =>
      concat(
        get {
          onComplete(getAllTrainingHuntingSubscriptions) {
            case Success(trainingHuntingSubscriptions) =>
              complete(StatusCodes.OK, trainingHuntingSubscriptions)
            case Failure(throwable) =>
              throwable match {
                case _ => complete(StatusCodes.InternalServerError, "Failed to get training hunting subscriptions.")
              }
          }
        },
        post {
          decodeRequest {
            entity(as[CreateTrainingHuntingSubscriptionCommand]) { trainingHuntingSubscriptionRequest =>
              complete(StatusCodes.Created, saveTrainingHuntingSubscription(trainingHuntingSubscriptionRequest))
            }
          }
        }
      ) ~ path(JavaUUID) { id =>
        delete {
          onComplete(deleteTrainingHuntingSubscription(id)) {
            case Success(trainingHuntingSubscription) =>
              trainingHuntingSubscription match {
                case Left(x) => complete(StatusCodes.NotFound, x.getMessage)
                case Right(x) => complete(StatusCodes.OK, x)
              }
            case Failure(throwable) =>
              throwable match {
                case _ => complete(StatusCodes.InternalServerError, "Failed to get training hunting subscriptions.")
              }
          }
        }
      }
    }
  }
}
