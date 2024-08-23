# Working with Files

Here you will find a curated collection of code examples that show the library in action.

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

We want to read the file, parse the scores, print them to the console and also append new scores to the file.  

In order to perform these operations we first need to create a representation of the score in Scala:  

```scala 3
case class Score(name: String, score: Int):
  def show: String = s"$name:$score"
```

And then a function that parses a string into a `Either[Throwable, Score]`:  

```scala 3
def parseScore(strScore: String): Either[Throwable, Score] =
  Either.catchNonFatal(                                               // (1)
    strScore.split(':') match
      case Array(name, score) => Score(name, score.toInt)   
      case _                  => Score("Cant parse this score", -1)   // (2)
  )
```
The method is going to return a `Left` or a `Right` according to to the outcome of the score parsing. The function consists in two steps:  
1. `Either.catchNonFatal` is a function that catches exceptions and wraps them in a `Left` value.
2. If the score cannot be parsed, we return a default score.

We can now create our script: 

```scala 3
for 
    lines  <- path.readLines                                     // (1)               
    scores <- lines.traverse(parseScore(_).liftTo[IO])           // (2)
    _      <- IO(scores.foreach(score => println(score.show)))   // (3)
    _      <- path.appendLine(Score("daniela", 100).show)        // (4)
yield ()
```

(1) We first read every line of the file as part of a list of strings. 
(2) Then, we use the `traverse` method to apply an effectfull function to every element in the list and then turn the type inside the list, inside out (like a pure `foreach`). We are parsing the scores and converting the `Either` type to an `IO` type (`List[String] => List[Either[Throwable, Score]] => List[IO[Score]] => IO[List[Score]`).  
(3) After that, we print every score to the console. 

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

import shellfish.os.FilesOs

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
      lines  <- FilesOs.readLines(path)
      scores <- lines.traverse(parseScore(_).liftTo[IO])
      _      <- IO(scores.foreach(score => println(score.show)))
      _      <- FilesOs.appendLine(path, Score("daniela", 100).show)
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

You can also manipule entire files in one go. In this example, we are going to read a file, convert it to uppercase, and write it to another file.

As the first thing, we are going to define the locations of the original file and the new file:  

```scala 3
val path      = Path("src/main/resources/quijote.txt")
val upperPath = Path("src/main/resources/quijote_screaming.txt")
```
We are going to use El Quijote for this example.  

Then, we can easily read the file, perform the transformation, and write it to a new file (we can also overwrite if we use the same path):

```scala 3
for
    file <- path.read
    _    <- upperPath.write(file.toUpperCase)
yield ()
```

Note that we loaded the whole file as a string, converted it to uppercase, and finally, wrote it to the new file.  

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

import shellfish.os.syntax.FilesOs

object Uppercase extends IOApp.Simple:

  val path      = Path("src/main/resources/quijote.txt")
  val upperPath = Path("src/main/resources/quijote_screaming.txt")

  def run: IO[Unit] =
    for
      file <- FilesOs.read(path)
      _    <- FilesOs.write(upperPath, file.toUpperCase)
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

Shellfish can handle binary files in custom serde formats since it includes [scodec](https://scodec.org/). In this example, we are going to create a binary file with a list of places.  

We start by defining type representations of the data we are going to work with:  

```scala 3
case class Place(position: Int, contestant: String)
```

Then we are going to use the `scodec` library to create a codec for the `Place` type.

A first approach would be to create your own custom codec using the combinators of scodec:

```scala 3
import scodec.codecs.*

given placeCodec: Codec[Place] = (uint8 :: utf8).as[Place]
```

But since Scala 3, you can also use the `derives` feature of Scala 3 to automatically derive a codec for your data types:  

```scala 3
case class Place(position: Int, contestant: String) derives Codec
```
That will automatically create a given instance of `Codec[Place]` for you!

We can now create a file with a list of places, read it, and print it to the console:  

```scala 3
for
    exists <- path.exists                                       // (1)
    _      <- path.createFile.unlessA(exists)                   // (2)
    _      <- path.writeAs[Place](Place(1, "Michael Phelps"))   // (3)
    place  <- path.readAs[Place]                                // (4)
    _      <- IO.println(place)                                 
yield ()
```

The scripts starts by checking the file in the path value exists (1). If it does, we create the file (2) using the `unlessA` combinator. It executes the effect if the condition is false, similar of doing a `if exists then IO.unit else path.createFile`.

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
      _      <- path.createFile.unlessA(exists)
      _      <- path.writeAs[Place](Place(1, "Michael Phelps"))
      place  <- path.readAs[Place]                                
      _      <- IO.println(place) 
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

import shellfish.os.syntax.FilesOs

object Place extends IOApp.Simple:

  case class Place(position: Int, contestant: String) derives Codec

  val path = Path("src/main/resources/place.data")

  def run: IO[Unit] =
    for
      exists <- FilesOs.exists(path)
             // Equivalent of doing `if (exists) IO.unit else path.createFile`
      _      <- FilesOs.createFile(path).unlessA(exists)
      _      <- FilesOs.writeAs[Place](path, Place(1, "Michael Phelps"))
      place  <- FilesOs.readAs[Place](path)                                
      _      <- IO.println(place)
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