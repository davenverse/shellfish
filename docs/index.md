# shellfish — Shell Scripting for Cats-Effect

## Getting started

To use shellfish in an existing SBT project with Scala 2.11 or a later version, add the following dependencies to your
`build.sbt` depending on your needs:

```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "shellfish" % "@VERSION@"
)
```

## Example
Shellfish is a library to perform common script operations such as working with processes and files while maintaining referential transparency! 

```scala 3 mdoc:reset
import cats.effect.{IO, IOApp, ExitCode}

import shellfish.*
import shellfish.syntax.path.*

object Main extends IOApp: 

  def run(args: List[String]): IO[ExitCode] = 
    for
      home   <- userHome
      config = home / ".shellfish" / "config.conf"
      _         <- config.createFile
      _         <- config.write("scripting.made.easy = true")
      newconfig <- config.read
      _         <- IO.println(s"Loading config: $newconfig")
    yield ExitCode.Success
    
end Main
```