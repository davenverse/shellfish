package io.chrisdavenport.shellfish

import cats.effect._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    import Shell.io._
    val p = SubProcess[IO]
    for {
      init <- pwd
      _ <- echo(init)
      _ <- cd("test")
      now <- pwd
      _ <- echo(now)
      _ <- writeTextFile("test.txt", "Hi There! 3")
      _ <- p.exec("pwd")
    } yield ExitCode.Success
  }

}