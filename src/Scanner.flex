%%

%public
%class Yylex
%unicode
%line
%column
%type Token

%{
/*
  7 token categories:
  IDENTIFIER, INT_LITERAL, FLOAT_LITERAL, STRING_LITERAL, CHAR_LITERAL, BOOL_LITERAL, COMMENTS
  (Comments include single-line and multi-line)
*/

private ScanReport report;

// buffers/state (moved here so we don't need a 2nd %% section)
private final StringBuilder stringBuf = new StringBuilder();
private boolean stringHasError = false;

private final StringBuilder charBuf = new StringBuilder();
private int charStage = 0;
private boolean charHasError = false;

public void setReport(ScanReport r) { this.report = r; }

private int line1() { return yyline + 1; }
private int col1()  { return yycolumn + 1; }

private Token emit(TokenType type, String lexeme) {
    Token t = new Token(type, lexeme, line1(), col1());
    if (report != null) report.addToken(t);
    return t;
}

private Token emitError(ErrorHandler.ErrorType errType, String lexeme, String reason) {
    if (report != null) report.addError(errType, line1(), col1(), lexeme, reason);
    return new Token(TokenType.ERROR, lexeme, line1(), col1());
}
%}

%x STRING
%x CHAR
%x CHAR_ESC
%x MLCOMMENT

// ---------- Macros ----------
SIGN   = [+-]
DIGIT  = [0-9]
UPPER  = [A-Z]
IDTAIL = [a-z0-9_]

WS      = [ \t\r\n]+

ID      = {UPPER}{IDTAIL}*
INT_OK  = {SIGN}?{DIGIT}+

EXP       = [eE]{SIGN}?{DIGIT}+
FLOAT_OK  = {SIGN}?{DIGIT}+"."{DIGIT}{1,6}({EXP})?

%%

// =========================================================
// COMMENTS (single-line + multi-line)  — skip but count
// =========================================================

// Multi-line starts with #*
"#*" {
    if (report != null) report.noteCommentRemoved(line1());
    yybegin(MLCOMMENT);
}

<MLCOMMENT>{
    // End: one-or-more '*' followed by '#'
    "*"+ "#" { yybegin(YYINITIAL); }

    // keep line/col updates stable on Windows too
    \r\n|\r|\n { /* consume newline */ }

    // consume any other char
    [^] { /* consume */ }

    // EOF without closing
    <<EOF>> {
        yybegin(YYINITIAL);
        return emitError(ErrorHandler.ErrorType.UNCLOSED_MULTILINE_COMMENT,
                         "#*",
                         "Reached EOF before closing *#");
    }
}

// Single-line comment: ##... (until newline)
"##"[^\r\n]* {
    if (report != null) report.noteCommentRemoved(line1());
    /* skip */
}

// =========================================================
// WHITESPACE (skip)
// =========================================================
{WS} {
    if (report != null) report.noteWhitespace(line1());
    /* skip */
}

// =========================================================
// BOOL
// =========================================================
"true"|"false" { return emit(TokenType.BOOL_LITERAL, yytext()); }

// =========================================================
// IDENTIFIER (must start uppercase) + length check in Java
// =========================================================
{ID} {
    String s = yytext();
    if (s.length() > 31) {
        return emitError(ErrorHandler.ErrorType.INVALID_IDENTIFIER, s,
                         "Identifier too long (max 31 characters)");
    }
    return emit(TokenType.IDENTIFIER, s);
}

[a-z_][A-Za-z0-9_]* {
    return emitError(ErrorHandler.ErrorType.INVALID_IDENTIFIER, yytext(),
                     "Identifier must start with uppercase A-Z");
}

// =========================================================
// FLOATS (valid first, malformed later)  <-- RULE ORDER
// =========================================================
{FLOAT_OK} { return emit(TokenType.FLOAT_LITERAL, yytext()); }

// digits '.'  => missing decimals
{SIGN}?{DIGIT}+"." {
    return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, yytext(),
                     "Missing digits after decimal point");
}

