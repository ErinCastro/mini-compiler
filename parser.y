%{
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <string>
#include <map>

extern FILE* yyin;
int yylex(void);
void yyerror(const char* s) { std::cerr << "Parse error: " << s << std::endl; }

/* ---- Symbol value that carries raw + typed view ---- */
enum class Kind { INTK, DOUBLEK, CHARK, STRINGK };

struct Value {
  Kind kind = Kind::STRINGK;
  int i = 0;
  double d = 0.0;
  std::string raw;  // exactly what user typed via cin (word token)
};

static std::map<std::string, Value> SYM;

/* helpers to classify a token read by cin >> token */
static bool is_int_tok(const std::string& s) {
  if (s.empty()) return false;
  size_t i = (s[0]=='+'||s[0]=='-') ? 1 : 0;
  if (i>=s.size()) return false;
  for (; i<s.size(); ++i) if (s[i]<'0'||s[i]>'9') return false;
  return true;
}
static bool is_double_tok(const std::string& s) {
  bool dot=false, digit=false;
  size_t i = (s[0]=='+'||s[0]=='-') ? 1 : 0;
  for (; i<s.size(); ++i) {
    if (s[i]=='.') { if (dot) return false; dot=true; }
    else if (s[i]>='0'&&s[i]<='9') digit=true;
    else return false;
  }
  return digit && dot;
}

/* When outputting cout << expr; if expr was a lone IDENT with non-int,
   we want to print the raw token instead of an integer.
   We'll set this flag only for IDENT leafs; arithmetic resets it. */
static bool g_lastExprWasNonIntIdent = false;
static std::string g_lastExprRaw;
%}

/* ---- semantic values ---- */
%union {
    int   ival;
    char* sval;
}

%token CIN COUT SHIFTIN SHIFTOUT
%token <sval> IDENT
%token <ival> NUMBER
%token <sval> STRINGLIT

%left '+'
%type <ival> expr

%%

program
  : stmt_list
  ;

stmt_list
  : /* empty */
  | stmt_list stmt
  ;

stmt
  : input_stmt  ';'
  | output_stmt ';'
  | assign_stmt ';'
  ;

/* cin >> IDENT;  */
input_stmt
  : CIN SHIFTIN IDENT
    {
      std::string tok;
      std::cin >> tok;
      Value v; v.raw = tok;
      if (is_int_tok(tok)) {
        v.kind = Kind::INTK; v.i = std::stoi(tok);
      } else if (is_double_tok(tok)) {
        v.kind = Kind::DOUBLEK; v.d = std::stod(tok);
      } else if (tok.size()==1) {
        v.kind = Kind::CHARK; v.i = static_cast<unsigned char>(tok[0]);
      } else {
        v.kind = Kind::STRINGK;
      }
      SYM[std::string($3)] = v;
      free($3);
    }
  ;

/* cout << expr;   OR   cout << "hello"; */
output_stmt
  : COUT SHIFTOUT expr
    {
      if (g_lastExprWasNonIntIdent) {
        std::cout << g_lastExprRaw << std::endl;
      } else {
        std::cout << $3 << std::endl;
      }
      g_lastExprWasNonIntIdent = false;
      g_lastExprRaw.clear();
    }
  | COUT SHIFTOUT STRINGLIT
    { std::cout << $3 << std::endl; free($3); }
  ;

/* IDENT = expr;  (assignment remains int-only per spec for PL2) */
assign_stmt
  : IDENT '=' expr
    {
      Value v; v.kind = Kind::INTK; v.i = $3; v.raw = std::to_string($3);
      SYM[std::string($1)] = v;
      free($1);
    }
  ;

/* expressions (arithmetic is int-only, as spec says “addition of single-digit operands”) */
expr
  : NUMBER
    { $$ = $1; g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear(); }
  | IDENT
    {
      auto name = std::string($1);
      auto it = SYM.find(name);
      if (it == SYM.end()) {
        $$ = 0;
        g_lastExprWasNonIntIdent = true; g_lastExprRaw = ""; // unknown; print empty
      } else {
        const Value& v = it->second;
        switch (v.kind) {
          case Kind::INTK:
            $$ = v.i;
            g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear();
            break;
          case Kind::DOUBLEK:
          case Kind::CHARK:
          case Kind::STRINGK:
            /* For arithmetic, treat as 0. For PL1 printing a lone IDENT, remember raw. */
            $$ = 0;
            g_lastExprWasNonIntIdent = true; g_lastExprRaw = v.raw;
            break;
        }
      }
      free($1);
    }
  | expr '+' expr
    { $$ = $1 + $3; g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear(); }
      | expr '-' expr
    { $$ = $1 - $3; g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear(); }
  | expr '*' expr
    { $$ = $1 * $3; g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear(); }
  | expr '/' expr
    { $$ = $1 / $3; g_lastExprWasNonIntIdent = false; g_lastExprRaw.clear(); }
  ;
  ;

%%

/* int yyparse(void);  // optional */
