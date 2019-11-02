package com.github.wkicior.gymhunter.domain.training

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingToHuntRepositorySpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {

  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A TrainingToHuntRepository Actor" should {
    "return initially empty trainings to hunt" in {
      val probe = TestProbe()
      val trainingTracker = system.actorOf(TrainingToHuntRepository.props)
      trainingTracker.tell(TrainingToHuntRepository.GetAllTrainingsToHunt(), probe.ref)
      val response = probe.expectMsgType[Set[TrainingToHunt]]
      response.size shouldEqual 0
    }
  }
}