package io.chrisdavenport.shellfish

import cats._
import cats.syntax.all._
import cats.effect._
import fs2._
import java.nio.file._
import cats.effect.std.Supervisor


/**
 * A Subprocess Approach To Running Shell Commands
 * Note: cp in this shell will do nothing, as the working directory is
 * determined by the Shell.
 **/
trait SubProcess[F[_]]{
  def shell(
    command: String,
    arguments: List[String] = List.empty,
    stdIn: Option[Stream[F, String]] = None // Each string should be a single line
  ): F[ExitCode]

  def shellS(
    command: String,
    arguments: List[String] = List.empty,
    stdIn: Option[Stream[F, String]] = None
  ): F[Unit]

  def inShell(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): Stream[F, String]

  def inShellWithError(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): Stream[F, Either[String, String]]

  def shellStrict(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): F[(ExitCode, String)]

  def shellStrictWithErr(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): F[(ExitCode, String, String)]

  // Lower Level Control
  def run(command: String, arguments: List[String] = List.empty): Resource[F, SubProcess.RunningProcess[F]]
}
object SubProcess {

  def apply[F[_]](implicit ev: SubProcess[F]): ev.type = ev

  val io: SubProcess[IO] = new SubProcessImpl[IO](Shell.io)

  def global[F[_]: Async]: SubProcess[F] = 
    new SubProcessImpl[F](Shell.global[F])

  // The relevant shell is important as java does not have
  // any idea where we are, so we use the shell to figure
  // out what working directory we should execute processes in.
  def fromShell[F[_]: Async](shell: Shell[F]): SubProcess[F] = 
    new SubProcessImpl(shell)

  private class SubProcessImpl[F[_]: Async](shell: Shell[F]) extends SubProcess[F]{
    val pr = ProcessRunner[F]

    def shell(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): F[ExitCode] = {
      for {
        where <- shell.pwd
        out <- pr.run(where, command :: arguments).use(p => 
          stdIn.fold(Applicative[F].unit)(s => 
            p.setInput(
              s.intersperse("\n").through(fs2.text.utf8Encode)
            )
          ) >>
          p.exitCode
        )
      } yield out
    }

    def shellS(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): F[Unit] = 
      shell(command, arguments, stdIn).flatMap{
        case ExitCode.Success => Applicative[F].unit
        case othewise => new RuntimeException("shs received non-zero exit code").raiseError
      }
    
    def inShell(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): Stream[F, String] = 
      for {
        where <- Stream.eval(shell.pwd)
        p <- Stream.resource(pr.run(where, command :: arguments))
        _ <- Stream.eval(
          stdIn.fold(Applicative[F].unit)(s => 
            p.setInput(
              s.intersperse("\n").through(fs2.text.utf8Encode)
            )
          )
        )
        line <- p.output.through(fs2.text.utf8Decode[F]).through(fs2.text.lines)
      } yield line

    def inShellWithError(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): Stream[F, Either[String, String]] = 
      for {
        where <- Stream.eval(shell.pwd)
        p <- Stream.resource(pr.run(where, command :: arguments))
        _ <- Stream.eval(
          stdIn.fold(Applicative[F].unit)(s => 
            p.setInput(
              s.intersperse("\n").through(fs2.text.utf8Encode)
            )
          )
        )
        line <- p.output.through(fs2.text.utf8Decode[F]).through(fs2.text.lines).either(
          p.errorOutput.through(fs2.text.utf8Decode[F]).through(fs2.text.lines)
        )
      } yield line

    def shellStrict(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): F[(ExitCode, String)] = 
      for {
        where <- shell.pwd
        out <- pr.run(where, command :: arguments).use(p => 
          stdIn.fold(Applicative[F].unit)(s => 
            p.setInput(
              s.intersperse("\n").through(fs2.text.utf8Encode)
            )
          ) >>
          (
            p.exitCode,
            p.output.through(fs2.text.utf8Decode[F]).compile.string,
          ).tupled
        )
      } yield out

