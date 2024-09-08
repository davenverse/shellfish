# Using Paths

Shellfish is implemented with [fs2-io](https://fs2.io/#/io), and one of the abstraction it provides is [`Path`](https://www.javadoc.io/static/co.fs2/fs2-docs_3/3.8.0/fs2/io/file/Path.html). It represents a path to a file or a directory.  

You can start using `Path`s by importing the domain from the library: 

```scala
import shellfish.Path
```

## Creating a `Path` 

The recommended way for creating a `Path` is using the `apply` method of the companion object:  

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

Note that these paths are compatible with the Java NIO paths, so you can use them interchangeably by calling the `toNioPath` value.  