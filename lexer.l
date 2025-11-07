%option noyywrap
%option nounput noinput

%{
#include "parser.hpp"
#include <cstdlib>
#include <cstring>

/* Unquote "..." and unescape \n, \t, \", \\ */
static char* unescape_and_unquote(const char* yy) {
    size_t n = std::strlen(yy);
    if (n < 2 || yy[0] != '"' || yy[n-1] != '"') {
        char* s = (char*)std::malloc(n + 1);
        std::memcpy(s, yy, n + 1);
        return s;
    }
    char* out = (char*)std::malloc(n - 1); // remove 2 quotes + null
    size_t oi = 0;
    for (size_t i = 1; i < n - 1; ++i) {
        char c = yy[i];
        if (c == '\\' && i + 1 < n - 1) {
            char nx = yy[++i];
            switch (nx) {
                case 'n':  out[oi++] = '\n'; break;
                case 't':  out[oi++] = '\t'; break;
                case '\\': out[oi++] = '\\'; break;
                case '"':  out[oi++] = '"';  break;
                default:   out[oi++] = nx;   break;
            }
        } else {
            out[oi++] = c;
        }
    }
    out[oi] = '\0';
    return out;
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
"-"                 { return '-'; } 
"*"                 { return '*'; }  
"/"                 { return '/'; } 

{INT}               { yylval.ival = std::atoi(yytext); return NUMBER; }
{STRING}            { yylval.sval = unescape_and_unquote(yytext); return STRINGLIT; }
{ID}                { yylval.sval = strdup(yytext); return IDENT; }

.                   { return *yytext; }

%%
