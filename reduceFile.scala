#!/usr/bin/env scalas
/***
  scalaVersion := "2.11.7"
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.3"
  )
*/
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.io.Framing
import akka.stream.stage._
import akka.util.ByteString

import scala.collection.mutable
import java.io.File

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
    .fold(mutable.LinkedHashSet.empty[ByteString])((ls, l) => ls -= l += l)
    .transform(() => new PushPullStage[mutable.LinkedHashSet[ByteString], ByteString]() {
      var buffer: Option[Iterator[ByteString]] = None

      override def onPush(set: mutable.LinkedHashSet[ByteString], ctx: Context[ByteString]): SyncDirective = {
        buffer = Some(set.iterator)
        doParse(ctx)
      }

      override def onPull(ctx: Context[ByteString]): SyncDirective = {
        if (buffer.isEmpty) ctx.pull()
        else if (buffer.get.hasNext) doParse(ctx)
        else ctx.finish()
      }

      override def onUpstreamFinish(ctx: Context[ByteString]): TerminationDirective = {
        if (buffer.get.hasNext) ctx.absorbTermination()
        else ctx.finish()
      }

      private def doParse(ctx: Context[ByteString]): SyncDirective = ctx.push(buffer.get.next())
    })
    .map(_ ++ delim)
    .toMat(fileSink)(Keep.right).run()
    .onComplete { case _ => system.shutdown() }
}
Main.main(args)
