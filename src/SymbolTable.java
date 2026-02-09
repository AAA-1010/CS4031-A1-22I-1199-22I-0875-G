import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Symbol table for identifiers.
 * Stores:
 *  - first occurrence position (line, column)
 *  - frequency count
 *
 * Requirement: "Symbol Table: Store identifiers, first occurrence, frequency." :contentReference[oaicite:1]{index=1}
 */
public class SymbolTable {

    public static final class Entry {
        private final String name;
        private final int firstLine;
        private final int firstColumn;
        private int frequency;

        private Entry(String name, int firstLine, int firstColumn) {
            this.name = name;
            this.firstLine = firstLine;
            this.firstColumn = firstColumn;
            this.frequency = 1;
        }

        public String getName() { return name; }
        public int getFirstLine() { return firstLine; }
        public int getFirstColumn() { return firstColumn; }
        public int getFrequency() { return frequency; }

        private void increment() { frequency++; }

        @Override
        public String toString() {
            return String.format("%s | first: (%d,%d) | freq: %d",
                    name, firstLine, firstColumn, frequency);
        }
    }

    // LinkedHashMap keeps insertion order -> stable printing
    private final Map<String, Entry> table = new LinkedHashMap<>();

    /** Add identifier token. If already exists, increment frequency. */
    public void observeIdentifier(Token token) {
        if (token == null) return;
        String name = token.getLexeme();

        Entry existing = table.get(name);
        if (existing == null) {
            table.put(name, new Entry(name, token.getLine(), token.getColumn()));
        } else {
            existing.increment();
        }
    }

    /** Read-only view */
    public Map<String, Entry> entries() {
        return Collections.unmodifiableMap(table);
    }

    public int size() {
        return table.size();
    }

    /** Print in a simple table-like format */
    public String formatForReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("SYMBOL TABLE (Identifiers)\n");
        sb.append("Name | First(Line,Col) | Frequency\n");
        sb.append("-----------------------------------\n");
        for (Entry e : table.values()) {
            sb.append(String.format("%s | (%d,%d) | %d\n",
                    e.getName(), e.getFirstLine(), e.getFirstColumn(), e.getFrequency()));
        }
        return sb.toString();
    }
}
