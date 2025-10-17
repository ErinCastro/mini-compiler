%{
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <string>

extern FILE* yyin;
int yylex(void);
void yyerror(const char* s) { std::cerr << "Parse error: " << s << std::endl; }
%}

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
  : input_stmt ';'
  | output_stmt ';'
  | assign_stmt ';'
  ;

input_stmt
  : CIN SHIFTIN IDENT
    { std::cout << "[INPUT] cin >> " << $3 << "\n"; free($3); }
  ;

output_stmt
  : COUT SHIFTOUT expr
    { std::cout << "[OUTPUT] cout << <expr>\n"; }
  | COUT SHIFTOUT STRINGLIT
    { std::cout << "[OUTPUT] cout << " << $3 << "\n"; free($3); }
  ;

assign_stmt
  : IDENT '=' expr
    { std::cout << "[ASSIGN] " << $1 << " = <expr>\n"; free($1); }
  ;

expr
  : NUMBER                { $$ = $1; }
  | IDENT                 { $$ = 0; free($1); }
  | expr '+' expr         { $$ = 0; }
  ;

%%

int yyparse(void);