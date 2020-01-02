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

    //  Operators (in precidence order)

    |   fun='(' expr ')'
    |   <assoc=right> expr fun='^' expr
    |   fun=('+' | '-' | '!' | 'not') expr
    |   expr fun=('*' | '/' | '%') expr
    |   expr fun=('+' | '-') expr
    |   expr fun=('<' | '>' | '<=' | '>=') expr
    |   expr fun=('==' | '!=') expr
    |   expr fun=('&&' | 'and') expr
    |   expr fun=('||' | 'or') expr

    //  Functions (alphabetical)

    |   fun='firstNonNull(' WS* itemNumStringLiteral WS* (',' WS* itemNumStringLiteral WS* )* ')'
    |   fun='greatest(' expr (',' expr )* ')'
    |   fun='if(' expr ',' expr ',' expr ')'
    |   fun='isNotNull(' WS* item WS* ')'
    |   fun='isNull(' WS* item WS* ')'
    |   fun='least(' expr (',' expr )* ')'

    //  Aggergation functions (alphabetical)

    |   fun='avg(' expr ')'
    |   fun='count(' expr ')'
    |   fun='max(' expr ')'
    |   fun='median(' expr ')'
    |   fun='min(' expr ')'
    |   fun='percentileCont(' expr ',' expr ')'
    |   fun='stddev(' expr ')'
    |   fun='stddevPop(' expr ')'
    |   fun='stddevSamp(' expr ')'
    |   fun='sum(' expr ')'
    |   fun='variance(' expr ')'

    //  Program variables (alphabtical)

    |   'V{' fun='analytics_period_end' '}'
    |   'V{' fun='analytics_period_start' '}'
    |   'V{' fun='creation_date' '}'
    |   'V{' fun='current_date' '}'
    |   'V{' fun='due_date' '}'
    |   'V{' fun='enrollment_count' '}'
    |   'V{' fun='enrollment_date' '}'
    |   'V{' fun='enrollment_id' '}'
    |   'V{' fun='enrollment_status' '}'
    |   'V{' fun='environment' '}'
    |   'V{' fun='event_count' '}'
    |   'V{' fun='event_date' '}'
    |   'V{' fun='event_id' '}'
    |   'V{' fun='event_status' '}'
    |   'V{' fun='execution_date' '}'
    |   'V{' fun='incident_date' '}'
    |   'V{' fun='org_unit_count' '}'
    |   'V{' fun='org_unit' '}'
    |   'V{' fun='orgunit_code' '}'
    |   'V{' fun='program_name' '}'
    |   'V{' fun='program_stage_id' '}'
    |   'V{' fun='program_stage_name' '}'
    |   'V{' fun='sync_date' '}'
    |   'V{' fun='tei_count' '}'
    |   'V{' fun='value_count' '}'
    |   'V{' fun='zero_pos_value_count' '}'

    //  Program functions (alphabetical)
    |   fun='d2:ceil(' expr ')'
    |   fun='d2:floor(' expr ')'
    |   fun='d2:addDays(' expr ',' expr ')'
    |   fun='d2:concatenate(' expr ',' expr * ')'
    |   fun='d2:countIfZeroPos(' expr ')'
    |   fun='d2:substring(' expr ',' expr ',' expr ')'
    |   fun='d2:length(' expr ')'
    |   fun='d2:left(' expr ',' expr ')'
    |   fun='d2:right(' expr ',' expr ')'
    |   fun='d2:modulus(' expr ',' expr ')'
    |   fun='d2:round(' expr ')'
    |   fun='d2:condition(' WS* stringLiteral WS* ',' expr ',' expr ')'
    |   fun='d2:zScoreHFA(' expr ',' expr ',' expr ')'
    |   fun='d2:zScoreWFA(' expr ',' expr ',' expr ')'
    |   fun='d2:zScoreWFH(' expr ',' expr ',' expr ')'
    |   fun='d2:split(' expr ',' expr ',' expr ')'
    |   fun='d2:count(' ( WS* stageDataElement WS* | expr ) ')'
    |   fun='d2:countIfCondition(' WS* stageDataElement ',' WS* stringLiteral WS* ')'
    |   fun='d2:countIfValue(' ( WS* stageDataElement WS* | expr ) ',' ( WS* numStringLiteral WS* | expr ) ')'
    |   fun='d2:daysBetween(' compareDate ',' compareDate ')'
    |   fun='d2:hasValue(' ( WS* item WS* | expr ) ')'
    |   fun='d2:maxValue(' ( item | expr | compareDate ) ')'
    |   fun='d2:minutesBetween(' compareDate ',' compareDate ')'
    |   fun='d2:minValue(' ( item | expr | compareDate ) ')'
    |   fun='d2:monthsBetween(' compareDate ',' compareDate ')'
    |   fun='d2:oizp(' expr ')'
    |   fun='d2:relationshipCount(' WS* QUOTED_UID? WS* ')'
    |   fun='d2:weeksBetween(' compareDate ',' compareDate ')'
    |   fun='d2:yearsBetween(' compareDate ',' compareDate ')'
    |   fun='d2:zing(' expr ')'
    |   fun='d2:zpvc(' ( WS* item WS* | expr) (',' ( WS* item WS* | expr ) )* ')'
    |   fun='d2:validatePattern(' expr ',' expr ')'
    |   fun='d2:hasUserRole(' expr ')'
    |   fun='d2:inOrgUnitGroup(' expr ')'
    |   fun='d2:lastEventDate(' expr ')'

    //  Other

    |   item
    |   programRuleVariable
    |   constantValue
    |   numericLiteral
    |   stringLiteral
    |   booleanLiteral
    |   empty
    ;

