# CS4031 - Compiler Construction (Assignment 01)
## Lexical Analyzer for MyLang (Spring 2026)

**Team**
- 22I-1199 - Ali Aamir - Section G
- 22I-0875 - Dawood Cheema - Section G

---

## Language Info
- **Language name:** MyLang
- **File extension:** `.lang`

---

## Keywords (Case-Sensitive, Exact Match)

| Keyword | Meaning |
|---------|---------|
| `start` | Program entry point / block start |
| `finish` | Program end / block end |
| `loop` | Loop construct |
| `condition` | Conditional branching (if) |
| `else` | Alternative branch |
| `declare` | Variable declaration |
| `output` | Print / write to console |
| `input` | Read from console |
| `function` | Function definition |
| `return` | Return value from function |
| `break` | Exit loop |
| `continue` | Skip to next loop iteration |

---

## Identifiers
- Must start with uppercase `A–Z`
- Followed by lowercase letters (`a-z`), digits (`0-9`), or underscore (`_`)
- Max length: 31 characters total
- Regex: `[A-Z][a-z0-9_]{0,30}`
- **Valid:** `Count`, `Variable_name`, `X`, `Total_sum_2024`
- **Invalid:** `count` (lowercase start), `_Variable` (underscore start), `2Count` (digit start)

---

## Literals

### Integer Literals
- Regex: `[+-]?[0-9]+`
- **Valid:** `42`, `+100`, `-567`, `0`
- **Invalid:** `12.34`, `1,000`

### Floating-Point Literals
- Up to 6 decimal digits, optional exponent
- Regex: `[+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?`
- **Valid:** `3.14`, `+2.5`, `-0.123456`, `1.5e10`, `2.0E-3`
- **Invalid:** `3.` (no decimals), `.14` (no leading digits), `1.2345678` (>6 decimals)

### String Literals
- Regex: `"([^"\\\n]|\\["\\ntr])*"`
- Supported escape sequences: `\"`, `\\`, `\n`, `\t`, `\r`
- **Valid:** `"Hello"`, `"Line1\nLine2"`, `"Quote: \"OK\""`

### Character Literals
- Regex: `'([^'\\\n]|\\['\\ntr])'`
- Supported escape sequences: `\'`, `\\`, `\n`, `\t`, `\r`
- **Valid:** `'A'`, `'\n'`, `'\\'`

### Boolean Literals
- Regex: `(true|false)` (case-sensitive)
- **Valid:** `true`, `false`

---

## Operators

### By Precedence (highest to lowest)

| Precedence | Category | Operators |
|-----------|----------|-----------|
| 1 | Increment/Decrement | `++`, `--` |
| 2 | Unary | `!`, `+` (unary), `-` (unary) |
| 3 | Exponentiation | `**` |
| 4 | Multiplicative | `*`, `/`, `%` |
| 5 | Additive | `+`, `-` |
| 6 | Relational | `<`, `>`, `<=`, `>=` |
| 7 | Equality | `==`, `!=` |
| 8 | Logical AND | `&&` |
| 9 | Logical OR | `\|\|` |
| 10 | Assignment | `=`, `+=`, `-=`, `*=`, `/=` |

---

## Punctuators
`(` `)` `{` `}` `[` `]` `,` `;` `:`

---

## Comments
- **Single-line:** `##` followed by any characters until newline
  ```
  ## This is a single-line comment
  ```
- **Multi-line:** `#*` ... `*#` (supports nesting)
  ```
  #* This is a
     multi-line comment *#
  ```

---

## Project Structure
```
22I-1199-22I-0875-G/
├── src/
│   ├── ManualScanner.java    # Hand-coded DFA scanner (Part 1)
│   ├── Token.java            # Token class (shared)
│   ├── TokenType.java        # Token type enum (shared)
│   ├── SymbolTable.java      # Identifier symbol table
│   ├── ErrorHandler.java     # Error collection and reporting
│   ├── Scanner.flex          # JFlex specification (Part 2)
│   └── Yylex.java            # JFlex-generated scanner
├── tests/
│   ├── test1.lang            # All valid tokens
│   ├── test2.lang            # Complex expressions
│   ├── test3.lang            # String/char with escapes
│   ├── test4.lang            # Lexical errors
│   ├── test5.lang            # Comments
│   └── TestResults.txt       # Output of both scanners
├── docs/
│   ├── Automata_Design.pdf   # RE, NFA, DFA diagrams
│   └── Comparison.pdf        # Side-by-side scanner comparison
└── README.md
```

---

## How to Compile and Run

### Manual Scanner (Part 1)
From project root:
```powershell
cd src
javac *.java
java ManualScanner ..\tests\test1.lang
java ManualScanner ..\tests\test2.lang
java ManualScanner ..\tests\test3.lang
java ManualScanner ..\tests\test4.lang
java ManualScanner ..\tests\test5.lang
```

### JFlex Scanner (Part 2)
From project root:
```powershell
jflex -d .\src .\src\Scanner.flex
mkdir .\out -Force | Out-Null
javac -d .\out .\src\*.java
java -cp .\out JFlexMain .\tests\test1.lang
```

---

## Sample Programs

### Sample 1 — Basic Variable Declaration and Output
```
## Simple variable usage
declare
Count = 42
Rate = 3.14
Flag = true
Msg = "Hello World"
output Count
output Msg
finish
```

### Sample 2 — Loop with Condition
```
## Loop example
start
declare
I = 0
loop
    condition I < 10
        output I
        I = I + 1
    else
        break
finish
```

### Sample 3 — Function with Expressions
```
## Function and expressions
start
function
declare
X = 5
Y = 10
Result = (X ** 2) + (Y * 3)
condition Result >= 50
    output "Result is large"
    output Result
return Result
finish
```

---

## Output Format
Each token is printed as:
```
<TOKEN_TYPE, "lexeme", Line: N, Col: N>
```
Example:
```
<KEYWORD, "start", Line: 1, Col: 1>
<IDENTIFIER, "Count", Line: 2, Col: 1>
<OP_ASSIGNMENT, "=", Line: 2, Col: 7>
<INT_LITERAL, "42", Line: 2, Col: 9>
```

---

## Error Handling
The scanner detects and reports:
- **Invalid characters:** `@`, `$`, `?`, etc.
- **Invalid identifiers:** lowercase start, exceeding 31 chars
- **Malformed literals:** unterminated strings, bad escapes, >6 decimal digits
- **Unclosed multi-line comments**

Error format:
```
ERROR [ERROR_TYPE] Line N, Col N | Lexeme="..." | Reason=...
```

The scanner recovers from errors by skipping to the next valid token and continues scanning.
