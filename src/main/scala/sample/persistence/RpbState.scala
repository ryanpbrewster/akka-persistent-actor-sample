package sample.persistence

import scopt.OptionParser

case class RpbState(
  boom: Boolean,
  hang: Boolean
)

object RpbState {
  var global: RpbState = RpbState(boom = false, hang = false)

  final val parser = new OptionParser[RpbState]("rpb") {
    opt[Unit]("boom").action { (_, c) =>
      c.copy(boom = true)
    }.text("""The message "boom" will halt the JVM""")

    opt[Unit]("hang").action { (_, c) =>
      c.copy(hang = true)
    }.text("""The message "hang" will hang indefinitely""")
  }
}
