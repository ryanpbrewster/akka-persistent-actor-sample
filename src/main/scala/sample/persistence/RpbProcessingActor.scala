package sample.persistence

import java.time.Instant
import java.util.concurrent._

import akka.actor._
import akka.persistence._
import sample.persistence.data._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

class RpbProcessingActor extends PersistentActor with AtLeastOnceDelivery {
  private final val limitedExecutor = {
    val maxConcurrency = 5
    Executors.newWorkStealingPool(maxConcurrency)
  }
  private final val limitedContext = ExecutionContext.fromExecutor(limitedExecutor)

  override def persistenceId = "sample-id-1"

  private def updateState(int: Internal) = {
    int match {
      case msg: Message =>
        deliver(self.path)(id => Process(id, msg))
      case Confirm(id) =>
        confirmDelivery(id)
    }
    gc()
  }

  private def gc(): Unit = {
    if (numberOfUnconfirmed == 0) {
      deleteMessages(lastSequenceNr)
    }
  }

  override def receiveRecover: Receive = {
    case int: Internal =>
      println(s"${Instant.now()} --- RECOVER $int")
      updateState(int)

    case RecoveryCompleted =>
      println(s"${Instant.now()} --- RECOVERY_COMPLETE")

    case other =>
      println(s"${Instant.now()} --- ???RECOVER??? $other")
  }

  override def receiveCommand: Receive = {
    case msg: Message =>
      println(s"${Instant.now()} --- RECEIVE ${msg.label}")
      persist(msg)(updateState)

    case Process(id, msg) =>
      println(s"${Instant.now()} --- PROCESS ${msg.label} --> $id")
      val fut = process(msg)
      println(s"${Instant.now()} --- LAUNCHED ${msg.label} --> $id")
      fut.onComplete {
        case Success(_) =>
          println(s"${Instant.now()} --- DONE $msg")
          self ! Confirm(id)
        case Failure(_) =>
          // do not confirm failures
      }(ExecutionContext.Implicits.global)

    case confirm: Confirm =>
      println(s"${Instant.now()} --- CONFIRM ${confirm.id}")
      persist(confirm)(updateState)

    case gc: DeleteMessagesSuccess =>
      println(s"${Instant.now()} --- GC $gc")

    case other =>
      println(s"${Instant.now()} --- ???RECEIVE??? $other")
  }


  private def process(msg: Message): Future[Unit] = msg.label match {
    case "boom" if RpbState.global.boom =>
      sys.runtime.halt(42)
      ???
    case "hang" if RpbState.global.hang =>
      Future.never
    case other =>
      val millis = try {
        Integer.parseUnsignedInt(other)
      } catch {
        case _: NumberFormatException =>
          Random.nextInt(50)
      }

      Future {
        println(s"${Instant.now()} --- SLEEPING $millis")
        Thread.sleep(millis)
        println(s"${Instant.now()} --- SLEPT $millis")
      }(limitedContext)
  }
}
