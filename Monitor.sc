#!/usr/bin/env amm
import ammonite.ops._
import ammonite.ops.ImplicitWd._

import scala.sys.process._
import scala.util.Try
import scala.concurrent.duration._

val key = sys.env("TELEGRAM")

@scala.annotation.tailrec
def monitor(): Unit = {
  Try {
    val interface = %%('route).out.lines |? (_.startsWith("default")) | (_.split(" ").last) head
    val speed = (%%('ethtool, interface).out.lines |? (_.endsWith("Mb/s")) headOption).filter(s => !s.endsWith("1000Mb/s"))
    speed.foreach(s => %curl(s"https://api.telegram.org/bot$key/sendMessage?chat_id=266581762&text=$s"))
  }
  Thread.sleep(5.minutes.toMillis)
  monitor()
}

@scala.annotation.tailrec
def batt(): Unit = {
  Try {
    val b = %%('pmset, "-g", 'batt).out.lines
    if (b.exists(_.contains("discharging"))) {
      val p = (b |? (_.contains("-InternalBattery-0")) head).split("-InternalBattery-0")(1).trim.split("%")(0).toInt
      if (p < 90) %curl(s"https://api.telegram.org/bot$key/sendMessage?chat_id=266581762&text=$p")
    }
  }
  Thread.sleep(5.minutes.toMillis)
  batt()
}

%%('hostname, "-s").out.lines.last match {
  case "dmytro-desk" => monitor()
  case "DMYTRO-15" => batt()
  case _ =>
}
