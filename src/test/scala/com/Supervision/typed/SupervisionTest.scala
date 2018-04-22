package com.Supervision.typed

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import com.Supervision.typed.FromActorMsg.{CurrentState, Spawned}
import com.Supervision.typed.ToChildMsg.{Fail, GetState, UpdateState}
import com.Supervision.typed.ToParentMsg.{Spawn, Stop, Watch}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SupervisionTest extends FunSuite with Matchers {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "test-typed-system")
  implicit val settings: TestKitSettings    = TestKitSettings(system)
  implicit val timeout: Timeout             = Timeout(5.seconds)

  test("an actor can watch an actor it creates for signals") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent-1"), 5.seconds)

    parent ! Spawn(testProbe.ref)

    val spawnedActor = testProbe.expectMessageType[Spawned]
    spawnedActor.ref ! UpdateState(100)
    spawnedActor.ref ! GetState(testProbe.ref)

    testProbe.expectMessage(CurrentState(100))

    spawnedActor.ref ! Fail(new RuntimeException("You need to stop"))
    watcherProbe.expectMessage(LifecycleMsg.PreRestart(spawnedActor.ref))
    spawnedActor.ref ! GetState(testProbe.ref)
    testProbe.expectMessage(CurrentState(0))

    spawnedActor.ref ! Fail(new RuntimeException("You need to stop"))
    watcherProbe.expectMessage(LifecycleMsg.PostStop(spawnedActor.ref))
    watcherProbe.expectMessage(LifecycleMsg.Terminated(spawnedActor.ref))
  }

  test("an actor can watch an actor it does not create") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent-2"), 5.seconds)
    val stray  = Await.result(system.systemActorOf(Child.behavior(watcherProbe.ref), "Stray"), 5.seconds)

    parent ! Watch(stray)

    stray ! UpdateState(100)
    stray ! GetState(testProbe.ref)

    testProbe.expectMessage(CurrentState(100))

    stray ! Fail(new RuntimeException("You need to stop"))
    watcherProbe.expectMessage(LifecycleMsg.PostStop(stray))
    watcherProbe.expectMessage(LifecycleMsg.Terminated(stray))
  }

  test("an actor can stop an actor it creates") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent-3"), 5.seconds)

    parent ! Spawn(testProbe.ref)

    val spawnedActor = testProbe.expectMessageType[Spawned]
    spawnedActor.ref ! UpdateState(100)
    spawnedActor.ref ! GetState(testProbe.ref)

    testProbe.expectMessage(CurrentState(100))

    parent ! Stop(spawnedActor.ref)
    watcherProbe.expectMessage(LifecycleMsg.PostStop(spawnedActor.ref))
    watcherProbe.expectMessage(LifecycleMsg.Terminated(spawnedActor.ref))
  }

  test("an actor can not stop an actor it does not create") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent-4"), 5.seconds)
    val stray  = Await.result(system.systemActorOf(Child.behavior(watcherProbe.ref), "Child"), 5.seconds)

    stray ! UpdateState(100)
    stray ! GetState(testProbe.ref)

    testProbe.expectMessage(CurrentState(100))

    parent ! Watch(stray)
    parent ! Stop(stray)
    watcherProbe.expectNoMessage(1.second)
    stray ! GetState(testProbe.ref)

    testProbe.expectMessage(CurrentState(100))
  }

  test("default supervision") {}

  test("signals") {}

  test("terminate system test") {
    val testProbe    = TestProbe[FromActorMsg]
    val watcherProbe = TestProbe[LifecycleMsg]

    val parent = Await.result(system.systemActorOf(Supervisor.behavior(watcherProbe.ref), "Parent-5"), 5.seconds)

    parent ! Spawn(testProbe.ref)

    val spawnedActor = testProbe.expectMessageType[Spawned]
    spawnedActor.ref ! UpdateState(100)
    spawnedActor.ref ! GetState(testProbe.ref)

    testProbe.expectMessage(CurrentState(100))

    Await.result(system.terminate(), 10.seconds)
  }

}
