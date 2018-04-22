package com.Supervision.typed

import akka.actor.typed.ActorRef
import com.Supervision.typed.FromActorMsg.Spawned

sealed trait ToParentMsg
object ToParentMsg {
  case class Spawn(replyTo: ActorRef[Spawned])            extends ToParentMsg
  case class AreYouThere(replyTo: ActorRef[FromActorMsg]) extends ToParentMsg
  case class Watch(child: ActorRef[ToChildMsg])           extends ToParentMsg
  case class Stop(child: ActorRef[ToChildMsg])            extends ToParentMsg
}

sealed trait ToChildMsg
object ToChildMsg {
  case class UpdateState(x: Int)                       extends ToChildMsg
  case class GetState(replyTo: ActorRef[FromActorMsg]) extends ToChildMsg
  case class Fail(ex: Exception)                       extends ToChildMsg
}

sealed trait FromActorMsg
object FromActorMsg {
  case class Spawned(ref: ActorRef[ToChildMsg]) extends FromActorMsg
  case class CurrentState(x: Int)               extends FromActorMsg
  case object Alive                             extends FromActorMsg
}

sealed trait LifecycleMsg
object LifecycleMsg {
  case class Terminated(ref: ActorRef[_]) extends LifecycleMsg
  case class PostStop(ref: ActorRef[_])   extends LifecycleMsg
  case class PreRestart(ref: ActorRef[_]) extends LifecycleMsg
}
