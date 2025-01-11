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

package io.chrisdavenport.shellfish.contacts.cli

import cats.effect.IO
import cats.syntax.all.*

import io.chrisdavenport.shellfish.contacts.core.ContactManager
import io.chrisdavenport.shellfish.contacts.domain.flag.*
import io.chrisdavenport.shellfish.contacts.domain.contact.*

object Cli {

  def addCommand(implicit cm: ContactManager): IO[Unit] =
    for {
      username    <- IO.println("Enter the username: ") >> IO.readLine
      firstName   <- IO.println("Enter the first name: ") >> IO.readLine
      lastName    <- IO.println("Enter the last name: ") >> IO.readLine
      phoneNumber <- IO.println("Enter the phone number: ") >> IO.readLine
      email       <- IO.println("Enter the email: ") >> IO.readLine

      contact = Contact(username, firstName, lastName, phoneNumber, email)

      _ <- cm
        .addContact(contact)
        .flatMap(username => IO.println(s"Contact $username added"))
        .handleErrorWith {
          case ContactFound(username) =>
            IO.println(s"Contact $username already exists")
          case e =>
            IO.println(s"An error occurred: \n${e.printStackTrace()}")
        }
    } yield ()

  def removeCommand(username: Username)(implicit cm: ContactManager): IO[Unit] =
    cm.removeContact(username) >> IO.println(s"Contact $username removed")

  def searchUsernameCommand(
      username: Username
  )(implicit cm: ContactManager): IO[Unit] =
    cm.searchUsername(username).flatMap {
      case Some(c) => IO.println(c.show)
      case None    => IO.println(s"Contact $username not found")
    }

  def searchNameCommand(name: Name)(implicit cm: ContactManager): IO[Unit] =
    for {
      contacts <- cm.searchName(name)
      _        <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def searchEmailCommand(email: Email)(implicit cm: ContactManager): IO[Unit] =
    for {
      contacts <- cm.searchEmail(email)
      _        <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def searchNumberCommand(
      number: PhoneNumber
  )(implicit cm: ContactManager): IO[Unit] =
    for {
      contacts <- cm.searchNumber(number)
      _        <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def viewAllCommand(implicit cm: ContactManager): IO[Unit] = for {
    contacts <- cm.getAll
    _        <- contacts.traverse_(c => IO.println(c.show))
  } yield ()

  def updateCommand(username: Username, options: List[Flag])(implicit
      cm: ContactManager
  ): IO[Unit] = cm
    .updateContact(username) { prev =>
      options.foldLeft(prev) { (acc, flag) =>
        flag match {
          case FirstNameFlag(name)     => acc.copy(firstName = name)
          case LastNameFlag(name)      => acc.copy(lastName = name)
          case PhoneNumberFlag(number) => acc.copy(phoneNumber = number)
          case EmailFlag(email)        => acc.copy(email = email)
          case UnknownFlag(_)          => acc
        }
      }
    }
    .flatMap(c => IO.println(s"Updated contact ${c.username}"))
    .handleErrorWith {
      case ContactNotFound(username) =>
        IO.println(s"Contact $username not found")
      case e =>
        IO.println(s"An error occurred: \n${e.printStackTrace()}")
    }

  def helpCommand: IO[Unit] = IO.println(
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
