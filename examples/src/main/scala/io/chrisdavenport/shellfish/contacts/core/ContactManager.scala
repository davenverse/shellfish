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

package io.chrisdavenport.shellfish.contacts.core

import cats.syntax.all.*
import cats.effect.IO
import fs2.io.file.Path
import io.chrisdavenport.shellfish.syntax.path.*
import io.chrisdavenport.shellfish.contacts.domain.contact.*

trait ContactManager {
  def addContact(contact: Contact): IO[Username]
  def removeContact(username: Username): IO[Unit]
  def searchUsername(username: Username): IO[Option[Contact]]
  def searchName(name: Name): IO[List[Contact]]
  def searchEmail(email: Email): IO[List[Contact]]
  def searchNumber(number: PhoneNumber): IO[List[Contact]]
  def getAll: IO[List[Contact]]
  def updateContact(username: String)(modify: Contact => Contact): IO[Contact]
}

object ContactManager {
  def apply(bookPath: Path): ContactManager = new ContactManager {

    private def parseContact(contact: String): IO[Contact] =
      contact.split('|') match {
        case Array(id, firstName, lastName, phoneNumber, email) =>
          Contact(id, firstName, lastName, phoneNumber, email).pure[IO]
        case _ =>
          new Exception(s"Invalid contact format: $contact")
            .raiseError[IO, Contact]
      }

    private def encodeContact(contact: Contact): String =
      s"${contact.username}|${contact.firstName}|${contact.lastName}|${contact.phoneNumber}|${contact.email}"

    private def saveContacts(contacts: List[Contact]): IO[Unit] =
      bookPath.writeLines(contacts.map(encodeContact))

    override def addContact(contact: Contact): IO[Username] = for {
      contacts <- getAll
      _ <- IO(contacts.contains(contact)).ifM(
        ContactFound(contact.username).raiseError[IO, Unit],
        saveContacts(contact :: contacts)
      )
    } yield contact.username

    override def removeContact(username: Username): IO[Unit] =
      for {
        contacts <- getAll
        filteredContacts = contacts.filterNot(_.username === username)
        _ <- saveContacts(filteredContacts)
      } yield ()

    override def searchUsername(username: Username): IO[Option[Contact]] =
      getAll.map(contacts => contacts.find(_.username === username))

    override def searchName(name: Name): IO[List[Contact]] =
      getAll.map(contacts =>
        contacts.filter(c => c.firstName === name || c.lastName === name)
      )

    override def searchEmail(email: Email): IO[List[Contact]] =
      getAll.map(contacts => contacts.filter(_.email === email))

    override def searchNumber(number: PhoneNumber): IO[List[Contact]] =
      getAll.map(contacts => contacts.filter(_.phoneNumber === number))

    override def getAll: IO[List[Contact]] = for {
      lines    <- bookPath.readLines
      contacts <- lines.traverse(parseContact)
    } yield contacts

    override def updateContact(
        username: Username
    )(modify: Contact => Contact): IO[Contact] = for {
      contacts <- getAll
      oldContact <- contacts.find(_.username === username) match {
        case None          => ContactNotFound(username).raiseError[IO, Contact]
        case Some(contact) => contact.pure[IO]
      }
      updatedContact  = modify(oldContact)
      updatedContacts = updatedContact :: contacts.filterNot(_ == oldContact)
      _ <- saveContacts(updatedContacts)
    } yield updatedContact
  }
}
