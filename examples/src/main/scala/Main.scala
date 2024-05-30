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

import cats.effect._
import cats.syntax.all._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    import Shell.io._
    val p = SubProcess.io
    for {
      // init <- pwd
      // _ <- echo(init)
      // _ <- mkdir("test")
      // _ <- cd("test")
      // now <- pwd
      // _ <- echo(now)
      // _ <- writeTextFile("test.txt", "Hi There! 3")

      // _ <- p.shellStrict("pwd").flatTap(s => echo(s.toString))
      // got <- p.shellStrict("find", List("-wholename", "*.scala"))
      // got <- which("java")
      // got <- hostname
      _ <- cd("..")
      got <- ls.compile.toList
      _ <- echo(got.toString)
      // _ <- p.shellStrict("which", "java":: Nil).flatMap(a => echo(a.toString))
    } yield ExitCode.Success
  }

}
