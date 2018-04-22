package com.Supervision.untyped

import akka.actor.ActorRef

case class ChildTerminated(actor: ActorRef) {}
case object WatchChild
