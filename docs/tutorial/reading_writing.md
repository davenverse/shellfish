# Reading and writing

In this section, we'll guide you through the process of opening and extracting information from files using our simple, intuitive API. Whether you're working with text, binary, or other formats, you'll find the tools you need to seamlessly integrate file reading into your Scala project. Let's dive in and unlock the power of data stored in files!

**Important:** To avoid tutorial hell, we recomment to do a really small project while you read this turorial in order to place acquired knowledge into practice. It doesn't have to be fancy, but it should tap into your curiosity and challenge you just a little.

## Reading and printing a file

Let say you want to load the contents of a file in memory...

The first thing you need to do is import the `read` method and the `Path` type:

@:select(api-style)

@:choice(syntax)

```scala
import shellfish.Path
import shellfish.syntax.path.*
```

@:choice(static)

```scala
import shellfish.Path
import shellfish.FilesOs.*
```

@:@

Next, define the `run` function to execute the `IO`. In order to do that, you need to extend your application with a `cats.effect.IOApp`, lets name it `App`:

```scala mdoc:compile-only
import cats.effect.{IO, IOApp}

object App extends IOApp.Simple:

  def run: IO[Unit] = ???

end App
```

Now we can start using the library! First, create a `Path` contanining the path to the file you want to read:

```scala mdoc:compile-only
import fs2.io.file.Path // TODO: delete this and use the Shellfish's one
val path = Path("testdata/readme.txt")
```

And simply use the `read` function to load the file in memory as a string:

@:select(api-style)

@:choice(syntax)

```scala mdoc:fail
import fs2.io.file.Path // TODO: delete this and use the Shellfish's one
import cats.effect.{IO, IOApp}

object App extends IOApp.Simple:

  val path = Path("testdata/readme.txt")

  def run: IO[Unit] = path.read

end App
```

@:choice(static)

```scala mdoc:fail
import cats.effect.{IO, IOApp}

object App extends IOApp.Simple:

  val path = Path("testdata/readme.txt")

  def run: IO[Unit] = read(path)

end App
```

@:@

Oops! We got an error saying that the `run` function accepts an `IO[Unit]`, but the `read` function returns an `IO[String]`. This happens because we are not doing anything with the string loaded and therefore not returning `IO[Unit]`. Fortunately, because `IO` is a monad, we can secuence the value obtained by evaluating the `read` computation with another that prints things to the console (and thus returns `IO[Unit]`!). This is achieved by using the `flatMap` function as follows:

@:select(api-style)

@:choice(syntax)

```scala
path.read.flatMap(file => IO(println(file)))
```

@:choice(static)

```scala
read(path).flatMap(file => IO(println(file)))
```

@:@

As you might know, whats happening above is that we are calling the `flatMap` method and passing as parameter a function describing what we want to do with the `file` inside the `IO`, in this case, passing it to the computation `IO(println(file))`.

Now pass the program to the `run` method and everything should go nicely:


@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object App extends IOApp.Simple:

  val path = Path("testdata/readme.txt")

  def run: IO[Unit] = path.read.flatMap(file => IO(println(file)))

end App
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object App extends IOApp.Simple:

  val path = Path("testdata/readme.txt")

  def run: IO[Unit] = read(path).flatMap(file => IO(println(file)))

end App
```

@:@

Congratulations! You have just loaded the contents of a file in pure Scala 🎉.

### Exercise

You may not like to keep using the `flatMap` function over and over again to sequence computations. This is why there is the [macro keyword `for`](https://docs.scala-lang.org/scala3/book/control-structures.html#for-expressions) to automatically let the compiler write the `flatMap`s for you. Why don't you try rewriting the program we just did using for-comprenhensions? 

```scala
def run: IO[Unit] =
  for
    // Complete your code here!
    ...
  yield ()
```
[See solution](https://gist.github.com/f8cf952fd5f7341ef698dfe825cfca93.git)

## Writing and modifing the contents of a file

Now that you know how to load a file, you might also want to modify it and save it.

To write to a file, use the `write' function with one of its variants, so that you can save it to a completely different file. Here, we reverse the file so it reads backwards (if that's something you want to do):

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object App extends IOApp.Simple:

  val path = Path("testdata/change_me.txt")

  def run: IO[Unit] =
    for
      file <- path.read
      reversedFile = file.reverse
      _ <- path.write(reversedFile)
    yield ()

end App
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object App extends IOApp.Simple:

  val path = Path("testdata/change_me.txt")

  def run: IO[Unit] =
    for
      file <- read(path)
      reversedFile = file.reverse
      _ <- write(path, reversedFile)
    yield ()

end App
```

@:@

Be aware that this will overwrite the contents of the file. So be careful not to change important files while you are learning!

### Exercise

Try loading the contents of two different files, concatenating them, and saving the result to a third location. How would you do it?

[See possible solution](https://gist.github.com/99880af9b051b15f8adace7fe7aee2e3.git)

## Working line by line

There is a useful variant of the standalone methods called `Lines` (e.g. `readLines`), which reads the file line by line and stores them on a `List[String]`. This comes handy when you are working with a list of things that you want to convert:

`testdata/names.data`

```
Alex
Jamie
Morgan
Riley
Taylor
Casey
River
```

@:select(api-style)

@:choice(syntax)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.syntax.path.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Names extends IOApp.Simple:

  case class Name(value: String)

  val namesPath = Path("testdata/names.data")

  def run: IO[Unit] =
    for
      lines <- namesPath.readLines      // (1)
      names = lines.map(Name(_))        // (2)
      _ <- IO(names.foreach(println))   // (3)
    yield ()

end Names
```

@:choice(static)

```scala mdoc:compile-only
import io.chrisdavenport.shellfish.FilesOs.*
import fs2.io.file.Path
import cats.effect.{IO, IOApp}

object Names extends IOApp.Simple:

  case class Name(value: String)

  val namesPath = Path("testdata/names.data")

  def run: IO[Unit] =
    for
      lines <- readLines(namesPath)     // (1)
      names = lines.map(Name(_))        // (2)
      _ <- IO(names.foreach(println))   // (3)
    yield ()

end Names
```

@:@

In the example from above, we first load the list of names as a `List` of `String` (1), then we map over this list and convert the strings to a `List` of `Name` (2), and finally we print each of one the elements recently mapped to the console (3). Note that using `println` with `foreach` creates side effects, thats why we wrap the computation on an `IO`.

### Exercise

Imagine we want to add some spacing between lines on a text and save the result on a different file:

_`testdata/edgar_allan_poe/no_spaced_dream.txt`_

```
Take this kiss upon the brow!
And, in parting from you now,
Thus much let me avow —
You are not wrong, who deem
That my days have been a dream;
Yet if hope has flown away
In a night, or in a day,
In a vision, or in none,
Is it therefore the less gone?
All that we see or seem
Is but a dream within a dream.
```

Convert it to:

_`testdata/edgar_allan_poe/spaced_dream.txt`_

```
Take this kiss upon the brow!

And, in parting from you now,

Thus much let me avow —

You are not wrong, who deem

That my days have been a dream;

Yet if hope has flown away

In a night, or in a day,

In a vision, or in none,

Is it therefore the less gone?

All that we see or seem

Is but a dream within a dream.
```

How would you do this? (hint: use `writeLines`).

[See possible solution](https://gist.github.com/eaf4e550a90c355b9698f51c8311eeff.git)
