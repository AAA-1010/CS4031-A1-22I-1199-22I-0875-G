import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects lexical errors (instead of crashing).
 * Scanners should call report(...) and then continue scanning.
 */
public class ErrorHandler {

    public static final class LexicalError {
        private final String message;
        private final int line;
        private final int column;
        private final String fragment;

        public LexicalError(String message, int line, int column, String fragment) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.fragment = fragment;
        }

        @Override
        public String toString() {
            return String.format("[LEXICAL ERROR] Line %d, Col %d: %s (near: \"%s\")",
                    line, column, message, fragment == null ? "" : fragment);
        }
    }

    private final List<LexicalError> errors = new ArrayList<>();

    public void report(String message, int line, int column, String fragment) {
        errors.add(new LexicalError(message, line, column, fragment));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<LexicalError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public String formatForReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("ERRORS\n");
        sb.append("------\n");
        if (errors.isEmpty()) {
            sb.append("No lexical errors.\n");
            return sb.toString();
        }
        for (LexicalError e : errors) {
            sb.append(e.toString()).append("\n");
        }
        return sb.toString();
    }
}
