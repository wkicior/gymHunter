package com.github.wkicior.gymhunter.web

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntProvider.GetTrainingsToHunt
import com.github.wkicior.gymhunter.domain.training.{TrainingToHunt, TrainingToHuntProvider, TrainingToHuntRequest}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


trait TrainingToHuntController {
  implicit def system: ActorSystem
  implicit def trainingToHuntRepository: ActorRef
  import com.github.wkicior.gymhunter.app.JsonProtocol._

  def createTrainingToHuntProvider(): ActorRef = system.actorOf(TrainingToHuntProvider.props(trainingToHuntRepository))

  lazy val trainingToHuntProvider: ActorRef = createTrainingToHuntProvider()

  def getTrainingsToHunt(): Future[Set[TrainingToHunt]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingsToHunt()).mapTo[Set[TrainingToHunt]]
  }

  def saveTrainingToHunt(trainingToHuntRequest: TrainingToHuntRequest): Future[TrainingToHunt] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, trainingToHuntRequest).mapTo[TrainingToHunt]
  }

  lazy val trainingToHuntRoutes: Route = pathPrefix("trainings-to-hunt") {
    concat(
      get {
        onComplete(getTrainingsToHunt()) {
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
          entity(as[TrainingToHuntRequest]) { trainingToHuntRequest =>
            complete(StatusCodes.Created, saveTrainingToHunt(trainingToHuntRequest))
          }
        }
      }
    )
  }
}
