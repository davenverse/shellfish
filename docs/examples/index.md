# Examples

Here you'll find a curated collection of code examples that show the library in action. 

## Scores
``` scala 3 mdoc
import cats.syntax.all.*
import cats.effect.{IO, IOApp}
import fs2.io.file.Path

import shellfish.os.syntax.path.*

object Scores extends IOApp.Simple:

  case class Score(name: String, score: Int):
    def show: String = s"$name:$score"

  def parseScore(strScore: String): Either[Throwable, Score] =
    Either.catchNonFatal(
      strScore.split(':') match 
        case Array(name, score) => Score(name, score.toInt)
        case _                  => Score("Cant parse this score", -1)
    )

  val path = Path("src/main/resources/scores.txt")

  def run: IO[Unit] =
    for
      lines  <- path.readLines
      scores <- lines.traverse(parseScore(_).liftTo[IO])
      _      <- IO(scores.foreach(score => println(score.show)))
      _      <- path.appendLine(Score("daniela", 100).show)
    yield ()

end Scores
```

## Uppercase
``` scala 3 mdoc
import cats.effect.{IO, IOApp}
import fs2.io.file.Path

import shellfish.os.syntax.path.*

object Uppercase extends IOApp.Simple:

  val path      = Path("src/main/resources/quijote.txt")
  val upperPath = Path("src/main/resources/quijote_screaming.txt")

  def run: IO[Unit] =
    for
      file <- path.read
      _    <- upperPath.write(file.toUpperCase)
    yield ()

end Uppercase

```

## Places

Shellfish OS has compatibility for working with scodecs!

``` scala 3 mdoc
import cats.syntax.applicative.*
import cats.effect.{IO, IOApp}

import fs2.io.file.Path

import scodec.codecs.*
import scodec.Codec

import shellfish.os.syntax.path.*

object Place extends IOApp.Simple:

  case class Place(number: Int, name: String) derives Codec

  // Only in Scala 2:
  // implicit val placeCodec: Codec[Place] = (int32 :: utf8).as[Place]

  val path = Path("src/main/resources/place.data")

  def run: IO[Unit] =
    for
      exists <- path.exists
           // Equivalent of doing `if (exists) IO.unit else path.createFile`
      _ <- path.createFile.whenA(exists)
      _ <- path.writeAs[Place](Place(1, "Michael Phelps"))
      _ <- path.readAs[Place].flatMap(IO.println)
    yield ()

end Place
```