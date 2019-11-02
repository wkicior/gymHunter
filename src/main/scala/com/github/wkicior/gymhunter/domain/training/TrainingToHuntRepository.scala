package com.github.wkicior.gymhunter.domain.training


import java.util.UUID

import akka.actor.{ActorLogging, Props, _}
import akka.pattern.pipe
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.github.wkicior.gymhunter.domain.training.TrainingToHuntRepository.{OptionalTrainingToHunt, TrainingToHuntEvent, TrainingToHuntNotFound}

import scala.concurrent.{Future, Promise}
import scala.language.postfixOps


object TrainingToHuntRepository {
  def props: Props = Props[TrainingToHuntRepository]
  final case class GetAllTrainingsToHunt()
  final case class AddTrainingToHunt(training: TrainingToHuntRequest)

  sealed trait TrainingToHuntEvent {
    val id: TrainingToHuntId
    val trainingToHunt: TrainingToHunt
  }

  final case class TrainingToHuntAdded(id: TrainingToHuntId, trainingToHunt: TrainingToHunt) extends TrainingToHuntEvent

  type OptionalTrainingToHunt[+A] = Either[TrainingToHuntNotFound, A]
  final case class TrainingToHuntNotFound(id: TrainingToHuntId) extends RuntimeException(s"Training to hunt not found with id $id")
}


final case class State(trainingsToHunt: Map[TrainingToHuntId, TrainingToHunt] = Map.empty) {
  def apply(): Set[TrainingToHunt] = trainingsToHunt.values.toSet
  def apply(id: TrainingToHuntId): OptionalTrainingToHunt[TrainingToHunt] = trainingsToHunt.get(id).toRight(TrainingToHuntNotFound(id))
  def +(event: TrainingToHuntEvent): State = State(trainingsToHunt.updated(event.id, event.trainingToHunt))
}

class TrainingToHuntRepository extends PersistentActor with ActorLogging {
  import TrainingToHuntRepository._
  import context._
  implicit val system: ActorSystem = ActorSystem("GymHunter")


  override def persistenceId = "training-to-hunt-id-1"
  var state = State()

  val receiveRecover: Receive = {
    case event: TrainingToHuntEvent => state += event
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val snapShotInterval = 1000
  val receiveCommand: Receive = {
    case AddTrainingToHunt(tr) =>
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), tr.externalSystemId, tr.clubId, tr.huntingEndTime)
      handleEvent(TrainingToHuntAdded(trainingToHunt.id, trainingToHunt)) pipeTo sender()
      ()

    case GetAllTrainingsToHunt() =>
      sender() ! state()
    case "print" => println(state)
  }

  private def handleEvent[E <: TrainingToHuntEvent](e: => E): Future[TrainingToHunt] = {
    val p = Promise[TrainingToHunt]
    persist(e) { event =>
      state += event
      p.success(event.trainingToHunt)
      system.eventStream.publish(event)
      if (lastSequenceNr != 0 && lastSequenceNr % 1000 == 0) saveSnapshot(state)
    }
    p.future
  }
}