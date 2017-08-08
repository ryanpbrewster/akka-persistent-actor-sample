package sample.persistence

import java.time.Instant
import java.util.concurrent._

import akka.actor._
import akka.persistence._
import sample.persistence.data._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

class RpbProcessingActor extends Actor {
  private implicit final val limitedContext = {
    val maxConcurrency = 5
    ExecutionContext.fromExecutor(Executors.newWorkStealingPool(maxConcurrency))
  }

  override def receive: Receive = {
    case Process(id, msg) =>
      println(s"${Instant.now()} --- PROCESS ${msg.label} --> $id")
      val fut = process(msg)
      println(s"${Instant.now()} --- LAUNCHED ${msg.label} --> $id")
      val sndr = sender()
      fut.onComplete {
        case Success(_) =>
          println(s"${Instant.now()} --- DONE $msg")
          sndr ! Confirm(id)
        case Failure(_) =>
          // do not confirm failures
      }
  }

  private def process(msg: Message): Future[Unit] = {
    if (RpbState.recovery) Future.successful(())
    else msg.label match {
      case "boom" =>
        sys.runtime.halt(42)
        Future.never

      case "hang" =>
        Future.never

      case other =>
        val duration = try {
          Duration.create(other)
        } catch {
          case _: NumberFormatException =>
            Random.nextInt(50).milliseconds
        }
        Future(Thread.sleep(duration.toMillis))
    }
  }
}
