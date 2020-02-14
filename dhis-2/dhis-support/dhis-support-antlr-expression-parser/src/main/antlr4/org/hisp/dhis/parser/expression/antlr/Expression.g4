// ANTLR V4 grammar file to define DHIS2 expression syntax

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

    //  Operators (in precedence order)

    |   it='(' expr ')'
    |   <assoc=right> expr it='^' expr
    |   it=('+' | '-' | '!' | 'not') expr
    |   expr it=('*' | '/' | '%') expr
    |   expr it=('+' | '-') expr
    |   expr it=('<' | '>' | '<=' | '>=') expr
    |   expr it=('==' | '!=') expr
    |   expr it=('&&' | 'and') expr
    |   expr it=('||' | 'or') expr

    //  Functions (alphabetical)

    |   it='firstNonNull(' expr (',' expr )* ')'
    |   it='greatest(' expr (',' expr )* ')'
    |   it='if(' expr ',' expr ',' expr ')'
    |   it='isNotNull(' expr ')'
    |   it='isNull(' expr ')'
    |   it='least(' expr (',' expr )* ')'

    //  Aggergation functions (alphabetical)

    |   it='avg(' expr ')'
    |   it='count(' expr ')'
    |   it='max(' expr ')'
    |   it='median(' expr ')'
    |   it='min(' expr ')'
    |   it='percentileCont(' expr ',' expr ')'
    |   it='stddev(' expr ')'
    |   it='stddevPop(' expr ')'
    |   it='stddevSamp(' expr ')'
    |   it='sum(' expr ')'
    |   it='variance(' expr ')'

    //  Program functions (alphabetical)

    |   it='d2:addDays(' expr ',' expr ')'
    |   it='d2:ceil(' expr ')'
    |   it='d2:concatenate(' expr (',' expr )* ')'
    |   it='d2:condition(' WS* stringLiteral WS* ',' expr ',' expr ')'
    |   it='d2:count(' WS* '#{' uid0=UID '.' uid1=UID '}' WS* ')'
    |   it='d2:count(' WS* '#{' variableName '}' WS* ')'
    |   it='d2:countIfCondition(' WS* '#{' uid0=UID '.' uid1=UID '}' WS* ',' WS* stringLiteral WS* ')'
    |   it='d2:countIfCondition(' WS* '#{' variableName '}' WS* ',' WS* stringLiteral WS* ')'
    |   it='d2:countIfValue(' WS* '#{' uid0=UID '.' uid1=UID '}' WS* ',' expr ')'
    |   it='d2:countIfValue(' WS* '#{' variableName '}' WS* ',' expr ')'
    |   it='d2:countIfZeroPos(' WS* '#{' uid0=UID '.' uid1=UID '}' WS* ')'
    |   it='d2:countIfZeroPos(' WS* '#{' variableName '}' WS* ')'
    |   it='d2:daysBetween(' expr ',' expr ')'
    |   it='d2:floor(' expr ')'
    |   it='d2:hasUserRole(' expr ')'
    |   it='d2:hasValue(' WS* '#{' uid0=UID '.' uid1=UID '}' WS* ')'
    |   it='d2:hasValue(' WS* '#{' variableName '}' WS* ')'
    |   it='d2:hasValue(' WS* 'A{' uid0=UID '}' WS* ')'
    |   it='d2:hasValue(' WS* 'V{' programVariable '}' WS* ')'
    |   it='d2:inOrgUnitGroup(' expr ')'
    |   it='d2:lastEventDate(' expr ')'
    |   it='d2:left(' expr ',' expr ')'
    |   it='d2:length(' expr ')'
    |   it='d2:maxValue(' WS* '#{' uid0=UID '.' uid1=UID '}' WS* ')'
    |   it='d2:maxValue(' WS* '#{' variableName '}' WS* ')'
    |   it='d2:maxValue(' WS* psEventDate='PS_EVENTDATE:' WS* uid0=UID WS* ')'
    |   it='d2:minutesBetween(' expr ',' expr ')'
    |   it='d2:minValue(' WS* '#{' uid0=UID '.' uid1=UID '}' WS* ')'
    |   it='d2:minValue(' WS* '#{' variableName '}' WS* ')'
    |   it='d2:minValue(' WS* psEventDate='PS_EVENTDATE:' WS* uid0=UID WS* ')'
    |   it='d2:modulus(' expr ',' expr ')'
    |   it='d2:monthsBetween(' expr ',' expr ')'
    |   it='d2:oizp(' expr ')'
    |   it='d2:relationshipCount(' WS* QUOTED_UID? WS* ')'
    |   it='d2:right(' expr ',' expr ')'
    |   it='d2:round(' expr ')'
    |   it='d2:split(' expr ',' expr ',' expr ')'
    |   it='d2:substring(' expr ',' expr ',' expr ')'
    |   it='d2:validatePattern(' expr ',' expr ')'
    |   it='d2:weeksBetween(' expr ',' expr ')'
    |   it='d2:yearsBetween(' expr ',' expr ')'
    |   it='d2:zing(' expr ')'
    |   it='d2:zpvc(' expr (',' expr )* ')'
    |   it='d2:zScoreHFA(' expr ',' expr ',' expr ')'
    |   it='d2:zScoreWFA(' expr ',' expr ',' expr ')'
    |   it='d2:zScoreWFH(' expr ',' expr ',' expr ')'

    //  Data items

    |   it='#{' uid0=UID (wild1='.*')? '}'
    |   it='#{' uid0=UID '.' uid1=UID '}'
    |   it='#{' uid0=UID '.' uid1=UID wild2='.*' '}'
    |   it='#{' uid0=UID '.*.' uid2=UID '}'
    |   it='#{' uid0=UID '.' uid1=UID '.' uid2=UID '}'
    |   it='#{' variableName '}'
    |   it='A{' uid0=UID '.' uid1=UID '}' // Program attribute in expressions (indicator, etc.)
    |   it='A{' uid0=UID '}' // Program attribute in program indicator expressions
    |   it='C{' uid0=UID '}'
    |   it='D{' uid0=UID '.' uid1=UID '}'
    |   it='I{' uid0=UID '}'
    |   it='N{' uid0=UID '}' // Indicator
    |   it='OUG{' uid0=UID '}'
    |   it='PS_EVENTDATE:' WS* uid0=UID
    |   it='R{' uid0=UID '.' REPORTING_RATE_TYPE '}'
    |   it='[days]'

    //  Program indicator/rule built-in variables

    |   it='V{' programVariable '}'

    //  Literals

    |   numericLiteral
    |   stringLiteral
    |   booleanLiteral
    ;

