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

package io.chrisdavenport.shellfish

import cats._
import cats.syntax.all._
import cats.effect._
import cats.effect.std.Env
import fs2._
import fs2.io.file.{Files, Path, WalkOptions}
import java.nio.file.{Files => JFiles}
import java.time._

/**
 * Shell Exposes a Set Of Utilities for building applications that mirrors what
 * you would have in a traditional shell environment.
 */
trait Shell[F[_]] {

  /**
   * Print to stdout
   */
  def echo[A](string: A)(implicit show: Show[A] = Show.fromToString): F[Unit]

  /**
   * Print to stderr
   */
  def err[A](err: A)(implicit show: Show[A] = Show.fromToString): F[Unit]

  /**
   * Read in the entire content of a text file.
   */
  def readTextFile(path: String): F[String]

  /**
   * Replace the entire content of a text file with the provided Text.
   */
  def writeTextFile(path: String, content: String): F[Unit]

  /**
   * Retrieve all environment variables
   */
  def env: F[Map[String, String]]

  /**
   * Look up an environment variable
   */
  def needEnv(variable: String): F[Option[String]]

  /**
   * Get the home directory
   */
  def home: F[String]

  /**
   * Get the path pointed to by a symlink
   */
  def readLink(path: String): F[String]

  /**
   * Canonicalize a path
   */
  def realPath(path: String): F[String]

  /**
   * Get the current directory
   */
  def pwd: F[String]

  /**
   * Change the current directory
   */
  def cd(string: String): F[Unit]

  /**
   * Check if a path exists
   */
  def exists(path: String): F[Boolean]

  /**
   * Copy a between locations
   */
  def cp(start: String, end: String): F[Unit]

  /**
   * Create a directory
   *
   * Fails if the directory is present
   */
  def mkdir(path: String): F[Unit]

  /**
   * Create a directory tree (equivalent to mkdir -p)
   *
   * Does not fail if the directory is present
   */
  def mktree(path: String): F[Unit]

  /**
   * Remove a file
   */
  def rm(path: String): F[Unit]

  /**
   * Remove a directory
   */
  def rmDir(path: String): F[Unit]

  /**
   * Create a symlink from one path to another
   */
  def symlink(createAt: String, linkTo: String): F[Unit]

  /**
   * Returns true if the given path is not a symbolic link
   */
  def isNotSymLink(path: String): F[Boolean]

  /**
   * Check if a file exists
   */
  def testFile(path: String): F[Boolean]

  /**
   * Check if a directory exists
   */
  def testDir(path: String): F[Boolean]

  /**
   * Check is a path exists
   */
  def testPath(path: String): F[Boolean]

  /** Get the current time */
  def date: F[Instant]

  /** Get the time a file was last modified */
  def dateFile(path: String): F[Instant]

  /**
   * Touch a file, updating the access and modification times to the current
   * time
   *
   * Creates an empty file if it does not exist
   */
  def touch(path: String): F[Unit]

  /** Get the system's host name */
  def hostname: F[String]

  /** Show the full path of an executable file */
  def which(path: String): F[Option[String]]

  /** Show all matching executables in PATH, not just the first */
  def whichAll(path: String): Stream[F, String]

  def ls: Stream[F, String]
  def ls(path: String): Stream[F, String]
}

object Shell {

  val io: Shell[IO] = global[IO]

  def apply[F[_]](implicit ev: Shell[F]): ev.type = ev

  def create[F[_]: Async: Env: Files]: F[Shell[F]] = for {
    wd  <- Sync[F].delay(System.getProperty("user.dir"))
    ref <- Concurrent[F].ref(wd)
  } yield new ShellImpl(ref.get, s => ref.set(s))

  def global[F[_]: Async: Env: Files]: Shell[F] = new ShellImpl[F](
    Sync[F].delay(System.getProperty("user.dir")),
    s => Sync[F].delay(System.setProperty("user.dir", s)).void
  )

