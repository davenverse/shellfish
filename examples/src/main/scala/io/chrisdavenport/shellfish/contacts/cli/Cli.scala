package io.chrisdavenport.shellfish.contacts.cli

import cats.effect.IO
import cats.syntax.all.*

import io.chrisdavenport.shellfish.contacts.core.ContactManager
import io.chrisdavenport.shellfish.contacts.domain.*

class Cli private (cm: ContactManager) {

  def addContact: IO[Unit] =
    for {
      _ <- IO.print("Enter the username: ")
      username = IO.readLine
      _ <- IO.print("Enter the first name: ")
      firstName = IO.readLine
      _ <- IO.print("Enter the last name: ")
      lastName = IO.readLine
      _ <- IO.print("Enter the phone number: ")
      phoneNumber = IO.readLine
      _ <- IO.print("Enter the email: ")
      email = IO.readLine
      contact <- (username, firstName, lastName, phoneNumber, email).parMapN(
        Contact.apply
      )
      _ <- cm.addContact(contact).handleErrorWith {
        case ContactFound(username) =>
          IO.println(s"Contact $username already exists")
      }

    } yield ()

  def removeContact(username: Username): IO[Unit] = cm.removeContact(username) >> IO.println(s"Contact $username removed")

  def searchId(username: Username): IO[Unit] =
    for {
      contact <- cm.searchId(username)
      _ <- contact match {
        case Some(c) => IO.println(c.show)
        case None    => IO.println(s"Contact $username not found")
      }
    } yield ()

  def searchName(name: Name): IO[Unit] =
    for {
      contacts <- cm.searchName(name)
      _ <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def searchEmail(email: Email): IO[Unit] =
    for {
      contacts <- cm.searchEmail(email)
      _ <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def searchNumber(number: PhoneNumber): IO[Unit] =
    for {
      contacts <- cm.searchNumber(number)
      _ <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def viewAll: IO[Unit] =
    for {
      contacts <- cm.getAll
      _ <- contacts.traverse_(c => IO.println(c.show))
    } yield ()

  def updateContact(username: Username, options: List[Flag]): IO[Unit] = {
    val modifiedContact = options.foldLeft()
    
    cm.updateContact(username)(c => c.copy())
  }


}

object Cli {
  def make(cm: ContactManager): Cli = new Cli(cm)
}
