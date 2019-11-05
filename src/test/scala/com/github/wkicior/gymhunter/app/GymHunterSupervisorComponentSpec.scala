package com.github.wkicior.gymhunter.app

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntPersistence.GetAllTrainingsToHunt
import com.github.wkicior.gymhunter.domain.tohunt._
import com.github.wkicior.gymhunter.domain.training.{GetTraining, Training}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps


class GymHunterSupervisorComponentSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("GymHunter"))


  override def afterAll: Unit = {
    shutdown(system)
  }

  val trainingToHuntEventStoreProbe = TestProbe()
  val trainingFetcherProbe = TestProbe()

  val trainingToHuntEventStoreProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingToHuntEventStoreProbe.ref forward x
    }
  })

  val trainingFetcherProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingFetcherProbe.ref forward x
    }
  })

  private val trainingToHuntEventStore = system.actorOf(trainingToHuntEventStoreProps, "TrainingToHuntEventStore")
  private val trainingFetcher = system.actorOf(trainingFetcherProps, "GymsteerTrainingFetcher")

  private val gymHunterSupervisor = system.actorOf(GymHunterSupervisor.props(trainingToHuntEventStore, trainingFetcher), "GymHunterSupervisorIntegrationTest")

  "A GymHunterSupervisor Actor" should {
    """start new hunting
      |by fetching all trainings to hunt
      |and load training data for them
      |and notify users if slots are available on the training
    """.stripMargin in {
      //given
      val training = Training(44L, 1, OffsetDateTime.now().minusDays(1), OffsetDateTime.now.plusDays(1))
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), 44L, 8L, OffsetDateTime.now.plusDays(1))
      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsgType[GetAllTrainingsToHunt]
      trainingToHuntEventStoreProbe.reply(Set(trainingToHunt))


      trainingFetcherProbe.expectMsg(GetTraining(44L))
      trainingFetcherProbe.reply(training)

      fail("TODO: implement IFTT check")
    }

    """start new hunting
      |by fetching all trainings to hunt
      |and ignore trainingsToHunt which has been notified already
    """.stripMargin in {
      //given
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), 44L, 8L, OffsetDateTime.now.plusDays(1), Option(OffsetDateTime.now))
      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsgType[GetAllTrainingsToHunt]
      trainingToHuntEventStoreProbe.reply(Set(trainingToHunt))

      trainingFetcherProbe.expectNoMessage()

      fail("TODO: implement IFTT check expectNoMessage()")
    }

    """start new hunting
      |by fetching all trainings to hunt
      |and ignore trainings that cannot be booked
    """.stripMargin in {
      //given
      val training = Training(44L, 0, OffsetDateTime.now().minusDays(1), OffsetDateTime.now.plusDays(1))
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), 44L, 8L, OffsetDateTime.now.plusDays(1), None)
      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsgType[GetAllTrainingsToHunt]
      trainingToHuntEventStoreProbe.reply(Set(trainingToHunt))


      trainingFetcherProbe.expectMsg(GetTraining(44L))
      trainingFetcherProbe.reply(training)

      fail("TODO: implement IFTT check expectNoMessage()")
    }

    """start new hunting
      |by fetching all trainings to hunt
      |and ignore trainingsToHunt for which huntingEndTime has passed
    """.stripMargin in {
      //given
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), 44L, 8L, OffsetDateTime.now.minusDays(1), None)
      val probe = TestProbe()

      //when
      gymHunterSupervisor.tell(GymHunterSupervisor.RunGymHunting(), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsgType[GetAllTrainingsToHunt]
      trainingToHuntEventStoreProbe.reply(Set(trainingToHunt))

      trainingFetcherProbe.expectNoMessage()

      fail("TODO: implement IFTT check expectNoMessage()")
    }
  }
}