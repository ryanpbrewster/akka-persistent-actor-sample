package sample.persistence

import java.time.Instant

import akka.actor.ActorRef

package object data {
  sealed abstract class Internal
  case class Message(label: String) extends Internal
  case class Confirm(id: Long) extends Internal

  case class Process(id: Long, msg: Message)

  implicit class ActorExtensions(val actor: ActorRef) extends AnyVal {
    def !!(label: String): Unit = {
      println(s"${Instant.now()} --- ENQUEUE $label")
      actor ! Message(label)
    }
  }

}
