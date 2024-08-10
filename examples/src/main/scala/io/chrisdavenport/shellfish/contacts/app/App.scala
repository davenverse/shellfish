package io.chrisdavenport.shellfish.contacts.app

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.shellfish.contacts.cli.{Cli, Prompt}
import io.chrisdavenport.shellfish.contacts.core.ContactManager
import io.chrisdavenport.shellfish.contacts.domain.*
import io.chrisdavenport.shellfish.contacts.domain.contact.*


object App extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    
    val cm = ContactManager.impl
    val cli = Cli.make(cm)
    
    IO.pure(Prompt.parsePrompt(args)).flatMap {
      case Help => cli.help
      case AddContact => cli.addContact
      case RemoveContact(username) => cli.removeContact(username)
      case SearchId(username) => cli.searchId(username)
      case SearchName(name) => cli.searchName(name)
      case SearchEmail(email) => cli.searchEmail(email)
      case SearchNumber(number) => cli.searchNumber(number)
      case ViewAll => cli.viewAll
      case UpdateContact(username, flags) => cli.updateContact(username, flags)
    } >> IO(ExitCode.Success)
  }
}
