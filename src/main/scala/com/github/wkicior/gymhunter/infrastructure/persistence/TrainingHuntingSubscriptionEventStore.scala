package com.github.wkicior.gymhunter.infrastructure.persistence

import akka.actor.{ActorLogging, Props, _}
import akka.pattern.pipe
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionAggregate.{TrainingHuntingSubscriptionAdded, TrainingHuntingSubscriptionDeleted, TrainingHuntingSubscriptionEvent}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence._
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscriptionAggregate, TrainingHuntingSubscriptionId, TrainingHuntingSubscriptionNotFound}

import scala.concurrent.{Future, Promise}
import scala.language.postfixOps


object TrainingHuntingSubscriptionEventStore {
  def props: Props = Props[TrainingHuntingSubscriptionEventStore]

}

final case class State(trainingHuntingSubscriptions: Map[TrainingHuntingSubscriptionId, TrainingHuntingSubscriptionAggregate] = Map.empty) {
  def apply(): Set[TrainingHuntingSubscriptionAggregate] = trainingHuntingSubscriptions.values.toSet
  def apply(id: TrainingHuntingSubscriptionId): OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate] = trainingHuntingSubscriptions.get(id).toRight(TrainingHuntingSubscriptionNotFound(id))
  def +(event: TrainingHuntingSubscriptionEvent): State = {
    event match {
      case createEvent: TrainingHuntingSubscriptionAdded => State(trainingHuntingSubscriptions.updated(event.id, new TrainingHuntingSubscriptionAggregate(createEvent)))
      case deleteEvent: TrainingHuntingSubscriptionDeleted => State(trainingHuntingSubscriptions.removed(deleteEvent.id))
      case _ => State(trainingHuntingSubscriptions.updated(event.id, trainingHuntingSubscriptions(event.id)(event)))
    }
  }
}

class TrainingHuntingSubscriptionEventStore extends PersistentActor with ActorLogging {
  import context._

  override def persistenceId = "training-hunting-subscription-id-1"
  var state = State()

  val receiveRecover: Receive = {
    case event: TrainingHuntingSubscriptionEvent => state += event
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val snapShotInterval = 1000
  val receiveCommand: Receive = {
    case StoreEvents(id, events) =>
      Future.sequence(events.map(e => handleEvent(e)))
        .map(_ => state(id)) pipeTo sender()
    case GetAllTrainingHuntingSubscriptions() =>
      sender() ! state().map(a => a())
    case GetTrainingHuntingSubscriptionAggregate(id) =>
      sender() ! state(id)
    case GetTrainingHuntingSubscription(id) =>
      sender() ! state(id).map(x => x())
    case x => log.error(s"Unrecognized message $x")
  }

  private def handleEvent[E <: TrainingHuntingSubscriptionEvent](e: => E): Future[E] = {
    val p = Promise[E]
    persist(e) { event =>
      state += event
      p.success(e)
      context.system.eventStream.publish(event)
      if (lastSequenceNr != 0 && lastSequenceNr % 1000 == 0) saveSnapshot(state)
    }
    p.future
  }
}