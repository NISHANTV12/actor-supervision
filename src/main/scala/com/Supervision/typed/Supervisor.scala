package com.Supervision.typed

import akka.typed.{ActorRef, Behavior, PostStop, PreRestart, Signal, SupervisorStrategy, Terminated}
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import com.Supervision.typed.FromActorMsg.{Alive, Spawned}
import com.Supervision.typed.ToParentMsg.{AreYouThere, Spawn, Stop, Watch}

import scala.concurrent.duration.FiniteDuration

object Supervisor {
  def behavior(watcher: ActorRef[LifecycleMsg]): Behavior[ToParentMsg] =
    Actor.mutable(ctx ⇒ new Supervisor(ctx, watcher))
}

class Supervisor(ctx: ActorContext[ToParentMsg], watcher: ActorRef[LifecycleMsg]) extends MutableBehavior[ToParentMsg] {

  val signalHandler: PartialFunction[Signal, Behavior[ToParentMsg]] = {
    case Terminated(ref) ⇒
      watcher ! LifecycleMsg.Terminated(ref)
      this
    case PreRestart ⇒
      watcher ! LifecycleMsg.PreRestart(ctx.self)
      this
    case PostStop ⇒
      watcher ! LifecycleMsg.PostStop(ctx.self)
      this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ToParentMsg]] = signalHandler

  override def onMessage(msg: ToParentMsg): Behavior[ToParentMsg] = {
    var child: ActorRef[ToChildMsg] = null
    msg match {
      case Spawn(replyTo) =>
        child = ctx.spawn(
          Actor
            .supervise(Child.behavior(watcher))
            .onFailure[RuntimeException](
              SupervisorStrategy.restartWithLimit(1, FiniteDuration(1, "seconds")).withLoggingEnabled(true)
            ),
          "child"
        )
        ctx.watch(child)
        replyTo ! Spawned(child)
      case AreYouThere(replyTo: ActorRef[FromActorMsg]) ⇒ replyTo ! Alive
      case Watch(strayActor)                            ⇒ ctx.watch(strayActor)
      case Stop(actor)                                  ⇒ ctx.stop(actor)
    }
    this
  }
}
