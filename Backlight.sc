#!/usr/bin/env amm
import ammonite.ops._
import ammonite.ops.ImplicitWd._

import scala.annotation.tailrec
import scala.concurrent.duration._

val max = read(root/'sys/'class/'backlight/'acpi_video0/'max_brightness).trim
val intelMax = read(root/'sys/'class/'backlight/'intel_backlight/'max_brightness).trim

def intel = read(root/'sys/'class/'backlight/'intel_backlight/'brightness).trim

@tailrec
def backlight(): Unit = {
  write.over(root/'sys/'class/'backlight/"acpi_video0"/'brightness, (intel.toDouble / intelMax.toInt * max.toInt).round.toString)
  Thread.sleep(1.second.toMillis)
  backlight()
}


backlight()
