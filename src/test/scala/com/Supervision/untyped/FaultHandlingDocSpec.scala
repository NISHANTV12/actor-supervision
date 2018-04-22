package com.Supervision.untyped

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props, Terminated}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.collection.immutable
import scala.concurrent.duration.DurationInt

class FaultHandlingDocSpec(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterAll {

  def this() =
    this(
      ActorSystem(
        "FaultHandlingDocSpec",
        ConfigFactory.parseString("""
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        loglevel = "WARNING"
      }
      """)
      )
    )

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  test("an actor watches death of an actor it creates") {
    val supervisor = system.actorOf(Props(new Supervisor(this.testActor)), "supervisor")

    supervisor ! Props[Child]
    val child = expectMsgType[ActorRef] // retrieve answer from TestKit’s testActor

    child ! 40
    child ! "get"
    expectMsg(40)

    child ! PoisonPill

    expectNoMessage(3.seconds)
  }

  test("an actor watches death of an actor it creates") {
    val supervisor = system.actorOf(Props(new Supervisor(this.testActor)), "supervisor")

    supervisor ! Props[Child]
    val child = expectMsgType[ActorRef] // retrieve answer from TestKit’s testActor

    supervisor ! WatchChild
    child ! 40
    child ! "get"
    expectMsg(40)

    child ! PoisonPill

    expectMsg(ChildTerminated(child))
  }

  test("an actor can watch an actor it does not create") {
    val supervisor = system.actorOf(Props(new Supervisor(this.testActor)), "supervisor")

    supervisor ! Props[Child]
    val child = expectMsgType[ActorRef] // retrieve answer from TestKit’s testActor

    watch(child)
    child ! 40
    child ! "get"
    expectMsg(40)

    child ! PoisonPill

    val msg = expectMsgType[Terminated]
    msg.actor shouldBe child
  }

  test("an actor can stop an actor it creates") {}

  test("an actor can stop an actor it does not create") {}

  test("default supervision") {}

  test("signals") {}

  test("A supervisor must apply the chosen strategy for its child") {
    val supervisor = system.actorOf(Props(new Supervisor(testActor)), "supervisor")

    supervisor ! Props[Child]
    val child = expectMsgType[ActorRef] // retrieve answer from TestKit’s testActor

    child ! 42 // set state to 42
    child ! "get"
    expectMsg(42)

    child ! new ArithmeticException("ArithmeticException Message") // crash it
    child ! "get"
    expectMsg(42)

    child ! new NullPointerException("NullPointerException message") // crash it harder
    child ! "get"
    expectMsg(0)

    watch(child) // have testActor watch “child”
    child ! new IllegalArgumentException // break it
    expectMsgPF() { case Terminated(`child`) => println("Child is terminated") }
  }
}
