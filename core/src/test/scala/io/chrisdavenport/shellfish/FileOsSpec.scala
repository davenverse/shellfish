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

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import org.scalacheck.Gen

import cats.effect.IO

import fs2.io.file.*

import scodec.bits.ByteVector
import scodec.Codec

import scala.concurrent.duration.*

import syntax.path.*

object FileOsSpec extends SimpleIOSuite with Checkers {

  test("The API should delete a file") {
    withTempFile { path =>
      for {
        _       <- path.write("")
        deleted <- path.deleteIfExists
      } yield expect(deleted)
    }
  }

  test("Copying a file should have the same content as the original") {
    forall(Gen.asciiStr) { contents =>
      withTempFile { t1 =>
        withTempFile { t2 =>
          for {
            _  <- t1.write(contents)
            _  <- t1.copy(t2, CopyFlags(CopyFlag.ReplaceExisting))
            s1 <- t1.read
            s2 <- t2.read
            _  <- t1.deleteIfExists
            _  <- t2.deleteIfExists

          } yield expect.same(s1, s2)
        }
      }
    }
  }

  test(
    "Append to a file using `appendLine` should increase the size of the file in one"
  ) {

    val contentGenerator =
      Gen.size.flatMap(size => Gen.listOfN(size, Gen.asciiStr))

    forall(contentGenerator) { contentsList =>
      withTempFile { path =>
        for {
          _          <- path.writeLines(contentsList)
          sizeBefore <- path.readLines.map(_.size)
          _          <- path.appendLine("Im a last line!")
          sizeAfter  <- path.readLines.map(_.size)
        } yield expect(sizeBefore + 2 == sizeAfter)
        // That is, one new line of the appendLine and, one newline character of the writeLines method
      }
    }
  }

  test("Append should behave the same as adding at the end of the string") {
    forall(Gen.asciiStr) { contents =>
      withTempFile { path =>
        for {
          _          <- path.write(contents)
          sizeBefore <- path.read.map(_.length)
          _          <- path.append(contents)
          sizeAfter  <- path.read.map(_.length)
        } yield expect(sizeBefore + contents.length == sizeAfter)
      }
    }
  }

  test(
    "`append` should behave the same as `appendLine` when prepending a newline to the contents"
  ) {
    forall(Gen.asciiStr) { contents =>
      withTempFile { t1 =>
        withTempFile { t2 =>
          for {
            _     <- t1.append(s"\n$contents")
            _     <- t2.appendLine(contents)
            file1 <- t1.read
            file2 <- t2.read
          } yield expect.same(file1, file2)
        }
      }
    }
  }

  test(
    "`readAs` and `writeAs` should encode and decode correctly with the same codec"
  ) {
    implicit val codec: Codec[String] = scodec.codecs.ascii

    forall(Gen.asciiStr) { contents =>
      withTempFile { path =>
        for {
          _          <- path.writeAs(contents)
          sizeBefore <- path.read.map(_.length)
          _          <- path.append(contents)
          sizeAfter  <- path.read.map(_.length)
        } yield expect(sizeBefore + contents.length == sizeAfter)
      }
    }
  }

  test("We should write bytes and read bytes") {

    forall(Gen.identifier) { name =>
      withTempFile { path =>
        val bytes = ByteVector(name.getBytes)

        for {
          _    <- path.writeBytes(bytes)
          file <- path.readBytes
        } yield expect.same(bytes, file)
      }
    }
  }

  test(
    "We should be able to move a file and the contents should remain the same"
  ) {

    val pathsGenerator =
      for {
        pathLength <- Gen.choose(1, 10)
        names      <- Gen.listOfN(pathLength, Gen.alphaLowerStr)
      } yield names.foldLeft(Path(""))(_ / _)

    val multipleGenerators =
      for {
        path     <- pathsGenerator
        contents <- Gen.asciiStr
      } yield (path, contents)

    forall(multipleGenerators) { case (path, contents) =>
      withTempDirectory { dir =>
        val firstPath = dir / "moving_file.data"
        val movePath  = dir / path / "moved_file.data"

        for {
          _           <- firstPath.createFile
          _           <- firstPath.write(contents)
          _           <- (dir / path).createDirectories
          _           <- firstPath.move(movePath)
          moved       <- movePath.exists
          file        <- movePath.read
          oldLocation <- firstPath.exists
        } yield expect(moved) and expect.same(contents, file) and not(
          expect(oldLocation)
        )
      }
    }
  }

