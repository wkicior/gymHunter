package com.github.wkicior.gymhunter.domain.tohunt

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntId.OptionalTrainingToHunt
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntPersistence.{GetAllTrainingsToHunt, GetTrainingToHunt}
import com.github.wkicior.gymhunter.domain.tohunt.TrainingToHuntProvider.{GetTrainingToHuntQuery, GetTrainingsToHuntByTrainingIdQuery, GetTrainingsToHuntQuery}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingToHuntProviderSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll with Inside {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  private val probe = TestProbe()
  val trainingToHuntEventStoreProbe = TestProbe()

  val trainingToHuntEventStoreProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingToHuntEventStoreProbe.ref forward x
    }
  })
  private val trainingToHuntEventStore = system.actorOf(trainingToHuntEventStoreProps)
  private val trainingToHuntProvider = system.actorOf(TrainingToHuntProvider.props(trainingToHuntEventStore))

  "A TrainingToHuntProvider Actor" should {
    "return active trainings to hunt from the event store" in {
      //given
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now(), None)
      //when
      trainingToHuntProvider.tell(GetTrainingsToHuntQuery(), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsgType[GetAllTrainingsToHunt]
      trainingToHuntEventStoreProbe.reply(Set(trainingToHunt))

      val response = probe.expectMsgType[Set[TrainingToHunt]]
      response should contain only trainingToHunt
    }

    "return trainings to hunt by external system training Id" in {
      //given
      val trainingToHuntMatched = TrainingToHunt(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())
      val trainingToHuntNotMatched = TrainingToHunt(TrainingToHuntId(), 2L, 2L, OffsetDateTime.now())
      //when
      trainingToHuntProvider.tell(GetTrainingsToHuntByTrainingIdQuery(1L), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsgType[GetAllTrainingsToHunt]
      trainingToHuntEventStoreProbe.reply(Set(trainingToHuntMatched, trainingToHuntNotMatched))

      val response = probe.expectMsgType[Set[TrainingToHunt]]
      response should contain only trainingToHuntMatched
    }

    "return training to hunt by Id" in {
      //given
      val id = TrainingToHuntId()
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), 1L, 2L, OffsetDateTime.now())

      //when
      trainingToHuntProvider.tell(GetTrainingToHuntQuery(id), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsg(GetTrainingToHunt(id))
      trainingToHuntEventStoreProbe.reply(Right(trainingToHunt))

      val response = probe.expectMsgType[OptionalTrainingToHunt[TrainingToHunt]]
      response shouldEqual Right(trainingToHunt)
    }
  }
}