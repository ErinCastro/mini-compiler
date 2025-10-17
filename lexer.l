%option noyywrap
%option nounput noinput

%{
#include "parser.hpp"
#include <cstdlib>
#include <cstring>

static char* dup_yytext() {
    size_t n = std::strlen(yytext);
    char* s = (char*)std::malloc(n + 1);
    std::memcpy(s, yytext, n + 1);
    return s;
}
%}

WS      [ \t\r\n]+
ID      [a-zA-Z_][a-zA-Z0-9_]*
INT     [0-9]+
STRING  \"([^\\\"]|\\.)*\"

%%

{WS}                /* skip */ ;

"cin"               { return CIN; }
"cout"              { return COUT; }
">>"                { return SHIFTIN; }
"<<"                { return SHIFTOUT; }
"="                 { return '='; }
"+"                 { return '+'; }
";"                 { return ';'; }

{INT}               { yylval.ival = std::atoi(yytext); return NUMBER; }
{STRING}            { yylval.sval = dup_yytext(); return STRINGLIT; }
{ID}                { yylval.sval = dup_yytext(); return IDENT; }

.                   { return *yytext; }

%%