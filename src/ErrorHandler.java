import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects lexical errors (instead of crashing).
 * Both scanners should call report(...) and then continue scanning.
 */
public class ErrorHandler {

    /** Categorized error types to keep output consistent across scanners. */
    public enum ErrorType {
        INVALID_CHARACTER,
        INVALID_IDENTIFIER,
        MALFORMED_LITERAL,
        UNCLOSED_MULTILINE_COMMENT
    }

    /** A single lexical error entry. */
    public static final class LexicalError {
        private final ErrorType type;
        private final int line;
        private final int column;
        private final String lexeme;
        private final String reason;

        public LexicalError(ErrorType type, int line, int column, String lexeme, String reason) {
            this.type = type;
            this.line = line;
            this.column = column;
            this.lexeme = lexeme;
            this.reason = reason;
        }

        @Override
        public String toString() {
            String lx = (lexeme == null) ? "" : lexeme;
            String rs = (reason == null) ? "" : reason;
            return String.format(
                    "ERROR [%s] Line %d, Col %d | Lexeme=\"%s\" | Reason=%s",
                    type, line, column, lx, rs
            );
        }
    }

    private final List<LexicalError> errors = new ArrayList<>();

    /** Add an error entry (scanner should continue after calling this). */
    public void report(ErrorType type, int line, int column, String lexeme, String reason) {
        errors.add(new LexicalError(type, line, column, lexeme, reason));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<LexicalError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** Pretty-print errors section for final report. */
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
