package com.github.wkicior.gymhunter.domain.training.tohunt

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.training.tohunt.TrainingToHuntProvider.GetTrainingsToHuntQuery
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
    def receive = {
      case x => {
        trainingToHuntEventStoreProbe.ref forward x
      }
    }
  })
  private val trainingToHuntEventStore = system.actorOf(trainingToHuntEventStoreProps)
  private val trainingToHuntProvider = system.actorOf(TrainingToHuntProvider.props(trainingToHuntEventStore))

  "A TrainingToHuntProvider Actor" should {
    "return active trainings to hunt from the event store" in {
      //given
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), 1L, 2L)
      //when
      trainingToHuntProvider.tell(GetTrainingsToHuntQuery(), probe.ref)

      //then
      trainingToHuntEventStoreProbe.expectMsgType[TrainingToHuntEventStore.GetAllTrainingsToHunt]
      trainingToHuntEventStoreProbe.reply(Set(trainingToHunt))

      val response = probe.expectMsgType[Set[TrainingToHunt]]
      response should contain only trainingToHunt
    }
  }
}