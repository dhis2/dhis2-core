// ANTLR V4 grammar file to define DHIS2 epression syntax

grammar Expression;

// -----------------------------------------------------------------------------
// Parser rules
// -----------------------------------------------------------------------------

expression  // The expression must last until the end of the string
    :   expr EOF
    ;

expr
    // Allow whitespace on either side of any expression

    :   WS+ expr
    |   expr WS+

    // Operators (in precidence order)

    |   op='(' expr ')'
    |   <assoc=right> expr op='^' expr
    |   op=('+' | '-' | '!' | 'not') expr
    |   expr op=('*' | '/' | '%') expr
    |   expr op=('+' | '-') expr
    |   expr op=('<' | '>' | '<=' | '>=') expr
    |   expr op=('==' | '!=') expr
    |   expr op=('&&' | 'and') expr
    |   expr op=('||' | 'or') expr

    // Others

    |   function
    |   item
    |   programVariable
    |   programFunction
    |   literal
    ;

function // (alphabtical)
    :   fun='firstNonNull' '(' WS* itemNumStringLiteral WS* (',' WS* itemNumStringLiteral WS* )* ')'
    |   fun='greatest' '(' expr (',' expr )* ')'
    |   fun='if' '(' expr ',' expr ',' expr ')'
    |   fun='isNotNull' '(' WS* item WS* ')'
    |   fun='isNull' '(' WS* item WS* ')'
    |   fun='least' '(' expr (',' expr )* ')'
    ;

item // (alphabtical)
    :   it='#{' uid0=UID '}'
    |   it='#{' uid0=UID '.' uid1=UID '}'
    |   it='#{' uid0=UID '.' uid1=UID wild2='.*' '}'
    |   it='#{' uid0=UID '.*.' uid2=UID '}'
    |   it='#{' uid0=UID '.' uid1=UID '.' uid2=UID '}'
    |   it='A{' uid0=UID '.' uid1=UID '}' // Program attribute in expressions (indicator, etc.)
    |   it='A{' uid0=UID '}' // Program attribute in program indicator expressions
    |   it='C{' uid0=UID '}'
    |   it='D{' uid0=UID '.' uid1=UID '}'
    |   it='I{' uid0=UID '}'
    |   it='N{' uid0=UID '}' // Indicator
    |   it='OUG{' uid0=UID '}'
    |   it='R{' uid0=UID '.' REPORTING_RATE_TYPE '}'
    |   it='[days]'
    ;

programVariable // (alphabtical)
    :   'V{' var='analytics_period_end' '}'
    |   'V{' var='analytics_period_start' '}'
    |   'V{' var='creation_date' '}'
    |   'V{' var='current_date' '}'
    |   'V{' var='due_date' '}'
    |   'V{' var='enrollment_count' '}'
    |   'V{' var='enrollment_date' '}'
    |   'V{' var='enrollment_status' '}'
    |   'V{' var='event_count' '}'
    |   'V{' var='event_date' '}'
    |   'V{' var='execution_date' '}'
    |   'V{' var='incident_date' '}'
    |   'V{' var='org_unit_count' '}'
    |   'V{' var='program_stage_id' '}'
    |   'V{' var='program_stage_name' '}'
    |   'V{' var='sync_date' '}'
    |   'V{' var='tei_count' '}'
    |   'V{' var='value_count' '}'
    |   'V{' var='zero_pos_value_count' '}'
    ;

programFunction // (alphabetical)
    :   d2='d2:condition(' WS* stringLiteral WS* ',' expr ',' expr ')'
    |   d2='d2:count(' WS* stageDataElement WS* ')'
    |   d2='d2:countIfCondition(' WS* stageDataElement ',' WS* stringLiteral WS* ')'
    |   d2='d2:countIfValue(' WS* stageDataElement WS* ',' WS* numStringLiteral WS*  ')'
    |   d2='d2:daysBetween(' compareDate ',' compareDate ')'
    |   d2='d2:hasValue(' item ')'
    |   d2='d2:minutesBetween(' compareDate ',' compareDate ')'
    |   d2='d2:monthsBetween(' compareDate ',' compareDate ')'
    |   d2='d2:oizp(' expr ')'
    |   d2='d2:relationshipCount(' WS* QUOTED_UID? WS* ')'
    |   d2='d2:weeksBetween(' compareDate ',' compareDate ')'
    |   d2='d2:yearsBetween(' compareDate ',' compareDate ')'
    |   d2='d2:zing(' expr ')'
    |   d2='d2:zpvc(' item (',' item )* ')'

    // program functions for aggregation

    |   d2='avg(' expr ')'
    |   d2='count(' expr ')'
    |   d2='max(' expr ')'
    |   d2='min(' expr ')'
    |   d2='stddev(' expr ')'
    |   d2='sum(' expr ')'
    |   d2='variance(' expr ')'
    ;

stageDataElement
    :   '#{' uid0=UID '.' uid1=UID '}'
    ;

programAttribute
    :   'A{' uid0=UID '}'
    ;

compareDate
    :   expr
    |   WS* 'PS_EVENTDATE:' WS* uid0=UID WS*
    ;

itemNumStringLiteral
    :   item
    |   numStringLiteral
    ;

numStringLiteral
    :   numericLiteral
    |   stringLiteral
    ;

literal
    :   numericLiteral
    |   stringLiteral
    |   BOOLEAN_LITERAL
    ;

numericLiteral
    :   NUMERIC_LITERAL
    ;

stringLiteral
    :   STRING_LITERAL
    |   QUOTED_UID // Resolve that quoted UID can also be a string literal
    ;

// -----------------------------------------------------------------------------
// Assign token names to parser symbols
// -----------------------------------------------------------------------------

// Operators