// '.' digits => missing leading digits
{SIGN}?"."{DIGIT}+ {
    return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, yytext(),
                     "Missing digits before decimal point");
}

// incomplete exponent: 1.23e or 1.23E+
{SIGN}?{DIGIT}+"."{DIGIT}+[eE]{SIGN}? {
    return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, yytext(),
                     "Malformed exponent (missing exponent digits)");
}

// catch float-like things (usually too many decimals), validate in Java
{SIGN}?{DIGIT}+"."{DIGIT}+({EXP})? {
    String s = yytext();

    int dot = s.indexOf('.');
    int ePos = -1;
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == 'e' || c == 'E') { ePos = i; break; }
    }
    int end = (ePos == -1) ? s.length() : ePos;
    int decimals = end - dot - 1;

    if (decimals > 6) {
        return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, s,
                         "Too many decimal digits (max 6)");
    }
    return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, s,
                     "Malformed floating-point literal");
}

// =========================================================
// INT (after floats)
// =========================================================
{INT_OK} { return emit(TokenType.INT_LITERAL, yytext()); }

// =========================================================
// STRING
// =========================================================
\" {
    stringBuf.setLength(0);
    stringBuf.append("\"");
    stringHasError = false;
    yybegin(STRING);
}

<STRING>{
  \" {
      stringBuf.append("\"");
      yybegin(YYINITIAL);
      if (stringHasError) {
          return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, stringBuf.toString(),
                           "Malformed string literal");
      }
      return emit(TokenType.STRING_LITERAL, stringBuf.toString());
  }

  \\[\"\\ntr\\] { stringBuf.append(yytext()); }  // valid escapes

  \\[^] {
      stringHasError = true;
      stringBuf.append(yytext());
  }

  [^\"\\\r\n]+ { stringBuf.append(yytext()); }  // normal chars

  \r\n|\r|\n {
      yybegin(YYINITIAL);
      return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, stringBuf.toString(),
                       "Unterminated string literal");
  }

  <<EOF>> {
      yybegin(YYINITIAL);
      return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, stringBuf.toString(),
                       "Unterminated string literal at EOF");
  }
}

// =========================================================
// CHAR
// =========================================================
"'" {
    charBuf.setLength(0);
    charBuf.append("'");
    charStage = 0;
    charHasError = false;
    yybegin(CHAR);
}

<CHAR>{
  "'" {
      charBuf.append("'");
      yybegin(YYINITIAL);
      if (charStage != 1 || charHasError) {
          return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, charBuf.toString(),
                           "Malformed character literal");
      }
      return emit(TokenType.CHAR_LITERAL, charBuf.toString());
  }

  "\\" { charBuf.append("\\"); yybegin(CHAR_ESC); }

  [^'\\\r\n] {
      charBuf.append(yytext());
      if (charStage == 0) charStage = 1;
      else charStage = 2;
  }

  \r\n|\r|\n {
      yybegin(YYINITIAL);
      return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, charBuf.toString(),
                       "Unterminated character literal");
  }

  <<EOF>> {
      yybegin(YYINITIAL);
      return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, charBuf.toString(),
                       "Unterminated character literal at EOF");
  }
}

<CHAR_ESC>{
  [\'\\ntr] {
      charBuf.append(yytext());
      yybegin(CHAR);
      if (charStage == 0) charStage = 1;
      else charStage = 2;
  }

  [^] {
      charHasError = true;
      charBuf.append(yytext());
      yybegin(CHAR);
      if (charStage == 0) charStage = 1;
      else charStage = 2;
  }

  <<EOF>> {
      yybegin(YYINITIAL);
      return emitError(ErrorHandler.ErrorType.MALFORMED_LITERAL, charBuf.toString(),
                       "Unterminated character literal at EOF");
  }
}

// =========================================================
// Fallback invalid char
// =========================================================
. {
    return emitError(ErrorHandler.ErrorType.INVALID_CHARACTER, yytext(),
                     "Character not recognized");
}

<<EOF>> { return emit(TokenType.EOF, "EOF"); }
