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

  val trainingFetcherProbe = TestProbe()
  val trainingFetcherProps = Props(new Actor {
    def receive = {
      case x => {
        println("got " + x)
        trainingFetcherProbe.ref forward x
      }
    }
  })

  val trainingHunter = system.actorOf(TrainingHunter.props(trainingFetcherProps))

  "A TrainingHunter Actor" should {
    """ask trainingFetcher for all tracked training ids
      |don't ask for training details on empty trainings
      |don't notify vacant training manager on empty trainings
    """.stripMargin in {
      val probe = TestProbe()
      trainingHunter.tell(TrainingHunter.Hunt(), probe.ref)

      trainingFetcherProbe.expectMsgType[TrainingTracker.GetTrackedTrainings]
      trainingFetcherProbe.reply(TrainingTracker.TrackedTrainingIds(List()))
    }
  }
}