# Using Paths

One of the main types that use this library is [`Path'](https://www.javadoc.io/static/co.fs2/fs2-docs_3/3.8.0/fs2/io/file/Path.html). It represents a path to a file or a directory.

You can start using `Path`s by importing the domain from the library: 

```scala
import shellfish.Path
```

## How to create a `Path`

You have many ways to do that, the most common is using the `apply` method of the companion object:

```scala mdoc
import fs2.io.file.Path // TODO: Change this to the import of the library itself

val path = Path("path/to/a/file/or/directory")
```

Once you did that, you can combine paths using some convenience methods:

```scala mdoc
val parent = Path("parent/dir")

val child = Path("child/dir")

parent / child / "some/more/locations"
```

Now you can start using paths around the library! Note that these paths are compatible with the Java NIO paths, so you can use them interchangeably by calling the `toNioPath` value.

