package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.training.VacantTrainingManager.ProcessVacantTraining
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionProvider}
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotificationSender

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps


object TrainingHunter {
  private [gymhunter] def props(thsEventStore: ActorRef, trainingFetcher: ActorRef, ifttNotifiationSender: ActorRef): Props = Props(new TrainingHunter(TrainingHuntingSubscriptionProvider.props(thsEventStore), trainingFetcher, VacantTrainingManager.props(thsEventStore, ifttNotifiationSender)))
  private [training] def props(trainingHunterProps: Props, trainingFetcher: ActorRef, vacantTrainingManagerProps: Props): Props = Props(
    new TrainingHunter(trainingHunterProps, trainingFetcher, vacantTrainingManagerProps)
  )
  final case class Hunt()
}

class TrainingHunter(thsProviderProps: Props, trainingFetcher: ActorRef, vacantTrainingManagerProps: Props) extends Actor with ActorLogging {
  import TrainingHunter._
  import TrainingHuntingSubscriptionProvider._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val trainingHuntingSubscriptionProvider: ActorRef = context.actorOf(thsProviderProps, "trainingHuntingSubscriptionProvider")
  val vacantTrainingManager: ActorRef = context.actorOf(RoundRobinPool(5).props(vacantTrainingManagerProps), "vacantTrainingManager")

  def receive: PartialFunction[Any, Unit] = {
    case Hunt() =>
      log.info("hunting begins...")
      getSubscriptions
        .map(trainings => trainings.map(training => getTraining(training.externalSystemId)))
        .flatMap(trainingFutures => Future.sequence(trainingFutures))
        .foreach(trainings => {
          trainings
            .filter(training => training.canBeBooked)
            .foreach(training =>  vacantTrainingManager ! ProcessVacantTraining(training))
        })
    case _ =>
      log.error("unrecognized message")
  }

  private def getSubscriptions: Future[Set[TrainingHuntingSubscription]] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    ask(trainingHuntingSubscriptionProvider, GetActiveTrainingHuntingSubscriptionsQuery()).mapTo[Set[TrainingHuntingSubscription]]
  }

  private def getTraining(id: Long): Future[Training] = {
    implicit val timeout: Timeout = Timeout(10 seconds)
    ask(trainingFetcher, GetTraining(id)).mapTo[Training]
  }
}