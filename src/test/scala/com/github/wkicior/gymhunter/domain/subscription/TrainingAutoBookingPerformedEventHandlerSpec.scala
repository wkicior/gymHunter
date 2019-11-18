package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetTrainingHuntingSubscriptionAggregate, StoreEvents}
import com.github.wkicior.gymhunter.domain.training.TrainingAutoBookingPerformedEvent
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingAutoBookingPerformedEventHandlerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  val thsEventStoreProbe = TestProbe()
  val slotsAvailableNotificationSenderProbe = TestProbe()

  val thsEventStoreProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => thsEventStoreProbe.ref forward x
    }
  })


  private val thsEventStore = system.actorOf(thsEventStoreProps)
  private val thsCommandHandler = system.actorOf(TrainingAutoBookingPerformedEventHandler.props(thsEventStore))

  "A TrainingAutoBookingPerformedEventHandler Actor" should {
    """handle training auto booking
      |and mark auto booking on training hunting subscription
      |and store events on THS Store
    """.stripMargin in {
      //given
      val id = TrainingHuntingSubscriptionId()
      val sampleThs = TrainingHuntingSubscriptionAggregate(id, 1L, 2L)

      //when
      thsCommandHandler ! TrainingAutoBookingPerformedEvent(sampleThs.externalSystemId, sampleThs.clubId, id, OffsetDateTime.now)

      //then
      thsEventStoreProbe.expectMsg(GetTrainingHuntingSubscriptionAggregate(id))
      thsEventStoreProbe.reply(Right(sampleThs))

      thsEventStoreProbe.expectMsgPF() {
        case ok@StoreEvents(_, List(TrainingHuntingSubscriptionAutoBookingEvent(sampleThs.id, _, _))) => ok
      }
      thsEventStoreProbe.reply(Right(sampleThs.id))

      sampleThs.autoBookingDateTime.get should be <= OffsetDateTime.now()
    }
  }
}