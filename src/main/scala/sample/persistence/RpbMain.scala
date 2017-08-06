package sample.persistence

import akka.actor._
import sample.persistence.data.ActorExtensions

object RpbMain {
  private val actorSystem = ActorSystem("example")
  private val persistentActor = actorSystem.actorOf(Props[RpbProcessingActor], "rpb-processing-actor")

  def main(args: Array[String]): Unit = {
    if (RpbState.parse(args)) {
      for {
        line <- scala.io.Source.stdin.getLines()
        label <- line.split("\\s+")
      } {
        persistentActor !! label
      }
    }
    actorSystem.terminate()
  }
}
