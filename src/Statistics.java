import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks scanner statistics:
 * - total tokens (excluding whitespace/comments)
 * - count per token type
 * - lines processed (max line number seen)
 * - comments removed
 */
public class Statistics {

    private final Map<TokenType, Integer> counts = new EnumMap<>(TokenType.class);

    private int totalTokens = 0;
    private int linesProcessed = 0;
    private int commentsRemoved = 0;

    public Statistics() {
        for (TokenType t : TokenType.values()) counts.put(t, 0);
    }

    /** Update line counter to the max line number seen. */
    public void updateLine(int line1Based) {
        if (line1Based > linesProcessed) linesProcessed = line1Based;
    }

    public void observe(Token token) {
        if (token == null) return;

        TokenType t = token.getType();
        counts.put(t, counts.getOrDefault(t, 0) + 1);

        updateLine(token.getLine());

        if (t != TokenType.WHITESPACE && t != TokenType.COMMENT) {
            totalTokens++;
        }
        if (t == TokenType.COMMENT) {
            commentsRemoved++;
        }
    }

    public void incrementCommentRemoved(int line1Based) {
        counts.put(TokenType.COMMENT, counts.getOrDefault(TokenType.COMMENT, 0) + 1);
        commentsRemoved++;
        updateLine(line1Based);
    }

    public void incrementWhitespaceSeen(int line1Based) {
        counts.put(TokenType.WHITESPACE, counts.getOrDefault(TokenType.WHITESPACE, 0) + 1);
        updateLine(line1Based);
    }

    public String formatForReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("STATISTICS\n");
        sb.append("----------\n");
        sb.append(String.format("TOTAL_TOKENS      : %d\n", totalTokens));
        sb.append(String.format("LINES_PROCESSED   : %d\n", linesProcessed));
        sb.append(String.format("COMMENTS_REMOVED  : %d\n", commentsRemoved));
        sb.append("\nCOUNT PER TOKEN TYPE\n");
        for (TokenType t : TokenType.values()) {
            int c = counts.getOrDefault(t, 0);
            if (c > 0) sb.append(String.format("%-16s : %d\n", t.name(), c));
        }
        return sb.toString();
    }
}
