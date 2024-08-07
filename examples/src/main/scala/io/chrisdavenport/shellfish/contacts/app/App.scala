package io.chrisdavenport.shellfish.contacts.app

import cats.syntax.all.*
import cats.effect.{IO, IOApp}

import io.chrisdavenport.shellfish.contacts.cli.Cli.*

object App extends IOApp.Simple {

  def run: IO[Unit] = IO.println("Hello, World!")
  
}
