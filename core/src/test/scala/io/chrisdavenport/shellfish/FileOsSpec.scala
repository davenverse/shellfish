package io.chrisdavenport.shellfish

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import org.scalacheck.Gen

import cats.effect.IO

import fs2.io.file.{Path, Files}

import syntax.path.*

object FileOsSpec extends SimpleIOSuite with Checkers {

  val stringGen: Gen[String] = Gen.asciiStr
  val contents: String =
    stringGen.sample.getOrElse("Can't gen the contents 🤷🏻‍♀️")

  test("The API should create and delete a file") {
    val t = Path("core/src/test/resources/temp.tmp")
    for {
      _       <- t.createFile
      _       <- t.write(contents)
      deleted <- t.deleteIfExists
    } yield expect(deleted)
  }

  test("Copying a file should have the same content as the original") {

    val t1 = Path("core/src/test/resources/temp1.tmp")
    val t2 = Path("core/src/test/resources/temp2.tmp")

    for {
      _  <- t1.createFile
      _  <- t1.write(contents)
      _  <- t1.copy(t2)
      s1 <- t1.read
      s2 <- t2.read
      _  <- t1.deleteIfExists
      _  <- t2.deleteIfExists

    } yield expect.same(s1, s2)
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
