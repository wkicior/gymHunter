package com.github.wkicior.gymhunter.domain.notification


import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntAggregate.TrainingToHuntNotificationSent
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntId.OptionalTrainingToHunt
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntProvider.GetTrainingToHuntQuery
import com.github.wkicior.gymhunter.domain.tohunt.{TrainingToHunt, TrainingToHuntId, TrainingToHuntProvider}
import com.github.wkicior.gymhunter.domain.training.Training

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

object IFTTNotifier {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new IFTTNotifier(TrainingToHuntProvider.props(trainingToHuntEventStore)))
  final case class Notify(training: Training, trainingToHunt: TrainingToHunt)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

}

class IFTTNotifier(trainingToHuntProviderProps: Props) extends Actor with ActorLogging {

  override def preStart(): Unit = context.system.eventStream.subscribe(self, classOf[TrainingToHuntNotificationSent])

  val trainingToHuntProvider: ActorRef = context.actorOf(trainingToHuntProviderProps)


  def receive: PartialFunction[Any, Unit] = {
    case TrainingToHuntNotificationSent(id) =>
      log.info(s"will send IFTT notification for $id")
      getTrainingToHunt(id)

//        .foreach(optionalTrainingToHunt => optionalTrainingToHunt {
//        case ot@Left(ex) => log.error(s"Could not load trainingToHunt ${ex}")
//        case Right(trainingToHunt) => log.info(s"will send IFTT notification for $id")
//      })
  }

  private def getTrainingToHunt(id: TrainingToHuntId): Future[OptionalTrainingToHunt[TrainingToHunt]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingToHuntQuery(id)).mapTo[OptionalTrainingToHunt[TrainingToHunt]]
  }
}