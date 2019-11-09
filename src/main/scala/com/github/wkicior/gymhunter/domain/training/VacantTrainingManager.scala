package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.notification.{Notification, SlotsAvailableNotificationSender}
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionProvider.GetTrainingHuntingSubscriptionsByTrainingIdQuery
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionProvider}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

private [training] object VacantTrainingManager {
  def props(thsEventStore: ActorRef, ifttNotificationSender: ActorRef): Props = Props(new VacantTrainingManager(TrainingHuntingSubscriptionProvider.props(thsEventStore), SlotsAvailableNotificationSender.props(ifttNotificationSender)))
  def props(thsProviderProps: Props, slotsAvailableNotificationSenderProps: Props): Props = Props(new VacantTrainingManager(thsProviderProps, slotsAvailableNotificationSenderProps)
  )
  final case class ProcessVacantTraining(training: Training)
}

class VacantTrainingManager(thsProviderProps: Props, slotsAvailableNotificationSenderProps: Props) extends Actor with ActorLogging {
  import VacantTrainingManager._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val thsProvider: ActorRef = context.actorOf(thsProviderProps)
  val slotsAvailableNotificationSender: ActorRef = context.actorOf(slotsAvailableNotificationSenderProps)


  def receive: PartialFunction[Any, Unit] = {
    case ProcessVacantTraining(training: Training) =>
      log.info(s"slots available on $training")
      getSubscriptions(training.id)
        .foreach(trainings =>
          trainings
            .foreach(t => slotsAvailableNotificationSender ! SendNotification(Notification(training.start_date, t.clubId, t.id))))
  }

  private def getSubscriptions(trainingId: Long): Future[Set[TrainingHuntingSubscription]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(thsProvider, GetTrainingHuntingSubscriptionsByTrainingIdQuery(trainingId)).mapTo[Set[TrainingHuntingSubscription]]
  }
}