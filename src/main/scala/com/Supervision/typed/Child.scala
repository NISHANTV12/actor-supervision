package com.Supervision.typed

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior, PostStop, PreRestart, Signal}
import com.Supervision.typed.FromActorMsg.CurrentState
import com.Supervision.typed.ToChildMsg.{Fail, GetState, UpdateState}

object Child {
  def behavior(watcher: ActorRef[LifecycleMsg]): Behavior[ToChildMsg] = Behaviors.setup(ctx ⇒ new Child(ctx, watcher))
}

class Child(ctx: ActorContext[ToChildMsg], watcher: ActorRef[LifecycleMsg]) extends MutableBehavior[ToChildMsg] {
  var state: Int = 0
  override def onMessage(msg: ToChildMsg): Behavior[ToChildMsg] = {
    msg match {
      case UpdateState(x)    => state = x
      case GetState(replyTo) => replyTo ! CurrentState(state)
      case Fail(ex)          ⇒ throw ex
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ToChildMsg]] = {
    case PostStop ⇒
      println("Post stop of child")
      watcher ! LifecycleMsg.PostStop(ctx.self)
      this
    case PreRestart ⇒
      println(s"Pre restart of Child$state")
      watcher ! LifecycleMsg.PreRestart(ctx.self)
      this
  }
}
