# shellfish — Shell Scripting for Cats-Effect

## Getting Started

To use shellfish in an existing SBT project with Scala 2.11 or a later version, add the following dependencies to your
`build.sbt` depending on your needs:

```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "shellfish" % "<version>"
)
```

## Example
Shellfish is a library to perform common script operations such as working with processes and files while maintaining referential transparency! 

```scala 3 mdoc:reset
import cats.effect.{IO, IOApp}

import shellfish.os.* 

object Main extends IOApp: 

  def run(args: List[String]): IO[ExitCode] = 
    import Shell.io.*
    for
      _   <- cd("..")
      got <- ls.compile.toList
      _   <- echo(got.toString)
    yield ExitCode.Success
    
end Main
```