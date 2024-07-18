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
import cats.effect.{IO, Resource}

import fs2.{Stream, Chunk}
import fs2.io.file.*

import scodec.Codec
import scodec.bits.ByteVector

import scala.concurrent.duration.FiniteDuration

import java.nio.charset.Charset

object FilesOs {

  private val files: Files[IO] = Files[IO]

  // Read operations:

  /**
   * Reads the contents of the file at the path using UTF-8 decoding. Returns it
   * as a String loaded in memory.
   *
   * @param path
   *   The path to read from
   * @return
   *   The file loaded in memory as a String
   */
  def read(path: Path): IO[String] = files.readUtf8(path).compile.string

  /**
   * Reads the contents of the file at the path using the provided charset.
   * Returns it as a String loaded in memory.
   *
   * @param path
   *   The path to read from
   * @param charset
   *   The charset to use to decode the file
   * @return
   *   The file loaded in memory as a String
   */
  def read(path: Path, charset: Charset): IO[String] =
    files
      .readAll(path)
      .through(fs2.text.decodeWithCharset(charset))
      .compile
      .string

  /**
   * Reads the contents of the file at the path and returns it as a ByteVector.
   * @param path
   *   The path to read from
   * @return
   *   The file loaded in memory as a ByteVector
   */
  def readBytes(path: Path): IO[ByteVector] =
    files.readAll(path).compile.to(ByteVector)

  /**
   * Reads the contents of the file at the path using UTF-8 decoding and returns
   * it line by line as a List of Strings.
   *
   * @param path
   *   The path to read from
   * @return
   *   The file loaded in memory as a collection of lines of Strings
   */
  def readLines(path: Path): IO[List[String]] =
    files.readUtf8Lines(path).compile.toList

  /**
   * Reads the contents of the file and deserializes its contents as `A` using
   * the provided codec.
   * @tparam A
   *   The type to read the file as
   * @param path
   *   The path to read from
   * @param Codec[A]
   *   The codec that translates the file contents into the type `A`
   * @return
   *   The file loaded in memory as a type `A`
   */
  def readAs[A: Codec](path: Path): IO[A] =
    readBytes(path).map(bytes => Codec[A].decodeValue(bytes.bits).require)

  // Write operations:

  /**
   * This function overwrites the contents of the file at the path using UTF-8
   * encoding with the contents provided in form of a entire string loaded in
   * memory.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def write(path: Path, contents: String): IO[Unit] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(files.writeUtf8(path))
      .compile
      .drain

  /**
   * This function overwrites the contents of the file at the path using the
   * provided charset with the contents provided in form of a entire string
   * loaded in memory.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   * @param charset
   *   The charset to use to encode the file
   */
  def write(
      path: Path,
      contents: String,
      charset: Charset
  ): IO[Unit] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(fs2.text.encode(charset))
      .through(files.writeAll(path))
      .compile
      .drain

  /**
   * This function overwrites the contents of the file at the path with the
   * contents provided in form of bytes loaded in memory.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def writeBytes(path: Path, contents: ByteVector): IO[Unit] =
    Stream
      .chunk(Chunk.byteVector(contents))
      .covary[IO]
      .through(files.writeAll(path))
      .compile
      .drain

  /**
   * This function overwrites the contents of the file at the path using UTF-8
   * encoding with the contents provided. Each content inside the list is
   * written as a line in the file.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def writeLines(path: Path, contents: Seq[String]): IO[Unit] =
    Stream
      .emits(contents)
      .covary[IO]
      .through(files.writeUtf8Lines(path))
      .compile
      .drain

  /**
   * The functions writes the contents of the file at the path with the contents
   * provided and returns the number of bytes written. The codec is used to
   * translate the type A into a ByteVector so it can be parsed into the file.
   *
   * @tparam A
   *   The type of the contents to write
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   * @param Codec[A]
   *   The codec that translates the type A into a ByteVector
   */
  def writeAs[A: Codec](path: Path, contents: A): IO[Unit] =
    writeBytes(path, Codec[A].encode(contents).require.bytes)