  private class ShellImpl[F[_]: Async: Env: Files](
      val pwd: F[String],
      setWd: String => F[Unit]
  ) extends Shell[F] {
    val console = cats.effect.std.Console.make[F]
    val files   = Files[F]

    private def getResolved(path: String): F[Path] = for {
      current <- pwd
    } yield
      if (path.startsWith("/") || path.startsWith("~")) Path(path)
      else Path(current).resolve(path)

    // print stdout
    def echo[A](string: A)(implicit
        show: Show[A] = Show.fromToString
    ): F[Unit] = console.println(string)
    // print stderr
    def err[A](err: A)(implicit show: Show[A] = Show.fromToString): F[Unit] =
      console.error(err)

    def readTextFile(path: String): F[String] =
      getResolved(path).flatMap(p =>
        files.readAll(p).through(fs2.text.utf8.decode).compile.string
      )
    def writeTextFile(path: String, content: String): F[Unit] = {
      for {
        p      <- getResolved(path)
        exists <- files.exists(p)
        isFile <- files.isRegularFile(p)
        _ <-
          if (exists && !isFile)
            new RuntimeException(s"$p exists and is not a file").raiseError
          else {
            files.deleteIfExists(p) >>
              Stream(content)
                .through(fs2.text.utf8.encode)
                .through(files.writeAll(p))
                .compile
                .drain
          }
      } yield ()
    }

    def env: F[Map[String, String]] = Env[F].entries.map(_.toMap)
    // What Env Variable You Want
    def needEnv(variable: String): F[Option[String]] = Env[F].get(variable)

    def home: F[String] = Sync[F].delay(System.getProperty("user.home"))

    // Get the path pointed to by a symlink
    def readLink(path: String): F[String] = getResolved(path).flatMap(p =>
      Sync[F].delay {
        JFiles.readSymbolicLink(p.toNioPath).toAbsolutePath.toString
      }
    )

    def realPath(path: String): F[String] = getResolved(path).flatMap(p =>
      Sync[F].delay {
        p.toNioPath.toRealPath().toString
      }
    )

    def cd(path: String): F[Unit] = for {
      newPath <- getResolved(path)
      real    <- realPath(newPath.toString)
      out <- files
        .isDirectory(Path(real))
        .ifM(
          setWd(real.toString),
          new RuntimeException(
            s"cd: no such file or directory $path"
          ).raiseError
        )
    } yield out

    def exists(path: String): F[Boolean] =
      getResolved(path).flatMap(files.exists)

    def cp(start: String, end: String): F[Unit] = for {
      r1 <- getResolved(start)
      r2 <- getResolved(end)
      _  <- files.copy(r1, r2)
    } yield ()

    // fs2 createDirectory
    def mkdir(path: String): F[Unit] =
      getResolved(path).flatMap(files.createDirectory(_).void)
    // mkdir -p
    def mktree(path: String): F[Unit] =
      getResolved(path).flatMap(files.createDirectories(_).void)

    def rm(path: String): F[Unit] = getResolved(path).flatMap(files.delete)
    def rmDir(path: String): F[Unit] =
      getResolved(path).flatMap(files.deleteRecursively)

    def symlink(createAt: String, linkTo: String): F[Unit] =
      for {
        r1 <- getResolved(createAt)
        r2 <- getResolved(linkTo)
        _  <- files.createSymbolicLink(r1, r2)
      } yield ()

    def isNotSymLink(path: String): F[Boolean] = for {
      newPath <- getResolved(path)
      symlink <- files.isSymbolicLink(newPath)
    } yield !symlink

    def testFile(path: String): F[Boolean] =
      getResolved(path).flatMap(files.isRegularFile)
    def testDir(path: String): F[Boolean] =
      getResolved(path).flatMap(files.isDirectory)
    def testPath(path: String): F[Boolean] =
      getResolved(path).flatMap(files.exists)

    def date: F[Instant] =
      cats.effect.Clock[F].realTime.map(_.toMillis).map(Instant.ofEpochMilli)
    // lastModified
    def dateFile(path: String): F[Instant] = for {
      p <- getResolved(path)
      t <- files.getLastModifiedTime(p)
    } yield Instant.ofEpochMilli(t.toMillis)

    def touch(path: String): F[Unit] = for {
      p   <- getResolved(path)
      now <- cats.effect.Clock[F].realTime
      _ <- exists(p.toString).ifM(
        files
          .setFileTimes(
            path = p,
            lastModified = Some(now),
            lastAccess = None,
            creationTime = Some(now),
            followLinks = true
          )
          .void,
        files
          .createFile(p)
          .handleErrorWith { _ =>
            new Throwable(
              s"touch: file creation unsuccessful for $path"
            ).raiseError
          }
      )
    } yield ()

    def hostname: F[String] =
      testFile("/etc/hostname").ifM(
        readTextFile("/etc/hostname"),
        Sync[F].delay(java.net.InetAddress.getLocalHost().getHostName())
      )

    // Used internally as a regular walk but with `maxDepth = 1`
    private def singleWalk(start: Path): Stream[F, Path] =
      files.walk(
        start,
        WalkOptions.Default
          .withMaxDepth(1)
          .withFollowLinks(true)
      )

    // Show the full path of an executable file
    def which(path: String): F[Option[String]] =
      needEnv("PATH")
        .flatMap(a =>
          a.fold(
            new Throwable("No PATH env variable").raiseError[F, List[String]]
          )(s => s.split(":").toList.pure[F])
        )
        .flatMap(list =>
          Stream
            .emits(list)
            .map(Path(_))
            .flatMap(singleWalk)
            .flatMap(p =>
              Stream
                .eval(files.isExecutable(p))
                .map(isExe => (p, !(isExe && p.fileName == Path(path))))
            )
            .takeThrough(_._2)
            .map(_._1)
            .compile
            .last
            .map(
              _.flatMap(s =>
                if (s.fileName == Path(path)) s.toString.some else None
              )
            )
        )
    // Show all matching executables in PATH, not just the first
    def whichAll(path: String): Stream[F, String] =
      Stream
        .eval(
          needEnv("PATH").flatMap(a =>
            a.fold(
              new Throwable("No PATH env variable").raiseError[F, List[String]]
            )(s => s.split(":").toList.pure[F])
          )
        )
        .flatMap(list =>
          Stream
            .emits(list)
            .map(Path(_))
            .flatMap(singleWalk)
            .flatMap(p =>
              Stream
                .eval(files.isExecutable(p))
                .map(isExe => (p, isExe && p.fileName == Path(path)))
            )
            .filter(_._2)
            .map(_._1.toString)
        )

    def ls: Stream[F, String] = ls("")
    def ls(path: String): Stream[F, String] =
      Stream
        .eval(getResolved(path))
        .flatMap(
          singleWalk(_).map(_.toString).drop(1)
        ) // Exclude Myself -> excluded :)

  }

}
