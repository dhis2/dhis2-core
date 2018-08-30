// ANTLR V4 grammar file to define DHIS2 epression syntax

grammar Expression;

// -----------------------------------------------------------------------------
// Parser rules
// -----------------------------------------------------------------------------

expr
    :   '(' expr ')'

    // Logical functions

    |   expr '.' fun='except' a1
    |   fun='if' a3
    |   fun='isNull' a1
    |   fun='coalesce' a1_n

    // Aggregation functions

    |   expr '.' fun='sum' a0
    |   expr '.' fun='max' a0
    |   expr '.' fun='min' a0
    |   expr '.' fun='average' a0
    |   expr '.' fun='stddev' a0
    |   expr '.' fun='variance' a0
    |   expr '.' fun='median' a0
    |   expr '.' fun='last' a0_1
    |   expr '.' fun='percentile' a1
    |   expr '.' fun='rankHigh' a1
    |   expr '.' fun='rankLow' a1
    |   expr '.' fun='rankPercentile' a1

    // Aggregation scope functions

    |   expr '.' fun='period' a1_n
    |   expr '.' fun='ouLevel' a1_2
    |   expr '.' fun='ouAncestor' a1
    |   expr '.' fun='ouDescendant' a1_2
    |   expr '.' fun='ouPeer' a1_2
    |   expr '.' fun='ouGroup' a1_n

    // Operators (in precidence order)

    |   <assoc=right> expr fun='^' expr
    |   fun=('-'|'!') expr
    |   '+' expr // (Doesn't change the expression)
    |   expr fun=('*'|'/'|'%') expr
    |   expr fun=('+'|'-') expr
    |   expr fun=('<=' | '>=' | '<' | '>') expr
    |   expr fun=('==' | '!=') expr
    |   expr fun='&&' expr
    |   expr fun='||' expr

    // Other

    |   value
    |   programIndicatorExpr
    ;

value
    :   dimensionItemObject
    |   orgUnitCount
    |   reportingRate
    |   constant
    |   days
    |   numericLiteral
    |   stringLiteral
    |   booleanLiteral
    ;

programIndicatorExpr
    :   'V{' programIndicatorVariable '}'
    |   'd2:' programIndicatorFunction
    ;

programIndicatorVariable
    :   var='event_date'
    |   var='due_date'
    |   var='incident_date'
    |   var='enrollment_date'
    |   var='enrollment_status'
    |   var='current_date'
    |   var='value_count'
    |   var='zero_pos_value_count'
    |   var='event_count'
    |   var='enrollment_count'
    |   var='tei_count'
    |   var='program_stage_name'
    |   var='program_stage_id'
    |   var='reporting_period_start'
    |   var='reporting_period_end'
    ;

programIndicatorFunction
    :   fun='hasValue' a1
    |   fun='minutesBetween' a2
    |   fun='daysBetween' a2
    |   fun='weeksBetween' a2
    |   fun='monthsBetween' a2
    |   fun='yearsBetween' a2
    |   fun='condition' a3
    |   fun='zing' a1
    |   fun='oizp' a1
    |   fun='zpvc' a1_n
    ;

a0  // 0 arguments
    :   '(' ')'
    ;

a0_1 // 0 - 1 arguments
    :   '(' expr? ')'
    ;

a1  // 1 argument
    :   '(' expr ')'
    ;

a1_2 // 1 - 2 arguments
    :   '(' expr (',' expr )? ')'
    ;

a1_n // 1 - n arguments
    :   '(' expr (',' expr )* ')'
    ;

a2  // 2 arguments
    :   '(' expr ',' expr ')'
    ;

a3  // 3 arguments
    :   '(' expr ',' expr ',' expr ')'
    ;

numericLiteral
    :   NUMERIC_LITERAL
    ;

stringLiteral
    :   STRING_LITERAL
    ;

booleanLiteral
    :   BOOLEAN_LITERAL
    ;

dimensionItemObject
    :   dataElementOperand
    |   programDataElement
    |   programTrackedEntityAttribute
    |   programIndicator
    ;

dataElementOperand
    :   '#{' UID ('.' UID ('.' UID)? )? '}'
    ;

programDataElement
    :   'D{' UID ('.' UID)? '}'
    ;

