package com.github.wkicior.gymhunter.domain.notification

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionId
import com.github.wkicior.gymhunter.domain.training.TrainingAutoBookingPerformedEvent
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotification
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotificationSender.SendIFTTNotification
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class AutoBookingNotificationSenderSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  val ifttNotificationSenderProbe = TestProbe()

  val ifttNotificationSenderProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => ifttNotificationSenderProbe.ref forward x
    }
  })
  private val ifttNotifiationSender = system.actorOf(ifttNotificationSenderProps)
  system.actorOf(AutoBookingNotificationSender.props(ifttNotifiationSender))

  "A AutoBookingNotificationSender Actor" should {
    """listens for TrainingAutoBookingPerformedEvents
      |sends auto booking performed to IFTTNotificationSender
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val id = TrainingHuntingSubscriptionId()
      val trainingTime = OffsetDateTime.now
      val notification = Notification(trainingTime, 1L, id)
      system.eventStream.subscribe(probe.ref, classOf[SlotsAvailableNotificationSentEvent])
      //when
      system.eventStream.publish(TrainingAutoBookingPerformedEvent(1L, 1L, id, trainingTime))

      //then
      ifttNotificationSenderProbe.expectMsg(SendIFTTNotification("gymHunterAutoBooking", new IFTTNotification(notification)))

    }

  }
}