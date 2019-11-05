package com.github.wkicior.gymhunter.infrastructure.persistence

import akka.actor.{ActorLogging, Props, _}
import akka.pattern.pipe
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntAggregate.{TrainingToHuntAdded, TrainingToHuntDeleted, TrainingToHuntEvent}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntPersistence._
import com.github.wkicior.gymhunter.domain.training.tohunt.{TrainingToHuntAggregate, TrainingToHuntId}

import scala.concurrent.{Future, Promise}
import scala.language.postfixOps


object TrainingToHuntEventStore {
  def props: Props = Props[TrainingToHuntEventStore]

}

final case class State(trainingsToHunt: Map[TrainingToHuntId, TrainingToHuntAggregate] = Map.empty) {
  def apply(): Set[TrainingToHuntAggregate] = trainingsToHunt.values.toSet
  def apply(id: TrainingToHuntId): OptionalTrainingToHunt[TrainingToHuntAggregate] = trainingsToHunt.get(id).toRight(TrainingToHuntNotFound(id))
  def +(event: TrainingToHuntEvent): State = {
    event match {
      case createEvent: TrainingToHuntAdded => State(trainingsToHunt.updated(event.id, new TrainingToHuntAggregate(createEvent)))
      case deleteEvent: TrainingToHuntDeleted => State(trainingsToHunt.removed(deleteEvent.id))
      case _ => State(trainingsToHunt.updated(event.id, trainingsToHunt(event.id)(event)))
    }
  }
}

class TrainingToHuntEventStore extends PersistentActor with ActorLogging {
  import TrainingToHuntEventStore._
  import context._

  override def persistenceId = "training-to-hunt-id-1"
  var state = State()

  val receiveRecover: Receive = {
    case event: TrainingToHuntEvent => state += event
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val snapShotInterval = 1000
  val receiveCommand: Receive = {
    case StoreEvents(id, events) =>
      Future.sequence(events.map(e => handleEvent(e)))
        .map(_ => state(id)) pipeTo sender()
    case GetAllTrainingsToHunt() =>
      sender() ! state().map(a => a())
    case GetTrainingToHuntAggregate(id) =>
      sender() ! state(id)
    case GetTrainingToHunt(id) =>
      sender() ! state(id).map(x => x())
    case x => log.error(s"Unrecognized message $x")
  }

  private def handleEvent[E <: TrainingToHuntEvent](e: => E): Future[E] = {
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