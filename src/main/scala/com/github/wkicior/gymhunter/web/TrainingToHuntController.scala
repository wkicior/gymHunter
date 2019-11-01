package com.github.wkicior.gymhunter.web

import java.time
import java.time.OffsetDateTime

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.{TrainingToHunt, TrainingToHuntProvider, TrainingToHuntRequest}
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntProvider.GetTrainingsToHunt
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntRepository.TrainingsToHunt

import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Failure, Success}


trait TrainingToHuntController {
  implicit def system: ActorSystem
  import com.github.wkicior.gymhunter.app.JsonProtocol._

  def createTrainingToHuntProvider(): ActorRef = system.actorOf(TrainingToHuntProvider.props)

  lazy val trainingToHuntProvider: ActorRef = createTrainingToHuntProvider()

  def getTrainingsToHunt(): Future[TrainingsToHunt] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingsToHunt()).mapTo[TrainingsToHunt]
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
            complete(StatusCodes.OK, trainingsToHunt.trainings)
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
