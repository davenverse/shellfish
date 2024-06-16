package io.chrisdavenport.shellfish

import cats.effect.{IO, IOApp}

import scodec.codecs.*
import scodec.Codec

import syntax.path.*
import fs2.io.file.Path

object Place extends IOApp.Simple {

  case class Place(number: Int, name: String)

  implicit val placeCodec: Codec[Place] = (int32 :: utf8).as[Place]

  val path = Path("src/main/resources/place.data")

  def run: IO[Unit] =
    for {
      exists <- path.exists()
      _      <- if (exists) IO.unit else path.createFile
      _      <- path.writeAs[Place](Place(1, "Michael Phelps"))
      _      <- path.readAs[Place].flatMap(p => IO.println(p))
    } yield ()

}
