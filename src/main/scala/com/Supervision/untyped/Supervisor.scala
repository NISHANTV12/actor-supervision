package com.Supervision.untyped

import akka.actor.{Actor, ActorRef, Props, Terminated}

class Supervisor(probe: ActorRef) extends Actor {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._

  import scala.concurrent.duration._

  var child: ActorRef = _

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case ex: ArithmeticException     ⇒ Resume
      case ex: NullPointerException    ⇒ Restart
      case _: IllegalArgumentException ⇒ Stop
      case _: Exception                ⇒ Escalate
    }

  def receive: PartialFunction[Any, Unit] = {
    case p: Props ⇒
      child = context.actorOf(p)
      sender() ! child
    case WatchChild ⇒ context.watch(child)
    case x: Terminated ⇒
      println(s"child is terminated")
      probe ! ChildTerminated(x.actor)
  }

}
