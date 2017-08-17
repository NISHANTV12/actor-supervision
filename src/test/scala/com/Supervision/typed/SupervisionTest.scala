package com.Supervision.typed

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.util.Timeout
import com.Supervision.typed.FromActorMsg.{CurrentState, Spawned}
import com.Supervision.typed.ToChildMsg.{Fail, GetState, UpdateState}
import com.Supervision.typed.ToParentMsg.{Spawn, Stop, Watch}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupervisionTest extends FunSuite with Matchers {

  implicit val system   = ActorSystem(Actor.empty, "testHcd")
  implicit val settings = TestKitSettings(system)
  implicit val timeout  = Timeout(5.seconds)

  test("an actor can watch an actor it creates for signals") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent"), 5.seconds)

    parent ! Spawn(testProbe.ref)

    val spawnedActor = testProbe.expectMsgType[Spawned]
    spawnedActor.ref ! UpdateState(100)
    spawnedActor.ref ! GetState(testProbe.ref)

    testProbe.expectMsg(CurrentState(100))

    spawnedActor.ref ! Fail(new RuntimeException("You need to stop"))
    watcherProbe.expectMsg(LifecycleMsg.PreRestart(spawnedActor.ref))
    spawnedActor.ref ! GetState(testProbe.ref)
    testProbe.expectMsg(CurrentState(0))

    spawnedActor.ref ! Fail(new RuntimeException("You need to stop again"))
    watcherProbe.expectMsg(LifecycleMsg.PostStop(spawnedActor.ref))
    watcherProbe.expectMsg(LifecycleMsg.Terminated(spawnedActor.ref))
  }

  test("an actor can watch an actor it does not create") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent"), 5.seconds)
    val stray  = Await.result(system.systemActorOf(Child.behavior(watcherProbe.ref), "Stray"), 5.seconds)

    parent ! Watch(stray)

    stray ! UpdateState(100)
    stray ! GetState(testProbe.ref)

    testProbe.expectMsg(CurrentState(100))

    stray ! Fail(new RuntimeException("You need to stop"))
    watcherProbe.expectMsg(LifecycleMsg.PostStop(stray))
    watcherProbe.expectMsg(LifecycleMsg.Terminated(stray))
  }

  test("an actor can stop an actor it creates") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent"), 5.seconds)

    parent ! Spawn(testProbe.ref)

    val spawnedActor = testProbe.expectMsgType[Spawned]
    spawnedActor.ref ! UpdateState(100)
    spawnedActor.ref ! GetState(testProbe.ref)

    testProbe.expectMsg(CurrentState(100))

    parent ! Stop(spawnedActor.ref)
    watcherProbe.expectMsg(LifecycleMsg.PostStop(spawnedActor.ref))
    watcherProbe.expectMsg(LifecycleMsg.Terminated(spawnedActor.ref))
  }

  test("an actor can not stop an actor it does not create") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent"), 5.seconds)
    val stray  = Await.result(system.systemActorOf(Child.behavior(watcherProbe.ref), "Parent"), 5.seconds)

    stray ! UpdateState(100)
    stray ! GetState(testProbe.ref)

    testProbe.expectMsg(CurrentState(100))

    parent ! Watch(stray)
    parent ! Stop(stray)
    watcherProbe.expectNoMsg(1.second)
    stray ! GetState(testProbe.ref)

    testProbe.expectMsg(CurrentState(100))
  }
}
