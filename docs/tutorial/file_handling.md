# File Handling

A scripting library does not only contain reading and writing operations, that is why we also include methods for manipulating files such as creations, deletions, permissions and others.  

## Create a file

You may want to create a file without immediately writing on it. For example, when you want to modify the permissions first or let another process/fiber handle it instead. For such and more reasons, you can create a file or directory using the `createFile` and `createDirectory` functions.

It is generally possible to create empty files and directories using the `createFile` and `createDirectory` functions:  

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Creating extends IOApp.Simple:

  val filePath = Path("path/to/your/desired/creation/NiceScript.scala")

  def run: IO[Unit] =
    for
      _ <- filePath.createFile
      created <- filePath.exists
      _ <- IO.println(s"File created? $created")
    yield ()

end Creating
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Creating extends IOApp.Simple:

  val filePath = Path("path/to/your/desired/creation/NiceScript.scala")

  def run: IO[Unit] =
    for
      _ <- FilesOs.createFile(filePath)
      created <- FilesOs.exists(filePath)
      _ <- IO.println(s"File created? $created")
    yield ()

end Creating
```

@:@

Here, we are first creating the file using the `createFile` method and then checking its existent with the `exists` method.

**Important:** The `createFile` and `createDirectory` methods will only work if the parent directory already exists, and fail otherwise. The `createDirectories` method will recursively create the directories instead:  

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Creating extends IOApp.Simple:

  val emptyDirectories = Path("create/me/first")

  def run: IO[Unit] =
    emptyDirectories.createDirectories >> Path("now_i_can_be_created.fs").createFile

end Creating
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Creating extends IOApp.Simple:

  val emptyDirectories = Path("create/me/first")

  def run: IO[Unit] =
    FilesOs.createDirectories(emptyDirectories) >> 
      FilesOs.createFile(Path("now_i_can_be_created.fs"))

end Creating
```

@:@

### Exercise

For this exercise, write a function that creates a file in a (deeply) nested directory. The directories should be created if they don't exist.  

@:select(api-style)

@:choice(syntax)

```scala
extension (path: Path) def createFileAndDirectories: IO[Unit] = ???
```

@:choice(static)

```scala
def createFileAndDirectories(path: Path): IO[Unit] = ???
```

@:@

