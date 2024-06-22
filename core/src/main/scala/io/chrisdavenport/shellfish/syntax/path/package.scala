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

import cats.syntax.all.*
import cats.effect.IO

import fs2.io.file.*

import scodec.bits.ByteVector
import scodec.Codec

import scala.concurrent.duration.FiniteDuration
import java.nio.charset.Charset

package object path {

  implicit class ReadWriteOps(val path: Path) extends AnyVal {

    /**
     * Reads the contents of the fileat the path using UTF-8 decoding. Returns
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
    def readWithCharset(charset: Charset)(path: Path): IO[String] =
      FilesOs.readWithCharset(charset)(path)

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
     * @param A
     *   The type to read the file as
     * @param path
     *   The path to read from
     * @param Codec[A]
     *   The codec that translates the file contents into the type `A`
     * @return
     *   The file loaded in memory as a type `A`
     */
    def readAs[A: Codec]: IO[A] = FilesOs.readAs(path)

    /**
     * The function reads the contents of the file at the path and returns it as
     * a Stream of Bytes, useful when working with large files.
     * @param path
     *   The path to read from
     * @return
     *   A Stream of Bytes
     */
    def readStream: fs2.Stream[IO, Byte] = FilesOs.readStream(path)

    // Write operations:

    /**
     * This function overwites the contents of the file at the path using UTF-8
     * encoding with the contents provided in form of a entire string loaded in
     * memory.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def write(contents: String): IO[Unit] = FilesOs.write(path)(contents)

    /**
     * This function overwites the contents of the file at the path with the
     * contents provided in form of bytes loaded in memory.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def writeBytes(contents: ByteVector): IO[Unit] =
      FilesOs.writeBytes(path)(contents)

    /**
     * This function overwites the contents of the file at the path using UTF-8
     * encoding with the contents provided. Each content inside the list is
     * written as a line in the file.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def writeLines(contents: Seq[String]): IO[Unit] =
      FilesOs.writeLines(path)(contents)

    /**
     * The functions writes the contents of the file at the path with the
     * contents provided and returns the number of bytes written. The codec is
     * used to translate the type A into a ByteVector so it can be parsed into
     * the file.
     *
     * @param A
     *   The type of the contents to write
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @param Codec[A]
     *   The codec that translates the type A into a ByteVector
     */
    def writeAs[A: Codec](contents: A): IO[Unit] =
      FilesOs.writeAs(path)(contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it.
     * Saves the content at the end of the file in form of a String.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def append(contents: String): IO[Unit] = FilesOs.append(path)(contents)

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
      FilesOs.appendBytes(path)(contents)

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
      FilesOs.appendLines(path)(contents)

    /**
     * Similar to append, but appends a single line to the end file as a newline
     * instead of overwriting it.
     *
     * Equivalent to `path.append(s"\n$contents")`
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     */
    def appendLine(contents: String): IO[Unit] =
      FilesOs.appendLine(path)(contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it
     * using the given `Codec[A]`
     *
     * @param A
     *   The type of the contents to write
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @param Codec[A]
     *   The codec that translates the type A into a ByteVector
     */
    def appendAs[A: Codec](contents: A): IO[Unit] =
      FilesOs.appendAs[A](path)(contents)

  }

  private val files = Files[IO]

  implicit class FileOps(val path: Path) extends AnyVal {

    /**
     * Copies the source to the target, failing if source does not exist or the
     * target already exists. To replace the existing instead, use
     * `source.copy(CopyFlags(CopyFlag.ReplaceExisting))(target)`.
     */
    def copy(to: Path): IO[Unit] = files.copy(path, to)

    /**
     * Copies the source to the target, following any directives supplied in the
     * flags. By default, an error occurs if the target already exists, though
     * this can be overriden via CopyFlag.ReplaceExisting.
     */
    def copy(flags: CopyFlags)(to: Path): IO[Unit] = files.copy(path, to, flags)

    /**
     * Creates the specified directory with the permissions of "rwxrwxr-x" by
     * default. Fails if the parent path does not already exist.
     */
    def createDirectory: IO[Unit] = files.createDirectories(path)

    /**
     * Creates the specified directory with the specified permissions. Fails if
     * the parent path does not already exist.
     */
    def createDirectory(permissions: Permissions): IO[Unit] =
      files.createDirectories(path, permissions.some)

    /**
     * Creates the specified directory and any non-existant parent directories.
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
     * Creates the specified file with the permissions of "rw-rw-r--" by
     * default. Fails if the parent path does not already exist.
     */
    def createFile: IO[Unit] = files.createFile(path)

    /**
     * Creates the specified file with the specified permissions. Fails if the
     * parent path does not already exist.
     */
    def createFile(permissions: Permissions): IO[Unit] =
      files.createFile(path, permissions.some)

    /** Creates a hard link with an existing file. */
    def createLink(existing: Path): IO[Unit] = files.createLink(path, existing)

    /** Creates a symbolic link which points to the supplied target. */
    def createSymbolicLink(target: Path): IO[Unit] =
      files.createSymbolicLink(path, target)

    /**
     * Creates a symbolic link which points to the supplied target with optional
     * permissions.
     */
    def createSymbolicLink(
        target: Path,
        permissions: Permissions
    ): IO[Unit] =
      files.createSymbolicLink(path, target, permissions.some)

    /**
     * Deletes the file/directory if it exists, returning whether it was
     * deleted.
     */
    def deleteIfExists: IO[Boolean] = files.deleteIfExists(path)

    /**
     * Deletes the file/directory recursively, without following the symbolic
     * links.
     */
    def deleteRecursively: IO[Unit] =
      files.deleteRecursively(path, true)

    /**
     * Deletes the file/directory recursively, optionally following symbolic
     * links.
     */
    def deleteRecursively(followLinks: Boolean): IO[Unit] =
      files.deleteRecursively(path, followLinks)

    /** Returns true if the path exists, following the symbolic links. */
    def exists: IO[Boolean] =
      files.exists(path, true)

    /** Returns true if the path exists, optionally following symbolic links. */
    def exists(followLinks: Boolean): IO[Boolean] =
      files.exists(path, followLinks)

    /** Gets file attributes, without following symbolic links. */
    def getBasicFileAttributes: IO[BasicFileAttributes] =
      files.getBasicFileAttributes(path, false)

    /** Gets file attributes, optionally following symbolic links. */
    def getBasicFileAttributes(followLinks: Boolean): IO[BasicFileAttributes] =
      files.getBasicFileAttributes(path, followLinks)

    /**
     * Gets POSIX file attributes, without following symbolic links (if
     * available).
     */
    def getPosixFileAttributes: IO[PosixFileAttributes] =
      files.getPosixFileAttributes(
        path,
        false
      ) // Note: May fail on non-POSIX systems

    /**
     * Gets POSIX file attributes, optionally following symbolic links (if
     * available).
     */
    def getPosixFileAttributes(
        followLinks: Boolean
    ): IO[PosixFileAttributes] =
      files.getPosixFileAttributes(
        path,
        followLinks
      ) // Note: May fail on non-POSIX systems

    /** Gets last modified time, following symbolic links. */
    def getLastModifiedTime: IO[FiniteDuration] =
      files.getLastModifiedTime(path, true)

    /** Gets last modified time, optionally following symbolic links. */
    def getLastModifiedTime(followLinks: Boolean): IO[FiniteDuration] =
      files.getLastModifiedTime(path, followLinks)

    /**
     * Gets POSIX permissions, following symbolic links (if available).
     */
    def getPosixPermissions: IO[PosixPermissions] =
      files.getPosixPermissions(
        path,
        true
      ) // Note: May fail on non-POSIX systems

    /**
     * Gets POSIX permissions, optionally following symbolic links (if
     * available).
     */
    def getPosixPermissions(followLinks: Boolean): IO[PosixPermissions] =
      files.getPosixPermissions(
        path,
        followLinks
      ) // Note: May fail on non-POSIX systems

    /**
     * Checks if the path is a directory, optionally following symbolic links.
     */
    def isDirectory: IO[Boolean] =
      files.isDirectory(path, true)

    /**
     * Checks if the path is a directory, optionally following symbolic links.
     */
    def isDirectory(followLinks: Boolean): IO[Boolean] =
      files.isDirectory(path, followLinks)

    def isExecutable: IO[Boolean] = files.isExecutable(path)
    def isHidden: IO[Boolean]     = files.isHidden(path)
    def isReadable: IO[Boolean]   = files.isReadable(path)

    /**
     * Checks if the path is a regular file, following symbolic links.
     */
    def isRegularFile: IO[Boolean] =
      files.isRegularFile(path, true)

    /**
     * Checks if the path is a regular file, optionally following symbolic
     * links.
     */
    def isRegularFile(followLinks: Boolean): IO[Boolean] =
      files.isRegularFile(path, followLinks)

    def isSymbolicLink: IO[Boolean] = files.isSymbolicLink(path)
    def isWritable: IO[Boolean]     = files.isWritable(path)

    /** Checks if this path references the same file as another. */
    def isSameFile(other: Path): IO[Boolean] = files.isSameFile(path, other)

    /** Gets the real path, resolving symbolic links. */
    def realPath: IO[Path] = files.realPath(path)

    /** Sets POSIX permissions (if supported). */
    def setPosixPermissions(permissions: PosixPermissions): IO[Unit] =
      files.setPosixPermissions(path, permissions)

    /** Gets the size of the supplied path, failing if it does not exist. */
    def size: IO[Long] = files.size(path)
  }
}
