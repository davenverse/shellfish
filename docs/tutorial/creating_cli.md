# Creating a Contact Manager CLI

Moving on from the basics, in this tutorial, we will create a simple CLI application that manages contacts. The application will allow users to add, list, update, and delete contacts.


## Basic Idea
We want a CLI application that stores the contacts in a file on your system and allows you to perform CRUD operations on the contacts. The application will look something like this: 


```bash
$ cm add 
> Enter username: contact_username
> First Name: contact_name
> Last Name: contact_name
> Phone: contact_phone
> Email: contact_email
```

```bash
$ cm get 
> ----- contact_username -----
> First Name:    contact_name
> Last Name:     contact_name
> Phone:         contact_phone
> Email:         contact_email
```

```bash
$ cm update contact_username --last-name new_contact_name
> Contact contact_username updated successfully
```

Now that we know what we want to build, let us start by creating the project structure.

## Project Structure
For this project, we will use the following project structure:

```
examples/
└── src
    └── main
        └── scala
            └── contacts
                ├── app
                ├── cli
                ├── core
                └── domain
```

- `app`: This package will contain the main entry point of the application.
- `cli`: This package will contain the CLI logic such as prompt parsing and command execution.
- `core`: This package will contain the business logic of the application, here is where we will use Shellfish!
- `domain`: This package will contain important business logic (mostly types) that `app`, `cli` and `core` will use. 

Let us first start by defining our domain.

## Domain
Our domain will be straightforward, we will have a `Contact` case class that will represent a contact. 