  test("`size` should correctly calculate the size in bytes of a file") {
    val contentGenerator: Gen[List[String]] =
      Gen.size.flatMap(size => Gen.listOfN(size, Gen.asciiStr))

    forall(contentGenerator) { contentsList =>
      withTempFile { path =>
        for {
          _          <- path.writeLines(contentsList)
          size       <- path.size
          streamSize <- Files[IO].readAll(path).compile.count
        } yield expect(size == streamSize)
      }
    }
  }

  test("We should create and delete recursively directories") {

    val pathsGenerator =
      for {
        pathLength <- Gen.choose(1, 10)
        names      <- Gen.listOfN(pathLength, Gen.alphaLowerStr)
      } yield names.foldLeft(Path(""))(_ / _)

    forall(pathsGenerator) { paths =>
      withTempDirectory { dir =>
        val tempDir = dir / paths

        for {
          _         <- tempDir.createDirectories
          exists    <- tempDir.exists
          _         <- tempDir.deleteRecursively
          notExists <- tempDir.exists
        } yield expect(exists) and not(expect(notExists))
      }
    }
  }

  test("We should be able to get and modify the last modified time of a file") {
    forall(Gen.asciiStr) { contents =>
      withTempFile { path =>
        for {
          _      <- path.write(contents)
          before <- path.getLastModifiedTime

          _ <- path.setFileTimes(
            lastModified = Option(before + 1.seconds),
            None,
            None,
            true
          )

          after <- path.getLastModifiedTime
          _     <- path.deleteIfExists
        } yield expect(before < after)
      }
    }
  }

  test(
    "Symbolic links should be created, and followed with the argument `followLinks`"
  ) {

    val pathsGenerator =
      for {
        pathLength <- Gen.choose(1, 10)
        names      <- Gen.listOfN(pathLength, Gen.alphaLowerStr)
      } yield names.foldLeft(Path(""))(_ / _)

    forall(pathsGenerator) { path =>
      withTempFile { file =>
        withTempDirectory { dir =>
          val link = dir / path / "link"
          for {
            _           <- (dir / path).createDirectories
            _           <- link.createSymbolicLink(file)
            exists      <- link.exists(followLinks = true)
            notFollowed <- link.exists(followLinks = false)
            _           <- link.deleteRecursively(followLinks = true)
            notExists   <- link.exists(followLinks = true)
          } yield expect(exists) and not(expect(notExists && notFollowed))
        }
      }
    }
  }

  pureTest("A line separator should add a new line to a string") {
    expect.same(s"$lineSeparator love live serve", s"\n love live serve")
  }

  // Warning: Platform dependent test; this may fail on some operating systems
  test("The permissions POSIX API should approve or prevent reading") {
    withTempFile { path =>
      for {
        _ <- path.setPosixPermissions(
          PosixPermissions.fromString("r--r--r--").get
        )
        readable <- path.isReadable

        _ <- path.setPosixPermissions(
          PosixPermissions.fromString("---------").get
        )
        notReadable <- path.isReadable
      } yield expect(readable) and not(expect(notReadable))
    }
  }

  // Warning: Platform dependent test; this may fail on some operating systems
  test("The permissions POSIX API should approve or prevent writing") {
    withTempFile { path =>
      for {
        _ <- path.setPosixPermissions(
          PosixPermissions.fromString("-w--w--w-").get
        )
        writable <- path.isWritable

        _ <- path.setPosixPermissions(
          PosixPermissions.fromString("---------").get
        )
        notWritable <- path.isWritable
      } yield expect(writable) and not(expect(notWritable))
    }
  }

  // Warning: Platform dependent test; this may fail on some operating systems
  test("The permissions POSIX API should approve or prevent executing") {
    withTempFile { path =>
      for {
        _ <- path.setPosixPermissions(
          PosixPermissions.fromString("--x--x--x").get
        )
        executable <- path.isExecutable

        _ <- path.setPosixPermissions(
          PosixPermissions.fromString("---------").get
        )
        notExecutable <- path.isExecutable
      } yield expect(executable) and not(expect(notExecutable))
    }
  }

  // Warning: Platform dependent test; this may fail on some operating systems
  test("We should be able to get the file permissions in POSIX systems") {

    val permissionsGenerator: Gen[PosixPermissions] =
      for {
        value <- Gen.choose(0, 511)
      } yield PosixPermissions.fromInt(value).get

    implicit val permShow: cats.Show[PosixPermissions] =
      cats.Show.show(_.toString)

    forall(permissionsGenerator) { permissions =>
      withTempFile { path =>
        for {
          _     <- path.setPosixPermissions(permissions)
          perms <- path.getPosixPermissions
        } yield expect(permissions == perms)
      }
    }
  }

}
