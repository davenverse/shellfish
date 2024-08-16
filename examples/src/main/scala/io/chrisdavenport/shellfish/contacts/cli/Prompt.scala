package io.chrisdavenport.shellfish.contacts.cli

import io.chrisdavenport.shellfish.contacts.domain.*

import scala.annotation.tailrec

object Prompt {
  def parsePrompt(args: List[String]): CliCommand =
    args match {
      case Nil                                   => Help
      case "add" :: Nil                          => AddContact
      case "remove" :: username :: Nil           => RemoveContact(username)
      case "search" :: "id" :: username :: Nil   => SearchId(username)
      case "search" :: "name" :: name :: Nil     => SearchName(name)
      case "search" :: "email" :: email :: Nil   => SearchEmail(email)
      case "search" :: "number" :: number :: Nil => SearchNumber(number)
      case "list" :: _                            => ViewAll
      case "update" :: username :: options =>
        UpdateContact(username, parseUpdateFlags(options))
      case "--help" :: _ => Help
      case "help" :: _   => Help
      case _             => Help
    }

  private def parseUpdateFlags(options: List[String]): List[Flag] = {

    @tailrec
    def tailParse(remaining: List[String], acc: List[Flag]): List[Flag] =
      remaining match {

        case Nil => acc

        case "--first-name" :: firstName :: tail =>
          tailParse(tail, FirstNameFlag(firstName) :: acc)

        case "--last-name" :: lastName :: tail =>
          tailParse(tail, LastNameFlag(lastName) :: acc)

        case "--phone-number" :: phoneNumber :: tail =>
          tailParse(tail, PhoneNumberFlag(phoneNumber) :: acc)

        case "--email" :: email :: tail =>
          tailParse(tail, EmailFlag(email) :: acc)

        case flag :: _ => List(UnknownFlag(flag))
      }

    tailParse(options, Nil)
  }
}
