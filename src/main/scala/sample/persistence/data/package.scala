package sample.persistence

package object data {
  sealed abstract class Internal
  case class Message(label: String) extends Internal
  case class Confirm(id: Long) extends Internal

  case class Process(id: Long, msg: Message)
}
