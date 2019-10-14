#!/usr/bin/env amm
import ammonite.ops._
import ammonite.ops.ImplicitWd._

def sortTags[A](fs: Seq[(Path, Set[String], A)]): List[String] = {
  fs.flatMap(_._2).foldLeft(Map.empty[String, Int].withDefaultValue(0)){ (acc, t) =>
    acc + (t -> (acc(t) + 1))
  }.toList.sortBy(t => - t._2).map(_._1)
}

@scala.annotation.tailrec
def process(fs: Seq[(Path, Set[String], Path)], ts: List[String]): Seq[(Path, Set[String], Path)] =
  ts match {
    case Nil => fs
    case h :: t =>
      process(
        fs.map {
          case (from, tags, to) if tags.contains(h) => (from, tags, to/h)
          case a => a
        },
        t
      )
  }

def removeJunk(wd: Path, name: String) = ls.rec! wd |? (_.last == name) | (f => rm(f))

@main
def main(wd: Path = pwd) = {
  val ignore = "private" :: pwd.segments.toList

  def sanitize(s: String) = s match {
    case "Pictures" => "picture"
    case s => s
  }

  def files(wd: Path): Seq[(Path, Set[String], Path)] =
    ls.rec! wd |? (_.isFile) | { p =>
      (p, %%('tag, "--list", "--no-name", p.toString)(wd).out.lines.lastOption.fold(Set.empty[String])(_.split(',').toSet) ++ p.segments.filter(s => s != p.last && !ignore.contains(s)).toSet.map(sanitize), wd)
    }

  removeJunk(wd, ".DS_Store")

  val fs = files(wd)
  process(fs, sortTags(fs).diff(ignore)).foreach {
    case (from, tags, to) =>
      mkdir(to)
      val toFile = to/from.last
      if (from != toFile)
        mv(from, toFile)
      %tag("--set", tags.mkString(","), toFile.toString)
  }
}
