package io.chrisdavenport.shellfish.contacts.domain

sealed abstract class Flag
case class UsernameFlag(username: String)       extends Flag
case class FirstNameFlag(firstName: String)     extends Flag
case class LastNameFlag(lastName: String)       extends Flag
case class PhoneNumberFlag(phoneNumber: String) extends Flag
case class EmailFlag(email: String)             extends Flag
case class UnknownFlag(flag: String)            extends Flag
