import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manual DFA-based lexical analyzer for MyLang.
 *
 * Implements all token types from the assignment spec (Section 3).
 * Uses longest-match principle and priority ordering (Section 3.12).
 * Integrates with Token, TokenType, SymbolTable, and ErrorHandler.
 */
public class ManualScanner {

    private final String src;
    private int pos;
    private int line;
    private int col;

    private final List<Token> tokens = new ArrayList<>();
    private final ErrorHandler errHandler = new ErrorHandler();
    private final SymbolTable symTable = new SymbolTable();

    // Statistics
    private int totalTokens = 0;
    private int linesProcessed = 1;
    private int commentsRemoved = 0;
    private final Map<TokenType, Integer> tokenCounts = new LinkedHashMap<>();

    public ManualScanner(String source) {
        this.src = source;
        this.pos = 0;
        this.line = 1;
        this.col = 1;
        for (TokenType tt : TokenType.values()) tokenCounts.put(tt, 0);
    }

    // ──────────────────────────────────────────────
    //  Helper: character access
    // ──────────────────────────────────────────────
    private char peek() { return pos < src.length() ? src.charAt(pos) : '\0'; }
    private char peekAt(int offset) { int i = pos + offset; return i < src.length() ? src.charAt(i) : '\0'; }
    private boolean atEnd() { return pos >= src.length(); }

