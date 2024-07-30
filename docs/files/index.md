# Reading, writing and appending 

This library comes with three different functions for reading and writing operations, `read`, `write` and `append`, each with four different variants: Standalone, `Bytes`, `WithCharset`, `Lines` and `As`, with these variants you will be able to work with the file and/or its contents as a string, as bytes, as a string with a custom `java.nio.charset.Charset` and with a custom codec respectively. 

## Reading

One of the most common operations in scripting is reading a file, thats why you can read a file in different ways: 

### `read`

This function loads the whole file as a string in memory using UTF-8 encoding.

@:select(api-style)

@:choice(syntax)

```scala 3
import cats.syntax.all.* // for the >>= operator, its just a rename for flatMap

import shellfish.syntax.path.*

val path = Path("link/to/your/file.txt")

path.read >>= IO.println
```

@:choice(static)

```scala 3
import cats.syntax.all.* // for the >>= operator, its just a rename for flatMap

import shellfish.FilesOs.*

val path = Path("link/to/your/file.txt")

read(path) >>= IO.println
```

@:choice(fs2)

```scala 3
import fs2.io.files.Files

val path = Path("link/to/your/file.txt")

Files[F].readUtf8(path).evalMap(IO.println).compile.drain
```

@:@


### `readWithCharset`

If UTF-8 is not your favorite flavour, you can also use a custom `java.nio.charset.Charset` to decode your file:

@:select(api-style)

@:choice(syntax)

```scala 3
import java.nio.charset.StandardCharsets

path.readWithCharset(StandardCharsets.UTF_16)
```

@:choice(static)

```scala 3
import java.nio.charset.StandardCharsets

readWithCharset(path, StandardCharsets.UTF_16)
```

@:choice(fs2)

```scala 3
import fs2.text.decodeWithCharset
import java.nio.charset.StandardCharsets

Files[F].readAll(path)
  .through(decodeWithCharset(StandardCharsets.UTF_16))
  .compile
  .string
```

@:@

### `readBytes`

Reads the file as a `ByteVector`, useful when working with binary data:

@:select(api-style)

@:choice(syntax)

```scala 3
path.readBytes.map(_.rotateLeft(2))
```

@:choice(static)

```scala 3
readBytes(path).map(_.rotateLeft(2))
```

@:choice(fs2)

```scala 3
import scodec.bits.ByteVector

Files[F].readAll(path).compile.as(ByteVector).map(_.rotateLeft(2))
```

@:@


### `readLines`

Similar to `read` as it reads the file as a UTF-8 string, but stores each line of the file on a `List`:

@:select(api-style)

@:choice(syntax)

```scala 3
for
  lines <- path.readLines
  _     <- IO(lines.foreach(println))
yield ()
```

@:choice(static)

```scala 3
for
  lines <- readLines(path)
  _     <- IO(lines.foreach(println))
yield ()
```

@:choice(fs2)

```scala 3
Files[F].readUtf8Lines(path).evalMap(IO.println).compile.drain
```

@:@


### `readAs`

This method reads the contents of the file given a `Codec[A]` in scope, useful when you want to convert a file into a custom type `A`:

@:select(api-style)

@:choice(syntax)

```scala 3
import scodec.Codec

case class Coordinates(x: Double, y: Double) derives Codec

path.readAs[Coordinates].map(coord => coord.x + coord.y)
```

@:choice(static)

```scala 3
import scodec.Codec

case class Coordinates(x: Double, y: Double) derives Codec

readAs[Coordinates](path).map(coord => coord.x + coord.y)
```

@:choice(fs2)

```scala 3
import scodec.Codec
import fs2.interop.scodec.StreamDecoder

case class Coordinates(x: Double, y: Double) derives Codec

Files[IO].readAll(path)
  .through(StreamDecoder.many(summon[Codec[Coordinates]]).toPipeByte)
  .map(coord => coord.x + coord.y)
  .compile
  .drain
```

@:@


## Writing

You can also overwrite the contents of a file using the `write` method and any of its variants. Just to note, if the file does not exist, the file will be created automatically:

### `write`

Writes to the desired file in the path. The contents of the file will be written as a UTF-8 string:

@:select(api-style)

@:choice(syntax)

```scala 3
val path = Path("path/to/write.txt")

val poem = 
  """Music, thou queen of heaven, care-charming spell,
    |That strik'st a stillness into hell;
    |Thou that tam'st tigers, and fierce storms, that rise,
    |With thy soul-melting lullabies;
    |Fall down, down, down, from those thy chiming spheres
    |To charm our souls, as thou enchant'st our ears.
    |
    |— Robert Herrick""".toStripMargin

path.write(poem)
```

@:choice(static)

```scala 3
val path = Path("path/to/write.txt")

val poem = 
  """Music, thou queen of heaven, care-charming spell,
    |That strik'st a stillness into hell;
    |Thou that tam'st tigers, and fierce storms, that rise,
    |With thy soul-melting lullabies;
    |Fall down, down, down, from those thy chiming spheres
    |To charm our souls, as thou enchant'st our ears.
    |
    |— Robert Herrick""".toStripMargin

write(path, poem)
```