    def shellStrictWithErr(command: String, arguments: List[String] = List.empty, stdIn: Option[Stream[F, String]] = None): F[(ExitCode, String, String)] = 
      for {
        where <- shell.pwd
        out <- pr.run(where, command :: arguments).use(p => 
          stdIn.fold(Applicative[F].unit)(s => 
            p.setInput(
              s.intersperse("\n").through(fs2.text.utf8Encode)
            )
          ) >>
          (
            p.exitCode,
            p.output.through(fs2.text.utf8Decode[F]).compile.string,
            p.errorOutput.through(fs2.text.utf8Decode[F]).compile.string
          ).tupled
        )
      } yield out

    def run(command: String, arguments: List[String] = List.empty): Resource[F, RunningProcess[F]] = 
      Resource.eval(shell.pwd).flatMap{where => 
        pr.run(where, command :: arguments)
      }
  }

  trait RunningProcess[F[_]] {
    def setInput(input: fs2.Stream[F, Byte]): F[Unit]
    def output: fs2.Stream[F, Byte]
    def outputUtf8: fs2.Stream[F, String]
    def errorOutput: fs2.Stream[F, Byte]
    def errorOutputUtf8: fs2.Stream[F, String]
    def exitCode: F[ExitCode]
  }

  // Shoutout to Jakub Kozłowski for his awesome code here.
  trait ProcessRunner[F[_]] {
    // Runs a program and returns a handle to it.
    // The handle allows you to start writing to the standard input of the process using setInput
    // and see its output, as well as the standard error, in the other methods of the handle.
    // the effect with the exit code returns when the process completes.
    // Closing the resource will automatically interrupt the input stream, if it was specified.
    // Behavior on setting multiple inputs is undefined. Probably results in interleaving, idk.
    def run(wd: String, program: List[String]): Resource[F, RunningProcess[F]]
  }

  object ProcessRunner {
    def apply[F[_]](implicit F: ProcessRunner[F]): ProcessRunner[F] = F

    implicit def instance[F[_]: Async]: ProcessRunner[F] = new ProcessRunner[F] {
      import scala.jdk.CollectionConverters._

      val readBufferSize = 4096

      def run(wd: String, program: List[String]): Resource[F, RunningProcess[F]] =
        Resource
          .make(Sync[F].blocking(new java.lang.ProcessBuilder(program.asJava).directory(new java.io.File(wd)).start()))(p => Sync[F].blocking(p.destroy()))
          .flatMap { process =>
            // manages the consumption of the input stream
            Supervisor[F].map { supervisor =>
              val done = Async[F].fromCompletableFuture(Sync[F].delay(process.onExit()))

              new RunningProcess[F] {
                def setInput(input: fs2.Stream[F, Byte]): F[Unit] =
                  supervisor
                    .supervise(
                      input
                        .through(fs2.io.writeOutputStream[F](Sync[F].blocking(process.getOutputStream())))
                        .compile
                        .drain
                    )
                    .void

                val output: fs2.Stream[F, Byte] = fs2
                  .io
                  .readInputStream[F](Sync[F].blocking(process.getInputStream()), chunkSize = readBufferSize)

                val outputUtf8 = output.through(fs2.text.utf8Decode)

                val errorOutput: fs2.Stream[F, Byte] = fs2
                  .io
                  .readInputStream[F](Sync[F].blocking(process.getErrorStream()), chunkSize = readBufferSize)
                  // Avoids broken pipe - we cut off when the program ends.
                  // Users can decide what to do with the error logs using the exitCode value
                  .interruptWhen(done.void.attempt)

                val errorOutputUtf8 = errorOutput.through(fs2.text.utf8Decode)

                val exitCode: F[ExitCode] = done.flatMap(p => Sync[F].blocking(p.exitValue())).map(ExitCode(_))
              }
            }
          }
    }
  }
}