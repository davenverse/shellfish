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

import cats.effect.{IO, Resource}

import fs2.io.file.{Files, CopyFlags}
import fs2.io.file.CopyFlag

import scodec.bits.ByteVector

import syntax.path.*

import weaver.scalacheck.CheckConfig

object FileOsSpec extends SimpleIOSuite with Checkers {

  override def checkConfig: CheckConfig =
    super.checkConfig.copy(perPropertyParallelism = 1)

  test("The API should create and delete a file") {
    FilesOs.files.tempFile.use { path =>
      for {
        _       <- path.write("")
        deleted <- path.deleteIfExists
      } yield expect(deleted)
    }
  }

  test("Copying a file should have the same content as the original") {

    val temp = Resource.both(Files[IO].tempFile, Files[IO].tempFile)

    forall(Gen.asciiStr) { contents =>
      temp.use { case (t1, t2) =>
        for {
          _  <- t1.write(contents)
          _  <- t1.copy(CopyFlags(CopyFlag.ReplaceExisting))(t2)
          s1 <- t1.read
          s2 <- t2.read
          _  <- t1.deleteIfExists
          _  <- t2.deleteIfExists

        } yield expect.same(s1, s2)
      }
    }
  }

  test(
    "Append to a file using `appendLine` should increate the size of the file in one"
  ) {

    val contentGenerator =
      Gen.size.flatMap(size => Gen.listOfN(size, Gen.asciiStr))

    forall(contentGenerator) { contentsList =>
      FilesOs.files.tempFile.use { path =>
        for {
          _          <- path.writeLines(contentsList)
          sizeBefore <- path.readLines.map(_.size)
          _          <- path.appendLine("Im a last line!")
          sizeAfter  <- path.readLines.map(_.size)
        } yield expect(sizeBefore + 1 == sizeAfter)
      }
    }
  }

  test("Append should behave the same as adding at the end of the string") {
    forall(Gen.asciiStr) { contents =>
      FilesOs.files.tempFile.use { path =>
        for {
          _          <- path.write(contents)
          sizeBefore <- path.read.map(_.size)
          _          <- path.append(contents)
          sizeAfter  <- path.read.map(_.size)
        } yield expect(sizeBefore + contents.size == sizeAfter)
      }
    }
  }

  test(
    "`readAs` and `writeAs` should encode and decode correctly with the same codec"
  ) {
    implicit val codec = scodec.codecs.ascii

    forall(Gen.asciiStr) { contents =>
      FilesOs.files.tempFile.use { path =>
        for {
          _          <- path.writeAs(contents)
          sizeBefore <- path.read.map(_.size)
          _          <- path.append(contents)
          sizeAfter  <- path.read.map(_.size)
        } yield expect(sizeBefore + contents.size == sizeAfter)
      }
    }
  }

  test("We should write bytes and read bytes") {

    forall(Gen.identifier) { name =>
      FilesOs.files.tempFile.use { path =>
        val bytes = ByteVector(name.getBytes)

        for {
          _    <- path.writeBytes(bytes)
          file <- path.readBytes
        } yield expect.same(bytes, file)
      }
    }
  }
}
