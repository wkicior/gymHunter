package com.github.wkicior.gymhunter.web

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntCommandHandler.{CreateTrainingToHuntCommand, DeleteTrainingToHuntCommand}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntId.OptionalTrainingToHunt
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntProvider.GetTrainingsToHuntQuery
import com.github.wkicior.gymhunter.domain.tohunt._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


trait TrainingToHuntController {
  implicit def system: ActorSystem
  implicit def trainingToHuntEventStore: ActorRef
  import com.github.wkicior.gymhunter.app.JsonProtocol._

  lazy val trainingToHuntProvider: ActorRef = system.actorOf(TrainingToHuntProvider.props(trainingToHuntEventStore))
  lazy val trainingToHuntCommandHandler: ActorRef = system.actorOf(TrainingToHuntCommandHandler.props(trainingToHuntEventStore))

  def getTrainingsToHunt: Future[Set[TrainingToHunt]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingsToHuntQuery()).mapTo[Set[TrainingToHunt]]
  }

  def saveTrainingToHunt(trainingToHuntRequest: CreateTrainingToHuntCommand): Future[TrainingToHunt] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntCommandHandler, trainingToHuntRequest).mapTo[TrainingToHunt]
  }

  def deleteTrainingToHunt(id: UUID): Future[OptionalTrainingToHunt[TrainingToHunt]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntCommandHandler, DeleteTrainingToHuntCommand(TrainingToHuntId(id))).mapTo[OptionalTrainingToHunt[TrainingToHunt]]
  }

  lazy val trainingToHuntRoutes: Route = pathPrefix("trainings-to-hunt") {
    concat(
      get {
        onComplete(getTrainingsToHunt) {
          case Success(trainingsToHunt) =>
            complete(StatusCodes.OK, trainingsToHunt)
          case Failure(throwable) =>
            throwable match {
              case _ => complete(StatusCodes.InternalServerError, "Failed to get trainings to hunt.")
            }
        }
      },
      post {
        decodeRequest {
          entity(as[CreateTrainingToHuntCommand]) { trainingToHuntRequest =>
            complete(StatusCodes.Created, saveTrainingToHunt(trainingToHuntRequest))
          }
        }
      }
    ) ~path(JavaUUID) { id =>
      delete {
        onComplete(deleteTrainingToHunt(id)) {
          case Success(trainingToHunt) =>
            trainingToHunt match {
              case Left(x) => complete(StatusCodes.NotFound, x.getMessage)
              case Right(x) => complete(StatusCodes.OK, x)
            }
          case Failure(throwable) =>
            throwable match {
              case _ => complete(StatusCodes.InternalServerError, "Failed to get trainings to hunt.")
            }
        }
      }
    }
  }
}
