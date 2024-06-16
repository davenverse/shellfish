package io.chrisdavenport.shellfish

import cats.syntax.all.*
import cats.effect.IO

import fs2.{Stream, Chunk}
import fs2.interop.scodec.*
import fs2.io.file.{Files, Path, Flags}

import scodec.{Codec, codecs}
import scodec.bits.ByteVector

object FilesOs {

  val files = Files[IO]

  // Read operations:

  /**
   * Reads the contents of the file at the path and returns it as a String.
   * @param path
   *   The path to read from
   * @return
   *   The file loaded in memory as a String
   */
  def read(path: Path): IO[String] = files.readUtf8(path).compile.string

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
   * Reads the contents of the file at the path and returns it line by line as a
   * Vector of Strings.
   *
   * @param path
   *   The path to read from
   * @return
   *   The file loaded in memory as a collection of lines of Strings
   */
  def readLines(path: Path): IO[Vector[String]] =
    files.readUtf8Lines(path).compile.toVector

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
   *   if the file cannot be read or the contents cannot be parsed into the type
   *   `A`
   */
  def readAs[A](path: Path)(implicit codec: Codec[A]): IO[A] =
    readBytes(path)
      .map(bytes => codec.decodeValue(bytes.bits).require)
      .handleErrorWith { e =>
        new Exception(s"Failed to read file at $path", e).raiseError[IO, A]
      }

  /**
   * The function reads the contents of the file at the path and returns it as a
   * Stream of Bytes, useful when working with large files.
   * @param path
   *   The path to read from
   * @return
   *   A Stream of Bytes
   */
  def stream(path: Path): Stream[IO, Byte] = files.readAll(path)

  /**
   * The function reads the contents of the file at the `path` and returns it as
   * a `Chunk[Bytes]`,
   * @param path
   *   The path to read from
   * @return
   *   The file loaded in memory as a Chunk of Bytes
   */
  def chunk(path: Path): IO[Chunk[Byte]] = files.readAll(path).compile.to(Chunk)

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
  def write(path: Path)(contents: String): IO[Long] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(files.writeUtf8(path))
      .compile
      .count

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
  def writeBytes(path: Path)(contents: ByteVector): IO[Long] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(StreamEncoder.many(codecs.bytes).toPipeByte)
      .through(files.writeAll(path))
      .compile
      .count

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
  def writeLines(path: Path)(contents: Vector[String]): IO[Long] =
    Stream
      .emits(contents)
      .covary[IO]
      .through(files.writeUtf8Lines(path))
      .compile
      .count

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
   * @param codec
   *   The codec that translates the type A into a ByteVector
   * @return
   *   The number of bytes written
   */
  def writeAs[A](path: Path)(contents: A)(implicit codec: Codec[A]): IO[Long] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(StreamEncoder.many(codec).toPipeByte)
      .through(files.writeAll(path))
      .compile
      .count

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
  def append(path: Path)(contents: String): IO[Long] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(files.writeUtf8(path, Flags.Append))
      .compile
      .count

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
  def appendBytes(path: Path)(contents: ByteVector): IO[Long] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(StreamEncoder.many(codecs.bytes).toPipeByte)
      .through(files.writeAll(path, Flags.Append))
      .compile
      .count

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
  def appendLines(path: Path)(contents: Vector[String]): IO[Long] =
    Stream
      .emits(contents)
      .covary[IO]
      .through(files.writeUtf8Lines(path, Flags.Append))
      .compile
      .count

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
   * @param codec
   *   The codec that translates the type A into a ByteVector
   * @return
   *   The number of bytes written
   */
  def appendAs[A](path: Path)(contents: A)(implicit codec: Codec[A]): IO[Long] =
    Stream
      .emit(contents)
      .covary[IO]
      .through(StreamEncoder.many(codec).toPipeByte)
      .through(files.writeAll(path, Flags.Append))
      .compile
      .count
}
