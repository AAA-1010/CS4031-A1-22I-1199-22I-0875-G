# CS4031 - Compiler Construction (Assignment 01)
## Lexical Analyzer for MyLang (Spring 2026)

**Team**
- 22I-1199 - Ali Aamir - Section G
- 22I-0875 - Groupmate - Section G

## Language Info
- **Language name:** MyLang
- **File extension:** `.lang`

## Implemented Token Categories (Current Progress)
This project currently recognizes and reports tokens for:

1. Identifier  
2. Integer literal  
3. Float literal  
4. String literal  
5. Character literal  
6. Boolean literal  
7. Comments (single-line + multi-line)

## Identifiers
- Must start with uppercase A–Z
- Followed by lowercase letters, digits, or underscore
- Max length: 31 characters total
- Valid: `Count`, `Variable_name`, `X`, `Total_sum_2024`
- Invalid: `count`, `_Variable`, `2Count`

## Literals
- Integer: `42`, `+100`, `-567`, `0`
- Float: up to 6 decimal digits, optional exponent  
  Examples: `3.14`, `+2.5`, `-0.123456`, `1.5e10`, `2.0E-3`
- String: supports escapes `\" \\ \n \t \r`
- Char: supports escapes `\' \\ \n \t \r`
- Boolean: `true`, `false`

## Comments
- Single-line: `##[^\n]*`
- Multi-line: `#* ... *#`

## Project Structure
- `src/` → Java source files + `Scanner.flex` + generated `Yylex.java`
- `tests/` → `test1.lang` … `test5.lang` + `TestResults.txt`
- `out/` → compiled `.class` files (generated)

## How to Run

### JFlex Scanner (Part 2)
From project root:

```powershell
jflex -d .\src .\src\Scanner.flex
mkdir .\out -Force | Out-Null
javac -d .\out .\src\*.java
java -cp .\out JFlexMain .\tests\test1.lang
