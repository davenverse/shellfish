package io.chrisdavenport.shellfish.contacts.domain

import io.chrisdavenport.shellfish.contacts.domain.*

sealed abstract class Argument
case object AddContact                       extends Argument
case class RemoveContact(username: Username) extends Argument
case class SearchId(username: Username)      extends Argument
case class SearchName(name: Name)            extends Argument
case class SearchEmail(email: Email)         extends Argument
case class SearchNumber(number: PhoneNumber) extends Argument

/**
 * TODO: How to handle options? For example, --first-name "John" --last-name
 * "Doe" can be on any order, and how to handle the case when the user doesn't
 * provide the value for an option?
 */
case class UpdateContact(
    username: Username,
    options: List[Flag]
) extends Argument

case object ViewAll extends Argument
case object Help    extends Argument
case object Info    extends Argument
