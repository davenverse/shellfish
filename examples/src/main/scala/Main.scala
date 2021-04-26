package io.chrisdavenport.shellfish

import cats.effect._
import cats.syntax.all._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    import Shell.io._
    val p = SubProcess.io
    for {
      // init <- pwd
      // _ <- echo(init)
      // _ <- mkdir("test")
      // _ <- cd("test")
      // now <- pwd
      // _ <- echo(now)
      // _ <- writeTextFile("test.txt", "Hi There! 3")

      // _ <- p.shellStrict("pwd").flatTap(s => echo(s.toString))
      // got <- p.shellStrict("find", List("-wholename", "*.scala"))
      // got <- which("java")
      // got <- hostname
      _ <- cd("..")
      got <- ls.compile.toList
      _ <- echo(got.toString)
      // _ <- p.shellStrict("which", "java":: Nil).flatMap(a => echo(a.toString))
    } yield ExitCode.Success
  }

}