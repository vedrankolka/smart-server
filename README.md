# Smart Server

A no-dependency, pure Java, multithreaded HTTP server with a custom server-side scripting language.

## Running it

To build the project, run:
```shell
mvn clean package
```

To run it, pass in the path to the properties file:
```shell
java -jar target/smart-server-1.0.jar config/server.properties
```

Head over to the home page at [localhost:5721/index.html](localhost:5721/index.html).

## Features

- session cookies
- smart scripts ([implementation](src/main/java/hr/fer/zemris/java/custom/scripting),
  [example](webroot/scripts/osnovni.smscr))
- only GET method
- multithreading
- extendable by implementing [`IWebWorker`](src/main/java/hr/fer/zemris/java/webserver/IWebWorker.java)

### Smart Scripts

A simple server-side scripting language with a stack-based execution engine for dynamically generated pages.
Supports basic arithmetic operations, for loops, variable assignments, and a predefined set of functions.
