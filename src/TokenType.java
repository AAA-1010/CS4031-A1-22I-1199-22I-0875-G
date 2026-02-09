import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Token categories for MyLang.
 *
 * Shared between ManualScanner and JFlex scanner so both
 * produce identical output and identical statistics buckets.
 */
public enum TokenType {

    // --- Core token categories ---
    KEYWORD(true),
    IDENTIFIER(true),

    INT_LITERAL(true),
    FLOAT_LITERAL(true),
    STRING_LITERAL(true),
    CHAR_LITERAL(true),
    BOOL_LITERAL(true),

    // Operators (split by family: easier stats + debugging)
    OP_ARITHMETIC(true),     // + - * / % **
    OP_RELATIONAL(true),     // == != <= >= < >
    OP_LOGICAL(true),        // && || !
    OP_ASSIGNMENT(true),     // = += -= *= /=
    OP_INCDEC(true),         // ++ --

    PUNCTUATOR(true),        // () {} [] , ; :

    // Usually skipped (not emitted), but scanners may count them
    COMMENT(false),
    WHITESPACE(false),

    EOF(true),
    ERROR(false);

    private final boolean emittable;

    TokenType(boolean emittable) {
        this.emittable = emittable;
    }

    public boolean isEmittable() {
        return emittable;
    }

    // ---- Shared vocabulary (prevents mismatch across 2 scanners) ----
    // Keywords are case-sensitive exact matches. :contentReference[oaicite:8]{index=8}
    public static final Set<String> KEYWORDS;
    public static final Set<String> BOOLEAN_LITERALS;

    static {
        KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "start", "finish", "loop", "condition", "declare", "output",
                "input", "function", "return", "break", "continue", "else"
        )));
        BOOLEAN_LITERALS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "true", "false"
        )));
    }

    public static boolean isKeywordLexeme(String s) {
        return KEYWORDS.contains(s);
    }

    public static boolean isBooleanLexeme(String s) {
        return BOOLEAN_LITERALS.contains(s);
    }
}
