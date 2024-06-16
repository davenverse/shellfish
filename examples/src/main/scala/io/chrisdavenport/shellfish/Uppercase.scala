package io.chrisdavenport.shellfish

import cats.effect.{IO, IOApp}
import fs2.io.file.Path
import syntax.path.*

object Uppercase extends IOApp.Simple {

  val path      = Path("src/main/resources/quijote.txt")
  val upperPath = Path("src/main/resources/quijote_screaming.txt")

  def run: IO[Unit] =
    for {
      file <- path.read
      _    <- upperPath.write(file.toUpperCase)
    } yield ()

}
