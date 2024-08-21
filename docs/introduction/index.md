# First steps

## Which API should I use?

In shellfish, you have two different ways of doing things:

- First, if you prefer a more concise syntax, use the extension methods (e.g. `path.read()`) by importing the `shellfish.syntax.path.*` package. 

- If you prefer calling static methods, use direct method calls on the `FilesOs` object (e.g., `FilesOs.read(path)`). You can also import all the functions inside the `shellfish.FilesOs` package if you don't want to call the `FilesOs` object every time (e.g., `read(path)`). 

In this documentation we'll call the methods on the `FilesOs` objects in the static variant to differentiate them from the extension ones.  

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

Which one you should use really depends on your preferences and choices; if you like the method style of calling functions directly on the objects, stick to the extension methods syntax, if you rather prefer a more Haskell-like style, stick to the static object calls!  

## Imports

If you just want to start scripting right away, importing `shellfish.*` will do the trick; it imports all the extension methods and functionality you need, such as types and functions, to start working right away.

But if you want more concise functionality, the `shellfish.syntax.path.*` will only import the extension methods.

For the static methods, the `shellfish.FilesOs` will provide the functions to work with files, if that is your preferred style.

Finally, `shellfish.domain.*` will import all the types needed to work with the library!



## Talking about computations

Throughout this library you will often see the word calculation, but what is it? A computation is a well-defined, step-by-step work that calculates a value when evaluated (and can also perform side effects along the way). For example, this is a computation: 

```scala
object ProgramOne:
  val computationOne = 
    val a = 2
    val b = 3
    println(s"a: $a, b: $b")
    a + 2 * b
```

In this case, the program `ProgramOne` has a computation that calculates 2 + 2 * 3 and logs some values to the console. When this computation is evaluated (for example, by a main function), it will compute the value 8.

This may seem trivial, but someone has to put the nail before hammering.

## What is this `IO` thing?

You may be wondering at this point, what is this `IO` thing that appears at the end of the functions? Well, that's the [IO monad](https://typelevel.org/cats-effect/docs/2.x/datatypes/io) of Cats Effect; the concept is much more extensive than we can explain on this page, but basically it is a type that allows us to suspend side effects so that they do not run instantly:

```scala mdoc
import cats.effect.IO

/* Will print to de console! */
val printingHello = println("Hello newbies!")

/* Will not do anything (yet) */
val suspendingHello = IO(println("Hello newbies!"))
```

To actually run the computation, you have two options, the first one (and not recommended) is to call the `unsafeRunSync()` function at the very end of the program:

```scala mdoc
import cats.effect.unsafe.implicits.global // Imports the runtime that executes the IO monad

suspendingHello.unsafeRunSync()
```

But this is not the usual way: the usual way is to passing it to the `run` function (similar to the main method but for `IO`). To that, you have to extend your application's main object with `IOApp`:  

```scala mdoc:silent
import cats.effect.IOApp

object Main extends IOApp.Simple:

  def run: IO[Unit] = suspendingHello

end Main
```

Either way, the `IO` will be executed and all the computation will be evaluated.  

But why's that useful? Well, one of the advantages is referential transparency, and that basically means that we can replace the code wherever it is referenced and expect the same results every time:

```scala mdoc
val num = 2

(num + num) == (2 + 2)
```

It may seem trivial, but that's not always the case:

```scala mdoc
val salute = 
  println("Hellow, ")
  "Hellow, "

val meow = 
  println("meow.")
  "meow"

def result1: String = salute + meow
```

If referential transparency exists in your program, replacing `println("Hellow, "); "Hellow, "` in `salute` should fire the print to the console two times, same with `meow`, which is not the case:

```scala mdoc
def result2: String = { println("Hellow, "); "Hellow, " } + { println("meow"); "meow" }

result1
result2
```

As you can see, only the `result1` printed twice, even though we replaced the exact same definitions with the implementations. With `IO`, we can solve this by delaying the print to the stout:

```scala mdoc
val saluteIO = IO:
  println("Hellow, ")
  "Hellow, " 

val meowIO = IO:
  println("meow.")
  "meow."

def result1IO: IO[String] = 
  for
    hello <- saluteIO
    meow  <- meowIO
  yield hello + meow

def result2IO: IO[String] =
  for
    hello <- IO { println("Hellow, "); "Hellow, " }
    meow  <- IO { println("meow"); "meow  " }
  yield hello + meow

result1IO.unsafeRunSync()
result2IO.unsafeRunSync()

```
Now both results are the same, an behave exactly the same!

Here's a [good explanation](https://blog.rockthejvm.com/referential-transparency/) about the benefits of referential transparency.  


Another benefit is gaining explicit control over code execution. By encapsulating computations within the `IO` monad, your programs become blueprints rather than direct statements. This gives you the ability to decide precisely when to execute those statements.


## Weird `>>` and `>>=` operators, what are those?

While reading the documentation of this library, you may come across some strange operator like `>>`. This is convenient syntax sugar for some fairly common methods!

You can import them using the syntax package in cats, like this:

```scala mdoc
import cats.syntax.all.*
```

For instance, you may seen something like this:

```scala mdoc:compile-only
IO("The result is: 42") >>= IO.println
```
That is just an alias for `flatMap`, so it's like writing `IO("The result is: 42").flatMap(IO.println(_))`, but without the added boilerplate. This use is more common in languages like Haskell, but we'll use it in the documentation to simplify things a bit!


The `>>` is even simpler:
```scala mdoc:compile-only
IO.println("Loggin here... ") >> IO("Returning this string!")
```
This is used for concatenating monads that you do not care about the result of the computation, just like doing `IO.println("Loggin here...").flatMap( _ => IO("Returning this string!"))`. When to use it? In the example from above, `IO.println` computes a Unit (), so you can't do much with it anyway, so the `>>` operator comes in handy!
