package sample.persistence

import scopt.OptionParser

object RpbState extends OptionParser[Unit]("rpb") {
  var recovery: Boolean = false

  help("help").text("prints this usage text")

  opt[Unit]("recovery").foreach { _ =>
    recovery = true
  }.text("""All messages will complete immediately and without errors""")
}
