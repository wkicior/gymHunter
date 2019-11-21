package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotification
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionId, TrainingHuntingSubscriptionProvider}
import com.github.wkicior.gymhunter.domain.training.VacantTrainingManager.ProcessVacantTraining
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._

import scala.language.postfixOps

class VacantTrainingManagerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }
  val thsProviderProbe = TestProbe()
  val slotsAvailableNotificationSenderProbe = TestProbe()
  val trainingBookerProbe = TestProbe()

  val thsProviderProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => thsProviderProbe.ref forward x
    }
  })
  val slotsAvailableNotificationSenderProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => slotsAvailableNotificationSenderProbe.ref forward x
    }
  })
  val trainingBookerProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingBookerProbe.ref forward x
    }
  })

  private val trainingHunter = system.actorOf(VacantTrainingManager.props(thsProviderProps, slotsAvailableNotificationSenderProps, trainingBookerProps))

  "A VacantTrainingManager Actor" should {
    """fetch all TrainingHuntingSubscription entities related to given Training
      |and send the notification command
      |and don't perform auto booking for them
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val training = Training(1, 1, Some(OffsetDateTime.now()), OffsetDateTime.now().plusDays(2))
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 1L, OffsetDateTime.now().plusDays(1))

      //when
      trainingHunter.tell(ProcessVacantTraining(training), probe.ref)

      //then
      thsProviderProbe.expectMsg(TrainingHuntingSubscriptionProvider.GetTrainingHuntingSubscriptionsByTrainingIdQuery(training.id))
      thsProviderProbe.reply(Set(ths))

      slotsAvailableNotificationSenderProbe.expectMsg(SendNotification(SlotsAvailableNotification(training.start_date, ths.clubId, ths.id)))
      trainingBookerProbe.expectNoMessage(1 second)
    }

    """fetch all TrainingHuntingSubscription entities related to given Training
      |and send the notification command for those training hunting subscriptions with passed autoBookingDeadline
      |and don't perform auto booking for them
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val training = Training(1, 1, Some(OffsetDateTime.now()), OffsetDateTime.now().plusDays(2))
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 1L, OffsetDateTime.now().plusDays(1), None, Some(OffsetDateTime.now.minusDays(1)))

      //when
      trainingHunter.tell(ProcessVacantTraining(training), probe.ref)

      //then
      thsProviderProbe.expectMsg(TrainingHuntingSubscriptionProvider.GetTrainingHuntingSubscriptionsByTrainingIdQuery(training.id))
      thsProviderProbe.reply(Set(ths))

      slotsAvailableNotificationSenderProbe.expectMsg(SendNotification(SlotsAvailableNotification(training.start_date, ths.clubId, ths.id)))
      trainingBookerProbe.expectNoMessage(1 second)
    }

    """fetch all TrainingHuntingSubscription entities related to given Training
      |and start auto booking for training hunting subscriptions with future autoBookingDeadline
      |and do not send notification commands for those subscriptions
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val training = Training(1, 1, Some(OffsetDateTime.now()), OffsetDateTime.now().plusDays(2))
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 1L, OffsetDateTime.now().plusDays(1), None, Some(OffsetDateTime.now.plusMinutes(2)))

      //when
      trainingHunter.tell(ProcessVacantTraining(training), probe.ref)

      //then
      thsProviderProbe.expectMsg(TrainingHuntingSubscriptionProvider.GetTrainingHuntingSubscriptionsByTrainingIdQuery(training.id))
      thsProviderProbe.reply(Set(ths))

      trainingBookerProbe.expectMsg(TrainingBooker.BookTraining(ths, training))
      slotsAvailableNotificationSenderProbe.expectNoMessage(1 second)
    }
  }
}