programVariable   // (alphabtical)
    :   var='analytics_period_end'
    |   var='analytics_period_start'
    |   var='creation_date'
    |   var='current_date'
    |   var='due_date'
    |   var='enrollment_count'
    |   var='enrollment_date'
    |   var='enrollment_id'
    |   var='enrollment_status'
    |   var='environment'
    |   var='event_count'
    |   var='event_date'
    |   var='event_id'
    |   var='event_status'
    |   var='execution_date'
    |   var='incident_date'
    |   var='org_unit_count'
    |   var='org_unit'
    |   var='orgunit_code'
    |   var='program_name'
    |   var='program_stage_id'
    |   var='program_stage_name'
    |   var='sync_date'
    |   var='tei_count'
    |   var='value_count'
    |   var='zero_pos_value_count'
    ;

variableName
    : UID
    | IDENTIFIER;

numericLiteral
    :   NUMERIC_LITERAL
    ;

stringLiteral
    :   STRING_LITERAL
    |   QUOTED_UID // Resolve that quoted UID can also be a string literal
    ;

booleanLiteral
    :   BOOLEAN_LITERAL
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
PS_EVENTDATE: 'PS_EVENTDATE:';
R_BRACE     : 'R{';
V_BRACE     : 'V{';
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

// In addition to its use in parsing program rule variables,
// IDENTIFIER has the effect of requiring spaces between words,
// for example it disallows notisNull (should be not isNull),
// but allows !isNull.
IDENTIFIER
    : [a-zA-Z_]+
    ;

EMPTY
    : EOF;

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
