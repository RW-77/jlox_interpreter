package lox;

// Thrown to unwind the interpreter's call stack back to the function call
// when a return statement executes.
class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
