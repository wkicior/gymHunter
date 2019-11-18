package com.github.wkicior.gymhunter.domain.training

import java.time.OffsetDateTime

import akka.actor.Status.Success
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.github.wkicior.gymhunter.domain.subscription.{TrainingHuntingSubscription, TrainingHuntingSubscriptionId}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.language.postfixOps

class TrainingBookerSpec(_system: ActorSystem) extends TestKit(_system) with Matchers with WordSpecLike with BeforeAndAfterAll {


  def this() = this(ActorSystem("GymHunter"))

  override def afterAll: Unit = {
    shutdown(system)
  }
  val gymsteerProxyProbe = TestProbe()

  val gymsteerProxyProps = Props(new Actor {
    def receive: PartialFunction[Any, Unit] = {
      case x => gymsteerProxyProbe.ref forward x
    }
  })

  private val gymsteerProxy = system.actorOf(gymsteerProxyProps)
  private val trainingBooker = system.actorOf(TrainingBooker.props(gymsteerProxy))

  "A TrainingBooker Actor" should {
    """ask gymsteer proxy actor to book training
      |and publish TrainingHuntingSubscriptionAutoBookingEvent event
    """.stripMargin in {
      //given
      val probe = TestProbe()
      system.eventStream.subscribe(probe.ref, classOf[TrainingAutoBookingPerformedEvent])
      val ths = TrainingHuntingSubscription(TrainingHuntingSubscriptionId(), 1L, 1L, OffsetDateTime.now().plusDays(1))

      //when
      trainingBooker.tell(TrainingBooker.BookTraining(ths), probe.ref)

      //then
      gymsteerProxyProbe.expectMsg(BookTraining(1L))
      gymsteerProxyProbe.reply(Success)

      probe.expectMsgPF() {
        case ok@TrainingAutoBookingPerformedEvent(ths.externalSystemId, ths.id) => ok
      }
    }
  }
}