  /**
   * Similar to `write`, but appends to the file instead of overwriting it.
   * Saves the content at the end of the file in form of a String using UTF-8
   * encoding.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def append(path: Path, contents: String): IO[Unit] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(files.writeUtf8(path, Flags.Append))
      .compile
      .drain

  /**
   * Similar to `write`, but appends to the file instead of overwriting it.
   * Saves the content at the end of the file in form of a String using the
   * provided charset.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   * @param charset
   *   The charset to use to encode the contents
   */
  def append(
      path: Path,
      contents: String,
      charset: Charset
  ): IO[Unit] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(fs2.text.encode(charset))
      .through(files.writeAll(path, Flags.Append))
      .compile
      .drain

  /**
   * Similar to `write`, but appends to the file instead of overwriting it.
   * Saves the content at the end of the file in form of a ByteVector.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def appendBytes(path: Path, contents: ByteVector): IO[Unit] =
    Stream
      .chunk(Chunk.byteVector(contents))
      .covary[IO]
      .through(files.writeAll(path, Flags.Append))
      .compile
      .drain

  /**
   * Similar to `write`, but appends to the file instead of overwriting it.
   * Saves each line of the content at the end of the file in form of a List of
   * Strings.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def appendLines(path: Path, contents: Seq[String]): IO[Unit] =
    Stream
      .emits(contents)
      .covary[IO]
      .through(files.writeUtf8Lines(path, Flags.Append))
      .compile
      .drain

  /**
   * Similar to append, but appends a single line to the end file as a newline
   * instead of overwriting it.
   *
   * Equivalent to `append(path, '\n' + contents)`
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def appendLine(path: Path, contents: String): IO[Unit] =
    append(path, s"\n$contents")

  /**
   * Similar to `write`, but appends to the file instead of overwriting it.
   * Saves the content at the end of the file in form of a type `A`.
   *
   * @tparam A
   *   The type of the contents to write
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   * @param Codec[A]
   *   The codec that translates the type A into a ByteVector
   */
  def appendAs[A: Codec](path: Path, contents: A): IO[Unit] =
    appendBytes(path, Codec[A].encode(contents).require.bytes)

  // Files operations:

  /**
   * Copies the source to the target, failing if source does not exist or the
   * target already exists. To replace the existing instead, use `copy(source,
   * target, CopyFlags(CopyFlag.ReplaceExisting))`.
   */
  def copy(source: Path, target: Path): IO[Unit] =
    copy(source, target, CopyFlags.empty)

  /**
   * Copies the source to the target, following any directives supplied in the
   * flags. By default, an error occurs if the target already exists, though
   * this can be overridden via CopyFlag.ReplaceExisting.
   */
  def copy(source: Path, target: Path, flags: CopyFlags): IO[Unit] =
    files.copy(source, target, flags)

  /**
   * Creates the specified directory with the permissions of "rwxrwxr-x" by
   * default. Fails if the parent path does not already exist.
   */
  def createDirectory(path: Path): IO[Unit] = files.createDirectories(path)

  /**
   * Creates the specified directory with the specified permissions. Fails if
   * the parent path does not already exist.
   */
  def createDirectory(path: Path, permissions: Permissions): IO[Unit] =
    files.createDirectories(path, permissions.some)

  /**
   * Creates the specified directory and any non-existent parent directories.
   */
  def createDirectories(path: Path): IO[Unit] = files.createDirectories(path)

  /**
   * Creates the specified directory and any parent directories, using the
   * supplied permissions for any directories that get created as a result of
   * this operation. For example if `/a` exists and
   * `createDirectories(Path("/a/b/c"), p)` is called, `/a/b` and `/a/b/c` are
   * created with permissions set to `p` on each (and the permissions of `/a`
   * remain unmodified).
   */
  def createDirectories(path: Path, permissions: Permissions): IO[Unit] =
    files.createDirectories(path, permissions.some)

  /**
   * Creates the specified file with the permissions of "rw-rw-r--" by default.
   * Fails if the parent path does not already exist.
   */
  def createFile(path: Path): IO[Unit] = files.createFile(path)

  /**
   * Creates the specified file with the specified permissions. Fails if the
   * parent path does not already exist.
   */
  def createFile(path: Path, permissions: Permissions): IO[Unit] =
    files.createFile(path, permissions.some)

  /** Creates a hard link with an existing file. */
  def createLink(link: Path, existing: Path): IO[Unit] =
    files.createLink(link, existing)

  /** Creates a symbolic link which points to the supplied target. */
  def createSymbolicLink(link: Path, target: Path): IO[Unit] =
    files.createSymbolicLink(link, target)

