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

import syntax.path.*

object MainSpec extends SimpleIOSuite {

  import Shell.io.{cd, pwd}

  pureTest("Main should exit successfully") {
    expect(1 == 1)
  }

  test("cd should some back and forth") {
    for {
      current  <- pwd
      _        <- cd("..")
      _        <- cd(current)
      current2 <- pwd
    } yield expect(current == current2)
  }

  test("We should be able to create and delete a directory") {

    for {
      home <- userHome
      dir = home / "shellfish"
      _       <- dir.createDirectory
      exists  <- dir.exists
      deleted <- dir.deleteIfExists
    } yield expect(exists && deleted)
  }
}
