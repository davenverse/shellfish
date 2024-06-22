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

import cats.effect.IO

import fs2.{Stream, Chunk}
import fs2.io.file.{Files, Path, Flags}

import scodec.Codec
import scodec.bits.ByteVector

import java.nio.charset.Charset

object FilesOs {

  val files: Files[IO] = Files[IO]

  // Read operations:

  /**
   * Reads the contents of the fileat the path using UTF-8 decoding. Returns it
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
   * @param charset
   *   The charset to use to decode the file
   * @param path
   *   The path to read from
   * @return
   *   The file loaded in memory as a String
   */
  def readWithCharset(charset: Charset)(path: Path): IO[String] =
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
   * @param A
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

  /**
   * The function reads the contents of the file at the path and returns it as a
   * Stream of Bytes, useful when working with large files.
   * @param path
   *   The path to read from
   * @return
   *   A Stream of Bytes
   */
  def readStream(path: Path): Stream[IO, Byte] = files.readAll(path)

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
  def write(path: Path)(contents: String): IO[Unit] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(files.writeUtf8(path))
      .compile
      .drain

  /**
   * This function overwites the contents of the file at the path with the
   * contents provided in form of bytes loaded in memory.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def writeBytes(path: Path)(contents: ByteVector): IO[Unit] =
    Stream
      .chunk(Chunk.byteVector(contents))
      .covary[IO]
      .through(files.writeAll(path))
      .compile
      .drain

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
  def writeLines(path: Path)(contents: Seq[String]): IO[Unit] =
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
   * @param A
   *   The type of the contents to write
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   * @param Codec[A]
   *   The codec that translates the type A into a ByteVector
   */
  def writeAs[A: Codec](path: Path)(contents: A): IO[Unit] =
    writeBytes(path)(Codec[A].encode(contents).require.bytes)

  /**
   * Similar to `write`, but appends to the file instead of overwriting it.
   * Saves the content at the end of the file in form of a String.
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def append(path: Path)(contents: String): IO[Unit] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(files.writeUtf8(path, Flags.Append))
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
  def appendBytes(path: Path)(contents: ByteVector): IO[Unit] =
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
  def appendLines(path: Path)(contents: Seq[String]): IO[Unit] =
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
   * Equivalent to `append(path)(s"\n$contents")`
   *
   * @param path
   *   The path to write to
   * @param contents
   *   The contents to write to the file
   */
  def appendLine(path: Path)(contents: String): IO[Unit] =
    appendLines(path)(List(contents))

  /**
   * Similar to `write`, but appends to the file instead of overwriting it.
   * Saves the content at the end of the file in form of a type `A`.
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
  def appendAs[A: Codec](path: Path)(contents: A): IO[Unit] =
    appendBytes(path)(Codec[A].encode(contents).require.bytes)
}
