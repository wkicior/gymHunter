package com.github.wkicior.gymhunter.domain.tohunt

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntAggregate.{TrainingToHuntAdded, TrainingToHuntNotificationSent}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntPersistence.{GetTrainingToHuntAggregate, StoreEvents}
import com.github.wkicior.gymhunter.domain.training.TrainingSlotsAvailableEvent
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingSlotsAvailableNotificationHandlerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

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
  private val trainingToHuntCommandHandler = system.actorOf(TrainingSlotsAvailableNotificationHandler.props(trainingToHuntEventStore))

  "A TrainingToHuntSlotsAvailableNotificationHandler Actor" should {
    "handle notification command on slots available for training to hunt" in {
      //given
      val trainingToHuntAddedEvent = TrainingToHuntAdded(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())
      val sampleTrainingToHunt = new TrainingToHuntAggregate(trainingToHuntAddedEvent) //creating from event in order to have clean events list

      //when
      trainingToHuntCommandHandler.tell(TrainingSlotsAvailableEvent(sampleTrainingToHunt.id), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsg(GetTrainingToHuntAggregate(sampleTrainingToHunt.id))
      trainingToHuntEventStoreProbe.reply(Right(sampleTrainingToHunt))

      trainingToHuntEventStoreProbe.expectMsg(StoreEvents(sampleTrainingToHunt.id, List(TrainingToHuntNotificationSent(sampleTrainingToHunt.id))))
      trainingToHuntEventStoreProbe.reply(Right(sampleTrainingToHunt.id))

      sampleTrainingToHunt.notificationOnSlotsAvailableSentTime should be <= OffsetDateTime.now()
    }
  }
}