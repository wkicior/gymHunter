package com.github.wkicior.gymhunter.domain.training

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps


class TrainingHunterSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }
  val trainingTrackerProbe = TestProbe()
  val trainingFetcherProbe = TestProbe()
  val vacantTrainingManagerProbe = TestProbe()

  val trainingFetcherProps = Props(new Actor {
    def receive = {
      case x => {
        trainingFetcherProbe.ref forward x
      }
    }
  })
  val trainingTrackerProps = Props(new Actor {
    def receive = {
      case x => {
        trainingTrackerProbe.ref forward x
      }
    }
  })
  val vacantTrainingManagerProps = Props(new Actor {
    def receive = {
      case x => {
        vacantTrainingManagerProbe.ref forward x
      }
    }
  })

  val trainingHunter = system.actorOf(TrainingHunter.props(trainingTrackerProps, trainingFetcherProps, vacantTrainingManagerProps))

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
      trainingTrackerProbe.expectMsgType[TrainingTracker.GetTrackedTrainings]
      trainingTrackerProbe.reply(TrainingTracker.TrackedTrainingIds(List()))

      trainingFetcherProbe.expectNoMessage()
      vacantTrainingManagerProbe.expectNoMessage()
    }

    """ask trainingFetcher for all tracked training ids
      |ask for training details on given Id
      |don't notify vacantTrainingManager on non vacant training
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val sampleNonVacantTraining = Training(42L, 0, "", "")

      //when
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      //then
      trainingTrackerProbe.expectMsgType[TrainingTracker.GetTrackedTrainings]
      trainingTrackerProbe.reply(TrainingTracker.TrackedTrainingIds(List(42L)))

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
      val sampleVacantTraining = Training(42L, 1, "", "")

      //when
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      //then
      trainingTrackerProbe.expectMsgType[TrainingTracker.GetTrackedTrainings]
      trainingTrackerProbe.reply(TrainingTracker.TrackedTrainingIds(List(42L)))

      trainingFetcherProbe.expectMsg(TrainingFetcher.GetTraining(42L))
      trainingFetcherProbe.reply(sampleVacantTraining)

      vacantTrainingManagerProbe.expectMsg(VacantTrainingManager.ProcessVacantTraining(sampleVacantTraining))
    }
  }
}