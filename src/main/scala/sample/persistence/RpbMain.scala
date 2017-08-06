package sample.persistence

import java.time.Instant

import akka.actor._
import sample.persistence.data.Message

object RpbMain {
  private val actorSystem = ActorSystem("example")
  private val persistentActor = actorSystem.actorOf(Props[RpbProcessingActor], "rpb-processing-actor")

  private implicit class ActorExtensions(val actor: ActorRef) extends AnyVal {
    def !!(label: String): Unit = {
      println(s"${Instant.now()} --- ENQUEUE $label")
      actor ! Message(label)
    }
  }

  def main(args: Array[String]): Unit = {
    RpbState.global = RpbState.parser.parse(args, RpbState.global).getOrElse {
      RpbState.parser.showUsageAsError()
      sys.exit(1)
    }

    println("waiting for input...")
    for {
      line <- scala.io.Source.stdin.getLines()
      label <- line.split("\\s+")
    } {
      persistentActor !! label
    }
    actorSystem.terminate()
  }
}
