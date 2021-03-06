package com.github.wkicior.gymhunter.persistence

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.subscription.OptionalTrainingHuntingSubscription.OptionalTrainingHuntingSubscription
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionErrors.TrainingHuntingSubscriptionNotFound
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscriptionAddedEvent, TrainingHuntingSubscriptionDeletedEvent}
import com.github.wkicior.gymhunter.domain.subscription.TrainingHuntingSubscriptionPersistence._
import com.github.wkicior.gymhunter.domain.subscription._
import com.github.wkicior.gymhunter.infrastructure.persistence.TrainingHuntingSubscriptionEventStore
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingHuntingSubscriptionEventStoreSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {
  def this() = this(ActorSystem("GymHunterLevelDB", config = ConfigFactory.load().getConfig("leveldbtest")))

  override def afterAll: Unit = {
    shutdown(system)
  }

  private val probe = TestProbe()
  private val thsEventStore = system.actorOf(TrainingHuntingSubscriptionEventStore.props)

  "A TrainingHuntingSubscriptionEventStore Actor" should {
    "return all training hunting subscriptions" in {

      thsEventStore.tell(GetAllTrainingHuntingSubscriptions(), probe.ref)
      val response = probe.expectMsgType[Set[TrainingHuntingSubscription]]
      response.size should be >= 0
    }

    "store TrainingToAdd on TrainingHuntingSubscriptionAdded event" in {
      //given
      val thsAddedEvent = TrainingHuntingSubscriptionAddedEvent(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now())

      //when
      thsEventStore.tell(StoreEvents(thsAddedEvent.id, List(thsAddedEvent)), probe.ref)
      val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]

      //then
      response.isRight shouldEqual true
      inside(response.toOption.get) { case TrainingHuntingSubscriptionAggregate(id, externalSystemId, clubId) =>
          id shouldEqual thsAddedEvent.id
          externalSystemId shouldEqual 1L
          clubId shouldEqual 2L
      }

      thsEventStore.tell(GetAllTrainingHuntingSubscriptions(), probe.ref)
      val getAllResponse = probe.expectMsgType[Set[TrainingHuntingSubscription]]
      getAllResponse.find(t => t.id == thsAddedEvent.id) shouldBe defined
    }
  }

  "delete TrainingHuntingSubscription on TrainingHuntingSubscriptionDeleted event" in {
    //given
    val thsAddedEvent = TrainingHuntingSubscriptionAddedEvent(TrainingHuntingSubscriptionId(), 1L, 2L, OffsetDateTime.now())
    val deleteEvent = TrainingHuntingSubscriptionDeletedEvent(thsAddedEvent.id)

    //when
    thsEventStore.tell(StoreEvents(thsAddedEvent.id, List(thsAddedEvent, deleteEvent)), probe.ref)
    val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]]

    //then
    response.isLeft shouldEqual true

    thsEventStore.tell(GetAllTrainingHuntingSubscriptions(), probe.ref)
    val getAllResponse = probe.expectMsgType[Set[TrainingHuntingSubscription]]
    getAllResponse.find(t => t.id == thsAddedEvent.id) shouldBe None
  }

  "return either.right TrainingHuntingSubscriptionAggregate by ID" in {
    //given
    val id = TrainingHuntingSubscriptionId()
    val thsAddedEvent = TrainingHuntingSubscriptionAddedEvent(id, 1L, 2L, OffsetDateTime.now())
    thsEventStore.tell(StoreEvents(thsAddedEvent.id, List(thsAddedEvent)), probe.ref)
    val thsAggregate = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]

    //when
    thsEventStore.tell(GetTrainingHuntingSubscriptionAggregate(id), probe.ref)
    val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]

    //then
    response shouldEqual thsAggregate
  }

  "return either.left on TrainingHuntingSubscriptionAggregate not found" in {
    //given
    val id = TrainingHuntingSubscriptionId()

    //when
    thsEventStore.tell(GetTrainingHuntingSubscriptionAggregate(id), probe.ref)

    //then
    val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]
    response shouldEqual Left(TrainingHuntingSubscriptionNotFound(id))
  }

  "return either.right TrainingHuntingSubscription by ID" in {
    //given
    val id = TrainingHuntingSubscriptionId()
    val thsAddedEvent = TrainingHuntingSubscriptionAddedEvent(id, 1L, 2L, OffsetDateTime.now())
    thsEventStore.tell(StoreEvents(thsAddedEvent.id, List(thsAddedEvent)), probe.ref)
    val thsAggregate = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscriptionAggregate]]

    //when
    thsEventStore.tell(GetTrainingHuntingSubscription(id), probe.ref)
    val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]]

    //then
    response shouldEqual thsAggregate.map(t => t())
  }

  "return either.left on TrainingHuntingSubscription not found" in {
    //given
    val id = TrainingHuntingSubscriptionId()

    //when
    thsEventStore.tell(GetTrainingHuntingSubscription(id), probe.ref)

    //then
    val response = probe.expectMsgType[OptionalTrainingHuntingSubscription[TrainingHuntingSubscription]]
    response shouldEqual Left(TrainingHuntingSubscriptionNotFound(id))
  }
}