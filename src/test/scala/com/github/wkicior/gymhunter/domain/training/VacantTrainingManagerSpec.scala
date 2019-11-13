package com.github.wkicior.gymhunter.domain.training


import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.notification.Notification
import com.github.wkicior.gymhunter.domain.notification.SlotsAvailableNotificationSender.SendNotification
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionId, TrainingHuntingSubscriptionProvider}
import com.github.wkicior.gymhunter.domain.training.VacantTrainingManager.ProcessVacantTraining
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps

class VacantTrainingManagerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }
  val thsProviderProbe = TestProbe()
  val slotsAvailableNotificationSenderProbe = TestProbe()

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

  private val trainingHunter = system.actorOf(VacantTrainingManager.props(thsProviderProps, slotsAvailableNotificationSenderProps))

  "A VacantTrainingManager Actor" should {
    """fetch all TrainingHungingSubscription entities related to given Training
      |and send the notification command
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val training = Training(1, 1, Some(OffsetDateTime.now()), OffsetDateTime.now().plusDays(2))
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 1L, OffsetDateTime.now().plusDays(1), None, None)

      //when
      trainingHunter.tell(ProcessVacantTraining(training), probe.ref)

      //then
      thsProviderProbe.expectMsg(TrainingHuntingSubscriptionProvider.GetTrainingHuntingSubscriptionsByTrainingIdQuery(training.id))
      thsProviderProbe.reply(Set(ths))

      slotsAvailableNotificationSenderProbe.expectMsg(SendNotification(Notification(training.start_date, ths.clubId, ths.id)))
    }
  }
}
