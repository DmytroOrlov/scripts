#!/usr/bin/env amm
import ammonite.ops._
import ammonite.ops.ImplicitWd._

def files(wd: Path): Seq[(Path, Seq[String], Path)] = {
  ls.rec! wd |? (_.isFile) | { p => (p, %%('tag, "--list", "--no-name", p.toString)(wd).out.lines.lastOption.fold(List.empty[String])(_.split(',').toList), wd) }
}

def tags[A](fs: Seq[(Path, Seq[String], A)]): Seq[String] = {
  fs.flatMap(_._2).foldLeft(Map.empty[String, Int].withDefaultValue(0)){ (acc, t) =>
    acc + (t -> (acc(t) + 1))
  }.toSeq.sortBy(t => - t._2).map(_._1)
}

@scala.annotation.tailrec
def process(wd: Path, fs: Seq[(Path, Seq[String], Path)], ts: Seq[String]): Seq[(Path, Seq[String], Path)] = {
  if (ts.isEmpty) fs
  else {
    val t = ts.head
    process(wd, fs.map {
      case (from, tags, to) => (from, tags, if (tags.contains(t)) to/t else to)
    }, ts.tail)
  }
}

ls.rec! pwd |? (_.name == ".DS_Store") | (f => rm(f))

val fs = files(pwd)
process(pwd, fs, tags(fs)).foreach {
  case (from, _, to) =>
    mkdir(to)
    val toFile = to/from.name
    if (from != toFile)
    mv(from, toFile)
}
