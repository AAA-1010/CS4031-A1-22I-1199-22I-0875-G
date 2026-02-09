import java.util.Objects;

/**
 * Immutable token produced by either scanner.
 *
 * Required by assignment:
 * TokenType, lexeme, line number, column number :contentReference[oaicite:9]{index=9}
 *
 * Required output format:
 * <KEYWORD, "start", Line: 1, Col: 1> :contentReference[oaicite:10]{index=10}
 */
public final class Token {

    private final TokenType type;
    private final String lexeme;
    private final int line;    // 1-based
    private final int column;  // 1-based

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.lexeme = Objects.requireNonNull(lexeme, "lexeme must not be null");
        if (line < 1) throw new IllegalArgumentException("line must be >= 1");
        if (column < 1) throw new IllegalArgumentException("column must be >= 1");
        this.line = line;
        this.column = column;
    }

    public TokenType getType() { return type; }
    public String getLexeme() { return lexeme; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    // Useful later for SymbolTable integration :contentReference[oaicite:11]{index=11}
    public boolean isIdentifier() { return type == TokenType.IDENTIFIER; }

    // Safe formatting for output
    public String lexemeForDisplay() {
        String s = lexeme;
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("\n", "\\n");
        s = s.replace("\t", "\\t");
        s = s.replace("\r", "\\r");
        return s;
    }

    @Override
    public String toString() {
        return String.format("<%s, \"%s\", Line: %d, Col: %d>",
                type.name(), lexemeForDisplay(), line, column);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Token)) return false;
        Token other = (Token) o;
        return type == other.type
                && lexeme.equals(other.lexeme)
                && line == other.line
                && column == other.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, lexeme, line, column);
    }
}
