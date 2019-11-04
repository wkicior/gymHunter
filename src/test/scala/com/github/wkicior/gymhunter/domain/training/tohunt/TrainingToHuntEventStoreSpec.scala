package com.github.wkicior.gymhunter.domain.training.tohunt

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntAggregate.{TrainingToHuntAdded, TrainingToHuntDeleted}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntEventStore._
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingToHuntEventStoreSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  private val probe = TestProbe()
  private val trainingToHuntEventStore = system.actorOf(TrainingToHuntEventStore.props)

  "A TrainingToHuntEventStore Actor" should {
    "return initially empty trainings to hunt" in {

      trainingToHuntEventStore.tell(TrainingToHuntEventStore.GetAllTrainingsToHunt(), probe.ref)
      val response = probe.expectMsgType[Set[TrainingToHunt]]
      response.size shouldEqual 0
    }

    "store TrainingToAdd on TrainingToHuntAdded event" in {
      //given
      val trainingToHuntAddedEvent = TrainingToHuntAdded(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())

      //when
      trainingToHuntEventStore.tell(StoreEvents(trainingToHuntAddedEvent.id, List(trainingToHuntAddedEvent)), probe.ref)
      val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHuntAggregate]]

      //then
      response.isRight shouldEqual true
      inside(response.toOption.get) { case TrainingToHuntAggregate(id, externalSystemId, clubId) =>
          id shouldEqual trainingToHuntAddedEvent.id
          externalSystemId shouldEqual 1L
          clubId shouldEqual 2L
      }

      trainingToHuntEventStore.tell(TrainingToHuntEventStore.GetAllTrainingsToHunt(), probe.ref)
      val getAllResponse = probe.expectMsgType[Set[TrainingToHunt]]
      getAllResponse.find(t => t.id == trainingToHuntAddedEvent.id) shouldBe defined
    }
  }

  "delete TrainingToHunt on TrainingToHuntDeleted event" in {
    //given
    val trainingToHuntAddedEvent = TrainingToHuntAdded(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())
    val deleteEvent = TrainingToHuntDeleted(trainingToHuntAddedEvent.id)

    //when
    trainingToHuntEventStore.tell(StoreEvents(trainingToHuntAddedEvent.id, List(trainingToHuntAddedEvent, deleteEvent)), probe.ref)
    val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHunt]]

    //then
    response.isLeft shouldEqual true

    trainingToHuntEventStore.tell(TrainingToHuntEventStore.GetAllTrainingsToHunt(), probe.ref)
    val getAllResponse = probe.expectMsgType[Set[TrainingToHunt]]
    getAllResponse.find(t => t.id == trainingToHuntAddedEvent.id) shouldBe None
  }

  "return either.right TrainingToHuntAggregate by ID" in {
    //given
    val id = TrainingToHuntId()
    val trainingToHuntAddedEvent = TrainingToHuntAdded(id, 1L, 2L, OffsetDateTime.now())
    trainingToHuntEventStore.tell(StoreEvents(trainingToHuntAddedEvent.id, List(trainingToHuntAddedEvent)), probe.ref)
    val trainingToHuntAggregate = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHuntAggregate]]

    //when
    trainingToHuntEventStore.tell(GetTrainingToHuntAggregate(id), probe.ref)
    val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHuntAggregate]]

    //then
    response shouldEqual trainingToHuntAggregate
  }

  "return either.left on TrainingToHuntAggregate not found" in {
    //given
    val id = TrainingToHuntId()

    //when
    trainingToHuntEventStore.tell(GetTrainingToHuntAggregate(id), probe.ref)

    //then
    val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHuntAggregate]]
    response shouldEqual Left(TrainingToHuntNotFound(id))
  }

  "return either.right TrainingToHunt by ID" in {
    //given
    val id = TrainingToHuntId()
    val trainingToHuntAddedEvent = TrainingToHuntAdded(id, 1L, 2L, OffsetDateTime.now())
    trainingToHuntEventStore.tell(StoreEvents(trainingToHuntAddedEvent.id, List(trainingToHuntAddedEvent)), probe.ref)
    val trainingToHuntAggregate = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHuntAggregate]]

    //when
    trainingToHuntEventStore.tell(GetTrainingToHunt(id), probe.ref)
    val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHunt]]

    //then
    response shouldEqual trainingToHuntAggregate.map(t => t())
  }

  "return either.left on TrainingToHunt not found" in {
    //given
    val id = TrainingToHuntId()

    //when
    trainingToHuntEventStore.tell(GetTrainingToHunt(id), probe.ref)

    //then
    val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHunt]]
    response shouldEqual Left(TrainingToHuntNotFound(id))
  }
}