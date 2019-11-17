package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionProvider}
import com.github.wkicior.gymhunter.domain.training.VacantTrainingManager.ProcessVacantTraining

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps


object TrainingHunter {
  private [gymhunter] def props(thsEventStore: ActorRef, gymsteerProxy: ActorRef, ifttNotifiationSender: ActorRef): Props = Props(
    new TrainingHunter(TrainingHuntingSubscriptionProvider.props(thsEventStore),
      gymsteerProxy,
      VacantTrainingManager.props(thsEventStore, ifttNotifiationSender, gymsteerProxy)))
  private [training] def props(trainingHunterProps: Props, trainingFetcher: ActorRef, vacantTrainingManagerProps: Props): Props = Props(
    new TrainingHunter(trainingHunterProps, trainingFetcher, vacantTrainingManagerProps)
  )
  final case class Hunt()
}

class TrainingHunter(thsProviderProps: Props, gymsteerProxy: ActorRef, vacantTrainingManagerProps: Props) extends Actor with ActorLogging {
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
            .filter(t => t.isDefined)
            .map(t => t.get)
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

  private def getTraining(id: Long): Future[Option[Training]] = {
    implicit val timeout: Timeout = Timeout(10 seconds)
    ask(gymsteerProxy, GetTraining(id)).mapTo[Training].map(t => Some(t)).recover {
      case ex: Exception =>
        log.warning(s"error on getting training $id ($ex), ignoring the training")
        Option.empty[Training]
    }
  }
}