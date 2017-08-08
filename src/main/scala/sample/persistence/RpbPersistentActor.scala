package sample.persistence

import java.time.Instant

import akka.actor._
import akka.persistence._
import sample.persistence.data._

class RpbPersistentActor(processor: ActorRef) extends PersistentActor with AtLeastOnceDelivery {
  override def persistenceId = "sample-id-1"

  private def updateState(int: Internal) = {
    int match {
      case msg: Message =>
        deliver(processor.path)(id => Process(id, msg))
      case Confirm(id) =>
        confirmDelivery(id)
        gc()
    }
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

    case confirm: Confirm =>
      println(s"${Instant.now()} --- CONFIRM ${confirm.id}")
      persist(confirm)(updateState)

    case gc: DeleteMessagesSuccess =>
      println(s"${Instant.now()} --- GC $gc")

    case other =>
      println(s"${Instant.now()} --- ???RECEIVE??? $other")
  }
}