[See possible implementation](../examples/solutions.md#create-a-file)


## Deleting here and there

Creating is just the first part. Because memory is not infinite, you may also want to delete a file on your system.

Deleting a file is as easy as using the `delete` method:  


@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Deleting extends IOApp.Simple:

  val annoyingFile = Path("desktop/extend_your_car_warranty_now.ad")

  def run: IO[Unit] =
    for
      exists <- annoyingFile.exists
      _ <- if exists then annoyingFile.delete
           else IO.unit
    yield ()

end Deleting
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Deleting extends IOApp.Simple:

  val annoyingFile = Path("desktop/extend_your_car_warranty_now.ad")

  def run: IO[Unit] =
    for
      exists <- FilesOs.exists(annoyingFile)
      _ <- if exists then FilesOs.delete(annoyingFile)
           else IO.unit
    yield ()

end Deleting
```

@:@

Note that we are first checking if the file exists before deleting it, this is because trying to delete a file that does not exist will result in an error. To avoid this error, you have two options. One is using the [`whenA` combinator](https://github.com/typelevel/cats/blob/main/core/src/main/scala/cats/Applicative.scala#L263) from [Applicative](https://typelevel.org/cats/typeclasses/applicative.html) importing `cats.syntax.applicative.*`:  

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.syntax.applicative.* // You can instead import cats.syntax.all.* !
import cats.effect.{IO, IOApp}

object Deleting extends IOApp.Simple:

  val annoyingFile = Path("desktop/extend_your_car_warranty_now.ad")

  def run: IO[Unit] =
    for
      exists <- annoyingFile.exists
      _ <- annoyingFile.delete.whenA(exists)
    yield ()

end Deleting
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs
import fs2.io.file.Path
import cats.syntax.applicative.* // You can instead import cats.syntax.all.* !
import cats.effect.{IO, IOApp}

object Deleting extends IOApp.Simple:

  val annoyingFile = Path("desktop/extend_your_car_warranty_now.ad")

  def run: IO[Unit] =
    for
      exists <- FilesOs.exists(annoyingFile)
      _ <- FilesOs.delete(annoyingFile).whenA(exists)
    yield ()

end Deleting
```

@:@

Or even better, use the convenience method `deleteIfExists`:

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Deleting extends IOApp.Simple:

  val annoyingFile = Path("desktop/extend_your_car_warranty_now.ad")

  def run: IO[Unit] =
    for
      deleted <- annoyingFile.deleteIfExists
      _ <- IO.println(s"Are they reaching out? $deleted")
    yield ()

end Deleting
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Deleting extends IOApp.Simple:

  val annoyingFile = Path("desktop/extend_your_car_warranty_now.ad")

  def run: IO[Unit] =
    for
      deleted <- FilesOs.deleteIfExists(annoyingFile)
      _ <- IO.println(s"Are they reaching out? $deleted")
    yield ()

end Deleting
```

@:@

This will return a boolean indicating whether the file and directories have been deleted.  

Finally, you may want to delete not one but multiple files and directories, here is when the `deleteDirectorires` comes handy, as it will delete all the files and directories recursively (similar to `rm -r`):  

**Before:**

```
/My files
├── /non empty folder
│   ├── 3751c91b_2024-06-16_7.csv
│   ├── Screenshot 2024-06-16 210523.png
│   ├── /downloaded
│   │   ├── /on_internet
│   │   │   └── Unconfirmed 379466.crdownload
│   │   └── ubuntu-24.04-desktop-amd64.iso
│   └── /spark
│       ├── output0-part-r-00000.nodes
│       └── output1-part-r-00000.nodes
│ 
├── /dont delete
│ 
```

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Deleting extends IOApp.Simple:

  val nonEmptyFolder = Path("downloads/non empty folder")

  def run: IO[Unit] = nonEmptyFolder.deleteRecursively

end Deleting
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Deleting extends IOApp.Simple:

  val nonEmptyFolder = Path("downloads/non empty folder")

  def run: IO[Unit] = FilesOs.deleteRecursively(nonEmptyFolder)

end Deleting
```

@:@

**After:**

```
/My files
├── /dont delete
│  
```

### Exercise

You are tired of people abusing the FTP server to upload enormous files, so you decide to implement a method that checks if a file exceeds a certain size, and if so, automatically delete it (hint: check the `size` method). The method must return `true` if the file has been deleted, `false` otherwise.  


@:select(api-style)

@:choice(syntax)

```scala
extension (path: Path) def deleteIfChubby(threshold: Long): IO[Boolean] = ???
```

@:choice(static)

```scala
def deleteIfChubby(path: Path, threshold: Long): IO[Boolean] = ???
```

@:@

[See possible implementation](../examples/solutions.md#deleting-here-and-there)

## Using temporary files

Maybe you do not want to manually delete a file after its use. This is where temporary files come in to play, as they are deleted automatically.

To create temporary files, you have two options, one is to make Cats Effect automatically handle their lifecycle with the `withTempFile` and `withTempDirectory` methods (useful when you want the files deleted right away), or, if you rather prefer the operating system to take hands in its lifecycle, you can use the `createTempFile` and `createTempDirectory` variants (suitable if you do not care if the files are deleted immediately).

The former takes as a parameter a function that describes how you want to use the file, like this:

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import cats.syntax.all.*
import cats.effect.{IO, IOApp}

object Temporary extends IOApp.Simple:

  def run: IO[Unit] = withTempFile: path =>
    for
      _ <- path.writeLines(LazyList.from('a').map(_.toChar.toString).take(26))
      alphabet <- path.read
      _ <- IO.println("ASCII took hispanics into account!").whenA(alphabet.contains('ñ'))
    yield ()

end Temporary
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs
import cats.syntax.all.*
import cats.effect.{IO, IOApp}

object Temporary extends IOApp.Simple:

  def run: IO[Unit] = FilesOs.withTempFile: path =>
    for
      _ <- FilesOs.writeLines(path, LazyList.from('a').map(_.toChar.toString).take(26))
      alphabet <- FilesOs.read(path)
      _ <- IO.println("ASCII took hispanics into account!").whenA(alphabet.contains('ñ'))
    yield ()

end Temporary
```

@:@

You will see that the `use` function goes from `Path => IO[A]`, and that `use` basically describes a path that will be used to compute an `A`, with some side effects along the way. When the computation is finished, the file will no longer exist.

The last alternative is with `createTempFile` or `createTempDirectory`. The difference between `createTempFile` and `withTempFile` is that the `create` functions return the path of the file, for example:  

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Temporary extends IOApp.Simple:

  val secretPath = Path(".secrets/to_my_secret_lover.txt")

  def run: IO[Unit] = createTempFile.flatMap: path =>
    for
      _ <- path.write("A confession to my lover: ")
      letter <- secretPath.read
      _ <- path.appendLine(letter)
    yield ()

end Temporary
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Temporary extends IOApp.Simple:

  val secretPath = Path(".secrets/to_my_secret_lover.txt")

  def run: IO[Unit] = FilesOs.createTempFile.flatMap: path =>
    for
      _ <- FilesOs.write(path,"A confession to my lover: ")
      letter <- FilesOs.read(secretPath)
      _ <- FilesOs.appendLine(path, letter)
    yield ()

end Temporary
```

@:@

### Exercise

Another really nice way to handle resource lifecycle [is with a `Resource`](https://typelevel.org/cats-effect/docs/std/resource#resource) from Cats Effect. Be adventurous and implement a third way of handling temporary files with a new function that returns a `Resource`.  


```scala
import cats.effect.Resource

def makeTempFile: Resource[IO, Path] = ???

def makeTempDirectory: Resource[IO, Path] = ???
```

[See possible implementations](../examples/solutions.md#using-temporary-files)
