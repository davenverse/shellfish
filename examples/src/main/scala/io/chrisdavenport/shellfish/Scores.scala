/*
 * Copyright (c) 2024 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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