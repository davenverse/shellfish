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

import syntax.path.*

object SyntaxSpec extends SimpleIOSuite with Checkers {

  test(
    "Extension methods for read, write and append should work the same as the normal ones"
  ) {
    forall(Gen.asciiStr) { contents =>
      tempFile.use { path =>
        for {
          _  <- path.write(contents)
          _  <- path.append("Hi from the test!")
          s1 <- path.read
          _  <- FilesOs.write(path, contents)
          _  <- FilesOs.append(path, "Hi from the test!")
          s2 <- FilesOs.read(path)
        } yield expect.same(s1, s2)
      }
    }
  }

  test(
    "Extension methods for creating and deleting a file should work the same as the normal ones"
  ) {
    tempDirectory.use { dir =>
      val path = dir / "sample.txt"
      for {
        _        <- path.createFile
        exists   <- path.exists
        deleted  <- path.deleteIfExists
        _        <- FilesOs.createFile(path)
        exists2  <- FilesOs.exists(path)
        deleted2 <- FilesOs.deleteIfExists(path)
      } yield expect(exists && deleted) and expect(exists2 && deleted2)
    }
  }

  test(
    "Extension methods for copying a file should work the same as the normal ones"
  ) {
    forall(Gen.asciiStr) { contents =>
      tempDirectory.use { dir =>
        val original = dir / "sample.txt"
        val copy1    = dir / "sample-copy.txt"
        val copy2    = dir / "sample-copy-jo2.txt"
        for {
          _  <- original.write(contents)
          _  <- original.copy(copy1)
          _  <- FilesOs.copy(original, copy2)
          s1 <- copy1.read
          s2 <- copy2.read
        } yield expect.same(s1, s2)
      }
    }
  }

  test(
    "Extension methods for moving a file should work the same as the normal ones"
  ) {

    forall(Gen.asciiStr) { contents =>
      tempDirectory.use { dir =>
        val original = dir / "sample.txt"
        val moved    = dir / "sample-moved.txt"
        for {
          _      <- original.write(contents)
          _      <- original.move(moved)
          exists <- moved.exists

          _ <- moved.delete

          _       <- FilesOs.write(original, contents)
          _       <- FilesOs.move(original, moved)
          exists2 <- moved.exists
        } yield expect(exists && exists2)
      }
    }
  }

}
