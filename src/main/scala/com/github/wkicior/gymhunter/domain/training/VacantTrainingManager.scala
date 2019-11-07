package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntProvider.GetTrainingsToHuntByTrainingIdQuery
import com.github.wkicior.gymhunter.domain.tohunt.{TrainingToHunt, TrainingToHuntProvider}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

private [training] object VacantTrainingManager {
  def props(trainingToHuntEventStore: ActorRef): Props = Props(new VacantTrainingManager(TrainingToHuntProvider.props(trainingToHuntEventStore), SlotsAvailableNotificationSender.props()))
  def props(trainingToHuntProviderProps: Props, slotsAvailableNotificationSenderProps: Props): Props = Props(new VacantTrainingManager(trainingToHuntProviderProps, slotsAvailableNotificationSenderProps)
  )
  final case class ProcessVacantTraining(training: Training)
}

class VacantTrainingManager(trainingToHuntProviderProps: Props, slotsAvailableNotificationSender: Props) extends Actor with ActorLogging {
  import VacantTrainingManager._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val trainingToHuntProvider: ActorRef = context.actorOf(trainingToHuntProviderProps)
  val trainingToHuntCommandHandler: ActorRef = context.actorOf(slotsAvailableNotificationSender)


  def receive: PartialFunction[Any, Unit] = {
    case ProcessVacantTraining(training: Training) =>
      log.info(s"slots available on $training")
      getTrainingsToHunt(training.id)
        .foreach(trainings =>
          trainings
            .foreach(t => trainingToHuntCommandHandler ! SendNotification(t, training)))
  }

  private def getTrainingsToHunt(trainingId: Long): Future[Set[TrainingToHunt]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingToHuntProvider, GetTrainingsToHuntByTrainingIdQuery(trainingId)).mapTo[Set[TrainingToHunt]]
  }
}