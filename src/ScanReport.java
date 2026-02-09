import java.util.ArrayList;
import java.util.List;

/**
 * A shared collector that both scanners can use:
 * - Add tokens in order
 * - Update symbol table and stats
 * - Collect errors
 */
public class ScanReport {
    private final List<Token> tokens = new ArrayList<>();
    private final SymbolTable symbolTable = new SymbolTable();
    private final Statistics statistics = new Statistics();
    private final ErrorHandler errorHandler = new ErrorHandler();

    public void addToken(Token t) {
        if (t == null) return;
        tokens.add(t);
        statistics.observe(t);
        if (t.isIdentifier()) symbolTable.observeIdentifier(t);
    }

    public void addError(String message, int line, int col, String fragment) {
        errorHandler.report(message, line, col, fragment);
        statistics.incrementError();
    }

    public List<Token> getTokens() { return tokens; }
    public SymbolTable getSymbolTable() { return symbolTable; }
    public Statistics getStatistics() { return statistics; }
    public ErrorHandler getErrorHandler() { return errorHandler; }

    public String formatFullReport() {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            if (t.getType().isEmittable()) sb.append(t).append("\n");
        }
        sb.append("\n");
        sb.append(symbolTable.formatForReport()).append("\n");
        sb.append(statistics.formatForReport()).append("\n");
        sb.append(errorHandler.formatForReport()).append("\n");
        return sb.toString();
    }
}
