package com.Supervision.typed

import akka.actor.Actor
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior, PostStop, PreRestart, Signal, SupervisorStrategy, Terminated}
import com.Supervision.typed.FromActorMsg.{Alive, Spawned}
import com.Supervision.typed.ToParentMsg.{AreYouThere, Spawn, Stop, Watch}

import scala.concurrent.duration.FiniteDuration

object Supervisor {
  def behavior(watcher: ActorRef[LifecycleMsg]): Behavior[ToParentMsg] =
    Behaviors.setup(ctx ⇒ new Supervisor(ctx, watcher))
}

class Supervisor(ctx: ActorContext[ToParentMsg], watcher: ActorRef[LifecycleMsg]) extends MutableBehavior[ToParentMsg] {

  override def onMessage(msg: ToParentMsg): Behavior[ToParentMsg] = {
    var child: ActorRef[ToChildMsg] = null
    msg match {
      case Spawn(replyTo) =>
        child = ctx.spawn(
          Behaviors
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

  override def onSignal: PartialFunction[Signal, Behavior[ToParentMsg]] = {
    case Terminated(ref) ⇒
      println("Terminated in Supervisor")
      watcher ! LifecycleMsg.Terminated(ref)
      this
    case PreRestart ⇒
      println("Pre Restart in Supervisor")
      watcher ! LifecycleMsg.PreRestart(ctx.self)
      this
    case PostStop ⇒
      println("Post stop in Supervisor")
      watcher ! LifecycleMsg.PostStop(ctx.self)
      this
  }
}
