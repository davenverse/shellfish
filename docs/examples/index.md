# Working with Files

Here you'll find a curated collection of code examples that show the library in action.

## Scores

@:select(api-style)

@:choice(syntax)

```scala 3 mdoc
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

@:choice(static)

```scala 3 mdoc:reset
import cats.syntax.all.*
import cats.effect.{IO, IOApp}

import fs2.io.file.Path

import shellfish.os.FilesOs.*

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
      lines  <- readLines(path)
      scores <- lines.traverse(parseScore(_).liftTo[IO])
      _      <- IO(scores.foreach(score => println(score.show)))
      _      <- appendLine(path, Score("daniela", 100).show)
    yield ()

end Scores
```

@:choice(fs2)

```scala 3 mdoc:reset
import cats.syntax.all.*
import cats.effect.{IO, IOApp}

import fs2.Stream
import fs2.io.file.{Files, Path, Flags}


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
    Files[IO].readUtf8Lines(path)
      .evalMap(parseScore(_).liftTo[IO])
      .evalTap(scores => IO.println(scores.show))
      .last
      .flatMap( _ =>
        Stream.emit(s"\n${Score("daniela", 100).show}")
          .through(Files[IO].writeUtf8(path, Flags.Append))
      )
      .compile
      .drain

end Scores
```

@:@

## Uppercase

@:select(api-style)

@:choice(syntax)

```scala 3 mdoc
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

@:choice(static)

```scala 3 mdoc:reset
import cats.effect.{IO, IOApp}
import fs2.io.file.Path

import shellfish.os.syntax.FilesOs.*

object Uppercase extends IOApp.Simple:

  val path      = Path("src/main/resources/quijote.txt")
  val upperPath = Path("src/main/resources/quijote_screaming.txt")

  def run: IO[Unit] =
    for
      file <- read(path)
      _    <- write(upperPath, file.toUpperCase)
    yield ()

end Uppercase
```

@:choice(fs2)

```scala 3 mdoc:reset
import cats.effect.{IO, IOApp}

import fs2.io.file.{Path, Files}


object Uppercase extends IOApp.Simple:

  val path      = Path("src/main/resources/quijote.txt")
  val upperPath = Path("src/main/resources/quijote_screaming.txt")

  def run: IO[Unit] =
    Files[IO].readUtf8(path)
      .map(_.toUpperCase)
      .through(Files[IO].writeUtf8(upperPath))
      .compile
      .drain

end Uppercase
```

@:@

## Places

Shellfish OS has compatibility for working with [scodec](https://scodec.org/)!

@:select(api-style)

@:choice(syntax)

```scala 3 mdoc
import cats.syntax.applicative.*
import cats.effect.{IO, IOApp}

import fs2.io.file.Path

import scodec.codecs.*
import scodec.Codec

import shellfish.os.syntax.path.*

object Place extends IOApp.Simple:

  case class Place(number: Int, name: String) derives Codec

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

@:choice(static)

```scala 3 mdoc
import cats.syntax.applicative.*
import cats.effect.{IO, IOApp}

import fs2.io.file.Path

import scodec.codecs.*
import scodec.Codec

import shellfish.os.syntax.FilesOs.*

object Place extends IOApp.Simple:

  case class Place(number: Int, name: String) derives Codec

  val path = Path("src/main/resources/place.data")

  def run: IO[Unit] =
    for
      exists <- path.exists
           // Equivalent of doing `if (exists) IO.unit else path.createFile`
      _ <- createFile(path).whenA(exists)
      _ <- writeAs[Place](path, Place(1, "Michael Phelps"))
      _ <- readAs[Place](path).flatMap(IO.println)
    yield ()

end Place
```

@:choice(fs2)

```scala 3 mdoc
import cats.syntax.applicative.*
import cats.effect.{IO, IOApp}

import fs2.Stream
import fs2.io.file.Path
import fs2.interop.scodec.*

import scodec.codecs.*
import scodec.Codec


object Place extends IOApp.Simple:

  case class Place(number: Int, name: String) derives Codec

  val path = Path("src/main/resources/place.data")

  def run: IO[Unit] =
    for
      exists <- Files[IO].exists(path)
      // Equivalent of doing `if (exists) IO.unit else path.createFile`
      _ <- Files[IO].createFile(path).whenA(exists)

      _ <- Stream.emit(Place(1, "Michael Phelps"))
        .covary[IO]
        .through(StreamEncoder.many(placeCodec).toPipeByte)
        .through(Files[IO].writeAll(path))
        .compile
        .drain

      _ <- Files[IO].readAll(path)
        .through(StreamDecoder.many(placeCodec).toPipeByte)
        .evalTap(place => IO.println(place))
        .compile
        .drain
    yield ()

end Place
```

@:@