  /**
   * Creates a symbolic link which points to the supplied target with optional
   * permissions.
   */
  def createSymbolicLink(
      link: Path,
      target: Path,
      permissions: Permissions
  ): IO[Unit] =
    files.createSymbolicLink(link, target, permissions.some)

  /**
   * Creates a temporary file. The created file is not automatically deleted -
   * it is up to the operating system to decide when the file is deleted.
   * Alternatively, use `tempFile` to get a resource, which is deleted upon
   * resource finalization.
   */
  def createTempFile: IO[Path] = files.createTempFile

  /**
   * Creates a temporary file. The created file is not automatically deleted -
   * it is up to the operating system to decide when the file is deleted.
   * Alternatively, use `tempFile` to get a resource which deletes upon resource
   * finalization.
   *
   * @param dir
   *   the directory which the temporary file will be created in. Pass none to
   *   use the default system temp directory
   * @param prefix
   *   the prefix string to be used in generating the file's name
   * @param suffix
   *   the suffix string to be used in generating the file's name
   * @param permissions
   *   permissions to set on the created file
   */
  def createTempFile(
      dir: Option[Path],
      prefix: String,
      suffix: String,
      permissions: Permissions
  ): IO[Path] = files.createTempFile(dir, prefix, suffix, permissions.some)

  /**
   * Creates a temporary directory. The created directory is not automatically
   * deleted - it is up to the operating system to decide when the file is
   * deleted. Alternatively, use `tempDirectory` to get a resource which deletes
   * upon resource finalization.
   */
  def createTempDirectory: IO[Path] = files.createTempDirectory

  /**
   * Creates a temporary directory. The created directory is not automatically
   * deleted - it is up to the operating system to decide when the file is
   * deleted. Alternatively, use `tempDirectory` to get a resource which deletes
   * upon resource finalization.
   *
   * @param dir
   *   the directory which the temporary directory will be created in. Pass none
   *   to use the default system temp directory
   * @param prefix
   *   the prefix string to be used in generating the directory's name
   * @param permissions
   *   permissions to set on the created directory
   */
  def createTempDirectory(
      dir: Option[Path],
      prefix: String,
      permissions: Permissions
  ): IO[Path] = files.createTempDirectory(dir, prefix, permissions.some)

  /** User's current working directory */
  def currentWorkingDirectory: IO[Path] = files.currentWorkingDirectory

  /**
   * Deletes the specified file or empty directory, failing if it does not
   * exist.
   */
  def delete(path: Path): IO[Unit] = files.delete(path)

  /**
   * Deletes the specified file or empty directory, passing if it does not
   * exist.
   */
  def deleteIfExists(path: Path): IO[Boolean] = files.deleteIfExists(path)

  /**
   * Deletes the specified file or directory. If the path is a directory and is
   * non-empty, its contents are recursively deleted. Symbolic links are not
   * followed (but are deleted).
   */
  def deleteRecursively(
      path: Path
  ): IO[Unit] = deleteRecursively(path, false)

  /**
   * Deletes the specified file or directory. If the path is a directory and is
   * non-empty, its contents are recursively deleted. Symbolic links are
   * followed when `followLinks` is true.
   */
  def deleteRecursively(
      path: Path,
      followLinks: Boolean
  ): IO[Unit] = files.deleteRecursively(path, followLinks)

  /**
   * Returns true if the specified path exists. Symbolic links are followed --
   * see the overload for more details on links.
   */
  def exists(path: Path): IO[Boolean] = exists(path, true)

  /**
   * Returns true if the specified path exists. Symbolic links are followed when
   * `followLinks` is true. For example, if the symbolic link `foo` points to
   * `bar` and `bar` does not exist, `exists(Path("foo"), true)` returns `false`
   * but `exists(Path("foo"), false)` returns `true`.
   */
  def exists(path: Path, followLinks: Boolean): IO[Boolean] =
    files.exists(path, followLinks)

  /**
   * Gets `BasicFileAttributes` for the supplied path. Symbolic links are not
   * followed.
   */
  def getBasicFileAttributes(path: Path): IO[BasicFileAttributes] =
    getBasicFileAttributes(path, false)

  /**
   * Gets `BasicFileAttributes` for the supplied path. Symbolic links are
   * followed when `followLinks` is true.
   */
  def getBasicFileAttributes(
      path: Path,
      followLinks: Boolean
  ): IO[BasicFileAttributes] = files.getBasicFileAttributes(path, followLinks)

