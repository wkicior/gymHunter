package com.github.wkicior.gymhunter.domain.tohunt

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSentEvent
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntAggregate.{TrainingToHuntAdded, TrainingToHuntNotificationSent}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntPersistence.{GetTrainingToHuntAggregate, StoreEvents}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingSlotsAvailableEventHandlerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  private val probe = TestProbe()
  val trainingToHuntEventStoreProbe = TestProbe()
  val slotsAvailableNotificationSenderProbe = TestProbe()

  val trainingToHuntEventStoreProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingToHuntEventStoreProbe.ref forward x
    }
  })

  val slotsAvailableNotificationSenderProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => slotsAvailableNotificationSenderProbe.ref forward x
    }
  })
  private val trainingToHuntEventStore = system.actorOf(trainingToHuntEventStoreProps)
  private val trainingToHuntCommandHandler = system.actorOf(TrainingSlotsAvailableEventHandler.props(trainingToHuntEventStore, slotsAvailableNotificationSenderProps))

  "A TrainingToHuntSlotsAvailableNotificationHandler Actor" should {
    """handle notification command on slots available for training to hunt
      |send notification through SlotsAvailableNotificationSender
      |set notificationOnSlotsAvailableSentTime on trainingToHuntAggregate on successful notification sent
    """.stripMargin in {
      //given
      val trainingToHuntAddedEvent = TrainingToHuntAdded(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())
      val sampleTrainingToHunt = new TrainingToHuntAggregate(trainingToHuntAddedEvent) //creating from event in order to have clean events list

      //when
      system.eventStream.publish(SlotsAvailableNotificationSentEvent(sampleTrainingToHunt.id))

      //then
      trainingToHuntEventStoreProbe.expectMsg(GetTrainingToHuntAggregate(sampleTrainingToHunt.id))
      trainingToHuntEventStoreProbe.reply(Right(sampleTrainingToHunt))

      trainingToHuntEventStoreProbe.expectMsg(StoreEvents(sampleTrainingToHunt.id, List(TrainingToHuntNotificationSent(sampleTrainingToHunt.id))))
      trainingToHuntEventStoreProbe.reply(Right(sampleTrainingToHunt.id))

      sampleTrainingToHunt.notificationOnSlotsAvailableSentTime should be <= OffsetDateTime.now()
    }
  }
}