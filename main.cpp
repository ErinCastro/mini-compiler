#include <cstdio>
#include <iostream>

extern int yyparse();
extern FILE* yyin;

int main(int argc, char** argv) {
    if (argc > 1) {
        yyin = std::fopen(argv[1], "r");
        if (!yyin) {
            std::perror("fopen");
            return 1;
        }
    }
    int rc = yyparse();
    if (yyin) std::fclose(yyin);
    return rc;
}
