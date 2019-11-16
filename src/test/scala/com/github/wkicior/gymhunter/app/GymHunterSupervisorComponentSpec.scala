package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.testkit.{TestKit, TestProbe}

import scala.concurrent.duration._
import com.github.wkicior.gymhunter.domain.notification.Notification
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscriptionAddedEvent, TrainingHuntingSubscriptionNotificationSentEvent}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetAllTrainingHuntingSubscriptions, GetTrainingHuntingSubscriptionAggregate, StoreEvents}
import com.github.wkicior.gymhunter.domain.subscription._
import com.github.wkicior.gymhunter.domain.training.{GetTraining, Training}
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotification
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps


class GymHunterSupervisorComponentSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("GymHunterSupervisorComponentSpec"))


  override def afterAll: Unit = {
    shutdown(system)
  }

  val thsEventStoreProbe = TestProbe()
  val trainingFetcherProbe = TestProbe()
  val ifttNotificationSenderProbe = TestProbe()

  val thsEventStoreProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => thsEventStoreProbe.ref forward x
    }
  })

  val trainingFetcherProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingFetcherProbe.ref forward x
    }
  })

  val ifttNotificationSenderProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => ifttNotificationSenderProbe.ref forward x
    }
  })

  private val thsEventStore = system.actorOf(thsEventStoreProps, "TrainingHuntingSubscriptionEventStore")
  private val trainingFetcher = system.actorOf(trainingFetcherProps, "GymsteerTrainingFetcher")
  private val ifttNotificationSender = system.actorOf(ifttNotificationSenderProps, "IFTTNotificationSender")

  private val gymHunterSupervisor = system.actorOf(GymHunterSupervisor.props(thsEventStore, trainingFetcher, ifttNotificationSender), "GymHunterSupervisorIntegrationTest")

  "A GymHunterSupervisor Actor" should {
    """start new hunting
      |by fetching all training hunting subscriptions
      |and load training data for them
      |and notify users if slots are available on the training
    """.stripMargin in {
      //given
      val training = Training(42L, 1, Some(OffsetDateTime.now().minusDays(1)), OffsetDateTime.now.plusDays(1))
      val thsAddedEvent = TrainingHuntingSubscriptionAddedEvent(TrainingHuntingSubscriptionId(), 42L, 9L, OffsetDateTime.now.plusDays(1))
      val ths = new TrainingHuntingSubscriptionAggregate(thsAddedEvent) //creating from event in order to have clean events list
      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions] // by TrainingHunter
      thsEventStoreProbe.reply(Set(ths()))

      trainingFetcherProbe.expectMsg(GetTraining(42)) // by TrainingHunter
      trainingFetcherProbe.reply(training)

      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions] //by VacantTrainingManager
      thsEventStoreProbe.reply(Set(ths()))

      ifttNotificationSenderProbe.expectMsg(new IFTTNotification(Notification(training.start_date, ths.clubId, ths.id)))
      ifttNotificationSenderProbe.reply(Status.Success)

      thsEventStoreProbe.expectMsg(GetTrainingHuntingSubscriptionAggregate(ths.id)) //by TrainingSlotsAvailableNotificationSentEventHandler
      thsEventStoreProbe.reply(Right(ths))

      thsEventStoreProbe.expectMsgPF() {
        case ok@StoreEvents(_, List(TrainingHuntingSubscriptionNotificationSentEvent(ths.id,  _, _))) => ok
      }
      ths.notificationOnSlotsAvailableSentTime.get should be <= OffsetDateTime.now
    }

    """start new hunting
      |by fetching all trainings hunting subscriptions
      |and ignore training hunting subscriptions which has been notified already
    """.stripMargin in {
      //given
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 44L, 8L, OffsetDateTime.now.plusDays(1), Option(OffsetDateTime.now), None)
      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions]
      thsEventStoreProbe.reply(Set(ths))

      trainingFetcherProbe.expectNoMessage(1 second)

      ifttNotificationSenderProbe.expectNoMessage(1 second)
    }

    """start new hunting
      |by fetching all trainings hunting subscriptions
      |and ignore trainings that cannot be booked
    """.stripMargin in {
      //given
      val training = Training(44L, 0, Some(OffsetDateTime.now().minusDays(1)), OffsetDateTime.now.plusDays(1))
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 44L, 7L, OffsetDateTime.now.plusDays(1), None)
      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions]
      thsEventStoreProbe.reply(Set(ths))


      trainingFetcherProbe.expectMsg(GetTraining(44L))
      trainingFetcherProbe.reply(training)

      ifttNotificationSenderProbe.expectNoMessage(1 second)
    }

    """start new hunting
      |by fetching all trainings trainings hunting subscriptions
      |and ignore training hunting subscriptions for which huntingEndTime has passed
    """.stripMargin in {
      //given
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 44L, 6L, OffsetDateTime.now.minusDays(1), None)
      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions]
      thsEventStoreProbe.reply(Set(ths))

      trainingFetcherProbe.expectNoMessage(1 second)
      ifttNotificationSenderProbe.expectNoMessage(1 second)
    }
  }
}