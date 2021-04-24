package io.chrisdavenport.shellfish

import cats.syntax.all._
import cats.effect._
import fs2._
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes
import java.time._

trait Shell[F[_]]{
  // print stdout
  def echo(string: String): F[Unit]
  // print stderr
  def err(err: String): F[Unit]

  def readTextFile(path: String): F[String]
  def writeTextFile(path: String, content: String): F[Unit]

  def env: F[Map[String, String]]
  // What Env Variable You Want
  def needEnv(variable: String): F[Option[String]]

  def home: F[String]

  // Get the path pointed to by a symlink
  def readLink(path: String): F[String]
  def realPath(path: String): F[String]

  def pwd: F[String]
  def cd(string: String): F[Unit]
  def exists(path: String): F[Boolean]
  // System.setProperty("user.dir", "/foo/bar");

  def cp(start: String, end: String): F[Unit]

  // fs2 createDirectory
  def mkdir(path: String): F[Unit]
  // mkdir -p
  def mktree(path: String): F[Unit]

  def rm(path: String): F[Unit]
  def rmDir(path: String): F[Unit]

  def symlink(createAt: String, linkTo: String): F[Unit]
  def isNotSymLink(path: String): F[Boolean]

  def testFile(path: String): F[Boolean]
  def testDir(path: String): F[Boolean]
  def testPath(path: String): F[Boolean]


  def date: F[Instant]
  // lastModified
  def dateFile(path: String): F[Instant]

  def touch(path: String): F[Unit]

  // def hostname: F[String]

  // // Show the full path of an executable file
  // def which(path: String): F[Option[String]]
  // // Show all matching executables in PATH, not just the first
  // def whichAll(path: String): Stream[F, String]
}

object Shell {

  def apply[F[_]](implicit ev: Shell[F]): ev.type = ev

  def lastModified[F[_]: Sync](path: Path): F[Instant] = Sync[F].delay{
    Files.getLastModifiedTime(path).toInstant
  }

  implicit def forAsync[F[_]: Async]: Shell[F] = new ShellImpl[F]()

  private class ShellImpl[F[_]: Async]() extends Shell[F]{
    val console = cats.effect.std.Console.make[F]
    val files = fs2.io.file.Files[F]

    private def getResolved(path: String) : F[Path] = for {
      current <- pwd
    } yield if (path.startsWith("/")) Paths.get(path) else Paths.get(current).resolve(path)

    // print stdout
    def echo(string: String): F[Unit] = console.println(string)
    // print stderr
    def err(err: String): F[Unit] = console.error(err)

    def readTextFile(path: String): F[String] = 
      getResolved(path).flatMap(p => 
        files.readAll(p, 512).through(fs2.text.utf8Decode).compile.string
      )
    def writeTextFile(path: String, content: String): F[Unit] = {
      for {
        p <- getResolved(path)
        exists <- files.exists(p, List())
        isFile <- files.isFile(p)
        _ <- if (exists && !isFile) new RuntimeException(s"$p exists and is not a file").raiseError
          else {
            files.deleteIfExists(p) >> 
            Stream(content).through(fs2.text.utf8Encode).through(files.writeAll(p)).compile.drain
          }
      } yield ()
    }

    def env: F[Map[String, String]] = Sync[F].delay(scala.sys.env)
    // What Env Variable You Want
    def needEnv(variable: String): F[Option[String]] = env.map(_.get(variable))

    def home: F[String] = Sync[F].delay(System.getProperty("user.home"))

    // Get the path pointed to by a symlink
    def readLink(path: String): F[String] = getResolved(path).flatMap(p => 
      Sync[F].delay{
        Files.readSymbolicLink(p).toAbsolutePath.toString
      }
    )
    def realPath(path: String): F[String] = getResolved(path).flatMap(p => 
      Sync[F].delay{
        p.toRealPath().toString
      }
    )

    def pwd: F[String] = Sync[F].delay(System.getProperty("user.dir"))

    def cd(path: String): F[Unit] = for {
      newPath <- getResolved(path)
      out <- files.isDirectory(newPath).ifM(
        Sync[F].delay(System.setProperty("user.dir", newPath.toString)).void,
        new RuntimeException(s"cd: no such file or directory $path").raiseError
      )
    } yield out

    def exists(path: String): F[Boolean] = 
      getResolved(path).flatMap(files.exists(_))

    def cp(start: String, end: String): F[Unit] = for {
      r1 <- getResolved(start)
      r2 <- getResolved(end)
      _ <- files.copy(r1, r2)
    } yield ()

    // fs2 createDirectory
    def mkdir(path: String): F[Unit] = getResolved(path).flatMap(files.createDirectory(_).void)
    // mkdir -p
    def mktree(path: String): F[Unit] = getResolved(path).flatMap(files.createDirectories(_).void)

    def rm(path: String): F[Unit] = getResolved(path).flatMap(files.delete(_))
    def rmDir(path: String): F[Unit] = 
      getResolved(path).flatMap(files.deleteDirectoryRecursively(_))

    def symlink(createAt: String, linkTo: String): F[Unit] = 
      for {
        r1 <- getResolved(createAt)
        r2 <- getResolved(linkTo)
        _ <- Sync[F].delay(Files.createSymbolicLink(r1,r1))
      } yield ()
      
    def isNotSymLink(path: String): F[Boolean] = getResolved(path).flatMap(p => 
      Sync[F].delay{
        !Files.isSymbolicLink(p)
      }
    )

    def testFile(path: String): F[Boolean] = getResolved(path).flatMap(files.isFile(_))
    def testDir(path: String): F[Boolean] = getResolved(path).flatMap(files.isDirectory(_))
    def testPath(path: String): F[Boolean] = getResolved(path).flatMap(files.exists(_))


    def date: F[Instant] = cats.effect.Clock[F].realTime.map(_.toMillis).map(Instant.ofEpochMilli)
    // lastModified
    def dateFile(path: String): F[Instant] = for {
      p <- getResolved(path)
      i  <- Sync[F].delay(Files.getLastModifiedTime(p).toInstant)
    } yield i

    def touch(path: String): F[Unit] = for {
      p <- getResolved(path)
      now <- date
      _ <- exists(p.toString).ifM(
        Sync[F].delay(Files.setLastModifiedTime(p, java.nio.file.attribute.FileTime.from(now))).void,
        Sync[F].blocking(new java.io.File(p.toUri).createNewFile).ifM(
          Sync[F].unit,
          new Throwable(s"touch: file creation unsucessful for $path").raiseError
        )
      )
    } yield ()

    // def hostname: F[String] = ???

    // // Show the full path of an executable file
    // def which(path: String): F[Option[String]] = ???
    // // Show all matching executables in PATH, not just the first
    // def whichAll(path: String): Stream[F, String] = ???

  }

}
