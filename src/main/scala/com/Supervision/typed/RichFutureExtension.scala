package com.Supervision.typed

import akka.actor.Scheduler
import akka.pattern

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

object RichFutureExtension {
  implicit class RichFuture[T](val f: Future[T]) extends AnyVal {
    def within(
        finiteDuration: FiniteDuration,
        sched: Scheduler,
        futureCompletion: Future[T]
    )(implicit ec: ExecutionContext): Future[T] = {
      val delayedShutdown: Future[T] = pattern.after(finiteDuration, sched)(futureCompletion)
      Future firstCompletedOf Seq(f, delayedShutdown)
    }
  }
}
