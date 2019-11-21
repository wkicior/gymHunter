package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.notification.{SlotsAvailableNotification, SlotsAvailableNotificationSender}
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionProvider.GetTrainingHuntingSubscriptionsByTrainingIdQuery
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionProvider}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

private [training] object VacantTrainingManager {
  def props(thsEventStore: ActorRef, ifttNotificationSender: ActorRef, gymsteerProxy: ActorRef): Props = Props(
    new VacantTrainingManager(
      TrainingHuntingSubscriptionProvider.props(thsEventStore),
      SlotsAvailableNotificationSender.props(ifttNotificationSender),
      TrainingBooker.props(gymsteerProxy)))
  def props(thsProviderProps: Props, slotsAvailableNotificationSenderProps: Props, trainingBookerProps: Props): Props = Props(
    new VacantTrainingManager(thsProviderProps, slotsAvailableNotificationSenderProps, trainingBookerProps)
  )
  final case class ProcessVacantTraining(training: Training)
}

class VacantTrainingManager(thsProviderProps: Props, slotsAvailableNotificationSenderProps: Props, trainingBookerProps: Props) extends Actor with ActorLogging {
  import VacantTrainingManager._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val thsProvider: ActorRef = context.actorOf(thsProviderProps)
  val slotsAvailableNotificationSender: ActorRef = context.actorOf(slotsAvailableNotificationSenderProps)
  val trainingBooker: ActorRef = context.actorOf(trainingBookerProps)

  def receive: PartialFunction[Any, Unit] = {
    case ProcessVacantTraining(training: Training) =>
      log.info(s"slots available on $training")
      getSubscriptions(training.id)
        .foreach(subscriptions =>
          subscriptions
            .foreach(ths => performAutoBookingOrSendNotification(training, ths)))
  }

  private def performAutoBookingOrSendNotification(training: Training, ths: TrainingHuntingSubscription) = {
    if (ths.canBeAutoBooked) {
      trainingBooker ! TrainingBooker.BookTraining(ths, training)
    } else {
      slotsAvailableNotificationSender ! SendNotification(SlotsAvailableNotification(training.start_date, ths.clubId, ths.id))
    }
  }

  private def getSubscriptions(trainingId: Long): Future[Set[TrainingHuntingSubscription]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(thsProvider, GetTrainingHuntingSubscriptionsByTrainingIdQuery(trainingId)).mapTo[Set[TrainingHuntingSubscription]]
  }
}