  /**
   * Gets the last modified time of the supplied path. The last modified time is
   * represented as a duration since the Unix epoch. Symbolic links are
   * followed.
   */
  def getLastModifiedTime(path: Path): IO[FiniteDuration] =
    getLastModifiedTime(path, true)

  /**
   * Gets the last modified time of the supplied path. The last modified time is
   * represented as a duration since the Unix epoch. Symbolic links are followed
   * when `followLinks` is true.
   */
  def getLastModifiedTime(
      path: Path,
      followLinks: Boolean
  ): IO[FiniteDuration] = files.getLastModifiedTime(path, followLinks)

  /**
   * Gets the POSIX attributes for the supplied path. Symbolic links are not
   * followed.
   */
  def getPosixFileAttributes(path: Path): IO[PosixFileAttributes] =
    getPosixFileAttributes(path, false)

  /**
   * Gets the POSIX attributes for the supplied path. Symbolic links are
   * followed when `followLinks` is true.
   */
  def getPosixFileAttributes(
      path: Path,
      followLinks: Boolean
  ): IO[PosixFileAttributes] = files.getPosixFileAttributes(path, followLinks)

  /**
   * Gets the POSIX permissions of the supplied path. Symbolic links are
   * followed.
   */
  def getPosixPermissions(path: Path): IO[PosixPermissions] =
    getPosixPermissions(path, true)

  /**
   * Gets the POSIX permissions of the supplied path. Symbolic links are
   * followed when `followLinks` is true.
   */
  def getPosixPermissions(
      path: Path,
      followLinks: Boolean
  ): IO[PosixPermissions] = files.getPosixPermissions(path, followLinks)

  /**
   * Returns true if the supplied path exists and is a directory. Symbolic links
   * are followed.
   */
  def isDirectory(path: Path): IO[Boolean] = isDirectory(path, true)

  /**
   * Returns true if the supplied path exists and is a directory. Symbolic links
   * are followed when `followLinks` is true.
   */
  def isDirectory(path: Path, followLinks: Boolean): IO[Boolean] =
    files.isDirectory(path, followLinks)

  /** Returns true if the supplied path exists and is executable. */
  def isExecutable(path: Path): IO[Boolean] = files.isExecutable(path)

  /**
   * Returns true if the supplied path is a hidden file (note: may not check for
   * existence).
   */
  def isHidden(path: Path): IO[Boolean] = files.isHidden(path)

  /** Returns true if the supplied path exists and is readable. */
  def isReadable(path: Path): IO[Boolean] = files.isReadable(path)

  /**
   * Returns true if the supplied path is a regular file. Symbolic links are
   * followed.
   */
  def isRegularFile(path: Path): IO[Boolean] = isRegularFile(path, true)

  /**
   * Returns true if the supplied path is a regular file. Symbolic links are
   * followed when `followLinks` is true.
   */
  def isRegularFile(path: Path, followLinks: Boolean): IO[Boolean] =
    files.isRegularFile(path, followLinks)

  /** Returns true if the supplied path is a symbolic link. */
  def isSymbolicLink(path: Path): IO[Boolean] = files.isSymbolicLink(path)

  /** Returns true if the supplied path exists and is writable. */
  def isWritable(path: Path): IO[Boolean] = files.isWritable(path)

  /** Returns true if the supplied paths reference the same file. */
  def isSameFile(path1: Path, path2: Path): IO[Boolean] =
    files.isSameFile(path1, path2)

  /** Returns the line separator for the specific OS */
  def lineSeparator: String = files.lineSeparator

  /** Gets the contents of the specified directory. */
  def list(path: Path): Stream[IO, Path] = files.list(path)

  /**
   * Moves the source to the target, failing if source does not exist or the
   * target already exists. To replace the existing instead, use `move(source,
   * target, CopyFlags(CopyFlag.ReplaceExisting))`.
   */
  def move(source: Path, target: Path): IO[Unit] =
    move(source, target, CopyFlags.empty)

  /**
   * Moves the source to the target, following any directives supplied in the
   * flags. By default, an error occurs if the target already exists, though
   * this can be overridden via `CopyFlag.ReplaceExisting`.
   */
  def move(source: Path, target: Path, flags: CopyFlags): IO[Unit] =
    files.move(source, target, flags)

