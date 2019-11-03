package com.github.wkicior.gymhunter.domain.training.tohunt


import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntAggregate.{TrainingToHuntAdded, TrainingToHuntDeleted}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntCommandHandler.{CreateTrainingToHuntCommand, DeleteTrainingToHuntCommand}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntEventStore.{GetTraining, OptionalTrainingToHunt, TrainingToHuntNotFound}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingToHuntCommandHandlerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  private val probe = TestProbe()
  val trainingToHuntEventStoreProbe = TestProbe()

  val trainingToHuntEventStoreProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingToHuntEventStoreProbe.ref forward x
    }
  })
  private val trainingToHuntEventStore = system.actorOf(trainingToHuntEventStoreProps)
  private val trainingToHuntCommandHandler = system.actorOf(TrainingToHuntCommandHandler.props(trainingToHuntEventStore))

  "A TrainingToHuntCommandHandler Actor" should {
    "create new TrainingToHunt and store it to the event store" in {
      //given
      val createTrainingToHuntCommand = CreateTrainingToHuntCommand(1L, 2L, OffsetDateTime.now())
      val sampleTrainingToHunt = new TrainingToHuntAggregate(TrainingToHuntId(), 1L, 2L, createTrainingToHuntCommand.huntingEndTime)
      //when
      trainingToHuntCommandHandler.tell(createTrainingToHuntCommand, probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsgPF() {
        case ok@TrainingToHuntEventStore.StoreEvents(_, List(TrainingToHuntAdded(_, 1L, 2L, createTrainingToHuntCommand.huntingEndTime))) => ok
      }
      trainingToHuntEventStoreProbe.reply(Right(sampleTrainingToHunt))

      val response = probe.expectMsgType[TrainingToHunt]
      response shouldEqual sampleTrainingToHunt()
    }

    "return OptionalTrainingToHunt with Left(TrainingToHuntNotFound) exception if TrainingToHunt is not found on delete" in {
      //given
      val id = TrainingToHuntId()

      //when
      trainingToHuntCommandHandler.tell(DeleteTrainingToHuntCommand(id), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsg(GetTraining(id))
      trainingToHuntEventStoreProbe.reply(Left(TrainingToHuntNotFound(id)))

      val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHunt]]
      response shouldEqual Left(TrainingToHuntNotFound(id))
    }

    "delete existing TrainingToHunt and return OptionalTrainingToHunt with deleted TrainingToHunt" in {
      //given
      val trainingToHuntAddedEvent = TrainingToHuntAdded(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())
      val sampleTrainingToHunt = new TrainingToHuntAggregate(trainingToHuntAddedEvent) //creating from event in order to have clean events list

      //when
      trainingToHuntCommandHandler.tell(DeleteTrainingToHuntCommand(sampleTrainingToHunt.id), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsg(GetTraining(sampleTrainingToHunt.id))
      trainingToHuntEventStoreProbe.reply(Right(sampleTrainingToHunt))

      trainingToHuntEventStoreProbe.expectMsg(TrainingToHuntEventStore.StoreEvents(sampleTrainingToHunt.id, List(TrainingToHuntDeleted(sampleTrainingToHunt.id))))
      trainingToHuntEventStoreProbe.reply(Left(TrainingToHuntNotFound(sampleTrainingToHunt.id)))

      val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHunt]]
      response shouldEqual Right(sampleTrainingToHunt())
    }
  }
}