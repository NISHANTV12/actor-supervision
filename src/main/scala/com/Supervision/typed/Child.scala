package com.Supervision.typed

import akka.typed.{ActorRef, Behavior, PostStop, PreRestart, Signal}
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import com.Supervision.typed.FromActorMsg.CurrentState
import com.Supervision.typed.ToChildMsg.{Fail, GetState, UpdateState}

object Child {
  def behavior(watcher: ActorRef[LifecycleMsg]): Behavior[ToChildMsg] = Actor.mutable(ctx ⇒ new Child(ctx, watcher))
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
      watcher ! LifecycleMsg.PostStop(ctx.self)
      this
    case PreRestart ⇒
      watcher ! LifecycleMsg.PreRestart(ctx.self)
      this
  }
}
