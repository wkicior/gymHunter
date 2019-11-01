package com.github.wkicior.gymhunter.domain.training


import java.util.UUID

import akka.actor.{ActorLogging, Props, _}
import akka.persistence.{PersistentActor, _}

import scala.language.postfixOps


object TrainingToHuntRepository {
  def props: Props = Props[TrainingToHuntRepository]
  final case class GetAllTrainingsToHunt()
  final case class TrainingsToHunt(trainings: Set[TrainingToHunt])
  final case class AddTrainingToHunt(training: TrainingToHuntRequest)
}


case class State(events: List[TrainingToHunt] = Nil) {
  def updated(evt: TrainingToHunt): State = copy(evt :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}

class TrainingToHuntRepository extends PersistentActor with ActorLogging {
  import TrainingToHuntRepository._

  override def persistenceId = "training-to-hunt-id-1"
  var state = State()

  def updateState(event: TrainingToHunt): Unit =
    state = state.updated(event)

  def numEvents =
    state.size

  val receiveRecover: Receive = {
    case evt: TrainingToHunt => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val snapShotInterval = 1000
  val receiveCommand: Receive = {
    case AddTrainingToHunt(tr) =>
      val trainingToHunt = TrainingToHunt(UUID.randomUUID().toString, tr.externalSystemId, tr.clubId, tr.huntingEndTime)
      persist(trainingToHunt) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(state)
      }
      sender() ! trainingToHunt
    case GetAllTrainingsToHunt() =>
      sender() ! TrainingsToHunt(state.events.toSet)
    case "print" => println(state)
  }
}