package io.chrisdavenport.shellfish.contacts.app

import cats.syntax.applicative.*
import cats.effect.{ExitCode, IO, IOApp}
import fs2.io.file.Path
import io.chrisdavenport.shellfish.contacts.cli.{Cli, Prompt}
import io.chrisdavenport.shellfish.contacts.core.ContactManager
import io.chrisdavenport.shellfish.contacts.domain.*
import io.chrisdavenport.shellfish.syntax.path.*
import io.chrisdavenport.shellfish.contacts.domain.contact.*

object App extends IOApp {

  private val createBookPath: IO[Path] =
    for {
      home <- userHome
      dir  = home / ".shellfish"
      path = dir / "contacts.data"
      exists <- path.exists
      _      <- dir.createDirectories.unlessA(exists)
      _      <- path.createFile.unlessA(exists)
    } yield path

  def run(args: List[String]): IO[ExitCode] = createBookPath.flatMap {
    bookPath =>
      val cm  = ContactManager.impl(bookPath)
      val cli = Cli.make(cm)

      IO.pure(Prompt.parsePrompt(args)).flatMap {
        case Help                    => cli.helpCommand
        case AddContact              => cli.addCommand
        case RemoveContact(username) => cli.removeCommand(username)
        case SearchId(username)      => cli.searchIdCommand(username)
        case SearchName(name)        => cli.searchNameCommand(name)
        case SearchEmail(email)      => cli.searchEmailCommand(email)
        case SearchNumber(number)    => cli.searchNumberCommand(number)
        case ViewAll                 => cli.viewAllCommand
        case UpdateContact(username, flags) =>
          cli.updateCommand(username, flags)
      } >> IO(ExitCode.Success)
  }
}
