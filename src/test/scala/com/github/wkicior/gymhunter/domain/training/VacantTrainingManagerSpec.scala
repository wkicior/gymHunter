package com.github.wkicior.gymhunter.domain.training


import java.time.OffsetDateTime

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.tohunt.{TrainingToHunt, TrainingToHuntId, TrainingToHuntProvider}
import com.github.wkicior.gymhunter.domain.training.VacantTrainingManager.ProcessVacantTraining
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps

class VacantTrainingManagerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }
  val trainingToHuntProviderProbe = TestProbe()
  val trainingToHuntCommandHandlerProbe = TestProbe()

  val trainingToHuntProviderProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingToHuntProviderProbe.ref forward x
    }
  })
  val trainingToHuntCommandHandlerProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => trainingToHuntCommandHandlerProbe.ref forward x
    }
  })

  private val trainingHunter = system.actorOf(VacantTrainingManager.props(trainingToHuntProviderProps, trainingToHuntCommandHandlerProps))

  "A VacantTrainingManager Actor" should {
    """fetch all TrainingToHunt entities related to given Training
      |and send the notification command
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val training = Training(1, 1, OffsetDateTime.now(), OffsetDateTime.now().plusDays(2))
      val trainingToHunt = TrainingToHunt(TrainingToHuntId(), 1L, 1L, OffsetDateTime.now().plusDays(1), None)
      system.eventStream.subscribe(probe.ref, classOf[TrainingSlotsAvailableEvent])

      //when
      trainingHunter.tell(ProcessVacantTraining(training), probe.ref)

      //then
      trainingToHuntProviderProbe.expectMsg(TrainingToHuntProvider.GetTrainingsToHuntByTrainingIdQuery(training.id))
      trainingToHuntProviderProbe.reply(Set(trainingToHunt))

      probe.expectMsg(TrainingSlotsAvailableEvent(trainingToHunt.id))
    }
  }
}
