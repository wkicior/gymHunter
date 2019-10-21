package com.github.wkicior.gymhunter.domain.training

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps



class TrainingTrackerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A TrainingTracker Actor" should {
    "return some tracked training ids" in {
      val probe = TestProbe()
      val trainingTracker = system.actorOf(TrainingTracker.props)
      trainingTracker.tell(TrainingTracker.GetTrackedTrainings(), probe.ref)
      val response = probe.expectMsgType[TrainingTracker.TrackedTrainingIds]
      response.ids.size should be > 0
    }
  }
}