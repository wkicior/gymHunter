package com.github.wkicior.gymhunter.domain.subscription

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence.{GetAllTrainingHuntingSubscriptions, GetTrainingHuntingSubscription}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionProvider.{GetActiveTrainingHuntingSubscriptionsQuery, GetAllTrainingHuntingSubscriptionsQuery, GetTrainingHuntingSubscriptionQuery, GetTrainingHuntingSubscriptionsByTrainingIdQuery}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingHuntingSubscriptionProviderSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

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
  private val thsProvider = system.actorOf(TrainingHuntingSubscriptionProvider.props(thsEventStore))

  "A TrainingHuntingSubscriptionProvider Actor" should {
    "return all training hunting subscriptions from the event store" in {
      //given
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now(), None)
      //when
      thsProvider.tell(GetAllTrainingHuntingSubscriptionsQuery(), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions]
      thsEventStoreProbe.reply(Set(ths))

      val response = probe.expectMsgType[Set[TrainingHuntingSubscription]]
      response should contain only ths
    }

    "return active trainings to hunt from the event store" in {
      //given
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now().plusMinutes(1), None)
      //when
      thsProvider.tell(GetActiveTrainingHuntingSubscriptionsQuery(), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions]
      thsEventStoreProbe.reply(Set(ths))

      val response = probe.expectMsgType[Set[TrainingHuntingSubscription]]
      response should contain only ths
    }

    "ignore notified training hunting subscriptions from the event store" in {
      //given
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now(), Some(OffsetDateTime.now))
      //when
      thsProvider.tell(GetActiveTrainingHuntingSubscriptionsQuery(), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions]
      thsEventStoreProbe.reply(Set(ths))

      val response = probe.expectMsgType[Set[TrainingHuntingSubscription]]
      response shouldBe empty
    }

    "ignore training hunting subscriptions for which hunting end time has passed" in {
      //given
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now.minusSeconds(1), None)
      //when
      thsProvider.tell(GetActiveTrainingHuntingSubscriptionsQuery(), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions]
      thsEventStoreProbe.reply(Set(ths))

      val response = probe.expectMsgType[Set[TrainingHuntingSubscription]]
      response shouldBe empty
    }

    "return training hunting subscriptions by external system training Id" in {
      //given
      val thsMatched = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now())
      val thsNotMatched = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 2L, 2L, OffsetDateTime.now())
      //when
      thsProvider.tell(GetTrainingHuntingSubscriptionsByTrainingIdQuery(1L), probe.ref)

      //then
      thsEventStoreProbe.expectMsgType[GetAllTrainingHuntingSubscriptions]
      thsEventStoreProbe.reply(Set(thsMatched, thsNotMatched))

      val response = probe.expectMsgType[Set[TrainingHuntingSubscription]]
      response should contain only thsMatched
    }

    "return training hunting subscriptions by Id" in {
      //given
      val id = TrainingHuntingSubscriptionId()
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now())

      //when
      thsProvider.tell(GetTrainingHuntingSubscriptionQuery(id), probe.ref)

      //then
      thsEventStoreProbe.expectMsg(GetTrainingHuntingSubscription(id))
      thsEventStoreProbe.reply(Right(ths))

      val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]]
      response shouldEqual Right(ths)
    }
  }
}