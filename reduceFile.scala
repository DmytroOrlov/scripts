#!/usr/bin/env scalas
/***
  scalaVersion := "2.11.7"
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream-experimental" % "2.0-M2"
  )
*/
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.io.Framing
import akka.stream.stage._
import akka.util.ByteString

import scala.collection.mutable

object Main extends App {
  implicit val system = ActorSystem("main")
  implicit val context = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val delim: ByteString = ByteString("\n")
  val f = Source.inputStream(() => System.in)
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
    .toMat(
      Sink.outputStream(() => System.out)
    )(Keep.right).run()

  f.onComplete {
    //case Success(_) =>
    case _ =>
      system.shutdown()
      System.out.flush()
  }
}
Main.main(args)
