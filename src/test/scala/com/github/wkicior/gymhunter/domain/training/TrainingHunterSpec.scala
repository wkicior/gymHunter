package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionId, TrainingHuntingSubscriptionProvider}
import com.github.wkicior.gymhunter.infrastructure.gymsteer.GymsteerTrainingFetcherException
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps


class TrainingHunterSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }
  val thsProviderProbe = TestProbe()
  val trainingFetcherProbe = TestProbe()
  val vacantTrainingManagerProbe = TestProbe()

  val trainingFetcherProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingFetcherProbe.ref forward x
    }
  })

  val thsFetcherProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => thsProviderProbe.ref forward x
    }
  })
  val vacantTrainingManagerProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => vacantTrainingManagerProbe.ref forward x
    }
  })

  private val trainingFetcher = system.actorOf(trainingFetcherProps)
  private val trainingHunter = system.actorOf(TrainingHunter.props(thsFetcherProps, trainingFetcher, vacantTrainingManagerProps))

  "A TrainingHunter Actor" should {
    """ask trainingFetcher for all tracked training ids
      |don't ask for training details on empty trainings
      |don't notify vacant training manager on empty trainings
    """.stripMargin in {
      //given
      val probe = TestProbe()

      //when
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      //then
      thsProviderProbe.expectMsgType[TrainingHuntingSubscriptionProvider.GetActiveTrainingHuntingSubscriptionsQuery]
      thsProviderProbe.reply(Set())

      trainingFetcherProbe.expectNoMessage()
      vacantTrainingManagerProbe.expectNoMessage()
    }

    """ask trainingFetcher for all tracked training ids
      |ask for training details on given Id
      |don't notify vacantTrainingManager on non vacant training
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val sampleNonVacantTraining = Training(42L, 0, Some(OffsetDateTime.now()), OffsetDateTime.now().plusDays(2))

      //when
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      //then
      thsProviderProbe.expectMsgType[TrainingHuntingSubscriptionProvider.GetActiveTrainingHuntingSubscriptionsQuery]
      thsProviderProbe.reply(Set(TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 42, 8, OffsetDateTime.now, None)))

      trainingFetcherProbe.expectMsg(GetTraining(42L))
      trainingFetcherProbe.reply(sampleNonVacantTraining)

      vacantTrainingManagerProbe.expectNoMessage()
    }

    """ask trainingFetcher for all tracked training ids
      |ask for training details on given Id
      |notify vacantTrainingManager on vacant training
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val sampleVacantTraining = Training(42L, 1, Some(OffsetDateTime.now()), OffsetDateTime.now().plusDays(2))

      //when
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      //then
      thsProviderProbe.expectMsgType[TrainingHuntingSubscriptionProvider.GetActiveTrainingHuntingSubscriptionsQuery]
      thsProviderProbe.reply(Set(TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 42, 8, OffsetDateTime.now, None)))

      trainingFetcherProbe.expectMsg(GetTraining(42L))
      trainingFetcherProbe.reply(sampleVacantTraining)

      vacantTrainingManagerProbe.expectMsg(VacantTrainingManager.ProcessVacantTraining(sampleVacantTraining))
    }

    """ask trainingFetcher for all tracked training ids
      |ask for training details on given Id
      |notify vacantTrainingManager on vacant training only those training which fetching has been successful
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val sampleVacantTraining = Training(42L, 1, Some(OffsetDateTime.now()), OffsetDateTime.now().plusDays(2))

      //when
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      //then
      thsProviderProbe.expectMsgType[TrainingHuntingSubscriptionProvider.GetActiveTrainingHuntingSubscriptionsQuery]
      thsProviderProbe.reply(
        Set(TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 43, 8, OffsetDateTime.now, None),
          TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 42, 8, OffsetDateTime.now, None)))

      trainingFetcherProbe.expectMsg(GetTraining(43L))
      trainingFetcherProbe.reply(Failure(GymsteerTrainingFetcherException("some error on getting the training")))

      trainingFetcherProbe.expectMsg(GetTraining(42L))
      trainingFetcherProbe.reply(sampleVacantTraining)

      vacantTrainingManagerProbe.expectMsg(VacantTrainingManager.ProcessVacantTraining(sampleVacantTraining))
    }
  }
}