package com.github.wkicior.gymhunter.web

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntProvider
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntProvider.GetTrainingsToHunt
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntRepository.TrackedTrainingIds
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Failure, Success}


trait TrainingToHuntController extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def system: ActorSystem

  def createTrainingToHuntProvider(): ActorRef = system.actorOf(TrainingToHuntProvider.props)

  lazy val trainingToHuntProvider: ActorRef = createTrainingToHuntProvider()

  def getTrainings(): Future[TrackedTrainingIds] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingsToHunt()).mapTo[TrackedTrainingIds]
  }

  lazy val trainingToHuntRoutes: Route = pathPrefix("trainings-to-hunt") {
    get {
      onComplete(getTrainings()) {
        case Success(trainings) =>
          complete(StatusCodes.OK, trainings.ids)
        case Failure(throwable) =>
          throwable match {
            case _ => complete(StatusCodes.InternalServerError, "Failed to get trainings to hunt.")
          }
      }
    }
  }
}