```scala
//src/main/shellfish/contacts/domain/contact.scala
type Username    = String
type Name        = String
type PhoneNumber = String
type Email       = String

case class Contact(
  username: Username,
  firstName: Name,
  lastName: Name,
  phoneNumber: PhoneNumber,
  email: Email
)
``` 
The type aliases are great, not only for added readability, but they also provide a way of not accidentally passing the wrong argument to the function. You can also use refined types provided by nice libraries like [Iron Types](https://github.com/Iltotore/iron), but for the sake of simplicity, we will use plain strings.

We will also need a function to display the contact in a nice format. If you came from Java, this function is known to `toString()`. But in Scala, we use `show` for this purpose.

```scala 3
//src/main/shellfish/contacts/domain/contact.scala
case class Contact( ... ):
  def show: String = s"""
    |----- $username -----
    |
    |First Name:    $firstName
    |Last Name:     $lastName
    |Phone:         $phoneNumber
    |Email:         $email
  """.stripMargin
```

We will also define a custom error type that gets when a contact already exists.  


```scala 3
//src/main/shellfish/contacts/domain/contact.scala
case class ContactFound(username: Username) extends NoStackTrace
```

This will make `ContactFound` a subtype of `Throwable` and will allow us to propagate the error in the `IO` monad and handle it whenever we want.

The next step is defining the different commands that our CLI will support.

```scala 3
//src/main/shellfish/contacts/domain/argument.scala
enum CliCommand:
  case AddContact
  case RemoveContact(username: Username)
  case SearchId(username: Username)
  case SearchName(name: Name)
  case SearchEmail(email: Email)
  case SearchNumber(number: PhoneNumber)
  case UpdateContact(username: Username, options: List[Flag])
  case ViewAll
  case Help
```

We will also need a `Flag` type to represent the different options that we can update in a contact.

```scala 3
//src/main/shellfish/contacts/domain/flag.scala
enum Flag:
  case FirstNameFlag(firstName: String)
  case LastNameFlag(lastName: String)
  case PhoneNumberFlag(phoneNumber: String)
  case EmailFlag(email: String)
  case UnknownFlag(flag: String)
```

Now that we have our domain defined, let us move to the core of our application.

## Core business logic
In this section, we will define the core business logic of our application. We will define a `ContactManager` algebra that will contain all the logic to manage contacts:

```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala`
trait ContactManager:

  // Save a new contact to the contact list
  def addContact(contact: Contact): IO[Username]

  // Remove a contact from the contact list using the username
  def removeContact(username: Username): IO[Unit]
  
  /**
    * Search for a contact using different things like username,
    * name, email, or phone number. It will return a list with
    * the matching results, or an empty list if none was found.
    *
    * Searching by id should return only one result, se we use
    * an `Option` there.
    */
  def searchId(username: Username): IO[Option[Contact]]
  def searchName(name: Name): IO[List[Contact]]
  def searchEmail(email: Email): IO[List[Contact]]
  def searchNumber(number: PhoneNumber): IO[List[Contact]]

  // Lists all the saved contacts
  def getAll: IO[List[Contact]]

  /**
   * Update a contact using a `modify` function. For example,
   * `updateContact("username")(contact ⇒ contact.copy(firstName = "new name"))`
   */
  def updateContact(username: String)(modify: Contact => Contact): IO[Contact]

end ContactManager
```

Now that we have our algebra defined, let us implement it using Shellfish. 

For that, it's common to implement the interface (or in Scala argot, the *algebra*) in the companion object of the trait:

```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala
object ContactManager:
  
  def apply(bookPath: Path): ContactManager = new ContactManager:
    override def addContact(contact: Contact): IO[Username] = ???
    override def removeContact(username: Username): IO[Unit] = ???
    override def searchId(username: Username): IO[Option[Contact]] = ???
    override def searchName(name: Name): IO[List[Contact]] = ???
    override def searchEmail(email: Email): IO[List[Contact]] = ??? 
    override def searchNumber(number: PhoneNumber): IO[List[Contact]] = ???
    override def getAll: IO[List[Contact]] = ???
    override def updateContact(
      username: String
    )(modify: Contact => Contact): IO[Contact] = ???
```
As a dependency of the implementation, we will use the path to the file where we will store the contacts. Also, the `apply` function will let us call it using the class name!

The first function will add a contact to the contact list. But first, we need to define a couple of things: 

The fist one will be the encoding of the `Contact` in the file. We’re going to go with a format like this: 

```text
<username>|<firstName>|<lastName>|<phoneNumber>|<email>
```
But of course, you can use any format you want, like JSON, CSV, etc.

Once we defined the encoding, we need a function to parse the file representation to our type in Scala:


```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala
def apply(bookPath: Path): ContactManager = new ContactManager:
  
    private def parseContact(contact: String): IO[Contact] =
      contact.split('|') match 
        case Array(id, firstName, lastName, phoneNumber, email) =>
          Contact(id, firstName, lastName, phoneNumber, email).pure[IO]
        case _ =>
          new Exception(s"Invalid contact format: $contact")
            .raiseError[IO, Contact]
```

Next, a function to encode the contact to a string:

```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala
    private def encodeContact(contact: Contact): String =
      s"${contact.username}|${contact.firstName}|${contact.lastName}|${contact.phoneNumber}|${contact.email}"
```

Finally, a helper function to help us save the contacts in memory:
```scala 3
    private def saveContacts(contacts: List[Contact]): IO[Unit] =
      bookPath.writeLines(contacts.map(encodeContact))
```

Now we will start with the `getAll` function. This is because our contact manager will work by first loading and parsing all the contacts so that we can perform operations on them:

```scala 3
override def getAll: IO[List[Contact]] = 
  for 
    lines    <- bookPath.readLines
    contacts <- lines.traverse(parseContact)
  yield contacts
```

We first load all the contacts into memory using our `readLines` function and them parse them into the type `Contact`. But look! It's our friend `traverse` again! We're going to use it to convert our `List[IO[Contact]]` into a `IO[List[Contact]]`. Why? Because the `parseContacts` function already returns an `IO`, so it suits perfectly to the function that `traverse` needs as argument. 

Now, we can implement the `addContact` function:

```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala
override def addContact(contact: Contact): IO[Username] = 
  for 
    contacts <- getAll                                      // (1)
    _ <- IO(contacts.contains(contact)).ifM( // (2)
      ContactFound(contact.username).raiseError[IO, Unit],
      saveContacts(contact :: contacts)
    )
  yield contact.username 
```

First, we read all of our contacts, line by line using the `ContactManager#getAll` method (1). Then, we check if the contact already exists in the file checking if the username is contained (2). If it does, we raise an error using the `raiseError` method. If it doesn't, we save it to memory using our `saveContacts` methods. This is done with the `ifM`; this method basically checks if the boolean inside the `IO` is true or false and it executes an effect depending on the condition. 

Now that we have the `addContact` function implemented, we can move to the `removeContact` function:

```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala
override def removeContact(username: Username): IO[Unit] =
  for 
    contacts <- getAll
    filteredContacts = contacts.filterNot(_.username === username)
    _ <- saveContacts(filteredContacts)
  yield ()
```

Here we load all the contacts from the file, and then we filter the ones that don't match the username we want to remove. Finally, we write the filtered contacts back to the file.

Now onto the search functionality. The first one is the `searchId` function:

```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala
override def searchUsername(username: Username): IO[Option[Contact]] =
  getAll.map(contacts => contacts.find(_.username === username))
```

Again we load all the contacts from the file, and then we find the contact that matches the username we want to search using the `find` functions, it returns an `Option` whenever it found something or not. 

The next three functions are very similar, so we will only show one of them, the `searchName` function:

```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala
override def searchName(name: Name): IO[List[Contact]] =
  getAll.map(contacts =>
    contacts.filter(c => c.firstName === name || c.lastName === name)
  )
```

The first part is similar to the previous functions, we load all the contacts from the file, and then we filter the ones that contain the name (or field) we want to search. 


The last function we need to implement is the `updateContact` function, be aware that this is the most complex function to implement, so we will break it down into smaller parts.

```scala 3
//src/main/shellfish/contacts/core/ContactManager.scala
override def updateContact(
    username: Username
)(modify: Contact => Contact): IO[Contact] = 
  for 
    oldContact <- searchUsername(username) match // (1)
      case None          => ContactNotFound(username).raiseError[IO, Contact]
      case Some(contact) => contact.pure[IO]

    updatedContacts = modify(oldContact) :: contacts.filterNot(_ == oldContact) // (2)
    _ <- saveContacts(updatedContacts)
  yield updatedContact
      
    
```


1. First, we'll find if the Contact we want to modify exists. If it doesnt, we raise a `ContactNotFound` error.

2. In case the contact exists, we are going to apply to it the transform function and add it to the loaded list. While doing that, we delete the old contact via the `filterNot` method.

3. Finally, we save the new contact list to the memory.

Oof, that was a lot of code! But we're done with the core of our application. Now we can move to the CLI part.

## CLI
In this section, we will define the CLI part of our application. Here, we’re going to use the core logic previously implemented to interact with the CLI. For example, by printing to the standard output and reading from the user input.  

It makes little sense to define the behaviour of the CLI in terms of an interface, as there are only a few ways of interacting with the user via a console. That why we will just create some functions contained on a `Cli` package:

```scala 3
//src/main/shellfish/contacts/cli/Cli.scala
object Cli:
  def addCommand ...
```

One way of passing the `ContactManager` dependency is to pass it as an argument to the function. However, it can be tedious to do this every time we want to use the functionality of our `Cli`, that's why Scala has an option to let the compiler do it for us! That's via the `given`/`using` clauses; if we want the compiler to "inject" the dependency, we just need to use the `using` keyword in the argument to the function, and it will be passed automatically as long as there is a given instance of our dependency in scope. This feature is also know as [contextual abstractions](https://docs.scala-lang.org/scala3/book/ca-context-parameters.html), We recommend that you take a closer look at these and how to use them in more detail.

That said, the first function is going to be the `addCommand`. The idea is to ask the user step by step for the information of the contact and then add it to the contact list:

```scala 3
//src/main/shellfish/contacts/cli/Cli.scala
def addCommand(using cm: ContactManager): IO[Unit] =
  for 
    username    <- IO.println("Enter the username: ") >> IO.readLine
    firstName   <- IO.println("Enter the first name: ") >> IO.readLine
    lastName    <- IO.println("Enter the last name: ") >> IO.readLine
    phoneNumber <- IO.println("Enter the phone number: ") >> IO.readLine
    email       <- IO.println("Enter the email: ") >> IO.readLine

    contact = Contact(username, firstName, lastName, phoneNumber, email)
    
    _ <- cm.addContact(contact)
      .flatMap(username => IO.println(s"Contact $username added"))
      .handleErrorWith:
        case ContactFound(username) =>
          IO.println(s"Contact $username already exists")
        case e =>
          IO.println(s"An error occurred: \n${e.printStackTrace()}")
    
  yield ()
```

The first five `IO.readLine` calls in `for` asks the fields to save the contact. Then we create the `Contact` object with the information provided. Next, we call the `addContact` method of the `ContactManager` and handle the errors. If the contact already exists, we print a message to the user. If another error occurs, we print the stack trace of the error to see what went wrong.  

Now the `removeCommand` function:

```scala 3 
//src/main/shellfish/contacts/cli/Cli.scala
def removeCommand(username: Username)(using cm: ContactManager): IO[Unit] =
  cm.removeContact(username) >> IO.println(s"Contact $username removed")
```
It simply calls the `removeContact` function and prints a message to the user. The function doesn't check if the contact exists and will delete any matching username anyway, but you can add this logic if you want.

The `searchIdCommand` function is also straight forward, matches the `Options` returned by the `searchId` method and prints the user information if it exists:

```scala 3
//src/main/shellfish/contacts/cli/Cli.scala
def searchIdCommand(username: Username)(using cm: ContactManager): IO[Unit] =
  for
    contact <- cm.searchId(username)
    _ <- contact match 
      case Some(c) => IO.println(c.show)
      case None    => IO.println(s"Contact $username not found")
  yield ()
```

Like the previous `searchX` variants, they’re fairly similar, so we will only show one of them.
In this case, the `searchEmailCommand` one:

```scala 3
//src/main/shellfish/contacts/cli/Cli.scala
def searchEmailCommand(email: Email)(using cm: ContactManager): IO[Unit] =
  for {
    contacts <- cm.searchEmail(email)
    _        <- contacts.traverse_(c => IO.println(c.show))
} yield ()
``` 

It also uses the `traverse_` method, but with a little variant that optimises it a bit. It takes as a parameter a function from `A => IO[Unit]` and it will execute the effect for each element of the collection, but it will discard the result of the computation.

Last but not least, the `updateCommand` function. 

We take as a first parameter the username of the contact we want to update in the database, so we can search for it's existence. 
As the second parameter, we will pass a list of flags, essentially describing which fields of the contact will be changed:

```scala 3
//src/main/shellfish/contacts/cli/Cli.scala
def updateCommand(username: Username, options: List[Flag])(
  using cm: ContactManager
): IO[Unit] =
  cm.updateContact(username): prev =>
    options.foldLeft(prev): (acc, flag) => // (1)
      flag match 
        case FirstNameFlag(name)     => acc.copy(firstName = name) // (2)
        case LastNameFlag(name)      => acc.copy(lastName = name)
        case PhoneNumberFlag(number) => acc.copy(phoneNumber = number)
        case EmailFlag(email)        => acc.copy(email = email)
        case UnknownFlag(_)          => acc
    
  .flatMap(c => IO.println(s"Updated contact ${c.username}")) // (3)
  .handleErrorWith:
    case ContactNotFound(username) =>
      IO.println(s"Contact $username not found")
    case e =>
      IO.println(s"An error occurred: \n${e.printStackTrace()}")
```

This is also quite a bit of code, but let's break it down: 
First, we call the `updateContact` method of the `ContactManager` and pass the function that will modify the contact. To do this, we're going to reduce the list of flags using foldLeft. This function takes an initial value (the contact we want to update) and a function describing how to update the initial value (`acc`) with each one of the flags in turn (`flag`) (1). Where, we just use the `copy` methods and pattern match the flags to update the contact (2). After that, we print a message to the user saying that the contact was updated successfully (3). If the contact is not found, we print a message to the user saying that. 

And as a last nice thing, a `helpCommand` function that will print the available commands to the user:

```scala 3
//src/main/shellfish/contacts/cli/Cli.scala
def helpCommand: IO[Unit] =
  IO.println:
    s"""
      |Usage: contacts [command]
      |
      |Commands:
      |  add
      |  remove <username>
      |  search id <username>
      |  search name <name>
      |  search email <email>
      |  search number <number>
      |  list
      |  update <username> [flags]
      |  help
      |
      |Flags (for update command):
      |  --first-name <name>
      |  --last-name <name>
      |  --phone-number <number>
      |  --email <email>
      |
      |""".stripMargin
```

That was rough, but congratulations on making it so far! Now we can move to the last part of our application.

## Prompt
The last part is the prompt parsing, the functionality that is going to parse the user command line arguments into instructions that Scala can understand and then we can pass to the `Cli`.

We strongly suggest that you use a command parsing library, for example, [Decline](https://ben.kirw.in/decline/). 

But again, for the sake of simplicity, well demonstrate how we achieved that without the need of an external dependency. 

First thing is creating a parse function that will pattern match the `args` parameter of our entry point functions and, depending on the command, will call the respective function of the `Cli` class:

```scala 3
//src/main/shellfish/contacts/cli/Prompt.scala
def parsePrompt(args: List[String]): CliCommand =
  args match 
    case "add" :: Nil                          => AddContact
    case "remove" :: username :: Nil           => RemoveContact(username)
    case "search" :: "id" :: username :: Nil   => SearchId(username)
    case "search" :: "name" :: name :: Nil     => SearchName(name)
    case "search" :: "email" :: email :: Nil   => SearchEmail(email)
    case "search" :: "number" :: number :: Nil => SearchNumber(number)
    case "list" :: _                           => ViewAll
    case "update" :: username :: options =>
      UpdateContact(username, parseUpdateFlags(options))
    case Nil                                   => Help
    case "--help" :: _ => Help
    case "help" :: _   => Help
    case _             => Help
end parsePrompt
```

Here we're using pattern matching to match the command and the arguments, so we can create the corresponding `CliCommand`.

Notice that we also created a `parseUpdateFlags` function, this does a similar thing, but it creates a list of flags that can change the updating behaviour:  

```scala 3
//src/main/shellfish/contacts/cli/Prompt.scala`
private def parseUpdateFlags(options: List[String]): List[Flag] = 

  @tailrec
  def tailParse(remaining: List[String], acc: List[Flag]): List[Flag] =
    remaining match 
  
      case Nil => acc
  
      case "--first-name" :: firstName :: tail =>
        tailParse(tail, FirstNameFlag(firstName) :: acc)
  
      case "--last-name" :: lastName :: tail =>
        tailParse(tail, LastNameFlag(lastName) :: acc)
  
      case "--phone-number" :: phoneNumber :: tail =>
        tailParse(tail, PhoneNumberFlag(phoneNumber) :: acc)
  
      case "--email" :: email :: tail =>
        tailParse(tail, EmailFlag(email) :: acc)
  
      case flag :: _ => List(UnknownFlag(flag))
    
  
  tailParse(options, Nil)
  
end parseUpdateFlags
```

Because the update flags  are unordered, we use a recursive function to match each flag and its value, and then we create a list of `Flag` that will be passed to the `updateCommand` function like defined before.

## Main

Now that we have all the parts of our application, we can create the entry point of our application.

The first thing is creating an initial function that describes a computation that will create the file we will work with. In this case, our in memory database will be located in `~/.shellfish/contacts.data`:

```scala 3
//src/main/shellfish/contacts/app/App.scala
private val createBookPath: IO[Path] =
  for
    home <- userHome
    dir  = home / ".shellfish"
    path = dir / "contacts.data"
    exists <- path.exists
    _      <- dir.createDirectories.unlessA(exists)
    _      <- path.createFile.unlessA(exists)
  yield path
```
The `userHome` function will find the home directory and then, create the directory and the file if they don't exist.

Remember the `using` keyword? Now we can create a given instance of the contact manager like this:

```scala 3
given cm: ContactManager = ContactManager(path)
```

With that, we can use the instance whenever we have declared an argument with the `using` clause with the same type.

Or even better, we can create it like this:

```scala 3
createBookPath
  .map(ContactManager(_))
  .flatMap: cm =>
    given ContactManager = cm
```

With that, our main (`run`) function will be like this:

```scala 3
//src/main/shellfish/contacts/app/App.scala
  def run(args: List[String]): IO[ExitCode] = createBookPath
    .map(ContactManager(_))
    .flatMap: cm =>
      given ContactManager = cm

      Prompt.parsePrompt(args) match 
        case Help                    => Cli.helpCommand
        case AddContact              => Cli.addCommand
        case RemoveContact(username) => Cli.removeCommand(username)
        case SearchId(username)      => Cli.searchUsernameCommand(username)
        case SearchName(name)        => Cli.searchNameCommand(name)
        case SearchEmail(email)      => Cli.searchEmailCommand(email)
        case SearchNumber(number)    => Cli.searchNumberCommand(number)
        case ViewAll                 => Cli.viewAllCommand
        case UpdateContact(username, flags) =>
          Cli.updateCommand(username, flags)
    
    .as(ExitCode.Success)
```

After creating our working path, we start creating our dependencies by calling the methods inside the companion objects of our application, in this case, the contact manager and the CLI. Then we parse the command line arguments using the `parsePrompt` function and call the respective function of the `Cli` class.  


This is what the final code looks like:

```scala 3
//src/main/shellfish/contacts/app/App.scala
object App extends IOApp:

  private val createBookPath: IO[Path] =
    for
      home <- userHome
      dir  = home / ".shellfish"
      path = dir / "contacts.data"
      exists <- path.exists
      _      <- dir.createDirectories.unlessA(exists)
      _      <- path.createFile.unlessA(exists)
    yield path

  def run(args: List[String]): IO[ExitCode] =
    createBookPath.flatMap:
      bookPath =>
        val cm  = ContactManager.impl(bookPath)
        val cli = Cli.make(cm)

        Prompt.parsePrompt(args) match
          case Help                    => cli.helpCommand
          case AddContact              => cli.addCommand
          case RemoveContact(username) => cli.removeCommand(username)
          case SearchId(username)      => cli.searchIdCommand(username)
          case SearchName(name)        => cli.searchNameCommand(name)
          case SearchEmail(email)      => cli.searchEmailCommand(email)
          case SearchNumber(number)    => cli.searchNumberCommand(number)
          case ViewAll                 => cli.viewAllCommand
          case UpdateContact(username, flags) =>
            cli.updateCommand(username, flags)
            
    .as(ExitCode.Success)
```

And that's it!
We’ve created a simple CLI application that manages contacts using Shellfish and Cats Effect.
We hope you enjoyed this tutorial and learned a lot about how to use Shellfish in a real-world application.

### Exercise
We used a hardcoded path to store our contacts.  

But what if we want to change the path to another location?
Or maybe have multiple locations with different contacts? 

Try to modify the application so the user can initialise and change the location of a contact book on a specific path and then use that path to store the contacts.

To do that, you can create two new commands, `init` and `change` that will create a new contact book and change the current contact book, respectively.

You can also create new domain representations of that book, like `ContactBook(path: Path, contacts: List[Contacts])` and modify the `ContactManager` to use that representation.  

Good luck!