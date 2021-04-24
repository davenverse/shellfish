package io.chrisdavenport.shellfish

import cats._
import cats.syntax.all._
import cats.effect._
import fs2._
import java.nio.file._

// TODO STDIN/STDOUT/STDERR Variants
trait SubProcess[F[_]]{
  def exec(command: String, arguments: List[String] = List.empty): F[ExitCode]
  def execs(command: String, arguments: List[String] = List.empty): F[Unit]

}
object SubProcess {

  def apply[F[_]](implicit ev: SubProcess[F]): ev.type = ev

  val io: SubProcess[IO] = new SubProcessImpl[IO]

  implicit def forAsync[F[_]: Async]: SubProcess[F] = new SubProcessImpl[F]

  private class SubProcessImpl[F[_]: Async] extends SubProcess[F]{

    def exec(command: String, arguments: List[String] = List.empty): F[ExitCode] = {
      import scala.sys.process._
      val argumentsS = if (arguments.nonEmpty) arguments.mkString(" ") else ""
      for {
        where <- Sync[F].delay(System.getProperty("user.dir")) // Global Mutable State is fun
        out <- Sync[F].blocking{
          Process(command ++ argumentsS, Some(Paths.get(where).toFile)).run.exitValue()
        }
      } yield ExitCode(out)
    }
    def execs(command: String, arguments: List[String] = List.empty): F[Unit] = 
      exec(command, arguments).flatMap{
        case ExitCode.Success => Applicative[F].unit
        case othewise => new RuntimeException("shs received non-zero exit code").raiseError
      }

  }
}