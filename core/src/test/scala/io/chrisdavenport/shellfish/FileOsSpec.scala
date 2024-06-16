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

import fs2.io.file.{Files, CopyFlags}

import syntax.path.*
import fs2.io.file.CopyFlag

object FileOsSpec extends SimpleIOSuite with Checkers {

  val stringGen: Gen[String] = Gen.asciiStr
  val contents: String =
    stringGen.sample.getOrElse("Can't gen the contents 🤷🏻‍♀️")

  test("The API should create and delete a file") {
    val temp = Files[IO].tempFile

    temp.use { path =>
      for {
        _       <- path.write(contents)
        deleted <- path.deleteIfExists
      } yield expect(deleted)
    }
  }

  test("Copying a file should have the same content as the original") {

    val temp =
      Files[IO].tempFile.flatMap(t1 => Files[IO].tempFile.map(t2 => (t1, t2)))

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

  test(
    "Append to a file using `append` should increate the size of the file in one"
  ) {

    val temp = Files[IO].tempFile
    val contentGenerator =
      Gen.size.flatMap(size => Gen.listOfN(size, stringGen))
    val contentsList =
      contentGenerator.sample.getOrElse(List("Can't gen the contents 🤷🏻‍♀️"))

    temp.use { path =>
      for {
        _          <- path.writeLines(contentsList.toVector)
        sizeBefore <- path.readLines.map(_.size)
        _          <- path.append("\nIm a last line!")
        sizeAfter  <- path.readLines.map(_.size)
      } yield expect(sizeBefore + 1 == sizeAfter)

    }
  }

  test(
    "`readAs` and `writeAs` should encode and decode correctly with the same codec"
  ) {
    val temp           = Files[IO].tempFile
    implicit val codec = scodec.codecs.utf8

    temp.use { path =>
      for {
        _    <- path.writeAs(contents)
        file <- path.readAs[String]
      } yield expect.same(contents, file)
    }
  }

  test("We should write bytes and read bytes") {
    val temp  = Files[IO].tempFile
    val bytes = scodec.bits.ByteVector(contents.getBytes())
    temp.use { path =>
      for {
        _    <- path.writeBytes(bytes)
        file <- path.readBytes
      } yield expect.same(bytes, file)
    }
  }
}
