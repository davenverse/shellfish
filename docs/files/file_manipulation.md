# Handling files and doing operations

Beyond simple read and write operations, this section allows you to interact directly with the file system. There are functions for creating and deleting files and directories, creating temporary files for short-term use, and managing file and directory permissions for added control, among other useful methods.

## Creating files and directories

This section enables you to create new files and directories, as well as delete existing ones.

### `createFile`

Creates a new file in the specified path, failing if the parent directory does not exist. You can also specify some permissions if you wish. To see what the `exists` function does, see [the reference](#exists):

@:select(api-style)

@:choice(syntax)

```scala 3
import cats.syntax.all.* /* for the >> operator, its just a
                          * rename for flatMap( _ => IO(...) )
                          */

import shellfish.syntax.path.*

val path = Path("path/to/create/file.txt")

path.createFile >> path.exists // Should return true
```

@:choice(static)

```scala 3
import cats.syntax.all.* /* for the >> operator, its just a
                          * rename for flatMap( _ => IO(...) )
                          */

import shellfish.FilesOs.*

val path = Path("path/to/create/file.txt")

createFile(path) >> exists(path) // Should return true
```

@:choice(fs2)

```scala 3
import cats.syntax.all.* /* for the >> operator, its just a
                          * rename for flatMap( _ => IO(...) )
                          */

import fs2.io.file.Files

val path = Path("path/to/create/file.txt")

Files[IO].createFile(path) >> Files[IO].exists(path) // Should return true
```

@:@

### `createDirectories`

Creates all the directories in the path, with the default permissions or, with supplyed ones:

@:select(api-style)

@:choice(syntax)

```scala 3
val directories = Path("here/are/some/dirs")
val path = directories / Path("file.txt")

directories.createDirectories >> path.createFile
```

@:choice(static)

```scala 3
val directories = Path("here/are/some/dirs")
val path = directories / Path("file.txt")

createDirectories(directories) >> createFile(path)
```

@:choice(fs2)

```scala 3
val directories = Path("here/are/some/dirs")
val path = directories / Path("file.txt")

Files[IO].createDirectories(directories) >> Files[IO].createFile(path)
```

@:@

### `createTempFile`

If you want to create a temporary file that is automatically deleted by the operating system, you can use this function. It accepts cero parameters if you prefer to use the default ones, or multiple parameters such as a directory, a prefix (the name of the file), a suffix (the extension of the file) and some permissions if you feel like declaring them. It return the path of the recently created file: 

@:select(api-style)

@:choice(syntax)

```scala 3
for 
  path <- createTempFile

  // Its going to be deleted eventually!
  _ <- path.write("I don't wanna go!") 
yield ()
```

@:choice(static)

```scala 3
for 
  path <- createTempFile
  
  // Its going to be deleted eventually!
  _ <- write(path, "I don't wanna go!") 
yield ()
```

@:choice(fs2)

```scala 3
Stream.eval(Files[IO].createTempFile)
  flatMap( path => 
    Stream.emit("I don't wanna go!")
      .through(Files[IO].writeUtf(path))
  )
  .compile
  .drain
```

@:@

### `tempFile`

Very similar to `createTempFile`, but Cats Effect handles the deletion of the file via [Resource](https://typelevel.org/cats-effect/docs/std/resource). Accepts the same parameters like a custom directory, a prefix, a suffix, and some permissions but returns a `Resource` containing the path of the file:

@:select(api-style)

@:choice(syntax)

```scala 3
tempFile.use: path =>
  path.write("I have accepted my fate...")
```

@:choice(static)

```scala 3
tempFile.use: path =>
  write(path, "I have accepted my fate...")
```

@:choice(fs2)

```scala 3
Files[IO].tempFile.use: path =>
  Stream.emit("I have accepted my fate...")
   .through(Files[IO].writeUtf8(path))
   .compile
   .drain
```

@:@

### `createTempDirectory`

Creates a temporary directory that will eventually be deleted by the operating system. Takes as parameters a custom directory, a prefix (the name of the directory) and some permissions, or you can specify nothing and it will use some defaults. Returns the path to the created directory: 

@:select(api-style)

@:choice(syntax)

```scala 3
for 
  dir <- createTempDirectory
  _ <- (dir / "tempfile.tmp").createFile
yield ()
```

@:choice(static)

```scala 3
for 
  dir <- createTempDirectory
  _ <- createFile(dir / "tempfile.tmp")
yield ()

```

@:choice(fs2)

```scala 3
for 
  dir <- Files[IO].createTempDirectory
  _   <- Files[IO].createFile(dir / "tempfile.tmp")
yield ()

```

@:@

### `tempDirectory`

Similar to `createTempDirectory`, but the deletion of the directory is managed by Cats Effect using a [Resource](https://typelevel.org/cats-effect/docs/std/resource). Takes the same arguments as a custom directory, a prefix and some permissions. Returns a `Resource` containing the path to the directory:

@:select(api-style)

@:choice(syntax)

```scala 3
tempDirectory.use: dir => 
  (dir / "its_going_to_go_soon.mp3").createFile
```

@:choice(static)

```scala 3
tempDirectory.use: dir => 
  createFile(dir / "its_going_to_go_soon.mp3")
```

@:choice(fs2)

```scala 3
Files[IO].tempDirectory.use: dir => 
  Files[IO].createFile(dir / "its_going_to_go_soon.mp3")

```

@:@

### `createSymbolicLink`

Creates a [Symbolic Link](https://en.wikipedia.org/wiki/Symbolic_link) to a file. Requires the destination of the symlink and the path of the target file to link, and optionally some permissions:

@:select(api-style)

@:choice(syntax)

```scala 3
val linkPath   = Path("store/the/link/here/symlink")
val targetPath = Path("path/to/file/to/target.sh")

linkPath.createSymbolicLink(targetPath)
```

@:choice(static)

```scala 3
val linkPath   = Path("store/the/link/here/symlink")
val targetPath = Path("path/to/file/to/target.sh")

createSymbolicLink(linkPath, targetPath)
```

@:choice(fs2)

```scala 3
val linkPath   = Path("store/the/link/here/symlink")
val targetPath = Path("path/to/file/to/target.sh")

Files[IO].createSymbolicLink(linkPath, targetPath)
```

@:@

## Deleting files and directories

### `delete`

Deletes a file or empty directory that must exist (otherwise it will fail). 

@:select(api-style)

@:choice(syntax)

```scala 3
import cats.syntax.all.*

path.createFile >> 
  path.write("TOP SECRET 🚫, MUST DELETE") >>
    path.delete
```

@:choice(static)

```scala 3
import cats.syntax.all.*

createFile(path) >> 
  write(path, "TOP SECRET 🚫, MUST DELETE") >>
    path.delete
```

@:choice(fs2)

```scala 3
for
  _ <- Files[IO].createFile(path)

  _ <- Stream.emit("TOP SECRET 🚫, MUST DELETE")
        .through(Files[IO].writeUtf8(path))
        .compile
        .drain

  _ <- Files[IO].delete(path)
```

@:@

### `deleteIfExists`

Similar to `delete`, but returns `true` if the deletion was succesfull instead of failing: 

@:select(api-style)

@:choice(syntax)

```scala 3
path.deleteIfExists >>= 
  (deleted => IO.println(s"Was the file deleted? $deleted"))
```

@:choice(static)

```scala 3
deleteIfExists(path) >>= 
  (deleted => IO.println(s"Was the file deleted? $deleted"))
```

@:choice(fs2)

```scala 3
Files[IO].deleteIfExists(path) >>= 
  (deleted => IO.println(s"Was the file deleted? $deleted"))
```

@:@

### `deleteRecursively`

With the previous functions, the directory had to be empty to be deleted. The difference with this method is that it recursively deletes all files or folders contained inside it, optionally following symbolic links if specified: 

@:select(api-style)

@:choice(syntax)

```scala 3
val dirs = Path("this/folders/will/be/created/and/deleted")

for 
  _ <- dirs.createDirectories
  _ <- dirs.deleteRecursively // Will delete all of them!
yield ()
```

@:choice(static)

```scala 3
val dirs = Path("this/folders/will/be/created/and/deleted")

for 
  _ <- createDirectories(dirs)
  _ <- deleteRecursively(dirs) // Will delete all of them!
yield ()
```

@:choice(fs2)

```scala 3

val dirs = Path("this/folders/will/be/created/and/deleted")

for 
  _ <- Files[IO].createDirectories(dirs)
  _ <- Files[IO].deleteRecursively(dirs) // Will delete all of them!
yield ()
```

@:@


## File operations

Working directly with files is a common need in many scripting scenarios. This library provides essential functions for renaming, moving, and copying files, allowing you to efficiently manage and organize your data. 

### `copy`

Copies a file from a source path to a target path. The method will fail if the destination path already exists; to avoid this behaviour, you can pass flags to e.g., replace the contents in the destination:

@:select(api-style)

@:choice(syntax)

```scala 3
val source = Path("source/file/secret.txt")
val target = Path("target/dir/not_so_secret.txt")

for 
  _ <- source.write("The no-cloning theorem says you can't copy me!")
  _ <- source.copy(target)
yield ()
```

@:choice(static)

```scala 3
val source = Path("source/file/secret.txt")
val target = Path("target/dir/not_so_secret.txt")

for 
  _ <- write(source, "The no-cloning theorem says you can't copy me!")
  _ <- copy(source, target)
yield ()
```

@:choice(fs2)

```scala 3
val source = Path("source/file/secret.txt")
val target = Path("target/dir/not_so_secret.txt")

Stream.emit("The no-cloning theorem says you can't copy me!")
  .through(Files[IO].writeUtf8(source))
  .evalTap(_ => Files[IO].copy(source, target))
  .compile
  .drain
```

@:@

### `move`

Very similar to `copy`, but deletes the file in the original destination path. Also, optionally takes flags as arguments to define its move behaviour:

@:select(api-style)

@:choice(syntax)

```scala 3
val source = Path("i/cant/move.mp4")
val target = Path("teleporting/around/movie.mp4")

source.move(target)
```

@:choice(static)

```scala 3
val source = Path("i/cant/move.mp4")
val target = Path("teleporting/around/movie.mp4")

move(source, target)
```

@:choice(fs2)

```scala 3
val source = Path("i/cant/move.mp4")
val target = Path("teleporting/around/movie.mp4")

Files[IO].move(source, target)
```

@:@

### `exists`

This function checks whether or not a file exists in a specified path and returns `true` if it does:

@:select(api-style)

@:choice(syntax)

```scala 3
import cats.syntax.all.* // for the whenA method

val source = Path("need/to/ve/copied/bin.sha256")
val target = Path("need/to/be/deleted/bin.sha254")

for 
  _ <- target.delete // Delete before copying to avoid errors (and flags)
  exists <- target.exists
  _ <- source.copy(target).whenA(exists)
yield ()
```

@:choice(static)

```scala 3
import cats.syntax.all.* // for the whenA method

val source = Path("need/to/ve/copied/bin.sha256")
val target = Path("need/to/be/deleted/bin.sha254")

for 
  _ <- delete(target) // Delete before copying to avoid errors (and flags)
  exists <- exists(target)
  _ <- copy(source, target).whenA(exists)
yield ()
```

@:choice(fs2)

```scala 3
import cats.syntax.all.* // for the whenA method

val source = Path("need/to/ve/copied/bin.sha256")
val target = Path("need/to/be/deleted/bin.sha254")

for 
  _ <- Files[IO].delete(target) // Delete before copying to avoid errors (and flags)
  exists <- Files[IO].exists(target)
  _ <- Files[IO].copy(source, target).whenA(exists)
yield ()
```

@:@


