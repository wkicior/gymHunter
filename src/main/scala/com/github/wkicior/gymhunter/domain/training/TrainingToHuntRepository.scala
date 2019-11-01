package com.github.wkicior.gymhunter.domain.training


import java.time.OffsetDateTime
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.PersistentActor
import akka.actor._
import akka.persistence._

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

//  private var trainings = Set(
//    TrainingToHunt(UUID.randomUUID().toString, 550633L, 8, OffsetDateTime.now()),
//    TrainingToHunt(UUID.randomUUID().toString, 550656L, 8, OffsetDateTime.now()),
//    TrainingToHunt(UUID.randomUUID().toString, 699176L, 8, OffsetDateTime.now()),
//    TrainingToHunt(UUID.randomUUID().toString, 699158L, 8, OffsetDateTime.now()),
//    TrainingToHunt(UUID.randomUUID().toString, 550634L, 8, OffsetDateTime.now()),
//    TrainingToHunt(UUID.randomUUID().toString, 550635L, 8, OffsetDateTime.now()),
//    TrainingToHunt(UUID.randomUUID().toString, 550667L, 8, OffsetDateTime.now()))

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


//  def receive = {
//    case GetAllTrainingsToHunt() =>
//      //TODO: use source? val trainingIdsSource: Source[Long, NotUsed] = Source(List(550633, 550634))
//      sender() ! TrainingsToHunt(trainings)
//    case AddTrainingToHunt(tr) =>
//
//      this.trainings += trainingToHunt
//      sender() ! trainingToHunt
//    case _ =>
//      log.error("Unrecognized message")
//  }
}