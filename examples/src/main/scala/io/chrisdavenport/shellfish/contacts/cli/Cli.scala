package io.chrisdavenport.shellfish.contacts.cli

import cats.effect.IO
import cats.syntax.all.*

import io.chrisdavenport.shellfish.contacts.core.ContactManager
import io.chrisdavenport.shellfish.contacts.domain.*
import io.chrisdavenport.shellfish.contacts.domain.contact.*

class Cli private (cm: ContactManager) {

  def addCommand: IO[Unit] =
    for {
      username    <- IO.println("Enter the username: ") >> IO.readLine
      firstName   <- IO.println("Enter the first name: ") >> IO.readLine
      lastName    <- IO.println("Enter the last name: ") >> IO.readLine
      phoneNumber <- IO.println("Enter the phone number: ") >> IO.readLine
      email       <- IO.println("Enter the email: ") >> IO.readLine

      contact = Contact(username, firstName, lastName, phoneNumber, email)

      _ <- cm.addContact(contact).handleErrorWith {
        case ContactFound(username) =>
          IO.println(s"Contact $username already exists")
        case e =>
          IO.println(s"An error occurred: \n${e.printStackTrace()}")
      }
    } yield ()

  def removeCommand(username: Username): IO[Unit] =
    cm.removeContact(username) >> IO.println(s"Contact $username removed")

  def searchIdCommand(username: Username): IO[Unit] =
    for {
      contact <- cm.searchId(username)
      _ <- contact match {
        case Some(c) => IO.println(c.show)
        case None    => IO.println(s"Contact $username not found")
      }
    } yield ()

  def searchNameCommand(name: Name): IO[Unit] =
    for {
      contacts <- cm.searchName(name)
      _        <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def searchEmailCommand(email: Email): IO[Unit] =
    for {
      contacts <- cm.searchEmail(email)
      _        <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def searchNumberCommand(number: PhoneNumber): IO[Unit] =
    for {
      contacts <- cm.searchNumber(number)
      _        <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def viewAllCommand: IO[Unit] =
    for {
      contacts <- cm.getAll
      _        <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def updateCommand(username: Username, options: List[Flag]): IO[Unit] =
    cm.updateContact(username) { prev =>
      options.foldLeft(prev) { (acc, flag) =>
        flag match {
          case FirstNameFlag(name)     => acc.copy(firstName = name)
          case LastNameFlag(name)      => acc.copy(lastName = name)
          case PhoneNumberFlag(number) => acc.copy(phoneNumber = number)
          case EmailFlag(email)        => acc.copy(email = email)
          case UnknownFlag(_)          => acc
        }
      }
    }.flatMap(c => IO.println(s"Updated contact ${c.username}"))

  def helpCommand: IO[Unit] =
    IO.println(
      s"""
        |Usage: contacts [command]
        |
        |Commands:
        |  add
        |  remove <username>
        |  search id <username>
        |  search name <name>
        |  search email <email>
        |  search number <number>
        |  list
        |  update <username> [flags]
        |  help
        |
        |Flags (for update command):
        |  --first-name <name>
        |  --last-name <name>
        |  --phone-number <number>
        |  --email <email>
        |
        |""".stripMargin
    )
}

object Cli {
  def make(cm: ContactManager): Cli = new Cli(cm)
}
