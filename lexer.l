%option noyywrap  /* Disable the use of the yywrap function */
%option nounput noinput  /* Disable the input handling function */

/* Declaration section */
%{
#include "parser.hpp"  // Includes the header for the parser
#include <cstdlib>     // For atoi and malloc functions
#include <cstring>     // For string manipulation functions

/* Unquote "..." and unescape \n, \t, \", \\ */
static char* unescape_and_unquote(const char* yy) {
    size_t n = std::strlen(yy);  // Get the length of the input string
    if (n < 2 || yy[0] != '"' || yy[n-1] != '"') {  // If string is not quoted, return a copy of the original
        char* s = (char*)std::malloc(n + 1);  // Allocate memory for the string
        std::memcpy(s, yy, n + 1);  // Copy the string
        return s;
    }
    char* out = (char*)std::malloc(n - 1);  // Allocate memory for the unquoted string
    size_t oi = 0;
    for (size_t i = 1; i < n - 1; ++i) {  // Loop through the string and unescape characters
        char c = yy[i];
        if (c == '\\' && i + 1 < n - 1) {  // Handle escape sequences
            char nx = yy[++i];
            switch (nx) {
                case 'n':  out[oi++] = '\n'; break;  // Handle newline escape
                case 't':  out[oi++] = '\t'; break;  // Handle tab escape
                case '\\': out[oi++] = '\\'; break;  // Handle backslash escape
                case '"':  out[oi++] = '"';  break;  // Handle quote escape
                default:   out[oi++] = nx;   break;  // Default case for other characters
            }
        } else {
            out[oi++] = c;  // Copy non-escaped characters
        }
    }
    out[oi] = '\0';  // Null terminate the string
    return out;  // Return the unescaped and unquoted string
}
%}

/* Define regular expressions (patterns) for tokens */
WS      [ \t\r\n]+  /* Matches whitespace characters (spaces, tabs, newlines, carriage returns) */
ID      [a-zA-Z_][a-zA-Z0-9_]*  /* Matches identifiers (alphanumeric starting with a letter or underscore) */
INT     [0-9]+  /* Matches integer numbers (one or more digits) */
STRING  \"([^\\\"]|\\.)*\"  /* Matches string literals (enclosed in double quotes, with escaped characters) */

%%

{WS}                /* skip */ ;  // Skip whitespace characters (do nothing)

"cin"               { return CIN; }  // Matches 'cin' and returns the CIN token
"cout"              { return COUT; }  // Matches 'cout' and returns the COUT token
">>"                { return SHIFTIN; }  // Matches '>>' and returns the SHIFTIN token
"<<"                { return SHIFTOUT; }  // Matches '<<' and returns the SHIFTOUT token
"="                 { return '='; }  // Matches '=' and returns the '=' token
"+"                 { return '+'; }  // Matches '+' and returns the '+' token
";"                 { return ';'; }  // Matches ';' and returns the ';' token

{INT}               { yylval.ival = std::atoi(yytext); return NUMBER; }  // Matches an integer and returns the NUMBER token with its value
{STRING}            { yylval.sval = unescape_and_unquote(yytext); return STRINGLIT; }  // Matches a string literal and returns the STRINGLIT token with its value
{ID}                { yylval.sval = strdup(yytext); return IDENT; }  // Matches an identifier and returns the IDENT token with its value

.                   { return *yytext; }  // Matches any other character and returns its ASCII value as a token

%%  // End of the lexer rules
