#!/usr/bin/env amm
import ammonite.ops._
import ammonite.ops.ImplicitWd._

import scala.util.Try
import scala.concurrent.duration._

val key = sys.env("TELEGRAM")
val hostname = %%('hostname, "-s").out.lines.last

@scala.annotation.tailrec
def monitor(): Unit = {
  Try {
    hostname match {
      case "dmytro-desk" =>
        val interface = %%('route).out.lines |? (_.startsWith("default")) | (_.split(" ").last) head
        val speed = (%%('ethtool, interface).out.lines |? (_.endsWith("Mb/s")) headOption).filter(s => !s.endsWith("1000Mb/s"))
        speed.foreach(s => %curl(s"https://api.telegram.org/bot$key/sendMessage?chat_id=266581762&text=$s"))
      case "DMYTRO-15" =>
        val b = %%('pmset, "-g", 'batt).out.lines
        if (b.exists(_.contains("discharging"))) {
          val p = (b |? (_.contains("-InternalBattery-0")) head).split("\t")(1).trim.split("%")(0).toInt
          if (p < 90) %curl(s"https://api.telegram.org/bot$key/sendMessage?chat_id=266581762&text=$p")
        }
      case _ => throw new Exception
    }
  }
  Thread.sleep(5.minutes.toMillis)
  monitor()
}

monitor()
