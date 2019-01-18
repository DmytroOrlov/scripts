#!/usr/bin/env amm
import ammonite.ops._

val t = tmp()
read.lines(home/".bash_4history").map(_ + "\n").foldLeft(scala.collection.mutable.LinkedHashSet.empty[String])((acc, l) => acc -=l += l) |> (write.over(t, _))
mv.over(home/".bash_4history", home/".bash_4history-")
mv(t, home/".bash_4history")
