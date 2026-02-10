# CS4031 - Compiler Construction (Assignment 01)
## Lexical Analyzer for MyLang (Spring 2026)

**Team**
- 22I-1199 - Ali Aamir - Section G
- 22I-0875 - Dawood Cheema - Section G

## Language Info
- **Language name:** MyLang
- **File extension:** .lang

## Keywords (case-sensitive)
start, finish, loop, condition, declare, output, input, function, return, break, continue, else

## Identifiers
- Must start with uppercase A–Z
- Followed by lowercase letters, digits, or underscore
- Max length: 31 characters total
- Valid: Count, Variable_name, X, Total_sum_2024
- Invalid: count, Variable, 2Count, myVariable

## Literals
- Integer: 42, +100, -567, 0
- Float: 3.14, +2.5, -0.123456, 1.5e10, 2.0E-3
- String: supports escapes \" \\ \n \t \r
- Char: supports escapes \' \\ \n \t \r
- Boolean: true, false

## Operators (with precedence to be documented later)
- Arithmetic: + - * / % **
- Relational: == != <= >= < >
- Logical: && || !
- Assignment: = += -= *= /=
- Inc/Dec: ++ --

## Punctuators
( ) { } [ ] , ; :

## Comments
- Single-line: ## ...
- Multi-line: #* ... *#

## How to Run (fill later)
### Manual Scanner
```bash
javac src/*.java
java ManualScanner tests/test1.lang