    private char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') { line++; col = 1; }
        else { col++; }
        return c;
    }

    private void advanceN(int n) { for (int i = 0; i < n; i++) advance(); }

    // ──────────────────────────────────────────────
    //  Main scan loop
    // ──────────────────────────────────────────────
    public List<Token> scan() {
        while (!atEnd()) {
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        totalTokens = tokens.size();
        linesProcessed = line;
        return Collections.unmodifiableList(tokens);
    }

    /**
     * Priority order (Section 3.12):
     *  1. Multi-line comments   2. Single-line comments
     *  3. Multi-char operators  4. Keywords
     *  5. Boolean literals      6. Identifiers
     *  7. Float literals        8. Integer literals
     *  9. String/char literals  10. Single-char operators
     * 11. Punctuators          12. Whitespace
     */
    private void scanToken() {
        // 12. Whitespace — skip but track lines
        if (isWhitespace(peek())) { skipWhitespace(); return; }

        int startLine = line, startCol = col;

        // 1. Multi-line comment:  #* ... *#
        if (peek() == '#' && peekAt(1) == '*') { scanMultiLineComment(startLine, startCol); return; }

        // 2. Single-line comment:  ##[^\n]*
        if (peek() == '#' && peekAt(1) == '#') { scanSingleLineComment(startLine, startCol); return; }

        // 3. Multi-character operators (checked before keywords/identifiers)
        Token opTok = tryMultiCharOperator(startLine, startCol);
        if (opTok != null) { emit(opTok); return; }

        // 4-6. Keywords / Boolean / Identifiers (all start with a letter or underscore-like)
        //      Keywords & booleans are lowercase; identifiers start with A-Z
        if (isLowerAlpha(peek())) { scanKeywordOrBoolean(startLine, startCol); return; }
        if (isUpperAlpha(peek())) { scanIdentifier(startLine, startCol); return; }

        // 7-8. Numeric literals (integer / float), possibly with leading +/-
        //      Leading sign is ONLY consumed if preceded by an operator, punctuator, start, or nothing
        if (isDigit(peek())) { scanNumber(startLine, startCol); return; }
        if ((peek() == '+' || peek() == '-') && isDigit(peekAt(1)) && signIsUnary()) {
            scanNumber(startLine, startCol); return;
        }

        // Dot followed by digits (e.g., .14) → malformed float (missing leading digits)
        if (peek() == '.' && isDigit(peekAt(1))) {
            StringBuilder bad = new StringBuilder();
            bad.append(advance()); // consume '.'
            while (!atEnd() && isDigit(peek())) bad.append(advance());
            errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                    startLine, startCol, bad.toString(),
                    "Missing digits before decimal point");
            return;
        }

        // 9. String literal
        if (peek() == '"') { scanString(startLine, startCol); return; }

        // 9. Character literal
        if (peek() == '\'') { scanChar(startLine, startCol); return; }

        // 10. Single-character operators (those not caught by multi-char check)
        Token singleOp = trySingleCharOperator(startLine, startCol);
        if (singleOp != null) { emit(singleOp); return; }

        // 11. Punctuators
        Token punct = tryPunctuator(startLine, startCol);
        if (punct != null) { emit(punct); return; }

        // Unknown / invalid character → error, skip it
        char bad = advance();
        errHandler.report(ErrorHandler.ErrorType.INVALID_CHARACTER,
                startLine, startCol, sanitize(String.valueOf(bad)),
                "Unrecognized character");
    }

    // ──────────────────────────────────────────────
    //  Whitespace
    // ──────────────────────────────────────────────
    private boolean isWhitespace(char c) { return c == ' ' || c == '\t' || c == '\r' || c == '\n'; }

    private void skipWhitespace() {
        while (!atEnd() && isWhitespace(peek())) advance();
    }

    // ──────────────────────────────────────────────
    //  Comments
    // ──────────────────────────────────────────────
    private void scanSingleLineComment(int sL, int sC) {
        int start = pos;
        advanceN(2); // skip ##
        while (!atEnd() && peek() != '\n') advance();
        commentsRemoved++;
    }

    private void scanMultiLineComment(int sL, int sC) {
        int start = pos;
        advanceN(2); // skip #*
        int depth = 1; // support nested (bonus)
        while (!atEnd() && depth > 0) {
            if (peek() == '#' && peekAt(1) == '*') { advanceN(2); depth++; }
            else if (peek() == '*' && peekAt(1) == '#') { advanceN(2); depth--; }
            else advance();
        }
        if (depth > 0) {
            errHandler.report(ErrorHandler.ErrorType.UNCLOSED_MULTILINE_COMMENT,
                    sL, sC, src.substring(start, Math.min(start + 20, src.length())),
                    "Multi-line comment never closed");
        }
        commentsRemoved++;
    }

    // ──────────────────────────────────────────────
    //  Multi-character operators
    // ──────────────────────────────────────────────
    private Token tryMultiCharOperator(int sL, int sC) {
        char c = peek(), c2 = peekAt(1);

        // Two-char operators
        if (c == '*' && c2 == '*') { advanceN(2); return new Token(TokenType.OP_ARITHMETIC, "**", sL, sC); }
        if (c == '=' && c2 == '=') { advanceN(2); return new Token(TokenType.OP_RELATIONAL, "==", sL, sC); }
        if (c == '!' && c2 == '=') { advanceN(2); return new Token(TokenType.OP_RELATIONAL, "!=", sL, sC); }
        if (c == '<' && c2 == '=') { advanceN(2); return new Token(TokenType.OP_RELATIONAL, "<=", sL, sC); }
        if (c == '>' && c2 == '=') { advanceN(2); return new Token(TokenType.OP_RELATIONAL, ">=", sL, sC); }
        if (c == '&' && c2 == '&') { advanceN(2); return new Token(TokenType.OP_LOGICAL, "&&", sL, sC); }
        if (c == '|' && c2 == '|') { advanceN(2); return new Token(TokenType.OP_LOGICAL, "||", sL, sC); }
        if (c == '+' && c2 == '+') { advanceN(2); return new Token(TokenType.OP_INCDEC, "++", sL, sC); }
        if (c == '-' && c2 == '-') { advanceN(2); return new Token(TokenType.OP_INCDEC, "--", sL, sC); }
        if (c == '+' && c2 == '=') { advanceN(2); return new Token(TokenType.OP_ASSIGNMENT, "+=", sL, sC); }
        if (c == '-' && c2 == '=') { advanceN(2); return new Token(TokenType.OP_ASSIGNMENT, "-=", sL, sC); }
        if (c == '*' && c2 == '=') { advanceN(2); return new Token(TokenType.OP_ASSIGNMENT, "*=", sL, sC); }
        if (c == '/' && c2 == '=') { advanceN(2); return new Token(TokenType.OP_ASSIGNMENT, "/=", sL, sC); }

        return null;
    }

    // ──────────────────────────────────────────────
    //  Single-character operators
    // ──────────────────────────────────────────────
    private Token trySingleCharOperator(int sL, int sC) {
        char c = peek();
        switch (c) {
            case '+': advance(); return new Token(TokenType.OP_ARITHMETIC, "+", sL, sC);
            case '-': advance(); return new Token(TokenType.OP_ARITHMETIC, "-", sL, sC);
            case '*': advance(); return new Token(TokenType.OP_ARITHMETIC, "*", sL, sC);
            case '/': advance(); return new Token(TokenType.OP_ARITHMETIC, "/", sL, sC);
            case '%': advance(); return new Token(TokenType.OP_ARITHMETIC, "%", sL, sC);
            case '<': advance(); return new Token(TokenType.OP_RELATIONAL, "<", sL, sC);
            case '>': advance(); return new Token(TokenType.OP_RELATIONAL, ">", sL, sC);
            case '!': advance(); return new Token(TokenType.OP_LOGICAL, "!", sL, sC);
            case '=': advance(); return new Token(TokenType.OP_ASSIGNMENT, "=", sL, sC);
            default: return null;
        }
    }

    // ──────────────────────────────────────────────
    //  Punctuators
    // ──────────────────────────────────────────────
    private Token tryPunctuator(int sL, int sC) {
        char c = peek();
        switch (c) {
            case '(': case ')': case '{': case '}':
            case '[': case ']': case ',': case ';': case ':':
                advance();
                return new Token(TokenType.PUNCTUATOR, String.valueOf(c), sL, sC);
            default: return null;
        }
    }

    // ──────────────────────────────────────────────
    //  Keywords and Boolean literals
    //  All keywords/booleans are lowercase alpha only
    // ──────────────────────────────────────────────
    private void scanKeywordOrBoolean(int sL, int sC) {
        StringBuilder sb = new StringBuilder();
        while (!atEnd() && isLowerAlpha(peek())) sb.append(advance());

        String word = sb.toString();
        if (TokenType.isKeywordLexeme(word)) {
            emit(new Token(TokenType.KEYWORD, word, sL, sC));
        } else if (TokenType.isBooleanLexeme(word)) {
            emit(new Token(TokenType.BOOL_LITERAL, word, sL, sC));
        } else {
            // lowercase-start word that isn't keyword/boolean → error
            errHandler.report(ErrorHandler.ErrorType.INVALID_IDENTIFIER,
                    sL, sC, word,
                    "Identifiers must start with uppercase letter (A-Z)");
        }
    }

    // ──────────────────────────────────────────────
    //  Identifiers: [A-Z][a-z0-9_]{0,30}
    // ──────────────────────────────────────────────
    private void scanIdentifier(int sL, int sC) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance()); // first char: A-Z
        int bodyCount = 0;
        while (!atEnd() && isIdBody(peek())) {
            if (bodyCount >= 30) {
                // consume rest but report error
                while (!atEnd() && isIdBody(peek())) sb.append(advance());
                errHandler.report(ErrorHandler.ErrorType.INVALID_IDENTIFIER,
                        sL, sC, sb.toString(),
                        "Identifier exceeds maximum length of 31 characters");
                return;
            }
            sb.append(advance());
            bodyCount++;
        }
        Token tok = new Token(TokenType.IDENTIFIER, sb.toString(), sL, sC);
        emit(tok);
        symTable.observeIdentifier(tok);
    }

    private boolean isIdBody(char c) {
        return isLowerAlpha(c) || isDigit(c) || c == '_';
    }

    // ──────────────────────────────────────────────
    //  Numeric literals
    //  Integer: [+-]?[0-9]+
    //  Float:   [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
    // ──────────────────────────────────────────────
    private void scanNumber(int sL, int sC) {
        StringBuilder sb = new StringBuilder();

        // Optional sign
        if (peek() == '+' || peek() == '-') sb.append(advance());

        // Integer part: [0-9]+
        while (!atEnd() && isDigit(peek())) sb.append(advance());

        // Check for dot → possible float
        if (!atEnd() && peek() == '.') {
            if (isDigit(peekAt(1))) {
                sb.append(advance()); // consume '.'

                // Decimal digits: [0-9]{1,6}
                int decCount = 0;
                while (!atEnd() && isDigit(peek())) {
                    sb.append(advance());
                    decCount++;
                }
                if (decCount > 6) {
                    errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                            sL, sC, sb.toString(),
                            "Too many decimal digits (max 6)");
                    return;
                }

                // Check for another dot (e.g., 12.3.4) → malformed
                if (!atEnd() && peek() == '.') {
                    // Emit what we have as valid float, the next .4 will be caught separately
                    emit(new Token(TokenType.FLOAT_LITERAL, sb.toString(), sL, sC));
                    return;
                }

                // Optional exponent: [eE][+-]?[0-9]+
                if (!atEnd() && (peek() == 'e' || peek() == 'E')) {
                    sb.append(advance()); // consume e/E
                    if (!atEnd() && (peek() == '+' || peek() == '-')) sb.append(advance());
                    if (atEnd() || !isDigit(peek())) {
                        errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                                sL, sC, sb.toString(),
                                "Float exponent requires at least one digit");
                        return;
                    }
                    while (!atEnd() && isDigit(peek())) sb.append(advance());
                }

                emit(new Token(TokenType.FLOAT_LITERAL, sb.toString(), sL, sC));
            } else {
                // dot with no digit after it (e.g., "12.") → malformed
                sb.append(advance()); // consume the dot
                errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                        sL, sC, sb.toString(),
                        "Missing digits after decimal point");
                return;
            }
        } else {
            // Pure integer
            emit(new Token(TokenType.INT_LITERAL, sb.toString(), sL, sC));
        }
    }

    /**
     * Determines if a +/- sign should be treated as unary (part of number)
     * rather than as an arithmetic operator.
     * Sign is unary if preceded by: nothing, operator, punctuator, or keyword.
     */
    private boolean signIsUnary() {
        if (tokens.isEmpty()) return true;
        TokenType last = tokens.get(tokens.size() - 1).getType();
        return last == TokenType.OP_ARITHMETIC || last == TokenType.OP_RELATIONAL
                || last == TokenType.OP_LOGICAL || last == TokenType.OP_ASSIGNMENT
                || last == TokenType.PUNCTUATOR || last == TokenType.KEYWORD;
    }

    // ──────────────────────────────────────────────
    //  String literals: "([^"\\\n]|\\["\\ntr])*"
    // ──────────────────────────────────────────────
    private void scanString(int sL, int sC) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance()); // opening "
        boolean error = false;
        String errorReason = "";

        while (!atEnd()) {
            char c = peek();
            if (c == '"') {
                sb.append(advance()); // closing "
                if (!error) {
                    emit(new Token(TokenType.STRING_LITERAL, sb.toString(), sL, sC));
                } else {
                    errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                            sL, sC, sanitize(sb.toString()), errorReason);
                }
                return;
            }
            if (c == '\n') {
                // Unterminated — don't consume the newline, let whitespace handler get it
                errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                        sL, sC, sanitize(sb.toString()), "Unterminated string literal");
                return;
            }
            if (c == '\\') {
                sb.append(advance()); // consume backslash
                if (atEnd()) break;
                char esc = peek();
                if (esc == '"' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r') {
                    sb.append(advance());
                } else {
                    // Bad escape — mark error but keep scanning to find closing "
                    sb.append(advance());
                    if (!error) {
                        error = true;
                        errorReason = "Malformed string literal";
                    }
                }
            } else {
                sb.append(advance());
            }
        }
        // Reached EOF without closing quote
        errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                sL, sC, sanitize(sb.toString()), "Unterminated string literal");
    }

    // ──────────────────────────────────────────────
    //  Character literals: '([^'\\\n]|\\['\\ntr])'
    // ──────────────────────────────────────────────
    private void scanChar(int sL, int sC) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance()); // opening '

        if (atEnd() || peek() == '\n') {
            errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                    sL, sC, sanitize(sb.toString()), "Unterminated character literal");
            return;
        }

        // Empty char literal ''
        if (peek() == '\'') {
            sb.append(advance());
            errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                    sL, sC, sanitize(sb.toString()), "Malformed character literal");
            return;
        }

        boolean error = false;
        String errorReason = "";

        if (peek() == '\\') {
            // Escape sequence
            sb.append(advance()); // consume backslash
            if (atEnd() || peek() == '\n') {
                errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                        sL, sC, sanitize(sb.toString()), "Unterminated character literal");
                return;
            }
            char esc = peek();
            if (esc == '\'' || esc == '\\' || esc == 'n' || esc == 't' || esc == 'r') {
                sb.append(advance());
            } else {
                // Bad escape — consume it but mark error, keep going to find closing '
                sb.append(advance());
                error = true;
                errorReason = "Malformed character literal";
            }
        } else {
            // Normal single character
            sb.append(advance());
        }

        // Now look for closing ' — but also handle multi-char body like 'AB'
        // Consume any extra characters until we find ' or newline or EOF
        while (!atEnd() && peek() != '\'' && peek() != '\n') {
            sb.append(advance());
            if (!error) {
                error = true;
                errorReason = "Malformed character literal";
            }
        }

        if (!atEnd() && peek() == '\'') {
            sb.append(advance()); // closing '
            if (error) {
                errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                        sL, sC, sanitize(sb.toString()), errorReason);
            } else {
                emit(new Token(TokenType.CHAR_LITERAL, sb.toString(), sL, sC));
            }
        } else {
            // No closing quote found
            errHandler.report(ErrorHandler.ErrorType.MALFORMED_LITERAL,
                    sL, sC, sanitize(sb.toString()), "Unterminated character literal");
        }
    }

    // ──────────────────────────────────────────────
    //  Character classification helpers
    // ──────────────────────────────────────────────
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isUpperAlpha(char c) { return c >= 'A' && c <= 'Z'; }
    private boolean isLowerAlpha(char c) { return c >= 'a' && c <= 'z'; }

    /** Escape control chars so error messages print on one line */
    private String sanitize(String s) {
        return s.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ──────────────────────────────────────────────
    //  Emit token + update stats
    // ──────────────────────────────────────────────
    private void emit(Token t) {
        tokens.add(t);
        tokenCounts.merge(t.getType(), 1, Integer::sum);
    }

    // ──────────────────────────────────────────────
    //  Public accessors
    // ──────────────────────────────────────────────
    public ErrorHandler getErrorHandler() { return errHandler; }
    public SymbolTable getSymbolTable() { return symTable; }
    public List<Token> getTokens() { return Collections.unmodifiableList(tokens); }

    // ──────────────────────────────────────────────
    //  Statistics report
    // ──────────────────────────────────────────────
    public String formatStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("STATISTICS\n");
        sb.append("----------\n");
        sb.append(String.format("Total tokens:     %d\n", totalTokens));
        sb.append(String.format("Lines processed:  %d\n", linesProcessed));
        sb.append(String.format("Comments removed: %d\n", commentsRemoved));
        sb.append("\nToken counts by type:\n");
        for (Map.Entry<TokenType, Integer> e : tokenCounts.entrySet()) {
            if (e.getValue() > 0) {
                sb.append(String.format("  %-18s: %d\n", e.getKey(), e.getValue()));
            }
        }
        return sb.toString();
    }

    // ──────────────────────────────────────────────
    //  Main — run scanner on a source file
    // ──────────────────────────────────────────────
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ManualScanner <source-file>");
            System.out.println("Example: java ManualScanner tests/test1.lang");
            return;
        }

        String source;
        try {
            source = new String(Files.readAllBytes(Paths.get(args[0])));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        ManualScanner scanner = new ManualScanner(source);
        List<Token> tokens = scanner.scan();

        // Print all emittable tokens
        System.out.println("TOKENS");
        System.out.println("------");
        for (Token t : tokens) {
            if (t.getType().isEmittable()) {
                System.out.println(t);
            }
        }
        System.out.println();

        // Statistics
        System.out.println(scanner.formatStatistics());

        // Symbol table
        System.out.println(scanner.getSymbolTable().formatForReport());

        // Errors
        System.out.println(scanner.getErrorHandler().formatForReport());
    }
}