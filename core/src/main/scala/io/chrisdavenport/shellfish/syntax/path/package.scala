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
package syntax

import cats.effect.{IO, Resource}

import fs2.Stream
import fs2.io.file.*

import scodec.bits.ByteVector
import scodec.Codec

import scala.concurrent.duration.FiniteDuration
import java.nio.charset.Charset

package object path {

  private val files = Files[IO]

  implicit class FileOps(val path: Path) extends AnyVal {

    /**
     * Reads the contents of the file at the path using UTF-8 decoding. Returns
     * it as a String loaded in memory.
     *
     * @param path
     *   The path to read from
     * @return
     *   The file loaded in memory as a String
     */
    def read: IO[String] = FilesOs.read(path)

    /**
     * Reads the contents of the file at the path using the provided charset.
     * Returns it as a String loaded in memory.
     *
     * @param charset
     *   The charset to use to decode the file
     * @param path
     *   The path to read from
     * @return
     *   The file loaded in memory as a String
     */
    def read(charset: Charset): IO[String] =
      FilesOs.read(path, charset)

    /**
     * Reads the contents of the file at the path and returns it as a
     * ByteVector.
     * @param path
     *   The path to read from
     * @return
     *   The file loaded in memory as a ByteVector
     */
    def readBytes: IO[ByteVector] = FilesOs.readBytes(path)

    /**
     * Reads the contents of the file at the path using UTF-8 decoding and
     * returns it line by line as a List of Strings.
     *
     * @param path
     *   The path to read from
     * @return
     *   The file loaded in memory as a collection of lines of Strings
     */
    def readLines: IO[List[String]] = FilesOs.readLines(path)

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
    def readAs[A: Codec]: IO[A] = FilesOs.readAs(path)

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
    def write(contents: String): IO[Unit] = FilesOs.write(path, contents)

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
    def write(contents: String, charset: Charset): IO[Unit] =
      FilesOs.write(path, contents, charset)

    /**
     * This function overwrites the contents of the file at the path with the
     * contents provided in form of bytes loaded in memory.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def writeBytes(contents: ByteVector): IO[Unit] =
      FilesOs.writeBytes(path, contents)

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
    def writeLines(contents: Seq[String]): IO[Unit] =
      FilesOs.writeLines(path, contents)

    /**
     * The functions writes the contents of the file at the path with the
     * contents provided and returns the number of bytes written. The codec is
     * used to translate the type A into a ByteVector so it can be parsed into
     * the file.
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
    def writeAs[A: Codec](contents: A): IO[Unit] =
      FilesOs.writeAs(path, contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it.
     * Saves the content at the end of the file in form of a String.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def append(contents: String): IO[Unit] = FilesOs.append(path, contents)

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
        contents: String,
        charset: Charset
    ): IO[Unit] =
      FilesOs.append(path, contents, charset)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it.
     * Saves the content at the end of the file in form of a ByteVector.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def appendBytes(contents: ByteVector): IO[Unit] =
      FilesOs.appendBytes(path, contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it.
     * Saves each line of the content at the end of the file in form of a List
     * of Strings.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def appendLines(contents: Seq[String]): IO[Unit] =
      FilesOs.appendLines(path, contents)

    /**
     * Similar to append, but appends a single line to the end file as a newline
     * instead of overwriting it.
     *
     * Equivalent to `path.append('\n' + contents)`
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def appendLine(contents: String): IO[Unit] =
      FilesOs.appendLine(path, contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it
     * using the given `Codec[A]`
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
    def appendAs[A: Codec](contents: A): IO[Unit] =
      FilesOs.appendAs[A](path, contents)

    // File operations:

    /**
     * Copies the source to the target, failing if source does not exist or the
     * target already exists. To replace the existing instead, use
     * `path.copy(target, CopyFlags(CopyFlag.ReplaceExisting))`.
     */
    def copy(target: Path): IO[Unit] =
      FilesOs.copy(path, target, CopyFlags.empty)

    /**
     * Copies the source to the target, following any directives supplied in the
     * flags. By default, an error occurs if the target already exists, though
     * this can be overridden via CopyFlag.ReplaceExisting.
     */
    def copy(target: Path, flags: CopyFlags): IO[Unit] =
      FilesOs.copy(path, target, flags)

    /**
     * Creates the specified directory with the permissions of "rwxrwxr-x" by
     * default. Fails if the parent path does not already exist.
     */
    def createDirectory: IO[Unit] = FilesOs.createDirectory(path)

    /**
     * Creates the specified directory with the specified permissions. Fails if
     * the parent path does not already exist.
     */
    def createDirectory(permissions: Permissions): IO[Unit] =
      FilesOs.createDirectory(path, permissions)

    /**
     * Creates the specified directory and any non-existent parent directories.
     */
    def createDirectories: IO[Unit] = FilesOs.createDirectories(path)

    /**
     * Creates the specified directory and any parent directories, using the
     * supplied permissions for any directories that get created as a result of
     * this operation.
     */
    def createDirectories(permissions: Permissions): IO[Unit] =
      FilesOs.createDirectories(path, permissions)

    /**
     * Creates the specified file with the permissions of "rw-rw-r--" by
     * default. Fails if the parent path does not already exist.
     */
    def createFile: IO[Unit] = FilesOs.createFile(path)

    /**
     * Creates the specified file with the specified permissions. Fails if the
     * parent path does not already exist.
     */
    def createFile(permissions: Permissions): IO[Unit] =
      FilesOs.createFile(path, permissions)

    /** Creates a hard link with an existing file. */
    def createLink(existing: Path): IO[Unit] =
      FilesOs.createLink(path, existing)

    /** Creates a symbolic link which points to the supplied target. */
    def createSymbolicLink(target: Path): IO[Unit] =
      FilesOs.createSymbolicLink(path, target)

    /**
     * Creates a symbolic link which points to the supplied target with optional
     * permissions.
     */
    def createSymbolicLink(target: Path, permissions: Permissions): IO[Unit] =
      FilesOs.createSymbolicLink(path, target, permissions)

    // Deletion
    /**
     * Deletes the specified file or empty directory, failing if it does not
     * exist.
     */
    def delete: IO[Unit] = FilesOs.delete(path)

    /**
     * Deletes the specified file or empty directory, passing if it does not
     * exist.
     */
    def deleteIfExists: IO[Boolean] = FilesOs.deleteIfExists(path)

    /**
     * Deletes the specified file or directory. If the path is a directory and
     * is non-empty, its contents are recursively deleted. Symbolic links are
     * not followed (but are deleted).
     */
    def deleteRecursively: IO[Unit] = FilesOs.deleteRecursively(path)

    /**
     * Deletes the specified file or directory. If the path is a directory and
     * is non-empty, its contents are recursively deleted. Symbolic links are
     * followed when `followLinks` is true.
     */
    def deleteRecursively(followLinks: Boolean): IO[Unit] =
      FilesOs.deleteRecursively(path, followLinks)

    /**
     * Returns true if the specified path exists. Symbolic links are followed --
     * see the overload for more details on links.
     */
    def exists: IO[Boolean] = FilesOs.exists(path)

    /**
     * Returns true if the specified path exists. Symbolic links are followed
     * when `followLinks` is true.
     */
    def exists(followLinks: Boolean): IO[Boolean] =
      FilesOs.exists(path, followLinks)

    /**
     * Gets `BasicFileAttributes` for the supplied path. Symbolic links are not
     * followed.
     */
    def getBasicFileAttributes: IO[BasicFileAttributes] =
      FilesOs.getBasicFileAttributes(path)

    /**
     * Gets `BasicFileAttributes` for the supplied path. Symbolic links are
     * followed when `followLinks` is true.
     */
    def getBasicFileAttributes(followLinks: Boolean): IO[BasicFileAttributes] =
      FilesOs.getBasicFileAttributes(path, followLinks)

    /**
     * Gets the last modified time of the supplied path. The last modified time
     * is represented as a duration since the Unix epoch. Symbolic links are
     * followed.
     */
    def getLastModifiedTime: IO[FiniteDuration] =
      FilesOs.getLastModifiedTime(path)

    /**
     * Gets the last modified time of the supplied path. The last modified time
     * is represented as a duration since the Unix epoch. Symbolic links are
     * followed when `followLinks` is true.
     */
    def getLastModifiedTime(followLinks: Boolean): IO[FiniteDuration] =
      FilesOs.getLastModifiedTime(path, followLinks)

    /**
     * Gets the POSIX attributes for the supplied path. Symbolic links are not
     * followed.
     */
    def getPosixFileAttributes: IO[PosixFileAttributes] =
      FilesOs.getPosixFileAttributes(path)

    /**
     * Gets the POSIX attributes for the supplied path. Symbolic links are
     * followed when `followLinks` is true.
     */
    def getPosixFileAttributes(followLinks: Boolean): IO[PosixFileAttributes] =
      FilesOs.getPosixFileAttributes(path, followLinks)

    /**
     * Gets the POSIX permissions of the supplied path. Symbolic links are
     * followed.
     */
    def getPosixPermissions: IO[PosixPermissions] =
      FilesOs.getPosixPermissions(path)

    /**
     * Gets the POSIX permissions of the supplied path. Symbolic links are
     * followed when `followLinks` is true.
     */
    def getPosixPermissions(followLinks: Boolean): IO[PosixPermissions] =
      FilesOs.getPosixPermissions(path, followLinks)

    /**
     * Returns true if the supplied path exists and is a directory. Symbolic
     * links are followed.
     */
    def isDirectory: IO[Boolean] = FilesOs.isDirectory(path)

    /**
     * Returns true if the supplied path exists and is a directory. Symbolic
     * links are followed when `followLinks` is true.
     */
    def isDirectory(followLinks: Boolean): IO[Boolean] =
      FilesOs.isDirectory(path, followLinks)

    /** Returns true if the supplied path exists and is executable. */
    def isExecutable: IO[Boolean] = FilesOs.isExecutable(path)

    /**
     * Returns true if the supplied path is a hidden file (note: may not check
     * for existence).
     */
    def isHidden: IO[Boolean] = FilesOs.isHidden(path)

    /** Returns true if the supplied path exists and is readable. */
    def isReadable: IO[Boolean] = FilesOs.isReadable(path)

    /**
     * Returns true if the supplied path is a regular file. Symbolic links are
     * followed.
     */
    def isRegularFile: IO[Boolean] = FilesOs.isRegularFile(path)

    /**
     * Returns true if the supplied path is a regular file. Symbolic links are
     * followed when `followLinks` is true.
     */
    def isRegularFile(followLinks: Boolean): IO[Boolean] =
      FilesOs.isRegularFile(path, followLinks)

    /** Returns true if the supplied path is a symbolic link. */
    def isSymbolicLink: IO[Boolean] = FilesOs.isSymbolicLink(path)

    /** Returns true if the supplied path exists and is writable. */
    def isWritable: IO[Boolean] = FilesOs.isWritable(path)

    /** Returns true if the supplied path reference the same file. */
    def isSameFile(path2: Path): IO[Boolean] = FilesOs.isSameFile(path, path2)

    /** Gets the contents of the specified directory. */
    def list: Stream[IO, Path] = FilesOs.list(path)

    /**
     * Moves the source to the target, failing if source does not exist or the
     * target already exists. To replace the existing instead, use
     * `path.move(target, CopyFlags(CopyFlag.ReplaceExisting))`.
     */
    def move(target: Path): IO[Unit] = FilesOs.move(path, target)

    /**
     * Moves the source to the target, following any directives supplied in the
     * flags. By default, an error occurs if the target already exists, though
     * this can be overridden via `CopyFlag.ReplaceExisting`.
     */
    def move(target: Path, flags: CopyFlags): IO[Unit] =
      FilesOs.move(path, target, flags)

    /** Creates a `FileHandle` for the file at the supplied `Path`. */
    def open(flags: Flags): Resource[IO, FileHandle[IO]] =
      FilesOs.open(path, flags)

    /**
     * Returns a `ReadCursor` for the specified path, using the supplied flags
     * when opening the file.
     */
    def readCursor(flags: Flags): Resource[IO, ReadCursor[IO]] =
      FilesOs.readCursor(path, flags)

    // Real Path
    /** Returns the real path i.e. the actual location of `path`. */
    def realPath: IO[Path] = FilesOs.realPath(path)

    /**
     * Sets the last modified, last access, and creation time fields of the
     * specified path. Times which are supplied as `None` are not modified.
     */
    def setFileTimes(
        lastModified: Option[FiniteDuration],
        lastAccess: Option[FiniteDuration],
        creationTime: Option[FiniteDuration],
        followLinks: Boolean
    ): IO[Unit] =
      FilesOs.setFileTimes(
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
    def setPosixPermissions(permissions: PosixPermissions): IO[Unit] =
      FilesOs.setPosixPermissions(path, permissions)

    /** Gets the size of the supplied path, failing if it does not exist. */
    def size: IO[Long] = FilesOs.size(path)

  }

  // No path specific methods:

  /**
   * Creates a temporary file. The created file is not automatically deleted -
   * it is up to the operating system to decide when the file is deleted.
   * Alternatively, use `tempFile` to get a resource, which is deleted upon
   * resource finalization.
   */
  def createTempFile: IO[Path] = FilesOs.createTempFile

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
  ): IO[Path] =
    FilesOs.createTempFile(dir, prefix, suffix, permissions)

  /**
   * Creates a temporary directory. The created directory is not automatically
   * deleted - it is up to the operating system to decide when the file is
   * deleted. Alternatively, use `tempDirectory` to get a resource which deletes
   * upon resource finalization.
   */
  def createTempDirectory: IO[Path] = FilesOs.createTempDirectory

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
  ): IO[Path] =
    FilesOs.createTempDirectory(dir, prefix, permissions)

  /** User's current working directory */
  def currentWorkingDirectory: IO[Path] = FilesOs.currentWorkingDirectory

  /** Returns the line separator for the specific OS */
  def lineSeparator: String = FilesOs.lineSeparator

  /**
   * Creates a temporary file and deletes it at the end of the use of it.
   */
  def tempFile[A](use: Path => IO[A]): IO[A] =
    FilesOs.tempFile(use)

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
  )(use: Path => IO[A]): IO[A] =
    FilesOs.tempFile(dir, prefix, suffix, permissions)(use)

  /**
   * Creates a temporary directory and deletes it at the end of the use of it.
   */
  def tempDirectory[A](use: Path => IO[A]): IO[A] = FilesOs.tempDirectory(use)

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
  )(use: Path => IO[A]): IO[A] =
    FilesOs.tempDirectory(dir, prefix, permissions)(use)

  /** User's home directory */
  def userHome: IO[Path] = files.userHome

}
