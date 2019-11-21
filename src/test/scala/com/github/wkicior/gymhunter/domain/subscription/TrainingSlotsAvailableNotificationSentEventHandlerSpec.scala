package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.notification.{SlotsAvailableNotification, SlotsAvailableNotificationSentEvent}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetTrainingHuntingSubscriptionAggregate, StoreEvents}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingSlotsAvailableNotificationSentEventHandlerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  val thsEventStoreProbe = TestProbe()

  val thsEventStoreProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => thsEventStoreProbe.ref forward x
    }
  })

  private val thsEventStore = system.actorOf(thsEventStoreProps)
  private val thsCommandHandler = system.actorOf(TrainingSlotsAvailableNotificationSentEventHandler.props(thsEventStore))

  "A TrainingSlotsAvailableNotificationHandler Actor" should {
    """handle notification command on slots available for training hunting subscriptions
      |send notification through SlotsAvailableNotificationSender
      |set notificationOnSlotsAvailableSentTime on trainingHuntingSubscriptionAggregate on successful notification sent
    """.stripMargin in {
      //given
      val id = TrainingHuntingSubscriptionId()
      val sampleThs = TrainingHuntingSubscriptionAggregate(id, 1L, 2L)
      val notification = SlotsAvailableNotification(OffsetDateTime.now, 8L, id)

      //when
      thsCommandHandler ! SlotsAvailableNotificationSentEvent(notification)

      //then
      thsEventStoreProbe.expectMsg(GetTrainingHuntingSubscriptionAggregate(notification.trainingHuntingSubscriptionId))
      thsEventStoreProbe.reply(Right(sampleThs))

      thsEventStoreProbe.expectMsgPF() {
        case ok@StoreEvents(_, List(TrainingHuntingSubscriptionNotificationSentEvent(sampleThs.id, _, _))) => ok
      }
      thsEventStoreProbe.reply(Right(sampleThs.id))

      sampleThs.notificationOnSlotsAvailableSentTime.get should be <= OffsetDateTime.now()
    }
  }
}