  /**
   * Creates a `FileHandle` for the file at the supplied `Path`. The supplied
   * flags indicate the mode used when opening the file (e.g. read, write,
   * append) as well as the ability to specify additional options (e.g.
   * automatic deletion at process exit).
   */
  def open(path: Path, flags: Flags): Resource[IO, FileHandle[IO]] =
    files.open(path, flags)

  /**
   * Returns a `ReadCursor` for the specified path, using the supplied flags
   * when opening the file.
   */
  def readCursor(path: Path, flags: Flags): Resource[IO, ReadCursor[IO]] =
    files.readCursor(path, flags)

  /**
   * Returns the real path i.e. the actual location of `path`. The precise
   * definition of this method is implementation dependent but in general it
   * derives from this path, an absolute path that locates the same file as this
   * path, but with name elements that represent the actual name of the
   * directories and the file.
   */
  def realPath(path: Path): IO[Path] = files.realPath(path)

  /**
   * Sets the last modified, last access, and creation time fields of the
   * specified path.
   *
   * Times which are supplied as `None` are not modified. E.g., `setTimes(p,
   * Some(t), Some(t), None, false)` sets the last modified and last access time
   * to `t` and does not change the creation time.
   *
   * If the path is a symbolic link and `followLinks` is true, the target of the
   * link as times set. Otherwise, the link itself has times set.
   */
  def setFileTimes(
      path: Path,
      lastModified: Option[FiniteDuration],
      lastAccess: Option[FiniteDuration],
      creationTime: Option[FiniteDuration],
      followLinks: Boolean
  ): IO[Unit] = files.setFileTimes(
    path,
    lastModified,
    lastAccess,
    creationTime,
    followLinks
  )

  /**
   * Sets the POSIX permissions for the supplied path. Fails on non-POSIX file
   * systems.
   */
  def setPosixPermissions(path: Path, permissions: PosixPermissions): IO[Unit] =
    files.setPosixPermissions(path, permissions)

  /** Gets the size of the supplied path, failing if it does not exist. */
  def size(path: Path): IO[Long] = files.size(path)

  /**
   * Creates a temporary file and deletes it at the end of the use of it.
   */
  def tempFile[A](use: Path => IO[A]): IO[A] =
    IO.defer(files.createTempFile).bracket(use)(deleteIfExists(_).void)

  /**
   * Creates a temporary file and deletes it at the end of the use of it.
   *
   * @tparam A
   *   the type of the result computation
   *
   * @param dir
   *   the directory which the temporary file will be created in. Pass in None
   *   to use the default system temp directory
   * @param prefix
   *   the prefix string to be used in generating the file's name
   * @param suffix
   *   the suffix string to be used in generating the file's name
   * @param permissions
   *   permissions to set on the created file
   * @return
   *   The result of the computation after using the temporary file
   */
  def tempFile[A](
      dir: Option[Path],
      prefix: String,
      suffix: String,
      permissions: Permissions
  )(use: Path => IO[A]): IO[A] = IO
    .defer(files.createTempFile(dir, prefix, suffix, permissions.some))
    .bracket(use)(deleteIfExists(_).void)

  /**
   * Creates a temporary directory and deletes it at the end of the use of it.
   */
  def tempDirectory[A](use: Path => IO[A]): IO[A] =
    IO.defer(files.createTempDirectory)
      .bracket(use)(deleteRecursively(_).recover {
        case _: NoSuchFileException => ()
      })

  /**
   * Creates a temporary directory and deletes it at the end of the use of it.
   *
   * @tparam A
   *   the type of the result computation
   *
   * @param dir
   *   the directory which the temporary directory will be created in. Pass in
   *   None to use the default system temp directory
   * @param prefix
   *   the prefix string to be used in generating the directory's name
   * @param permissions
   *   permissions to set on the created file
   * @return
   *   the result of the computation after using the temporary directory
   */
  def tempDirectory[A](
      dir: Option[Path],
      prefix: String,
      permissions: Permissions
  )(use: Path => IO[A]): IO[A] = IO
    .defer(files.createTempDirectory(dir, prefix, permissions.some))
    .bracket(use)(deleteRecursively(_).recover { case _: NoSuchFileException =>
      ()
    })

  /** User's home directory */
  def userHome: IO[Path] = files.userHome

}
