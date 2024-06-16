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

import cats.effect.IO

import fs2.io.file.*

import scodec.bits.ByteVector
import scodec.Codec

import scala.concurrent.duration.FiniteDuration

package object path {

  implicit class ReadWriteOps(val path: Path) extends AnyVal {

    /**
     * Reads the contents of the file at the path and returns it as a String.
     * @param path
     *   The path to read from
     * @return
     *   The file loaded in memory as a String
     */
    def read: IO[String] = FilesOs.read(path)

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
     * Reads the contents of the file at the path and returns it line by line as
     * a Vector of Strings.
     * @param path
     *   The path to read from
     * @return
     *   The file loaded in memory as a collection of lines of Strings
     */
    def readLines: IO[Vector[String]] = FilesOs.readLines(path)

    /**
     * Reads the contents of the file and loads it as a type `A` using the
     * provided codec.
     * @param A
     *   The type to read the file as
     * @param path
     *   The path to read from
     * @param codec
     *   The codec that translates the file contents into the type `A`
     * @return
     *   The file loaded in memory as a type `A`
     * @throws Exception
     *   if the file cannot be read or the contents cannot be parsed into the
     *   type `A`
     */
    def readAs[A](implicit codec: scodec.Codec[A]): IO[A] = FilesOs.readAs(path)

    /**
     * The function reads the contents of the file at the path and returns it as
     * a Stream of Bytes, useful when working with large files.
     * @param path
     *   The path to read from
     * @return
     *   A Stream of Bytes
     */
    def stream: fs2.Stream[IO, Byte] = FilesOs.stream(path)

    /**
     * The function reads the contents of the file at the `path` and returns it
     * as a `Chunk[Bytes]`,
     * @param path
     *   The path to read from
     * @return
     *   The file loaded in memory as a Chunk of Bytes
     */
    def chunk: IO[fs2.Chunk[Byte]] = FilesOs.chunk(path)

    // Write operations:

    /**
     * This function overwites the contents of the file at the path with the
     * contents provided in form of a entire string loaded in memory.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @return
     *   The number of bytes written
     */
    def write(contents: String): IO[Long] = FilesOs.write(path)(contents)

    /**
     * This function overwites the contents of the file at the path with the
     * contents provided in form of bytes loaded in memory.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @return
     *   The number of bytes written
     */
    def writeBytes(contents: ByteVector): IO[Long] =
      FilesOs.writeBytes(path)(contents)

    /**
     * This function overwites the contents of the file at the path with the
     * contents provided. Each content inside the vector is written as a line in
     * the file.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @return
     *   The number of bytes written
     */
    def writeLines(contents: Vector[String]): IO[Long] =
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
     * @param codec
     *   The codec that translates the type A into a ByteVector
     * @return
     *   The number of bytes written
     */
    def writeAs[A](contents: A)(implicit codec: Codec[A]): IO[Long] =
      FilesOs.writeAs(path)(contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it.
     * Saves the content at the end of the file in form of a String.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @return
     *   The number of bytes written
     */
    def append(contents: String): IO[Long] = FilesOs.append(path)(contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it.
     * Saves the content at the end of the file in form of a ByteVector.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @return
     *   The number of bytes written
     */
    def appendBytes(contents: ByteVector): IO[Long] =
      FilesOs.appendBytes(path)(contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it.
     * Saves each line of the content at the end of the file in form of a Vector
     * of Strings.
     *
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @return
     *   The number of bytes written
     */
    def appendLines(contents: Vector[String]): IO[Long] =
      FilesOs.appendLines(path)(contents)

    /**
     * Similar to `write`, but appends to the file instead of overwriting it.
     *
     * @param A
     *   The type of the contents to write
     * @param path
     *   The path to write to
     * @param contents
     *   The contents to write to the file
     * @param codec
     *   The codec that translates the type A into a ByteVector
     * @return
     *   The number of bytes written
     */
    def appendAs[A](contents: A)(implicit codec: Codec[A]): IO[Long] =
      FilesOs.appendAs[A](path)(contents)

  }

  private val files = Files[IO]

  implicit class FileOps(val path: Path) extends AnyVal {

    /**
     * Copies the source to the target, failing if source does not exist or the
     * target already exists. To replace the existing instead, use `copy(source,
     * target, CopyFlags(CopyFlag.ReplaceExisting))`.
     */
    def copy(to: Path): IO[Unit] = files.copy(path, to)

    /**
     * Creates the specified directory. Fails if the parent path does not
     * already exist.
     */
    def createDirectory: IO[Unit] = files.createDirectories(path)

    /**
     * Creates the specified directory with the specified permissions. Fails if
     * the parent path does not already exist.
     */
    def createDirectory(permissions: Option[Permissions]): IO[Unit] =
      files.createDirectories(path, permissions)

    /**
     * Creates the specified directory and any non-existant parent directories.
     */
    def createFile: IO[Unit] = files.createFile(path)

    /**
     * Creates the specified directory and any parent directories, using the
     * supplied permissions for any directories that get created as a result of
     * this operation. For example if `/a` exists and
     * `createDirectories(Path("/a/b/c"), Some(p))` is called, `/a/b` and
     * `/a/b/c` are created with permissions set to `p` on each (and the
     * permissions of `/a` remain unmodified).
     */
    def createFile(permissions: Option[Permissions]): IO[Unit] =
      files.createFile(path, permissions)

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
        permissions: Option[Permissions]
    ): IO[Unit] =
      files.createSymbolicLink(path, target, permissions)

    /**
     * Deletes the file/directory if it exists, returning whether it was
     * deleted.
     */
    def deleteIfExists: IO[Boolean] = files.deleteIfExists(path)

    /** Deletes the file/directory recursively, following symbolic links. */
    def deleteRecursively(followLinks: Boolean = false): IO[Unit] =
      files.deleteRecursively(path, followLinks)

    /** Returns true if the path exists, optionally following symbolic links. */
    def exists(followLinks: Boolean = true): IO[Boolean] =
      files.exists(path, followLinks)

    /** Gets file attributes, optionally following symbolic links. */
    def getBasicFileAttributes(
        followLinks: Boolean = false
    ): IO[BasicFileAttributes] =
      files.getBasicFileAttributes(path, followLinks)

    /**
     * Gets POSIX file attributes, optionally following symbolic links (if
     * available).
     */
    def getPosixFileAttributes(
        followLinks: Boolean = false
    ): IO[PosixFileAttributes] =
      files.getPosixFileAttributes(
        path,
        followLinks
      ) // Note: May fail on non-POSIX systems

    /** Gets last modified time, optionally following symbolic links. */
    def getLastModifiedTime(followLinks: Boolean = true): IO[FiniteDuration] =
      files.getLastModifiedTime(path, followLinks)

    /**
     * Gets POSIX permissions, optionally following symbolic links (if
     * available).
     */
    def getPosixPermissions(followLinks: Boolean = true): IO[PosixPermissions] =
      files.getPosixPermissions(
        path,
        followLinks
      ) // Note: May fail on non-POSIX systems

    /**
     * Checks if the path is a directory, optionally following symbolic links.
     */
    def isDirectory(followLinks: Boolean = true): IO[Boolean] =
      files.isDirectory(path, followLinks)

    def isExecutable: IO[Boolean] = files.isExecutable(path)
    def isHidden: IO[Boolean]     = files.isHidden(path)
    def isReadable: IO[Boolean]   = files.isReadable(path)

    /**
     * Checks if the path is a regular file, optionally following symbolic
     * links.
     */
    def isRegularFile(followLinks: Boolean = true): IO[Boolean] =
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
