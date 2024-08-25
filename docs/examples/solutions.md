# Solutions 

This section offers possible solutions to the documentation's exercises. Refer to them if you get stuck or simply want to compare your approach with another.

## Reading and printing a file

```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import fs2.io.file.Path
import io.chrisdavenport.shellfish.syntax.path.*

object ReadingSolution extends IOApp.Simple:

  val path = Path("testdata/readme.txt")

  def run: IO[Unit] =
    for
      file <- path.read      // This part corresponds more or less to the path.read.flatMap ( ... )
      _ <- IO(println(file)) // And this one is the ... flatMap( file => IO(println(file) )
    yield ()

end ReadingSolution
```


## Writing and modifying the contents of a file

```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import fs2.io.file.Path
import io.chrisdavenport.shellfish.syntax.path.*

object WritingSolution extends IOApp.Simple:

  val part1 = Path("books/Dune: Part One")
  val part2 = Path("books/Dune: Part Two")

  def run: IO[Unit] =
    for
      book1 <- part1.read
      book2 <- part2.read
      _ <- Path("books/Dune: The whole Saga").write(book1 ++ book2)
    yield ()

end WritingSolution
```

## Working line by line

```scala mdoc:compile-only
import cats.effect.{IO, IOApp}
import fs2.io.file.Path
import io.chrisdavenport.shellfish.syntax.path.*

object LinesSolution extends IOApp.Simple:

  val noSpace = Path("poems/edgar_allan_poe/no_spaced_dream.txt")
  val spaced  = Path("poems/edgar_allan_poe/spaced_dream.txt")

  def run: IO[Unit] =
    for
      poemLines <- noSpace.readLines
      _ <- spaced.writeLines(poemLines.map(verse => s"$verse\n"))
    yield ()

end LinesSolution
```


## Create a file

```scala mdoc:compile-only
import cats.effect.IO
import fs2.io.file.Path
import io.chrisdavenport.shellfish.syntax.path.*

extension (path: Path) 
  def createFileAndDirectories: IO[Unit] = 
    path.parent match
      case Some(dirs) => dirs.createDirectories >> path.fileName.createFile
      case None       => path.createFile
```


## Deleting here and there

```scala mdoc:compile-only
import cats.syntax.all.*
import cats.effect.IO
import fs2.io.file.Path
import io.chrisdavenport.shellfish.syntax.path.*

extension (path: Path) 
  def deleteIfChubby(threshold: Long): IO[Boolean] = 
    for
      size    <- path.size
      isFat   = (size >= threshold).pure[IO]
      deleted <- isFat.ifM(path.deleteIfExists, false.pure[IO])
    yield deleted
```

## Using temporary files

```scala mdoc:compile-only
import cats.effect.{IO, Resource}
import fs2.io.file.Path
import io.chrisdavenport.shellfish.syntax.path.*

def makeTempFile: Resource[IO, Path] = 
  Resource.make(createTempFile)(p => p.deleteIfExists.void)

def makeTempDirectory: Resource[IO, Path] = 
  Resource.make(createTempDirectory): tempDir =>
    tempDir.deleteRecursively.recoverWith:
      case _: java.nio.file.NoSuchFileException => IO.unit

```