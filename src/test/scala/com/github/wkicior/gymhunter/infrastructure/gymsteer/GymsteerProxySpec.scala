package com.github.wkicior.gymhunter.infrastructure.gymsteer

import java.time.OffsetDateTime

import akka.actor.Status.Success
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.training.{BookTraining, GetTraining, Training}
import com.github.wkicior.gymhunter.infrastructure.gymsteer.auth.GymsteerTokenProvider.GetToken
import com.github.wkicior.gymhunter.infrastructure.gymsteer.training.GymsteerTrainingBooker
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps

class GymsteerProxySpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }
  val gymsteerTrainingFetcherProbe = TestProbe()
  val gymsteerTokenProviderProbe = TestProbe()
  val gymsteerTrainingBookerProbe = TestProbe()

  val gymsteerTrainingFetcherProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => gymsteerTrainingFetcherProbe.ref forward x
    }
  })

  val gymsteerTokenProviderProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => gymsteerTokenProviderProbe.ref forward x
    }
  })

  val gymsteerTrainingBookerProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => gymsteerTrainingBookerProbe.ref forward x
    }
  })

  val username = "root"
  val password = "Z10N0101"

  private val gymsteerProxy = system.actorOf(GymsteerProxy.props(gymsteerTrainingFetcherProps, gymsteerTokenProviderProps, gymsteerTrainingBookerProps, username, password))

  "A GymsteerProxy Actor" should {
    """ask GymsteerTrainingFetcher for training and return response to sender
    """.stripMargin in {
      //given
      val probe = TestProbe()
      val training = Training(1, 1, Some(OffsetDateTime.now()), OffsetDateTime.now().plusDays(2))

      //when
      gymsteerProxy.tell(GetTraining(1), probe.ref)

      //then
      gymsteerTrainingFetcherProbe.expectMsg(GetTraining(1))
      gymsteerTrainingFetcherProbe.reply(training)

      probe.expectMsg(training)
    }

    """book a training by
      |getting the gymsteer token
      |and use it in order to book a training
    """.stripMargin in {
      //given
      val probe = TestProbe()

      //when
      gymsteerProxy.tell(BookTraining(1), probe.ref)

      //then
      gymsteerTokenProviderProbe.expectMsg(GetToken(username, password))
      gymsteerTokenProviderProbe.reply("some-token")

      gymsteerTrainingBookerProbe.expectMsg(GymsteerTrainingBooker.BookTraining(1, "some-token"))
      gymsteerTrainingBookerProbe.reply(Success)

      probe.expectMsg(Success)
    }
  }
}
