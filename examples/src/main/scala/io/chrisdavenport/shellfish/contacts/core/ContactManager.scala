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
  def impl: ContactManager = new ContactManager {

    private val bookPath =
      userHome.map(usrHome => usrHome / ".shellfish" / "contacts.data")

    private def parseContact(contact: String): Either[Exception, Contact] =
      contact.split('|') match {
        case Array(id, firstName, lastName, phoneNumber, email) =>
          Contact(id, firstName, lastName, phoneNumber, email).asRight

        case _ => new Exception(s"Invalid contact format: $contact").asLeft
      }

    private def showPersisted(contact: Contact): String =
      s"${contact.username}|${contact.firstName}|${contact.lastName}|${contact.phoneNumber}|${contact.email}"
    
    override def addContact(contact: Contact): IO[Username] =
      bookPath.flatMap { path =>
        
        path.readLines.flatMap { lines =>
          lines.find(_.startsWith(contact.username)) match {
            case Some(_) =>
              ContactFound(contact.username).raiseError[IO, Username]

            case None =>
              path.exists.ifM(
                path.appendLine(showPersisted(contact)),
                path.write(showPersisted(contact))
              ) >> contact.username.pure[IO]
          }
        }
      }

    override def removeContact(username: Username): IO[Unit] =
      for {
        path     <- bookPath
        contacts <- path.readLines
        _ <- path.writeLines(
          contacts.filterNot(_.startsWith(username))
        )
      } yield ()

    override def searchId(username: Username): IO[Option[Contact]] =
      for {
        path     <- bookPath
        contacts <- path.readLines
      } yield contacts
        .find(_.startsWith(username))
        .flatMap(parseContact(_).toOption)

    override def searchName(name: Name): IO[List[Contact]] =
      for {
        path     <- bookPath
        contacts <- path.readLines.map(_.filter(_.contains(name)))
        parsed <- contacts.traverseEither(parseContact(_).pure[IO])((_, e) =>
          IO.raiseError(e).void
        )
      } yield parsed

    override def searchEmail(email: Email): IO[List[Contact]] =
      for {
        path     <- bookPath
        contacts <- path.readLines.map(_.filter(_.contains(email)))
        parsed <- contacts.traverseEither(parseContact(_).pure[IO])((_, e) =>
          IO.raiseError(e).void
        )
      } yield parsed

    override def searchNumber(number: PhoneNumber): IO[List[Contact]] =
      for {
        path     <- bookPath
        contacts <- path.readLines.map(_.filter(_.contains(number)))
        parsed <- contacts.traverseEither(parseContact(_).pure[IO])((_, e) =>
          IO.raiseError(e).void
        )
      } yield parsed

    override def getAll: IO[List[Contact]] =
      for {
        path     <- bookPath
        contacts <- path.readLines
        parsed <- contacts.traverseEither(parseContact(_).pure[IO])((_, e) =>
          IO.raiseError(e).void
        )
      } yield parsed

    override def updateContact(
        username: Username
    )(modify: Contact => Contact ): IO[Contact] =
      bookPath.flatMap { path =>
        path.readLines.map(_.toVector).flatMap { contacts =>
          contacts
            .find(_.startsWith(username))
            .flatMap(parseContact(_).toOption) match {
            case Some(contact) =>
              path.writeLines(
                contacts.map(
                  _.replaceFirst(
                    s"${username}.*",
                    showPersisted(modify(contact))
                  )
                )
              ) >> modify(contact).pure[IO]
            case None =>
              IO.raiseError(
                new Exception(s"Contact with id $username not found")
              )

          }
        }
      }
  }
}
