package com.github.wkicior.gymhunter.domain.notification


import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.tohunt.{TrainingToHunt, TrainingToHuntId}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntId.OptionalTrainingToHunt
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntPersistence.{GetAllTrainingsToHunt, GetTrainingToHunt}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntProvider.{GetTrainingToHuntQuery, GetTrainingsToHuntByTrainingIdQuery, GetTrainingsToHuntQuery}
import com.github.wkicior.gymhunter.infrastructure.iftt.IFTTNotification
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class SlotsAvailableNotificationSenderSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  private val probe = TestProbe()
  val ifttNotificationSenderProbe = TestProbe()

  val ifttNotificationSenderProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => ifttNotificationSenderProbe.ref forward x
    }
  })

  private val slotsAvailableNotificationSender = system.actorOf(SlotsAvailableNotificationSender.props(ifttNotificationSenderProbe.ref))

  "A SlotsAvailableNotificationSender Actor" should {
    """receive SendNotification status
      |forward the notification to IFTTNotificationSender
      |publishes SlotsAvailableNotificationSentEvent to the event stream
    """.stripMargin in {
      //given
      val notification = Notification(OffsetDateTime.now, 1L, TrainingToHuntId())
      system.eventStream.subscribe(probe.ref, classOf[SlotsAvailableNotificationSentEvent])
      //when
      slotsAvailableNotificationSender.tell(notification, probe.ref)

      //then
      ifttNotificationSenderProbe.expectMsg(new IFTTNotification(notification))
      ifttNotificationSenderProbe.reply(Status.Success)

      val event = probe.expectMsgType[SlotsAvailableNotificationSentEvent]
      event.notification shouldEqual notification
    }

    """receive SendNotification status
      |forward the notification to IFTTNotificationSender
      |but do not publish SlotsAvailableNotificationSentEvent on IFTTNotification failure
    """.stripMargin in {
      //given
      val notification = Notification(OffsetDateTime.now, 1L, TrainingToHuntId())
      system.eventStream.subscribe(probe.ref, classOf[SlotsAvailableNotificationSentEvent])
      //when
      slotsAvailableNotificationSender.tell(notification, probe.ref)

      //then
      ifttNotificationSenderProbe.expectMsg(new IFTTNotification(notification))
      ifttNotificationSenderProbe.reply(Status.Failure)

      probe.expectNoMessage()
    }


  }
}