@:choice(fs2)

```scala 3
val path = Path("path/to/write.txt")

val poem = 
  """Music, thou queen of heaven, care-charming spell,
    |That strik'st a stillness into hell;
    |Thou that tam'st tigers, and fierce storms, that rise,
    |With thy soul-melting lullabies;
    |Fall down, down, down, from those thy chiming spheres
    |To charm our souls, as thou enchant'st our ears.
    |
    |— Robert Herrick""".toStripMargin

Stream.emit(poem)
  .through(Files[F].writeUtf8(path))
  .compile
  .drain
```

@:@

### `writeWithCharset`

The default charset can also be changed for encoding when writing strings:

@:select(api-style)

@:choice(syntax)

```scala 3
import java.nio.charset.StandardCharsets

val message = "Imagine writing Java 😭"

path.writeWithCharset(message, StandardCharsets.US_ASCII)
```

@:choice(static)

```scala 3
import java.nio.charset.StandardCharsets

val message = "Imagine writing Java 😭"

writeWithCharset(path, message, StandardCharsets.US_ASCII)
```

@:choice(fs2)

```scala 3
import fs2.text.encodeWithCharset
import java.nio.charset.StandardCharsets

val message = "Imagine writing Java 😭"

Stream.emit(message)
  .through(encodeWithCharset(StandardCharsets.US_ASCII))
  .through(Files[F].writeAll(path))
  .compile
  .drain
```

@:@

### `writeBytes`

With this method you can write bytes directly to a binary file. The contents of the file require to be in form of a `scodec.bits.ByteVector`:

@:select(api-style)

@:choice(syntax)

```scala 3
import scodec.bits.*
val niceBytes = hex"BadBabe"

path.writeBytes(niceBytes)
```

@:choice(static)

```scala 3
import scodec.bits.*
val niceBytes = hex"BadBabe"

writeBytes(path, niceBytes)
```

@:choice(fs2)

```scala 3
import scodec.bits.*
val niceBytes = hex"BadBabe"

Stream.emit(niceBytes)
  .through(Files[F].writeAll(path))
  .compile
  .drain
```

@:@

### `writeLines`

If you want to write many lines to a file, you can provide the contents as a collection of strings and this function will write each element as a new line:

@:select(api-style)

@:choice(syntax)

```scala 3
val todoList = List(
  "Buy Milk",
  "Finish the pending work",
  "Close the bank account",
  "Mow the yard",
  "Take the dog for a walk"
)

path.writeLines(todoList)
```

@:choice(static)

```scala 3
val todoList = List(
  "Buy Milk",
  "Finish the pending work",
  "Close the bank account",
  "Mow the yard",
  "Take the dog for a walk"
)

writeLines(path, todoList)
```

@:choice(fs2)

```scala 3
val todoList = List(
  "Buy Milk",
  "Finish the pending work",
  "Close the bank account",
  "Mow the lawn",
  "Take the dog for a walk"
)

Stream.emits(todoList)
  .through(Files[F].writeUtf8Lines(path))
  .compile
  .drain
```                                                                                                                                                              

@:@

### `writeAs`

This method allows you to write a custom type `A` to a file, given a `Codec[A]` in scope:

@:select(api-style)

@:choice(syntax)

```scala 3
import scodec.Codec

case class Rectangle(base: Float, height: Float) derives Codec

path.writeAs[Rectangle]( Rectangle(2.4, 6.0) )
```

@:choice(static)

```scala 3
import scodec.Codec

case class Rectangle(base: Float, height: Float) derives Codec

writeAs[Rectangle](path, Rectangle(2.4, 6.0))
```

@:choice(fs2)

```scala 3
import scodec.Codec
import fs2.interop.scodec.StreamEncoder

case class Rectangle(base: Float, height: Float) derives Codec

Stream.emit( Rectangle(2.4, 6.0) )
  .through(StreamEncoder.many(summon[Codec[Rectangle]]).toPipeByte)
  .through(Files[F].writeAll(path))
  .compile
  .drain
```

@:@

## Appending

Appending is very similar to writing, except that, instead of overwriting the contents of the file, it writes them to the very end of the existing file without deleting the previous contents.

### `append`

