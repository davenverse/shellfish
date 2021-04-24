package io.chrisdavenport.shellfish

import cats.effect._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val s = Shell[IO]
    val p = SubProcess[IO]
    for {
      _ <- s.cd("test")
      now <- s.pwd
      _ <- s.writeTextFile("test.txt", "Hi There! 3")
      _ <- p.exec("pwd")
    } yield ExitCode.Success
  }

}