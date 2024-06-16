package io.chrisdavenport.shellfish

import cats.syntax.all.*
import cats.effect.{IO, IOApp}

import fs2.io.file.*

import io.chrisdavenport.shellfish.syntax.path.*

object Scores extends IOApp.Simple {

  case class Score(name: String, score: Int) {
    def show: String = s"$name:$score"
  }

  def parseScore(strScore: String): Either[Throwable, Score] =
    Either.catchNonFatal(
      strScore.split(':') match {
        case Array(name, score) => Score(name, score.toInt)
        case _                  => Score("Cant parse this score", -1)
      }
    )

  val path = Path("src/main/resources/scores.txt")
  override def run: IO[Unit] =
    for {
      lines  <- path.readLines
      scores <- lines.map(parseScore(_).liftTo[IO]).sequence
      _      <- IO(scores.foreach(score => println(score.show)))
      bytes  <- path.append(s"\n${Score("daniela", 100).show}")
      _      <- IO.println(s"Successfully written $bytes bytes.")
    } yield ()
}