programTrackedEntityAttribute
    :   'A{' UID '.' UID '}'
    ;

programIndicator
    :   'I{' UID '}'
    ;

orgUnitCount
    :   'OUG{' UID '}'
    ;

reportingRate
    :   'R{' UID '.REPORTING_RATE}'
    ;

constant
    :   'C{' UID '}'
    ;

days
    :   '[days]'
    ;

// -----------------------------------------------------------------------------
// Assign token names to parser symbols
// -----------------------------------------------------------------------------

// Operators

POWER : '^';
MINUS : '-';
PLUS :  '+';
NOT :   '!';
MUL :   '*';
DIV :   '/';
MOD :   '%';
LEQ :   '<=';
GEQ :   '>=';
LT  :   '<';
GT  :   '>';
EQ  :   '==';
NE  :   '!=';
AND :   '&&';
OR  :   '||';

// Logical functions

IF: 'if';
EXCEPT: 'except';
IS_NULL: 'isNull';
COALESCE: 'coalesce';

// Aggregation functions

SUM: 'sum';
AVERAGE: 'average';
LAST: 'last';
COUNT: 'count';
STDDEV: 'stddev';
VARIANCE: 'variance';
MIN: 'min';
MAX: 'max';
MEDIAN: 'median';
PERCENTILE: 'percentile';
RANK_HIGH: 'rankHigh';
RANK_LOW: 'rankLow';
RANK_PERCENTILE: 'rankPercentile';

// Aggregation scope functions

PERIOD: 'period';
OU_LEVEL: 'ouLevel';
OU_ANCESTOR : 'ouAncestor';
OU_DESCENDANT: 'ouDescendant';
OU_PEER: 'ouPeer';
OU_GROUP: 'ouGroup';

// Program indicator variables

EVENT_DATE : 'event_date';
DUE_DATE : 'due_date';
INCIDENT_DATE : 'incident_date';
ENROLLMENT_DATE : 'enrollment_date';
ENROLLMENT_STATUS : 'enrollment_status';
CURRENT_STATUS : 'current_date';
VALUE_COUNT : 'value_count';
ZERO_POS_VALUE_COUNT : 'zero_pos_value_count';
EVENT_COUNT : 'event_count';
ENROLLMENT_COUNT : 'enrollment_count';
TEI_COUNT : 'tei_count';
PROGRAM_STAGE_NAME : 'program_stage_name';
PROGRAM_STAGE_ID : 'program_stage_id';
REPORTING_PERIOD_START : 'reporting_period_start';
REPORTING_PERIOD_END : 'reporting_period_end';

// Program indicator functions

HAS_VALUE : 'hasValue';
MINUTES_BETWEEN : 'minutesBetween';
DAYS_BETWEEN : 'daysBetween';
WEEKS_BETWEEN : 'weeksBetween';
MONTHS_BETWEEN : 'monthsBetween';
YEARS_BETWEEN : 'yearsBetween';
CONDITION : 'condition';
ZING : 'zing';
OIZP : 'oizp';
ZPVC :  'zpvc';

// -----------------------------------------------------------------------------
// Lexer rules
// -----------------------------------------------------------------------------

NUMERIC_LITERAL
    :   ('0' | [1-9] [0-9]*) ('.' [0-9]*)? Exponent?
    |   '.' [0-9]+ Exponent?
    ;

STRING_LITERAL
    :   '"' (~["\\\r\n] | EscapeSequence)* '"'
    ;

BOOLEAN_LITERAL
    :   [Tt][Rr][Uu][Ee]
    |   [Ff][Aa][Ll][Ss][Ee]
    ;

UID :   Alpha
        AlphaNum AlphaNum AlphaNum AlphaNum AlphaNum
        AlphaNum AlphaNum AlphaNum AlphaNum AlphaNum
    ;

WS  :   [ \t\n\r]+ -> skip // toss out all whitespace
    ;

// Lexer fragments

fragment Exponent
    : ('e'|'E') ('+'|'-')? [0-9]+
    ;

fragment Alpha
    :    [a-zA-Z]
    ;

fragment AlphaNum
    :   [a-zA-Z0-9]
    ;

fragment EscapeSequence
    : '\\' [btnfr"'\\]
    | '\\' ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
    ;

fragment HexDigit
    : [0-9a-fA-F]
    ;
