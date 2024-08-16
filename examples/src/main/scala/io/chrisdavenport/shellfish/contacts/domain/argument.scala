package io.chrisdavenport.shellfish.contacts.domain

import io.chrisdavenport.shellfish.contacts.domain.contact.*

sealed abstract class CliCommand
case object AddContact                       extends CliCommand
case class RemoveContact(username: Username) extends CliCommand
case class SearchId(username: Username)      extends CliCommand
case class SearchName(name: Name)            extends CliCommand
case class SearchEmail(email: Email)         extends CliCommand
case class SearchNumber(number: PhoneNumber) extends CliCommand

/**
 * TODO: How to handle options? For example, --first-name "John" --last-name
 * "Doe" can be on any order, and how to handle the case when the user doesn't
 * provide the value for an option?
 */
case class UpdateContact(
    username: Username,
    options: List[Flag]
) extends CliCommand

case object ViewAll extends CliCommand
case object Help    extends CliCommand
