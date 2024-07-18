# Which API should I use?

In shellfish, you have two different ways of doing things:  

* First, if you prefer a more concise, fluent syntax, use the extension methods (e.g. `path.read()`) by importing the `shellfish.syntax.all.*` package. 

* If you prefer calling static methods, use direct method calls on the `FilesOs` and `ProcessesOs` objects (e.g., `FilesOs.read(path)`).

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

