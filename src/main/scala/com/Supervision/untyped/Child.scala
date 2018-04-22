package com.Supervision.untyped

import akka.actor.Actor

class Child extends Actor {
  var state = 0
  def receive: PartialFunction[Any, Unit] = {
    case ex: Exception => throw ex
    case x: Int        => state = x
    case "get"         => sender() ! state
  }
}