Similar to `write`, but adds the content to the end of the file instead of overwriting it. The function will append the contents to the last line of the file, if you want to append the contents as a new line, see [`appendLine`](#appendline).

@:select(api-style)

@:choice(syntax)

```scala 3
val path = Path("path/to/append.txt")

val secretFormula = "And the dish's final secret ingredient is "

for 
  _ <- path.write(secretFormula)
  _ <- path.append("Rustacean legs! 🦀")
yield ()
```

@:choice(static)

```scala 3
val path = Path("path/to/append.txt")

val secretFormula = "And the dish's final secret ingredient is "

for 
  _ <- write(path, secretFormula)
  _ <- append(path, "Rustacean legs! 🦀")
yield ()

```

@:choice(fs2)

```scala 3
import fs2.io.files.Flags

val path = Path("path/to/append.txt")

val secretFormula = "And the dish's final secret ingredient is "

Stream.emit(secretFormula)
  .through(Files[F].writeUtf8(path))
  .flatMap( _ =>
    Stream.emit("Rustacean legs! 🦀")
      .through(Files[F].writeUtf8(path, Flags.Append))
  )
  .compile
  .drain
```

@:@


### `appendWithCharset`

Like in [`writeWithCharset`](#writewithcharset), you can also use a custom charset to encode strings:

@:select(api-style)

@:choice(syntax)

```scala 3
import java.nio.charset.StandardCharsets

path.appendWithCharset("What encoding I'm I?", StandardCharsets.ISO_8859_1)
```

@:choice(static)

```scala 3
import java.nio.charset.StandardCharsets

appendWithCharset(path, "What encoding I'm I?", StandardCharsets.ISO_8859_1)
```

@:choice(fs2)

```scala 3
import java.nio.charset.StandardCharsets

Stream.emit("What encoding I'm I?")
  .through(encodeWithCharset(StandardCharsets.ISO_8859_1))
  .through(Files[F].writeAll(path))
  .compile
  .drain
```

@:@


### `appendLine`

Very similar to `append`, with the difference that appends the contents as a new line (equivalent to prepending a `\n` to the contents): 

@:select(api-style)

@:choice(syntax)

```scala 3
for 
  _ <- path.write("I'm at the top!")
  _ <- path.appendLine("I'm at the bottom 😞")
yield ()
```

@:choice(static)

```scala 3
for 
  _ <- write(path, "I'm at the top!")
  _ <- appendLine(path, "I'm at the bottom 😞")
yield ()
```

@:choice(fs2)

```scala 3
Stream.emit("I'm at the top!")
  .through(Files[F].writeUtf8(path))
  .flatMap( _ =>
    Stream.emit("\nI'm at the bottom 😞")
      .through(Files[F].writeUtf8(path, Flags.Append))
  )
  .compile
  .drain
```

@:@


### `appendBytes`

This function can be used to append bytes to the end of a binary file:

@:select(api-style)

@:choice(syntax)

```scala 3
import scodec.bits.ByteVector

for 
  document  <- path.read
  signature <- IO(document.hashCode().toByte)
  _ <- path.appendBytes(ByteVector(signature))
yield ()
```

@:choice(static)

```scala 3
import scodec.bits.ByteVector

for 
  document  <- read(path)
  signature <- IO(document.hashCode().toByte)
  _ <- appendBytes(path, ByteVector(signature))
yield ()
```

@:choice(fs2)

```scala 3
import fs2.io.file.Flags

Files[IO].readUtf8(path)
  .evalMap(document => IO(document.hashCode().toByte))
  .through(Files[IO].writeAll(path, Flags.Append))
  .compile
  .drain
```

@:@


### `appendLines`

You can also append multiple lines at the end of the file in the following way:

@:select(api-style)

@:choice(syntax)

```scala 3
val missingIngredients = Vector(
  "40 ladyfingers."
  "6 egg yolks",
  "3/4 cup granulated sugar.",
  "500 ml mascarpone, cold."
)

path.appendLines(missingIngredients)
```

@:choice(static)

```scala 3
val missingIngredients = Vector(
  "40 ladyfingers."
  "6 egg yolks",
  "3/4 cup granulated sugar.",
  "500 ml mascarpone, cold."
)

appendLines(path, missingIngredients)
```

@:choice(fs2)

```scala 3
import fs2.io.file.Flags

val missingIngredients = Vector(
  "40 ladyfingers."
  "6 egg yolks",
  "3/4 cup granulated sugar.",
  "500 ml mascarpone, cold."
)

Stream.emits(missingIngredients)
 .through(Files[IO].writeUtf8Lines(path, Flags.Append))
 .compile
 .drain
```

@:@

### `appendAs`
Finally, given a `Codec[A]` in the scope, this method will append a custom type `A` to the end of a file:

@:select(api-style)

@:choice(syntax)

```scala 3
import scodec.Codec
import scodec.codecs.*

opaque type Ranking = (Int, Long)
given rankingCodec: Codec[Ranking] = uint8 :: int64

path.appendAs[Ranking]( (2, 3120948123123L) )
```

@:choice(static)

```scala 3
import scodec.Codec
import scodec.codecs.*

opaque type Ranking = (Int, Long)
given rankingCodec: Codec[Ranking] = uint8 :: int64

appendAs[Ranking](path, (2, 3120948123123L))
```

@:choice(fs2)

```scala 3
import scodec.Codec
import scodec.codecs.*
import fs2.interop.scodec.*
import fs2.io.files.Flags

opaque type Ranking = (Int, Long)
given rankingCodec: Codec[Ranking] = uint8 :: int64

Stream.emit( (2, 3120948123123L) )
  .through(StreamEncoder.many(summon[Codec[Ranking]]).toPipeByte)
  .through(Files[F].writeAll(path, Flags.Append))
  .compile
  .drain
```

@:@