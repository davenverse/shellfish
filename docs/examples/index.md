# Working with Files

Here you'll find a curated collection of code examples that show the library in action.

## Scores

Imagine we have a file with scores in the following format (`<name>:<score>`):

```text
michel:21839
zeta:9929
thanh:20933
tonio:12340
arman:6969
gabriel:32123
```

We also want to read the file, parse the scores, and print them to the console. We also want to append a new score to the file.

For that, you will first need to create a representation of the scores in Scala, that's via a `case class`:

```scala 3
case class Score(name: String, score: Int):
  def show: String = s"$name:$score"
```
The `show` function is just a `toString()` method, but in the world of the Cats library!

Then you will need to create a function that parses a string into a `Score`:

```scala 3
def parseScore(strScore: String): Either[Throwable, Score] =
  Either.catchNonFatal(                                               // (1)
    strScore.split(':') match
      case Array(name, score) => Score(name, score.toInt)   
      case _                  => Score("Cant parse this score", -1)   // (2)
  )
```
The method is going to return an `Either` type, which is a type that can be either a `Left` or a `Right`. In this case, we are using it to represent a success or a failure in parsing the score, its very similar to the `Result` type in Rust! Here the function is doing two things:

1. `Either.catchNonFatal` is a function that catches exceptions and wraps them in a `Left` value.
2. If the score can't be parsed, we return a default score.

After that, we start creating our script: 

```scala 3
for 
    lines  <- path.readLines                                     // (1)               
    scores <- lines.traverse(parseScore(_).liftTo[IO])           // (2)
    _      <- IO(scores.foreach(score => println(score.show)))   // (3)
    _      <- path.appendLine(Score("daniela", 100).show)        // (4)
yield ()
```

First, we load every line as part of a list of strings (1). Then, we use the `traverse` method to apply an effect to every of the elements of the list and then turn the type inside the list, inside out (like a pure `foreach`) (2), in this case, we are parsing the scores and converting the `Either` type to an `IO` type(`List[String] => List[Right[Score]] => List[IO[Score]] => IO[List[Score]`). After that, we print every score to the console (3). 

Finally, we append a new score to the file (4) via the `appendLine` method.

Here is the complete example of the Scores example:

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

You can also manipule entire files on one go. In this example, we are going to read a file, convert it to uppercase, and write it to another file.

First thing, we are going to define the locations of the original file and the new file:

```scala 3
val path      = Path("src/main/resources/quijote.txt")
val upperPath = Path("src/main/resources/quijote_screaming.txt")
```
We are going to use El Quijote for this example!

Then, we can easily read the file, perform the transformation, and write it to a new file (we can also overwrite if we use the same path):

```scala 3
for
    file <- path.read
    _    <- upperPath.write(file.toUpperCase)
yield ()
```

Note that we loaded the file as a string, then we converted it to uppercase, and finally, we wrote it to the new file.

Here, the complete script:

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

Maybe you also want to work with binary files. In this example, we are going to create a binary file with a list of places. Because Shellfish has compatibility for working with [scodec](https://scodec.org/), we are going to use that library to encode and decode the binary file!

As always, we start by defining type representations of the data we are going to work with:

```scala 3
case class Place(position: Int, contestant: String)
```

After that, we are going to use the `scodec` library to create a codec for the `Place` type.

A first approach would be to create your own custom codec using the combinators of scodec:

```scala 3
import scodec.codecs.*

given placeCodec: Codec[Place] = (uint8 :: utf8).as[Place]
```

But since Scala 3, you can also use the `derives` feature of Scala 3 to automatically derive a codec for your type:

```scala 3
case class Place(position: Int, contestant: String) derives Codec
```
That will automatically create a given instance of `Codec[Place]` for you!

Then, we can create a file with a list of places, read it, and print it to the console:

```scala 3
for
    exists <- path.exists                                       // (1)
    _      <- path.createFile.whenA(exists)                     // (2)
    _      <- path.writeAs[Place](Place(1, "Michael Phelps"))   // (3)
    _      <- path.readAs[Place].flatMap(IO.println)            // (4)
yield ()
```

The scripts stats by checking the file in the path value exists (1). If it does, we create the file (2) using the `whenA` combinator. It executes the effect if the condition is true, similar of doing a `if exists then IO.unit else path.createFile`.

Then, when can automatically read and write the `Place` type to the file using the `writeAs` and `readAs` methods (3 and 4). That is thanks to the given codec in scope we created before! It handles the encoding and decoding of the binary file automatically. 

Here is the complete script of the Places example:

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

  case class Place(position: Int, contestant: String) derives Codec

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

  case class Place(position: Int, contestant: String) derives Codec

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

  case class Place(position: Int, contestant: String) derives Codec

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
