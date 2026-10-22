# Library Book Reservation System

A simple Java-based library project with a web interface.

## Exactly five project files
1. `Book.java` — stores book details and availability.
2. `LibraryServer.java` — Java HTTP server, search, issue and return operations.
3. `public/index.html` — website structure and basic JavaScript interactions.
4. `public/style.css` — website styling.
5. `README.md` — project instructions.

## Requirements
Java 11 or newer.

## How to run
Keep `Book.java`, `LibraryServer.java`, and the `public` folder together. Open a terminal in the project root and run:

```bash
javac Book.java LibraryServer.java
java LibraryServer
```

Then open `http://localhost:8080`.

This starter version stores book data in memory. Restarting the server resets the sample books.
