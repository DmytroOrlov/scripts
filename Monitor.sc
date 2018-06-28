#!/usr/bin/env amm
import ammonite.ops._
import ammonite.ops.ImplicitWd._

import scala.util.Try
import scala.concurrent.duration._

val key = sys.env("TELEGRAM")
val hostname = %%('hostname, "-s").out.lines.last

def gigabit() =
  for {
	interface <- (%%('route).out.lines |? (_.startsWith("default")) | (_.split(" ").last) headOption)
	speed <- (%%('ethtool, interface).out.lines |? (_.endsWith("Mb/s")) headOption).filter(s => !s.endsWith("1000Mb/s"))
  } %curl(s"https://api.telegram.org/bot$key/sendMessage?chat_id=266581762&text=$speed")

@scala.annotation.tailrec
def monitor(): Unit = {
  hostname match {
	case "dmytro-desk" => gigabit()
	case "DMYTRO-15" =>
	  val b = %%('pmset, "-g", 'batt).out.lines
	  if (b.exists(_.contains("discharging"))) {
		for {
		  bat <- (b |? (_.contains("-InternalBattery-0")) headOption)
		  p <- Try(bat.split("\t")(1).split("%")(0).toInt).toOption if p < 90
		} %curl(s"https://api.telegram.org/bot$key/sendMessage?chat_id=266581762&text=$p")
	  }
	case _ => throw new Exception
  }
  Thread.sleep(10.minutes.toMillis)
  monitor()
}

monitor()
