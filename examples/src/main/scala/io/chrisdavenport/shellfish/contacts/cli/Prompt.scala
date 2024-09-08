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

import io.chrisdavenport.shellfish.contacts.domain.argument.*
import io.chrisdavenport.shellfish.contacts.domain.flag.*

import scala.annotation.tailrec

object Prompt {
  def parsePrompt(args: List[String]): CliCommand = args match {
    case "add" :: Nil                          => AddContact
    case "remove" :: username :: Nil           => RemoveContact(username)
    case "search" :: "id" :: username :: Nil   => SearchId(username)
    case "search" :: "name" :: name :: Nil     => SearchName(name)
    case "search" :: "email" :: email :: Nil   => SearchEmail(email)
    case "search" :: "number" :: number :: Nil => SearchNumber(number)
    case "list" :: _                           => ViewAll
    case "update" :: username :: options =>
      UpdateContact(username, parseUpdateFlags(options))
    case Nil           => Help
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
