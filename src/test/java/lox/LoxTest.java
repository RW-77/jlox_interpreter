package lox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LoxTest {
    private static final Pattern EXPECT = Pattern.compile("// expect: (.*)");

    // Output captured from a single run of the interpreter.
    private record Result(List<String> stdout, String stderr,
                          boolean hadError, boolean hadRuntimeError) {}

    private static Result run(String source) {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Lox.hadError = false;
        Lox.hadRuntimeError = false;

        try {
            System.setOut(new PrintStream(out, true));
            System.setErr(new PrintStream(err, true));

            List<Token> tokens = new Scanner(source).scanTokens();
            List<Stmt> statements = new Parser(tokens).parse();

            if (!Lox.hadError) {
                Interpreter interpreter = new Interpreter();
                new Resolver(interpreter).resolve(statements);
                if (!Lox.hadError) interpreter.interpret(statements);
            }
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }

        return new Result(out.toString().lines().toList(), err.toString(),
                Lox.hadError, Lox.hadRuntimeError);
    }

    static Stream<Path> examples() throws IOException {
        return Files.list(Paths.get("examples"))
                .filter(path -> path.toString().endsWith(".lox"))
                .sorted();
    }

    @ParameterizedTest
    @MethodSource("examples")
    void exampleProducesExpectedOutput(Path path) throws IOException {
        String source = Files.readString(path);

        List<String> expected = new ArrayList<>();
        Matcher matcher = EXPECT.matcher(source);
        while (matcher.find()) {
            expected.add(matcher.group(1));
        }
        assertFalse(expected.isEmpty(), "no expectations in " + path);

        Result result = run(source);

        assertEquals("", result.stderr(), "unexpected error output");
        assertEquals(expected, result.stdout());
    }

    @Test
    void keywordsAreNotScannedAsIdentifiers() {
        List<Token> tokens = new Scanner("true false nil orchid").scanTokens();

        assertEquals(TokenType.TRUE, tokens.get(0).type);
        assertEquals(TokenType.FALSE, tokens.get(1).type);
        assertEquals(TokenType.NIL, tokens.get(2).type);
        // Maximal munch: "orchid" is one identifier, not "or" + "chid".
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).type);
    }

    @Test
    void parsesWithCorrectPrecedence() {
        List<Token> tokens = new Scanner("-1 + 2 * 3 == 5;").scanTokens();
        Stmt.Expression stmt =
                (Stmt.Expression)new Parser(tokens).parse().get(0);

        assertEquals("(== (+ (- 1.0) (* 2.0 3.0)) 5.0)",
                new AstPrinter().print(stmt.expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "print 1 +;",
            "var a = 1 print a;",
            "a + b = c;",
            "fun f( {}",
    })
    void reportsSyntaxErrors(String source) {
        Result result = run(source);

        assertTrue(result.hadError());
        assertTrue(result.stderr().contains("Error"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "return 1;",
            "{ var a = a; }",
            "{ var a = 1; var a = 2; }",
            "class A < A {}",
            "print this;",
            "print super.foo;",
            "class A { init() { return 1; } }",
            "class A { foo() { super.foo(); } }",
    })
    void reportsResolutionErrors(String source) {
        Result result = run(source);

        assertTrue(result.hadError());
        assertFalse(result.hadRuntimeError());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "print \"a\" - 1;",
            "print -\"a\";",
            "print undefinedVar;",
            "\"not callable\"();",
            "fun f(a) {} f(1, 2);",
            "var x = 1; print x.field;",
            "class A {} print A().missing;",
            "var NotAClass = 1; class B < NotAClass {}",
    })
    void reportsRuntimeErrors(String source) {
        Result result = run(source);

        assertFalse(result.hadError());
        assertTrue(result.hadRuntimeError());
    }

    @Test
    void runtimeErrorStopsExecution() {
        Result result = run("print 1; print nil + 1; print 2;");

        assertEquals(List.of("1"), result.stdout());
        assertTrue(result.stderr().contains("[line 1]"));
    }
}
