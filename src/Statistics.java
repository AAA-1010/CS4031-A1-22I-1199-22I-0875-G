import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks counts of tokens and errors for reporting.
 * Requirement: "Display Statistics: Token counts." :contentReference[oaicite:3]{index=3}
 */
public class Statistics {

    private final Map<TokenType, Integer> counts = new EnumMap<>(TokenType.class);
    private int errorCount = 0;

    public Statistics() {
        for (TokenType t : TokenType.values()) counts.put(t, 0);
    }

    public void observe(Token token) {
        if (token == null) return;
        TokenType t = token.getType();
        counts.put(t, counts.getOrDefault(t, 0) + 1);
        if (t == TokenType.ERROR) errorCount++;
    }

    public void incrementError() {
        errorCount++;
        counts.put(TokenType.ERROR, counts.getOrDefault(TokenType.ERROR, 0) + 1);
    }

    public int getErrorCount() {
        return errorCount;
    }

    public String formatForReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("STATISTICS\n");
        sb.append("----------\n");
        for (TokenType t : TokenType.values()) {
            int c = counts.getOrDefault(t, 0);
            if (c > 0) sb.append(String.format("%-15s : %d\n", t.name(), c));
        }
        sb.append(String.format("TOTAL_ERRORS     : %d\n", errorCount));
        return sb.toString();
    }
}