item
    :   it='#{' uid0=UID ('.*')? '}'
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

programRuleVariable
    : var='X{' string=variableName '}';

constantValue
    : var='C{' string=variableName '}';

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

numericLiteral
    :   NUMERIC_LITERAL
    ;

stringLiteral
    :   STRING_LITERAL
    |   QUOTED_UID // Resolve that quoted UID can also be a string literal
    ;

variableName
    : UID
    | IDENTIFIER
    | UNQUOTED_STRING;

booleanLiteral
    :   BOOLEAN_LITERAL
    ;

empty
    : EMPTY;

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

FIRST_NON_NULL  : 'firstNonNull(';
GREATEST        : 'greatest(';
IF              : 'if(';
IS_NOT_NULL     : 'isNotNull(';
IS_NULL         : 'isNull(';
LEAST           : 'least(';

// Aggegation functions (alphabetical)

AVG             : 'avg(';
COUNT           : 'count(';
MAX             : 'max(';
MEDIAN          : 'median(';
MIN             : 'min(';
PERCENTILE_CONT : 'percentileCont(';
STDDEV          : 'stddev(';
STDDEV_POP      : 'stddevPop(';
STDDEV_SAMP     : 'stddevSamp(';
SUM             : 'sum(';
VARIANCE        : 'variance(';

// Program variables (alphabetical)

V_ANALYTICS_PERIOD_END  : 'analytics_period_end';
V_ANALYTICS_PERIOD_START: 'analytics_period_start';
V_CREATION_DATE         : 'creation_date';
V_CURRENT_DATE          : 'current_date';
V_DUE_DATE              : 'due_date';
V_ENROLLMENT_COUNT      : 'enrollment_count';
V_ENROLLMENT_DATE       : 'enrollment_date';
V_ENROLLMENT_ID         : 'enrollment_id';
V_ENROLLMENT_STATUS     : 'enrollment_status';
V_ENVIRONMENT           : 'environment';
V_EVENT_COUNT           : 'event_count';
V_EVENT_DATE            : 'event_date';
V_EVENT_ID              : 'event_id';
V_EVENT_STATUS          : 'event_status';
V_EXECUTION_DATE        : 'execution_date';
V_INCIDENT_DATE         : 'incident_date';
V_ORG_UNIT_COUNT        : 'org_unit_count';
V_OU                    : 'org_unit';
V_OU_CODE               : 'orgunit_code';
V_PROGRAM_NAME          : 'program_name';
V_PROGRAM_STAGE_ID      : 'program_stage_id';
V_PROGRAM_STAGE_NAME    : 'program_stage_name';
V_SYNC_DATE             : 'sync_date';
V_TEI_COUNT             : 'tei_count';
V_VALUE_COUNT           : 'value_count';
V_ZERO_POS_VALUE_COUNT  : 'zero_pos_value_count';

// Program functions (alphabetical)

D2_ADD_DAYS             : 'd2:addDays(';
D2_CEIL                 : 'd2:ceil(';
D2_CONCATENATE          : 'd2:concatenate(';
D2_CONDITION            : 'd2:condition(';
D2_COUNT                : 'd2:count(';
D2_COUNT_IF_CONDITION   : 'd2:countIfCondition(';
D2_COUNT_IF_VALUE       : 'd2:countIfValue(';
D2_COUNT_IF_ZERO_POS    : 'd2:countIfZeroPos(';
D2_DAYS_BETWEEN         : 'd2:daysBetween(';
D2_FLOOR                : 'd2:floor(';
D2_HAS_USER_ROLE        : 'd2:hasUserRole(';
D2_HAS_VALUE            : 'd2:hasValue(';
D2_IN_ORG_UNIT_GROUP    : 'd2:inOrgUnitGroup(';
D2_LAST_EVENT_DATE      : 'd2:lastEventDate(';
D2_LEFT                 : 'd2:left(';
D2_LENGTH               : 'd2:length(';
D2_MAX_VALUE            : 'd2:maxValue(';
D2_MINUTES_BETWEEN      : 'd2:minutesBetween(';
D2_MIN_VALUE            : 'd2:minValue(';
D2_MODULUS              : 'd2:modulus(';
D2_MONTHS_BETWEEN       : 'd2:monthsBetween(';
D2_OIZP                 : 'd2:oizp(';
D2_RELATIONSHIP_COUNT   : 'd2:relationshipCount(';
D2_RIGHT                : 'd2:right(';
D2_ROUND                : 'd2:round(';
D2_SPLIT                : 'd2:split(';
D2_SUBSTRING            : 'd2:substring(';
D2_VALIDATE_PATTERN     : 'd2:validatePattern(';
D2_WEEKS_BETWEEN        : 'd2:weeksBetween(';
D2_YEARS_BETWEEN        : 'd2:yearsBetween(';
D2_ZING                 : 'd2:zing(';
D2_ZPVC                 : 'd2:zpvc(';
D2_ZSCOREHFA            : 'd2:zScoreHFA(';
D2_ZSCOREWFA            : 'd2:zScoreWFA(';
D2_ZSCOREWFH            : 'd2:zScoreWFH(';



// Items (alphabetical by symbol)

HASH_BRACE  : '#{';
A_BRACE     : 'A{';
C_BRACE     : 'C{';
D_BRACE     : 'D{';
I_BRACE     : 'I{';
N_BRACE     : 'N{';
OUG_BRACE   : 'OUG{';
R_BRACE     : 'R{';
X_BRACE     : 'X{';
DAYS        : '[days]';

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

EMPTY
    : EOF;

UNQUOTED_STRING
    :   [a-zA-Z_]+;

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
