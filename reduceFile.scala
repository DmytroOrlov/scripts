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
import akka.util.ByteString

object Main {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("main")
    implicit val context = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val delim: ByteString = ByteString("\n")
    val f = Source.inputStream(() => System.in)
      .via(Framing.delimiter(delim, Int.MaxValue))
      .map(_ ++ delim)
      .toMat(
        //Sink.fold(mutable.LinkedHashSet.empty[ByteString])((set, line) => set -= line += line)
        Sink.outputStream(() => System.out)
      )(Keep.right).run()

    f.onComplete {
      //case Success(_) =>
      case _ =>
        system.shutdown()
        System.out.flush()
    }
  }
}
Main.main(args)
