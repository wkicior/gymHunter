package com.github.wkicior.gymhunter.domain.training.tohunt

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHunt.{TrainingToHuntAdded, TrainingToHuntDeleted}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntEventStore.{OptionalTrainingToHunt, StoreEvents}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingToHuntEventStoreSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  private val probe = TestProbe()
  private val trainingTracker = system.actorOf(TrainingToHuntEventStore.props)

  "A TrainingToHuntEventStore Actor" should {
    "return initially empty trainings to hunt" in {

      trainingTracker.tell(TrainingToHuntEventStore.GetAllTrainingsToHunt(), probe.ref)
      val response = probe.expectMsgType[Set[TrainingToHunt]]
      response.size shouldEqual 0
    }

    "store TrainingToAdd on TrainingToHuntAdded event" in {
      val trainingToHuntAddedEvent = TrainingToHuntAdded(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())
      trainingTracker.tell(StoreEvents(trainingToHuntAddedEvent.id, List(trainingToHuntAddedEvent)), probe.ref)
      val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHunt]]
      response.isRight shouldEqual true
      inside(response.toOption.get) { case TrainingToHunt(id, externalSystemId, clubId) =>
          id shouldEqual trainingToHuntAddedEvent.id
          externalSystemId shouldEqual 1L
          clubId shouldEqual 2L
      }

      trainingTracker.tell(TrainingToHuntEventStore.GetAllTrainingsToHunt(), probe.ref)
      val getAllResponse = probe.expectMsgType[Set[TrainingToHunt]]
      getAllResponse.find(t => t.id == trainingToHuntAddedEvent.id) shouldBe defined
    }
  }

  "delete TrainingToHunton TrainingToHuntDeleted event" in {
    val trainingToHuntAddedEvent = TrainingToHuntAdded(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())
    val deleteEvent = TrainingToHuntDeleted(trainingToHuntAddedEvent.id)
    trainingTracker.tell(StoreEvents(trainingToHuntAddedEvent.id, List(trainingToHuntAddedEvent, deleteEvent)), probe.ref)
    val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHunt]]
    response.isLeft shouldEqual true

    trainingTracker.tell(TrainingToHuntEventStore.GetAllTrainingsToHunt(), probe.ref)
    val getAllResponse = probe.expectMsgType[Set[TrainingToHunt]]
    getAllResponse.find(t => t.id == trainingToHuntAddedEvent.id) shouldBe None
  }
}