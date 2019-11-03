package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.training.tohunt.{TrainingToHunt, TrainingToHuntId, TrainingToHuntProvider}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps


class TrainingHunterSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }
  val trainingToHuntProviderProbe = TestProbe()
  val trainingFetcherProbe = TestProbe()
  val vacantTrainingManagerProbe = TestProbe()

  val trainingFetcherProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingFetcherProbe.ref forward x
    }
  })
  val trainingToHuntFetcherProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingToHuntProviderProbe.ref forward x
    }
  })
  val vacantTrainingManagerProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => vacantTrainingManagerProbe.ref forward x
    }
  })

  private val trainingHunter = system.actorOf(TrainingHunter.props(trainingToHuntFetcherProps, trainingFetcherProps, vacantTrainingManagerProps))

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
      trainingToHuntProviderProbe.expectMsgType[TrainingToHuntProvider.GetTrainingsToHuntQuery]
      trainingToHuntProviderProbe.reply(Set())

      trainingFetcherProbe.expectNoMessage()
      vacantTrainingManagerProbe.expectNoMessage()
    }

    """ask trainingFetcher for all tracked training ids
      |ask for training details on given Id
      |don't notify vacantTrainingManager on non vacant training
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val sampleNonVacantTraining = Training(42L, 0, OffsetDateTime.now(), OffsetDateTime.now().plusDays(2))

      //when
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      //then
      trainingToHuntProviderProbe.expectMsgType[TrainingToHuntProvider.GetTrainingsToHuntQuery]
      trainingToHuntProviderProbe.reply(Set(TrainingToHunt(TrainingToHuntId(), 42, 8, OffsetDateTime.now)))

      trainingFetcherProbe.expectMsg(TrainingFetcher.GetTraining(42L))
      trainingFetcherProbe.reply(sampleNonVacantTraining)

      vacantTrainingManagerProbe.expectNoMessage()
    }

    """ask trainingFetcher for all tracked training ids
      |ask for training details on given Id
      |notify vacantTrainingManager on vacant training
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val sampleVacantTraining = Training(42L, 1, OffsetDateTime.now(), OffsetDateTime.now().plusDays(2))

      //when
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      //then
      trainingToHuntProviderProbe.expectMsgType[TrainingToHuntProvider.GetTrainingsToHuntQuery]
      trainingToHuntProviderProbe.reply(Set(TrainingToHunt(TrainingToHuntId(), 42, 8, OffsetDateTime.now)))

      trainingFetcherProbe.expectMsg(TrainingFetcher.GetTraining(42L))
      trainingFetcherProbe.reply(sampleVacantTraining)

      vacantTrainingManagerProbe.expectMsg(VacantTrainingManager.ProcessVacantTraining(sampleVacantTraining))
    }
  }
}