PAREN : '(';
PLUS  : '+';
MINUS : '-';
POWER : '^';
MUL   : '*';
DIV   : '/';
MOD   : '%';
EQ    : '==';
NE    : '!=';
GT    : '>';
LT    : '<';
GEQ   : '>=';
LEQ   : '<=';
NOT   : 'not';
AND   : 'and';
OR    : 'or';
EXCLAMATION_POINT   : '!';
AMPERSAND_2         : '&&';
VERTICAL_BAR_2      : '||';

// Functions (alphabetical)

FIRST_NON_NULL  : 'firstNonNull';
GREATEST        : 'greatest';
IF              : 'if';
IS_NOT_NULL     : 'isNotNull';
IS_NULL         : 'isNull';
LEAST           : 'least';

// Items (alphabetical by symbol)

HASH_BRACE  : '#{';
A_BRACE     : 'A{';
C_BRACE     : 'C{';
D_BRACE     : 'D{';
I_BRACE     : 'I{';
N_BRACE     : 'N{';
OUG_BRACE   : 'OUG{';
R_BRACE     : 'R{';
DAYS        : '[days]';

// Program variables (alphabetical)

V_ANALYTICS_PERIOD_END  : 'analytics_period_end';
V_ANALYTICS_PERIOD_START: 'analytics_period_start';
V_CREATION_DATE         : 'creation_date';
V_CURRENT_DATE          : 'current_date';
V_DUE_DATE              : 'due_date';
V_ENROLLMENT_COUNT      : 'enrollment_count';
V_ENROLLMENT_DATE       : 'enrollment_date';
V_ENROLLMENT_STATUS     : 'enrollment_status';
V_EVENT_COUNT           : 'event_count';
V_EVENT_DATE            : 'event_date';
V_EXECUTION_DATE        : 'execution_date';
V_INCIDENT_DATE         : 'incident_date';
V_ORG_UNIT_COUNT        : 'org_unit_count';
V_PROGRAM_STAGE_ID      : 'program_stage_id';
V_PROGRAM_STAGE_NAME    : 'program_stage_name';
V_SYNC_DATE             : 'sync_date';
V_TEI_COUNT             : 'tei_count';
V_VALUE_COUNT           : 'value_count';
V_ZERO_POS_VALUE_COUNT  : 'zero_pos_value_count';

// Program functions (alphabetical)

D2_CONDITION            : 'd2:condition(';
D2_COUNT                : 'd2:count(';
D2_COUNT_IF_CONDITION   : 'd2:countIfCondition(';
D2_COUNT_IF_VALUE       : 'd2:countIfValue(';
D2_DAYS_BETWEEN         : 'd2:daysBetween(';
D2_HAS_VALUE            : 'd2:hasValue(';
D2_MINUTES_BETWEEN      : 'd2:minutesBetween(';
D2_MONTHS_BETWEEN       : 'd2:monthsBetween(';
D2_OIZP                 : 'd2:oizp(';
D2_RELATIONSHIP_COUNT   : 'd2:relationshipCount(';
D2_WEEKS_BETWEEN        : 'd2:weeksBetween(';
D2_YEARS_BETWEEN        : 'd2:yearsBetween(';
D2_ZING                 : 'd2:zing(';
D2_ZPVC                 : 'd2:zpvc(';

// Program functions for aggregation

AVG                     : 'avg(';
COUNT                   : 'count(';
MAX                     : 'max(';
MIN                     : 'min(';
STDDEV                  : 'stddev(';
SUM                     : 'sum(';
VARIANCE                : 'variance(';

// -----------------------------------------------------------------------------
// Lexer rules
//
// Some expression characters are grouped into lexer tokens before parsing.
// If a sequence of characters from the expression matches more than one
// lexer rule, the first lexer rule is used.
// -----------------------------------------------------------------------------

REPORTING_RATE_TYPE
    :   'REPORTING_RATE'
    |   'REPORTING_RATE_ON_TIME'
    |   'ACTUAL_REPORTS'
    |   'ACTUAL_REPORTS_ON_TIME'
    |   'EXPECTED_REPORTS'
    ;

NUMERIC_LITERAL
    :   ('0' | [1-9] [0-9]*) ('.' [0-9]*)? Exponent?
    |   '.' [0-9]+ Exponent?
    ;

BOOLEAN_LITERAL
    :   'true'
    |   'false'
    ;

// Quoted UID could also be a string literal. It must come first.
QUOTED_UID
    :   Q1 UID Q1
    |   Q2 UID Q2
    ;

STRING_LITERAL
    :   Q1 (~['\\\r\n] | EscapeSequence)* Q1
    |   Q2 (~["\\\r\n] | EscapeSequence)* Q2
    ;

Q1  :   '\'' // Single quote
    ;

Q2  :   '"' // Double quote
    ;

UID :   Alpha
        AlphaNum AlphaNum AlphaNum AlphaNum AlphaNum
        AlphaNum AlphaNum AlphaNum AlphaNum AlphaNum
    ;

// IDENTIFIER has the effect of requiring spaces between words,
// for example disallows notisNull (should be not isNull),
// but allows !isNull.
IDENTIFIER
    : [a-zA-Z]+
    ;

WS  :   [ \t\n\r]+
    ;

// Lexer fragments

fragment Exponent
    : ('e'|'E') ('+'|'-')? [0-9]+
    ;

fragment Alpha
    :   [a-zA-Z]
    ;

fragment AlphaNum
    :   [a-zA-Z0-9]
    ;

fragment EscapeSequence
    :   '\\' [btnfr"'\\]
    |   '\\' ([0-3]? [0-7])? [0-7]
    |   '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
    ;

fragment HexDigit
    :   [0-9a-fA-F]
    ;
