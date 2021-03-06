package com.github.wkicior.gymhunter.domain.subscription


import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionCommandHandler.{CreateTrainingHuntingSubscriptionCommand, DeleteTrainingHuntingSubscriptionCommand}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionErrors.TrainingHuntingSubscriptionNotFound
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetTrainingHuntingSubscriptionAggregate, StoreEvents}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingHuntingSubscriptionCommandHandlerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  private val probe = TestProbe()
  val thsEventStoreProbe = TestProbe()

  val thsEventStoreProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => thsEventStoreProbe.ref forward x
    }
  })
  private val thsEventStore = system.actorOf(thsEventStoreProps)
  private val thsCommandHandler = system.actorOf(TrainingHuntingSubscriptionCommandHandler.props(thsEventStore))

  "A TrainingHuntingSubscriptionCommandHandler Actor" should {
    "create new TrainingHuntingSubscription and store it to the event store" in {
      //given
      val createThsCommand = CreateTrainingHuntingSubscriptionCommand(1L, 2L, OffsetDateTime.now())
      val sampleThs = new TrainingHuntingSubscriptionAggregate(TrainingHuntingSubscriptionId(), 1L, 2L, createThsCommand.huntingDeadline)
      //when
      thsCommandHandler.tell(createThsCommand, probe.ref)

      //then
      thsEventStoreProbe.expectMsgPF() {
        case ok@StoreEvents(_, List(TrainingHuntingSubscriptionAddedEvent(_, 1L, 2L, createThsCommand.`huntingDeadline`, None, None, _, _))) => ok
      }
      thsEventStoreProbe.reply(Right(sampleThs))

      val response = probe.expectMsgType[TrainingHuntingSubscription]
      response shouldEqual sampleThs()
    }

    "create new TrainingHuntingSubscription with booking auto deadline and store it to the event store" in {
      //given
      val createThsCommand = CreateTrainingHuntingSubscriptionCommand(1L, 2L, OffsetDateTime.now, Some(OffsetDateTime.now))
      val sampleThs = new TrainingHuntingSubscriptionAggregate(TrainingHuntingSubscriptionId(), 1L, 2L, createThsCommand.huntingDeadline, createThsCommand.autoBookingDeadline)
      //when
      thsCommandHandler.tell(createThsCommand, probe.ref)

      //then
      thsEventStoreProbe.expectMsgPF() {
        case ok@StoreEvents(_, List(TrainingHuntingSubscriptionAddedEvent(_, 1L, 2L, createThsCommand.`huntingDeadline`, createThsCommand.autoBookingDeadline, None, _, _))) => ok
      }
      thsEventStoreProbe.reply(Right(sampleThs))

      val response = probe.expectMsgType[TrainingHuntingSubscription]
      response shouldEqual sampleThs()
    }

    "create new TrainingHuntingSubscription with hunting start time and store it to the event store" in {
      //given
      val createThsCommand = CreateTrainingHuntingSubscriptionCommand(1L, 2L, OffsetDateTime.now, None, Some(OffsetDateTime.now))
      val sampleThs = new TrainingHuntingSubscriptionAggregate(TrainingHuntingSubscriptionId(), 1L, 2L, createThsCommand.huntingDeadline, createThsCommand.autoBookingDeadline, createThsCommand.huntingStartTime)
      //when
      thsCommandHandler.tell(createThsCommand, probe.ref)

      //then
      thsEventStoreProbe.expectMsgPF() {
        case ok@StoreEvents(_, List(TrainingHuntingSubscriptionAddedEvent(_, 1L, 2L, createThsCommand.`huntingDeadline`, None, createThsCommand.`huntingStartTime`, _, _))) => ok
      }
      thsEventStoreProbe.reply(Right(sampleThs))

      val response = probe.expectMsgType[TrainingHuntingSubscription]
      response shouldEqual sampleThs()
    }

    "return OptionalTrainingHuntingSubscription with Left(TrainingHuntingSubscriptionNotFound) exception if TrainingHuntingSubscription is not found on delete" in {
      //given
      val id = TrainingHuntingSubscriptionId()

      //when
      thsCommandHandler.tell(DeleteTrainingHuntingSubscriptionCommand(id), probe.ref)

      //then
      thsEventStoreProbe.expectMsg(GetTrainingHuntingSubscriptionAggregate(id))
      thsEventStoreProbe.reply(Left(TrainingHuntingSubscriptionNotFound(id)))

      val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]]
      response shouldEqual Left(TrainingHuntingSubscriptionNotFound(id))
    }

    "delete existing TrainingHuntingSubscription and return OptionalTrainingHuntingSubscription with deleted TrainingHuntingSubscription" in {
      //given
      val thsAddedEvent = TrainingHuntingSubscriptionAddedEvent(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now())
      val sampleThs = new TrainingHuntingSubscriptionAggregate(thsAddedEvent) //creating from event in order to have clean events list

      //when
      thsCommandHandler.tell(DeleteTrainingHuntingSubscriptionCommand(sampleThs.id), probe.ref)

      //then
      thsEventStoreProbe.expectMsg(GetTrainingHuntingSubscriptionAggregate(sampleThs.id))
      thsEventStoreProbe.reply(Right(sampleThs))

      thsEventStoreProbe.expectMsgPF() {
        case ok@StoreEvents(_, List(TrainingHuntingSubscriptionDeletedEvent(sampleThs.id, _, _))) => ok
      }
      thsEventStoreProbe.reply(Left(TrainingHuntingSubscriptionNotFound(sampleThs.id)))

      val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]]
      response shouldEqual Right(sampleThs())
    }
  }
}