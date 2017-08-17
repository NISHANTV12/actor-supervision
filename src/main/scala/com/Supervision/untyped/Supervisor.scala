package com.Supervision.untyped

import akka.actor.{Actor, Props}

class Supervisor extends Actor {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._

  import scala.concurrent.duration._

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case ex: ArithmeticException     => println(s"Child is in trouble ${ex.getMessage}"); Resume
      case ex: NullPointerException    => println(s"Child is in big trouble ${ex.getMessage}"); Restart
      case _: IllegalArgumentException => Stop
      case _: Exception                => Escalate
    }

  def receive: PartialFunction[Any, Unit] = {
    case p: Props => sender() ! context.actorOf(p)
  }
}
