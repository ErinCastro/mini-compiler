%{  
/* Include necessary header files for file I/O, standard library, and string manipulation */
#include <cstdio>     // Includes C library for file I/O
#include <cstdlib>    // Includes C standard library functions like malloc
#include <iostream>   // Includes C++ standard I/O streams
#include <string>     // Includes C++ string handling functions
#include <map>        // Includes map for symbol table

/* Declaration for external input stream and lexer function */
extern FILE* yyin;  // Declares external input stream
int yylex(void);  // External function to call the lexer
void yyerror(const char* s) { std::cerr << "Parse error: " << s << std::endl; }  // Error handling function

/* Symbol value structure that carries raw and typed view */
enum class Kind { INTK, DOUBLEK, CHARK, STRINGK };  // Enumeration for different data types (int, double, char, string)

/* Structure to hold variable value */
struct Value {
  Kind kind = Kind::STRINGK;  // Default type is string
  int i = 0;  // Integer value
  double d = 0.0;  // Double value
  std::string raw;  // Raw input as typed by the user
};

static std::map<std::string, Value> SYM;  // Symbol table (map) for storing variable names and their associated values

/* Helper functions to classify a token read by cin >> token */
static bool is_int_tok(const std::string& s) {
  /* Check if token is an integer */
  if (s.empty()) return false;  // If the string is empty, return false
  size_t i = (s[0]=='+'||s[0]=='-') ? 1 : 0;  // Handle optional sign
  if (i>=s.size()) return false;  // If there's no character after the sign, return false
  for (; i<s.size(); ++i) if (s[i]<'0'||s[i]>'9') return false;  // Check if all characters are digits
  return true;  // If all characters are digits, it's an integer
}

static bool is_double_tok(const std::string& s) {
  /* Check if token is a double */
  bool dot=false, digit=false;
  size_t i = (s[0]=='+'||s[0]=='-') ? 1 : 0;  // Handle optional sign
  for (; i<s.size(); ++i) {
    if (s[i]=='.') { if (dot) return false; dot=true; }  // Ensure only one dot
    else if (s[i]>='0'&&s[i]<='9') digit=true;  // Check for digits
    else return false;  // Return false if any character is invalid
  }
  return digit && dot;  // Return true if it's a valid double number
}

/* When outputting cout << expr; if expr was a lone IDENT with non-int,
   we want to print the raw token instead of an integer.
   We'll set this flag only for IDENT leafs; arithmetic resets it. */
static bool g_lastExprWasNonIntIdent = false;  // Flag to check if last expression was non-integer IDENT
static std::string g_lastExprRaw;  // Holds raw value for non-integer IDENT
%}

/* ---- semantic values ---- */
%union {
    int   ival;  // Union for storing integer values
    char* sval;  // Union for storing string values
}

%token CIN COUT SHIFTIN SHIFTOUT IDENT NUMBER STRINGLIT  // Define tokens (from lexer)
%token <sval> IDENT  // Define IDENT token as string value
%token <ival> NUMBER  // Define NUMBER token as integer value
%token <sval> STRINGLIT  // Define STRINGLIT token as string value

%left '+'  // Set precedence of '+' to left
%type <ival> expr  // Define type for the expr rule

%%

/* Start rule for the program */
program
  : stmt_list  // A program consists of a list of statements
  ;

stmt_list
  : /* empty */  // Empty rule for an empty statement list
  | stmt_list stmt  // A list of statements can be followed by another statement
  ;

stmt
  : input_stmt  ';'  // A statement can be an input statement followed by a semicolon
  | output_stmt ';'  // A statement can be an output statement followed by a semicolon
  | assign_stmt ';'  // A statement can be an assignment followed by a semicolon
  ;

/* cin >> IDENT;  (input statement) */
input_stmt
  : CIN SHIFTIN IDENT  // Input statement takes CIN, SHIFTIN, and an identifier
    {
      /* Read user input into token */
      std::string tok;
      std::cin >> tok;  // Reads user input into tok
      Value v; v.raw = tok;  // Stores raw token in symbol table
      if (is_int_tok(tok)) {
        v.kind = Kind::INTK; v.i = std::stoi(tok);  // Convert to integer if valid
      } else if (is_double_tok(tok)) {
        v.kind = Kind::DOUBLEK; v.d = std::stod(tok);  // Convert to double if valid
      } else if (tok.size()==1) {
        v.kind = Kind::CHARK; v.i = static_cast<unsigned char>(tok[0]);  // Convert to char if valid
      } else {
        v.kind = Kind::STRINGK;  // Default to string
      }
      SYM[std::string($3)] = v;  // Add value to symbol table
      free($3);  // Free memory allocated for the identifier string
    }
  ;

/* cout << expr;   OR   cout << "hello";  (output statement) */
output_stmt
  : COUT SHIFTOUT expr  // Output statement with an expression
    {
      /* Check if last expression was non-int IDENT and print accordingly */
      if (g_lastExprWasNonIntIdent) {
        std::cout << g_lastExprRaw << std::endl;  // Print raw token if last expr was non-int IDENT
      } else {
        std::cout << $3 << std::endl;  // Else, print integer value of the expression
      }
      g_lastExprWasNonIntIdent = false;  // Reset flag after printing
      g_lastExprRaw.clear();  // Clear raw value
    }
  | COUT SHIFTOUT STRINGLIT  // Output statement with string literal
    { std::cout << $3 << std::endl; free($3); }  // Print string literal and free memory
  ;

/* IDENT = expr;  (assignment statement) */
assign_stmt
  : IDENT '=' expr  // Assignment statement: IDENT = expr
    {
      /* Create value for identifier and assign to symbol table */
      Value v; v.kind = Kind::INTK; v.i = $3; v.raw = std::to_string($3);  // Create value for identifier
      SYM[std::string($1)] = v;  // Add assignment to symbol table
      free($1);  // Free identifier memory
    }
  ;

/* expressions (arithmetic is int-only, as spec says “addition of single-digit operands”) */
expr
  : NUMBER  // Expression is a number
    { $$ = $1; g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear(); }
  | IDENT  // Expression is an identifier
    {
      /* Look up identifier in symbol table and process it */
      auto name = std::string($1);  // Get identifier name
      auto it = SYM.find(name);  // Look up identifier in symbol table
      if (it == SYM.end()) {  // If not found, treat as 0
        $$ = 0;
        g_lastExprWasNonIntIdent = true; g_lastExprRaw = "";  // Mark as non-int IDENT
      } else {
        const Value& v = it->second;  // Retrieve value from symbol table
        switch (v.kind) {
          case Kind::INTK:
            $$ = v.i;  // Assign integer value
            g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear();
            break;
          case Kind::DOUBLEK:
          case Kind::CHARK:
          case Kind::STRINGK:
            $$ = 0;  // Treat as 0 for arithmetic
            g_lastExprWasNonIntIdent = true; g_lastExprRaw = v.raw;  // Remember raw value for printing
            break;
        }
      }
      free($1);  // Free identifier memory
    }
  | expr '+' expr  // Addition operation: expr + expr
    { $$ = $1 + $3; g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear(); }

  ;
  ;

%%

/* int yyparse(void);  // Parse function to start the parsing process */
