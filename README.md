# jlox — a tree-walking interpreter for the Lox language

A Java implementation of **jlox**, the tree-walking interpreter from Robert
Nystrom's [*Crafting Interpreters*](https://craftinginterpreters.com/). This is
a from-scratch learning build — the code is written by hand while working
through the book rather than copied — to get hands-on with lexing, parsing, and
interpreter design.

> **Status:** in progress. The **scanner (lexical analysis)** is complete;
> parsing and evaluation are next.

## What works so far

- A hand-written scanner that turns Lox source into a stream of tokens
- Single- and multi-character lexemes via maximal-munch scanning
  (`!=`, `==`, `<=`, `>=`, `//` comments, string and number literals)
- A REPL (read a line, scan it, print the tokens) and a file runner
- Basic error reporting with line numbers

## Requirements

- Java 23+
- [Apache Maven](https://maven.apache.org/) 3.9+

## Build & run

```bash
# compile
mvn compile

# start the REPL (Ctrl-D to exit)
mvn exec:java

# run a Lox script file
mvn exec:java -Dexec.args="path/to/script.lox"

# build a runnable jar in target/
mvn package
```

Example REPL session (currently prints the token stream):

```
> (1 + 2) != 3
LEFT_PAREN ( null
NUMBER 1 1.0
PLUS + null
NUMBER 2 2.0
RIGHT_PAREN ) null
BANG_EQUAL != null
NUMBER 3 3.0
EOF  null
```

## Project structure

```
src/main/java/lox/
├── Lox.java        # entry point: REPL + file runner + error reporting
├── Scanner.java    # lexer: source string -> list of tokens
├── Token.java      # a single token (type, lexeme, literal, line)
└── TokenType.java  # enum of all token kinds
```

## Roadmap

- [x] Scanning / lexical analysis
- [ ] Representing code (AST) & a parser (recursive descent)
- [ ] Expression evaluation
- [ ] Statements, variables, and scope
- [ ] Control flow, functions, closures
- [ ] Classes and inheritance

## Credits

Based on *Crafting Interpreters* by Robert Nystrom. Implementation written by me
as I work through the book.
