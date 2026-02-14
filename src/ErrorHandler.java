import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects lexical errors (instead of crashing).
 * Scanners should call report(...) and then continue scanning.
 */
public class ErrorHandler {

    /** Categories of lexical errors (useful for consistent reporting). */
    public enum ErrorType {
        INVALID_CHARACTER,
        MALFORMED_LITERAL,
        INVALID_IDENTIFIER,
        UNCLOSED_MULTILINE_COMMENT
    }

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

        public ErrorType getType() { return type; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getLexeme() { return lexeme; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("ERROR [%s] Line %d, Col %d | Lexeme=\"%s\" | Reason=%s",
                    type.name(),
                    line,
                    column,
                    lexeme == null ? "" : lexeme.replace("\"", "\\\""),
                    reason == null ? "" : reason);
        }
    }

    private final List<LexicalError> errors = new ArrayList<>();

    /** Main report method used by scanners. */
    public void report(ErrorType type, int line, int column, String lexeme, String reason) {
        errors.add(new LexicalError(type, line, column, lexeme, reason));
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
            sb.append(e).append("\n");
        }
        return sb.toString();
    }
}
