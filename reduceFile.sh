#!/bin/sh
exec scala "$0" "$@"
!#

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.charset.CodingErrorAction.REPLACE

import scala.collection.mutable
import scala.io.{Codec, Source}

object ReduceFile {
  val defaultSource = System.getProperty("user.home") + "/.bash_4history"

  def main(args: Array[String]) {
    val source = if (args.length > 0) args(0) else defaultSource

    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(REPLACE)
    using(Source.fromFile(source)) { s =>
      val set = mutable.LinkedHashSet.empty[String]
      for (line <- s.getLines())
        set -= line += line

      val target = new File(source + (if (args.length > 1) args(1) else "-"))
      using(new BufferedWriter(new FileWriter(target))) { wr =>
        for (line <- set) {
          wr.append(line)
          wr.newLine()
        }
      }
    }
  }

  def using[A <: {def close() : Unit}, B](res: A)(f: A => B): B = {
    import scala.language.reflectiveCalls
    try {
      f(res)
    } finally {
      res.close()
    }
  }
}
