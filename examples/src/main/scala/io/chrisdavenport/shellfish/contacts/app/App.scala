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

package io.chrisdavenport.shellfish.contacts.app

import cats.syntax.applicative.*
import cats.effect.{ExitCode, IO, IOApp}
import fs2.io.file.Path
import io.chrisdavenport.shellfish.contacts.cli.{Cli, Prompt}
import io.chrisdavenport.shellfish.contacts.core.ContactManager
import io.chrisdavenport.shellfish.contacts.domain.argument.*
import io.chrisdavenport.shellfish.syntax.path.*

object App extends IOApp {

  private val createBookPath: IO[Path] = for {
    home <- userHome
    dir  = home / ".shellfish"
    path = dir / "contacts.data"
    exists <- path.exists
    _      <- dir.createDirectories.unlessA(exists)
    _      <- path.createFile.unlessA(exists)
  } yield path

  def run(args: List[String]): IO[ExitCode] = createBookPath
    .map(ContactManager(_))
    .flatMap { implicit cm =>
      Prompt.parsePrompt(args) match {
        case Help                    => Cli.helpCommand
        case AddContact              => Cli.addCommand
        case RemoveContact(username) => Cli.removeCommand(username)
        case SearchId(username)      => Cli.searchUsernameCommand(username)
        case SearchName(name)        => Cli.searchNameCommand(name)
        case SearchEmail(email)      => Cli.searchEmailCommand(email)
        case SearchNumber(number)    => Cli.searchNumberCommand(number)
        case ViewAll                 => Cli.viewAllCommand
        case UpdateContact(username, flags) =>
          Cli.updateCommand(username, flags)
      }
    }
    .as(ExitCode.Success)
}