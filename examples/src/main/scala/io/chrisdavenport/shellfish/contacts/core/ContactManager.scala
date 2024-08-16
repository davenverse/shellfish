package io.chrisdavenport.shellfish.contacts.core

import cats.syntax.all.*
import cats.effect.IO
import fs2.io.file.Path
import io.chrisdavenport.shellfish.syntax.path.*
import io.chrisdavenport.shellfish.contacts.domain.contact.*

trait ContactManager {

  def addContact(contact: Contact): IO[Username]

  def removeContact(username: Username): IO[Unit]

  def searchId(username: Username): IO[Option[Contact]]
  def searchName(name: Name): IO[List[Contact]]
  def searchEmail(email: Email): IO[List[Contact]]
  def searchNumber(number: PhoneNumber): IO[List[Contact]]

  def getAll: IO[List[Contact]]

  /**
   * Update a contact with a function that describes de modification
   *
   * Example: updateContact(c => c.copy(firstName = "New Name"))
   *
   * @param modify
   *   Function that describes the modification
   * @return
   *   The updated contact
   */
  def updateContact(username: String)(modify: Contact => Contact): IO[Contact]

}

object ContactManager {
  def impl(bookPath: Path): ContactManager = new ContactManager {

    private def parseContact(contact: String): Either[Exception, Contact] =
      contact.split('|') match {
        case Array(id, firstName, lastName, phoneNumber, email) =>
          Contact(id, firstName, lastName, phoneNumber, email).asRight

        case _ => new Exception(s"Invalid contact format: $contact").asLeft
      }

    private def showPersisted(contact: Contact): String =
      s"${contact.username}|${contact.firstName}|${contact.lastName}|${contact.phoneNumber}|${contact.email}"

    override def addContact(contact: Contact): IO[Username] = {
      bookPath.readLines.flatMap { lines =>
        lines.find(_.startsWith(contact.username)) match {
          case Some(_) =>
            ContactFound(contact.username).raiseError[IO, Username]

          case None =>
            lines.isEmpty
              .pure[IO]
              .ifM(
                bookPath.write(showPersisted(contact)).as(contact.username),
                bookPath.appendLine(showPersisted(contact)).as(contact.username)
              )
        }
      }
    }

    override def removeContact(username: Username): IO[Unit] =
      for {
        contacts <- bookPath.readLines
        _ <- bookPath.writeLines(
          contacts.filterNot(_.startsWith(username))
        )
      } yield ()

    override def searchId(username: Username): IO[Option[Contact]] =
      for {
        contacts <- bookPath.readLines
      } yield contacts
        .find(_.startsWith(username))
        .flatMap(parseContact(_).toOption)

    override def searchName(name: Name): IO[List[Contact]] =
      for {
        contacts <- bookPath.readLines.map(_.filter(_.contains(name)))
        parsed <- contacts.traverse(contact =>
          IO.fromEither(parseContact(contact))
        )
      } yield parsed

    override def searchEmail(email: Email): IO[List[Contact]] =
      for {
        contacts <- bookPath.readLines.map(_.filter(_.contains(email)))
        parsed <- contacts.traverse(contact =>
          IO.fromEither(parseContact(contact))
        )
      } yield parsed

    override def searchNumber(number: PhoneNumber): IO[List[Contact]] =
      for {
        contacts <- bookPath.readLines.map(_.filter(_.contains(number)))
        parsed <- contacts.traverse(contact =>
          IO.fromEither(parseContact(contact))
        )
      } yield parsed

    override def getAll: IO[List[Contact]] =
      for {
        contacts <- bookPath.readLines
        parsed <- contacts.traverse(contact =>
          IO.fromEither(parseContact(contact))
        )
      } yield parsed

    override def updateContact(
        username: Username
    )(modify: Contact => Contact): IO[Contact] =
      bookPath.readLines
        .flatMap(_.toVector.traverse(c => IO.fromEither(parseContact(c))))
        .flatMap { contacts =>
          contacts.zipWithIndex.find(_._1.username == username) match {
            case Some((contact, index)) =>
              val updated = modify(contact)
              bookPath.writeLines(
                contacts.updated(index, updated).map(showPersisted)
              ) *> IO.pure(updated)
            case None =>
              new Exception(s"Contact $username nor found")
                .raiseError[IO, Contact]
          }
        }
  }
}
