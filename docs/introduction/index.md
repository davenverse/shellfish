# Getting Started

## Which API should I use?

In shellfish, you have two different ways of doing things:

- First, if you prefer a more concise, fluent syntax, use the extension methods (e.g. `path.read()`) by importing the `shellfish.syntax.all.*` package.

- If you prefer calling static methods, use direct method calls on the `FilesOs` and `ProcessesOs` objects (e.g., `FilesOs.read(path)`).

@:select(api-style)

@:choice(syntax)

```scala 3
import shellfish.syntax.all.*

val path = Path("data/test.txt")

for
  file <- path.read
  _    <- path.append("I'll place this here.")
yield ()
```

@:choice(static)

```scala 3
import shellfish.FilesOs

val path = Path("data/test.txt")

for
  file <- FilesOs.read(path)
  _    <- FilesOs.append(path, "I'll place this here.")
yield ()
```

@:@

Which one you should use really depends on your preferences and choices; if you like the method style of calling functions directly to the object, stick to the extension methods syntax, if you rather prefer a more Haskell like style, stick to the static object calls!

## Imports

If you just want to start scripting right away, importing `shellfish.*` will do the trick; it imports all the extension methods and functionality you need, such as types and functions, to start working right away.

But if you want more consice functionality, the `shellfish.syntax.path.*` and `shellfish.syntax.process.*` will only import the extension methods.

For the static methods, the `shellfish.FilesOs` and `shellfish.ProcessesOs` imports will provide the functions to work with files and processes, if that is your preferred style.

Finally, `shellfish.domain.*` will import all the types needed to work with the library!

## What is this `IO` thing?

You may be wondering at this point, what is this `IO` thing that appears at the end of the functions? Well, thats the [IO monad](https://typelevel.org/cats-effect/docs/2.x/datatypes/io) of Cats Effect; the concept is much more extensive than we can explain on this page, but basically it is a type that allows us to suspend side effects so that they do not run instantly:

```scala mdoc
import cats.effect.IO

/* Will print to de console! */
val printingHello = println("Hello newbies!")

/* Will not do anything (yet) */
val suspendingHello = IO(println("Hello newbies!"))
```

In order to actually run the computation you have two options, the first one (and not recommended) is to call the `unsafeRunSync()` function at the very end of the program:

```scala mdoc
import cats.effect.unsafe.implicits.global // Imports the runtime that executes the IO monad

suspendingHello.unsafeRunSync()
```

But this is not the usual way, the common way is to pass it to the `run` function (similar to the main method in scala, but for `IO`!). To to that, you have to extend the object with `IOApp`:

```scala mdoc:silent
import cats.effect.IOApp

object Main extends IOApp.Simple:

  def run: IO[Unit] = suspendingHello

end Main
```

On either way, the `IO` will be executed and all the computacion will be evaluated.

But why's that useful? Well, one of the advantages is referential transparency, and that basically means that we can replace the code wherever it is referenced and expect the same results every time:

```scala mdoc
val num = 2

(num + num) == (2 + 2)
```

It may seem trivial, but that's not allways the case:

```scala mdoc
import scala.util.Random

val rndNum = Random.nextInt(10)

val result1 = rndNum + rndNum
```

If referential transparency exists in your program, replacing `Random.nextInt(10)` in `rndNum` whould yield the same result, which is not the case:

```scala mdoc
val result2 = Random.nextInt(10) + Random.nextInt(10)

result1 == result2
```

With `IO`, we can solve this by delaying the number generation:

```scala mdoc:compile-only
import cats.syntax.all.*

val rndNumIO = IO(Random.nextInt(10))

val result1IO = (rndNumIO, rndNumIO).mapN(_ + _)

val result2IO = (IO(Random.nextInt(10)), IO(Random.nextInt(10))).mapN(_ + _)
```

For more examples of why this is important, check out [this blog post](https://blog.rockthejvm.com/referential-transparency/) about referential transparency!


Another advantage is control over the code. Because "wrapping" computations inside the `IO` monad converts programs into descriptions of programs instead of statements, you automatically gain control over your program as to when and when not to execute those statements.

For those and many more reasons, we opted to make this library pure by implementing `IO` on the library!