grammar JsonFilteringExpression;

// Parser Rules ---------------------------------------------------------------

parse
    : expression_list EOF
    ;

expression_list
    : expression (',' expression)*
    ;

expression
    : negated_expression
    | field_list (nested_expression|empty_nested_expression)
    | dot_path (nested_expression|empty_nested_expression)
    | dot_path
    | field
    | deep
    ;

field_list
    : '(' field (('|'|',') field)* ')'
    | field
    ;

negated_expression
    : '-' field
    | '-' dot_path
    ;

nested_expression
    : LCRBRACE expression_list RCRBRACE
    | LBRACE expression_list RBRACE
    ;

empty_nested_expression
    : LCRBRACE RCRBRACE
    | LBRACE RBRACE
    ;

deep
    : WILDCARD_DEEP
    ;

dot_path
    : field ('.' field)+
    ;

field
    : exact_field
    | regex_field
    | wildcard_shallow_field
    | wildcard_field
    ;

exact_field
    : IDENTIFIER ('-' | IDENTIFIER)*
    ;

regex_field
    : '~' regex_pattern '~' regex_flag*
    | '/' regex_pattern '/' regex_flag*
    ;

regex_pattern
    : ('.' | '|' | ',' | LCRBRACE | RCRBRACE | LBRACE | RBRACE | '-' | REGEX_CHAR  | IDENTIFIER | WILDCARD_SHALLOW)+
    ;

regex_flag
    : 'i'
    ;

wildcard_field
   : exact_field wildcard_char
   | exact_field (wildcard_char exact_field)+ wildcard_char?
   | wildcard_char exact_field
   | wildcard_char (exact_field wildcard_char)+ exact_field?
   ;

wildcard_char
   : WILDCARD_SHALLOW
   | '?'
   ;

wildcard_shallow_field
    : WILDCARD_SHALLOW
    ;


// Lexer Tokens ---------------------------------------------------------------

fragment DIGIT
    : [0-9]
    ;

fragment LETTER
    : [a-zA-Z]
    ;

fragment VALID_FIELD_CHAR
    : (LETTER | DIGIT | '$' | '_')
    ;

LBRACE
    : '['
    ;

RBRACE
    : ']'
    ;

LCRBRACE
    : '{'
    ;

RCRBRACE
    : '}'
    ;

IDENTIFIER
    : VALID_FIELD_CHAR (VALID_FIELD_CHAR)*
    ;

WILDCARD_SHALLOW
    : '*'
    ;

WILDCARD_DEEP
    : '**'
    ;

REGEX_CHAR
    : ~('~' | '/')
    ;
