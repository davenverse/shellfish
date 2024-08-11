# Handling files and doing operations

Beyond simple read and write operations, this section allows you to interact directly with the file system. There are functions for creating and deleting files and directories, creating temporary files for short-term use, and managing file and directory permissions for added control, among other useful methods.

## Creating files and directories

In this section, you will see how to create new files and directories, as well as delete existing ones.

### `createFile`    

Creates a new file in the specified path, failing if the parent directory does not exist. You can also specify some permissions if you wish. To see what the `exists` function does, see [the reference](#exists):

```scala mdoc:invisible
// This sections adds every import to the code snippets

import cats.effect.IO
import cats.syntax.all.*

import fs2.Stream
import fs2.io.file.{Path, Files}

import io.chrisdavenport.shellfish
import shellfish.syntax.path.*
import shellfish.FilesOs

val path = Path("testdata/dummy.something")
```

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import shellfish.syntax.path.*

val path = Path("path/to/create/file.txt")

path.createFile >> path.exists // Should return true
```

@:choice(static)

```scala mdoc:compile-only
import shellfish.FilesOs

val path = Path("path/to/create/file.txt")

FilesOs.createFile(path) >> FilesOs.exists(path) // Should return true
```

@:choice(fs2)

```scala mdoc:compile-only
import fs2.io.file.Files

val path = Path("path/to/create/file.txt")

Files[IO].createFile(path) >> Files[IO].exists(path) // Should return true
```

@:@

### `createDirectories`

Creates all the directories in the path, with the default permissions or with the supplied ones:

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
val directories = Path("here/are/some/dirs")
val path = directories / Path("file.txt")

directories.createDirectories >> path.createFile
```

@:choice(static)

```scala mdoc:compile-only
val directories = Path("here/are/some/dirs")
val path = directories / Path("file.txt")

FilesOs.createDirectories(directories) >> FilesOs.createFile(path)
```

@:choice(fs2)

```scala mdoc:compile-only
val directories = Path("here/are/some/dirs")
val path = directories / Path("file.txt")

Files[IO].createDirectories(directories) >> Files[IO].createFile(path)
```

@:@

### `createTempFile`

This function creates a temporary file that gets automatically deleted by the operating system. It optionally accepts multiple parameters such as a directory, a prefix (the name of the file), a suffix (the extension of the file) and permissions. It returns the path of the newly created file:

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
for 
  path <- createTempFile

  // Its going to be deleted eventually!
  _ <- path.write("I don't wanna go!") 
yield ()
```

@:choice(static)

```scala mdoc:compile-only
for 
  path <- FilesOs.createTempFile
  
  // Its going to be deleted eventually!
  _ <- FilesOs.write(path, "I don't wanna go!") 
yield ()
```

@:choice(fs2)

```scala mdoc:compile-only
Stream.eval(Files[IO].createTempFile)
  .flatMap( path => 
    Stream.emit("I don't wanna go!")
      .through(Files[IO].writeUtf8(path))
  )
  .compile
  .drain
```

@:@

### `tempFile`

Very similar to `createTempFile`, but Cats Effect handles the deletion of the file by itself. Accepts the same parameters as a custom directory, a prefix, a suffix, and some permissions but takes a `use` function as well. This function is a description of how the path will be used and what will be computed after that:

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
tempFile: path =>
  path.write("I have accepted my fate...")
```

@:choice(static)

```scala mdoc:compile-only
FilesOs.tempFile: path =>
  FilesOs.write(path, "I have accepted my fate...")
```

@:choice(fs2)

```scala mdoc:compile-only
Files[IO].tempFile.use: path =>
  Stream.emit("I have accepted my fate...")
   .through(Files[IO].writeUtf8(path))
   .compile
   .drain
```

@:@

### `createTempDirectory`

Creates a temporary directory that will eventually be deleted by the operating system. It accepts a few optional parameters like a custom parent directory, a prefix, and some permissions. It returns the path to the newly created directory:

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
for 
  dir <- createTempDirectory
  _ <- (dir / "tempfile.tmp").createFile
yield ()
```

@:choice(static)

```scala mdoc:compile-only
for 
  dir <- FilesOs.createTempDirectory
  _ <- FilesOs.createFile(dir / "tempfile.tmp")
yield ()

```

@:choice(fs2)

```scala mdoc:compile-only
for 
  dir <- Files[IO].createTempDirectory
  _   <- Files[IO].createFile(dir / "tempfile.tmp")
yield ()

```

@:@

### `tempDirectory`

Similar to `createTempDirectory`, but the deletion of the directory is managed by Cats Effect. Takes the same arguments as a custom directory, a prefix and some permissions and most importantly, a `use` function that describes how the directory will be used and computed:

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
tempDirectory: dir => 
  (dir / "its_going_to_go_soon.mp3").createFile
```

@:choice(static)

```scala mdoc:compile-only
FilesOs.tempDirectory: dir =>
  FilesOs.createFile(dir / "its_going_to_go_soon.mp3")
```

@:choice(fs2)

```scala mdoc:compile-only
Files[IO].tempDirectory.use: dir => 
  Files[IO].createFile(dir / "its_going_to_go_soon.mp3")

```

@:@

### `createSymbolicLink`

Creates a [Symbolic Link](https://en.wikipedia.org/wiki/Symbolic_link) to a file. Requires the destination of the symlink and the path of the target file to link, and optionally some permissions:

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
val linkPath   = Path("store/the/link/here/symlink")
val targetPath = Path("path/to/file/to/target.sh")

linkPath.createSymbolicLink(targetPath)
```

@:choice(static)

```scala mdoc:compile-only
val linkPath   = Path("store/the/link/here/symlink")
val targetPath = Path("path/to/file/to/target.sh")

FilesOs.createSymbolicLink(linkPath, targetPath)
```

@:choice(fs2)

```scala mdoc:compile-only
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

```scala mdoc:compile-only
path.createFile >> 
  path.write("TOP SECRET 🚫, MUST DELETE") >>
    path.delete
```

@:choice(static)

```scala mdoc:compile-only
FilesOs.createFile(path) >>
  FilesOs.write(path, "TOP SECRET 🚫, MUST DELETE") >>
    FilesOs.delete(path)
```

@:choice(fs2)

```scala mdoc:compile-only
for
  _ <- Files[IO].createFile(path)

  _ <- Stream.emit("TOP SECRET 🚫, MUST DELETE")
        .through(Files[IO].writeUtf8(path))
        .compile
        .drain

  _ <- Files[IO].delete(path)
yield ()
```

@:@

### `deleteIfExists`

Similar to `delete`, but returns `true` if the deletion was succesfull instead of failing: 

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
path.deleteIfExists >>= 
  (deleted => IO.println(s"Was the file deleted? $deleted"))
```

@:choice(static)

```scala mdoc:compile-only
FilesOs.deleteIfExists(path) >>= 
  (deleted => IO.println(s"Was the file deleted? $deleted"))
```

@:choice(fs2)

```scala mdoc:compile-only
Files[IO].deleteIfExists(path) >>= 
  (deleted => IO.println(s"Was the file deleted? $deleted"))
```

@:@

### `deleteRecursively`

With the previous functions, the directory had to be empty to be deleted. The difference with this method is that it recursively deletes all files or folders contained inside it, optionally following symbolic links if specified: 

Recursively deletes all files or folders, following symbolic links if specified. 

Note that, unlike the previous functions, this one will not fail if the directories are empty or do not exist:

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
  _ <- FilesOs.createDirectories(dirs)
  _ <- FilesOs.deleteRecursively(dirs) // Will delete all of them!
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
  _ <- FilesOs.write(source, "The no-cloning theorem says you can't copy me!")
  _ <- FilesOs.copy(source, target)
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

Very similar to `copy`, but deletes the file in the original destination path. Optionally takes flags as arguments to define its move behaviour:

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

FilesOs.move(source, target)
```

@:choice(fs2)

```scala 3
val source = Path("i/cant/move.mp4")
val target = Path("teleporting/around/movie.mp4")

Files[IO].move(source, target)
```

@:@

### `exists`

This function checks whether a file exists at a specified path:

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
  _ <- FilesOs.delete(target) // Delete before copying to avoid errors (and flags)
  exists <- FilesOs.exists(target)
  _ <- FilesOs.copy(source, target).whenA(exists)
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


