#!/usr/bin/env scalas
/***
  scalaVersion := "2.11.8"
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % "2.4.2"
  )
*/
import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.collection.mutable

object Main extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit val context = system.dispatcher

  lazy val default = System.getProperty("user.home") + "/.bash_4history"
  val path = if (args.length > 0) args(0) else default

  val fileSource = FileIO.fromFile(new File(path))
  val fileSink = FileIO.toFile(new File(path + (if (args.length > 1) args(1) else "-")))

  val delim = ByteString("\n")
  fileSource
    .via(Framing.delimiter(delim, Int.MaxValue))
    .grouped(5000)
    .map(_.foldLeft(mutable.LinkedHashSet.empty[ByteString])((ls, l) => ls -= l += l))
    .mapConcat(ls => new scala.collection.immutable.Iterable[ByteString]() {
      def iterator = ls.iterator
    })
    .map(_ ++ delim)
    .toMat(fileSink)(Keep.right).run()
    .onComplete { case _ => system.terminate() }
}
Main.main(args)
