package com.Supervision.typed

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{TypedActorSystemOps, UntypedActorSystemOps}
import akka.actor.typed.{ActorRef, Behavior, Props, Terminated}
import akka.actor.{ActorSystem, CoordinatedShutdown, Scheduler}
import akka.util.Timeout
import com.Supervision.typed.Guardian.CreateActor

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

object SystemExt {
  implicit val timeout: Timeout = Timeout(2.seconds)

  class RichSystem(val system: ActorSystem) {
    implicit def sched: Scheduler = system.scheduler
    private val guardian          = system.spawn(Guardian.behavior, "system")
    def spawnTyped[T](behavior: Behavior[T], name: String, props: Props = Props.empty): Future[ActorRef[T]] = {
      guardian ? CreateActor(behavior, name, props)
    }
  }
}

object Guardian {

  sealed trait GuardianMsg
  case class CreateActor[T](behavior: Behavior[T], name: String, props: Props)(val replyTo: ActorRef[ActorRef[T]])
      extends GuardianMsg

  def behavior: Behavior[GuardianMsg] =
    Behaviors.receive[GuardianMsg] {
      case (ctx, msg) ⇒
        msg match {
          case create: CreateActor[t] =>
            val componentRef = ctx.spawn(create.behavior, create.name, create.props)
            ctx.watch(componentRef)
            create.replyTo ! componentRef
        }
        Behaviors.same
    } receiveSignal {
      case (ctx, Terminated(_)) ⇒
        CoordinatedShutdown(ctx.system.toUntyped).run(CoordinatedShutdown.unknownReason)
        Behaviors.stopped
    }
}
