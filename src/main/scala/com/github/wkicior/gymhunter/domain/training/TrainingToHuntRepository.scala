package com.github.wkicior.gymhunter.domain.training


import akka.actor.{ActorLogging, Props, _}
import akka.pattern.pipe
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.github.wkicior.gymhunter.domain.training.TrainingToHunt.{TrainingToHuntAdded, TrainingToHuntEvent}
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntRepository.{OptionalTrainingToHunt, TrainingToHuntNotFound}

import scala.concurrent.{Future, Promise}
import scala.language.postfixOps


object TrainingToHuntRepository {
  def props: Props = Props[TrainingToHuntRepository]
  final case class GetAllTrainingsToHunt()
  final case class StoreEvents(id: TrainingToHuntId, events: List[TrainingToHuntEvent])



  type OptionalTrainingToHunt[+A] = Either[TrainingToHuntNotFound, A]
  final case class TrainingToHuntNotFound(id: TrainingToHuntId) extends RuntimeException(s"Training to hunt not found with id $id")
}


final case class State(trainingsToHunt: Map[TrainingToHuntId, TrainingToHunt] = Map.empty) {
  def apply(): Set[TrainingToHunt] = trainingsToHunt.values.toSet
  def apply(id: TrainingToHuntId): OptionalTrainingToHunt[TrainingToHunt] = trainingsToHunt.get(id).toRight(TrainingToHuntNotFound(id))
  def +(event: TrainingToHuntEvent): State = {
    event match {
      case createEvent: TrainingToHuntAdded => State(trainingsToHunt.updated(event.id, new TrainingToHunt(createEvent)))
      case _ => State(trainingsToHunt.updated(event.id, trainingsToHunt(event.id)(event)))
    }
  }
}

class TrainingToHuntRepository extends PersistentActor with ActorLogging {
  import TrainingToHuntRepository._
  import context._

  override def persistenceId = "training-to-hunt-id-1"
  var state = State()

  val receiveRecover: Receive = {
    case event: TrainingToHuntEvent => state += event
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val snapShotInterval = 1000
  val receiveCommand: Receive = {
//    case AddTrainingToHunt(tr) =>
//      handleEvent(TrainingToHuntAdded(tr.id, tr)) pipeTo sender()
//      ()
    case StoreEvents(id, events) =>
      val results = events.map(e => handleEvent(e))
      Future.sequence(results).map(_ => state(id).toOption.get) pipeTo sender()
      //sender() ! state(id).toOption.get

    case GetAllTrainingsToHunt() =>
      sender() ! state()
    case "print" => println(state)
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