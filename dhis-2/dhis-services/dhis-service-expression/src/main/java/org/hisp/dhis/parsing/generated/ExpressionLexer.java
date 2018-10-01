// Generated from /Users/jim/Documents/dev/dhis2/dhis2-core/dhis-2/dhis-services/dhis-service-expression/src/main/resources/org/hisp/dhis/parsing/Expression.g4 by ANTLR 4.7.1
package org.hisp.dhis.parsing.generated;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ExpressionLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		POWER=18, MINUS=19, PLUS=20, NOT=21, MUL=22, DIV=23, MOD=24, LEQ=25, GEQ=26, 
		LT=27, GT=28, EQ=29, NE=30, AND=31, OR=32, IF=33, EXCEPT=34, IS_NULL=35, 
		COALESCE=36, SUM=37, AVERAGE=38, FIRST=39, LAST=40, COUNT=41, STDDEV=42, 
		VARIANCE=43, MIN=44, MAX=45, MEDIAN=46, PERCENTILE=47, RANK_HIGH=48, RANK_LOW=49, 
		RANK_PERCENTILE=50, AVERAGE_SUM_ORG_UNIT=51, LAST_AVERAGE_ORG_UNIT=52, 
		NO_AGGREGATION=53, PERIOD=54, OU_ANCESTOR=55, OU_DESCENDANT=56, OU_LEVEL=57, 
		OU_PEER=58, OU_GROUP=59, OU_DATA_SET=60, EVENT_DATE=61, DUE_DATE=62, INCIDENT_DATE=63, 
		ENROLLMENT_DATE=64, ENROLLMENT_STATUS=65, CURRENT_STATUS=66, VALUE_COUNT=67, 
		ZERO_POS_VALUE_COUNT=68, EVENT_COUNT=69, ENROLLMENT_COUNT=70, TEI_COUNT=71, 
		PROGRAM_STAGE_NAME=72, PROGRAM_STAGE_ID=73, REPORTING_PERIOD_START=74, 
		REPORTING_PERIOD_END=75, HAS_VALUE=76, MINUTES_BETWEEN=77, DAYS_BETWEEN=78, 
		WEEKS_BETWEEN=79, MONTHS_BETWEEN=80, YEARS_BETWEEN=81, CONDITION=82, ZING=83, 
		OIZP=84, ZPVC=85, NUMERIC_LITERAL=86, STRING_LITERAL=87, BOOLEAN_LITERAL=88, 
		UID=89, JAVA_IDENTIFIER=90, WS=91;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
		"POWER", "MINUS", "PLUS", "NOT", "MUL", "DIV", "MOD", "LEQ", "GEQ", "LT", 
		"GT", "EQ", "NE", "AND", "OR", "IF", "EXCEPT", "IS_NULL", "COALESCE", 
		"SUM", "AVERAGE", "FIRST", "LAST", "COUNT", "STDDEV", "VARIANCE", "MIN", 
		"MAX", "MEDIAN", "PERCENTILE", "RANK_HIGH", "RANK_LOW", "RANK_PERCENTILE", 
		"AVERAGE_SUM_ORG_UNIT", "LAST_AVERAGE_ORG_UNIT", "NO_AGGREGATION", "PERIOD", 
		"OU_ANCESTOR", "OU_DESCENDANT", "OU_LEVEL", "OU_PEER", "OU_GROUP", "OU_DATA_SET", 
		"EVENT_DATE", "DUE_DATE", "INCIDENT_DATE", "ENROLLMENT_DATE", "ENROLLMENT_STATUS", 
		"CURRENT_STATUS", "VALUE_COUNT", "ZERO_POS_VALUE_COUNT", "EVENT_COUNT", 
		"ENROLLMENT_COUNT", "TEI_COUNT", "PROGRAM_STAGE_NAME", "PROGRAM_STAGE_ID", 
		"REPORTING_PERIOD_START", "REPORTING_PERIOD_END", "HAS_VALUE", "MINUTES_BETWEEN", 
		"DAYS_BETWEEN", "WEEKS_BETWEEN", "MONTHS_BETWEEN", "YEARS_BETWEEN", "CONDITION", 
		"ZING", "OIZP", "ZPVC", "NUMERIC_LITERAL", "STRING_LITERAL", "BOOLEAN_LITERAL", 
		"UID", "JAVA_IDENTIFIER", "WS", "Exponent", "Alpha", "AlphaNum", "JavaIdentifierStart", 
		"JavaIdentifierPart", "EscapeSequence", "HexDigit"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'('", "')'", "'.'", "'V{'", "'}'", "'d2:'", "','", "'#{'", "'.*'", 
		"'D{'", "'A{'", "'I{'", "'OUG{'", "'R{'", "'C{'", "'[days]'", "'.*.'", 
		"'^'", "'-'", "'+'", "'!'", "'*'", "'/'", "'%'", "'<='", "'>='", "'<'", 
		"'>'", "'=='", "'!='", "'&&'", "'||'", "'if'", "'except'", "'isNull'", 
		"'coalesce'", "'sum'", "'average'", "'first'", "'last'", "'count'", "'stddev'", 
		"'variance'", "'min'", "'max'", "'median'", "'percentile'", "'rankHigh'", 
		"'rankLow'", "'rankPercentile'", "'averageSumOrgUnit'", "'lastAverageOrgUnit'", 
		"'noAggregation'", "'period'", "'ouAncestor'", "'ouDescendant'", "'ouLevel'", 
		"'ouPeer'", "'ouGroup'", "'ouDataSet'", "'event_date'", "'due_date'", 
		"'incident_date'", "'enrollment_date'", "'enrollment_status'", "'current_date'", 
		"'value_count'", "'zero_pos_value_count'", "'event_count'", "'enrollment_count'", 
		"'tei_count'", "'program_stage_name'", "'program_stage_id'", "'reporting_period_start'", 
		"'reporting_period_end'", "'hasValue'", "'minutesBetween'", "'daysBetween'", 
		"'weeksBetween'", "'monthsBetween'", "'yearsBetween'", "'condition'", 
		"'zing'", "'oizp'", "'zpvc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, "POWER", "MINUS", "PLUS", "NOT", "MUL", 
		"DIV", "MOD", "LEQ", "GEQ", "LT", "GT", "EQ", "NE", "AND", "OR", "IF", 
		"EXCEPT", "IS_NULL", "COALESCE", "SUM", "AVERAGE", "FIRST", "LAST", "COUNT", 
		"STDDEV", "VARIANCE", "MIN", "MAX", "MEDIAN", "PERCENTILE", "RANK_HIGH", 
		"RANK_LOW", "RANK_PERCENTILE", "AVERAGE_SUM_ORG_UNIT", "LAST_AVERAGE_ORG_UNIT", 
		"NO_AGGREGATION", "PERIOD", "OU_ANCESTOR", "OU_DESCENDANT", "OU_LEVEL", 
		"OU_PEER", "OU_GROUP", "OU_DATA_SET", "EVENT_DATE", "DUE_DATE", "INCIDENT_DATE", 
		"ENROLLMENT_DATE", "ENROLLMENT_STATUS", "CURRENT_STATUS", "VALUE_COUNT", 
		"ZERO_POS_VALUE_COUNT", "EVENT_COUNT", "ENROLLMENT_COUNT", "TEI_COUNT", 
		"PROGRAM_STAGE_NAME", "PROGRAM_STAGE_ID", "REPORTING_PERIOD_START", "REPORTING_PERIOD_END", 
		"HAS_VALUE", "MINUTES_BETWEEN", "DAYS_BETWEEN", "WEEKS_BETWEEN", "MONTHS_BETWEEN", 
		"YEARS_BETWEEN", "CONDITION", "ZING", "OIZP", "ZPVC", "NUMERIC_LITERAL", 
		"STRING_LITERAL", "BOOLEAN_LITERAL", "UID", "JAVA_IDENTIFIER", "WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public ExpressionLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Expression.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2]\u03dd\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\tT"+
		"\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_\4"+
		"`\t`\4a\ta\4b\tb\4c\tc\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\7"+
		"\3\7\3\7\3\7\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\13\3\f\3\f\3"+
		"\f\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\20\3\20\3\20"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23\3\23\3\24"+
		"\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32\3\32"+
		"\3\33\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\36\3\37\3\37\3\37\3 \3"+
		" \3 \3!\3!\3!\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3$\3$\3"+
		"%\3%\3%\3%\3%\3%\3%\3%\3%\3&\3&\3&\3&\3\'\3\'\3\'\3\'\3\'\3\'\3\'\3\'"+
		"\3(\3(\3(\3(\3(\3(\3)\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3+\3+\3+\3+\3+\3+"+
		"\3+\3,\3,\3,\3,\3,\3,\3,\3,\3,\3-\3-\3-\3-\3.\3.\3.\3.\3/\3/\3/\3/\3/"+
		"\3/\3/\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\61\3\61"+
		"\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\62\3\62\3\62\3\62\3\62\3\62\3\62"+
		"\3\62\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63"+
		"\3\63\3\63\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64"+
		"\3\64\3\64\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65"+
		"\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\66\3\66\3\66"+
		"\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\67\3\67\3\67"+
		"\3\67\3\67\3\67\3\67\38\38\38\38\38\38\38\38\38\38\38\39\39\39\39\39\3"+
		"9\39\39\39\39\39\39\39\3:\3:\3:\3:\3:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3;\3"+
		"<\3<\3<\3<\3<\3<\3<\3<\3=\3=\3=\3=\3=\3=\3=\3=\3=\3=\3>\3>\3>\3>\3>\3"+
		">\3>\3>\3>\3>\3>\3?\3?\3?\3?\3?\3?\3?\3?\3?\3@\3@\3@\3@\3@\3@\3@\3@\3"+
		"@\3@\3@\3@\3@\3@\3A\3A\3A\3A\3A\3A\3A\3A\3A\3A\3A\3A\3A\3A\3A\3A\3B\3"+
		"B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3C\3C\3C\3C\3C\3C\3"+
		"C\3C\3C\3C\3C\3C\3C\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3E\3E\3E\3E\3"+
		"E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3F\3F\3F\3F\3F\3F\3"+
		"F\3F\3F\3F\3F\3F\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3"+
		"H\3H\3H\3H\3H\3H\3H\3H\3H\3H\3I\3I\3I\3I\3I\3I\3I\3I\3I\3I\3I\3I\3I\3"+
		"I\3I\3I\3I\3I\3I\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3"+
		"K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3"+
		"L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3M\3M\3"+
		"M\3M\3M\3M\3M\3M\3M\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3O\3"+
		"O\3O\3O\3O\3O\3O\3O\3O\3O\3O\3O\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3"+
		"P\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3R\3R\3R\3R\3R\3R\3R\3R\3"+
		"R\3R\3R\3R\3R\3S\3S\3S\3S\3S\3S\3S\3S\3S\3S\3T\3T\3T\3T\3T\3U\3U\3U\3"+
		"U\3U\3V\3V\3V\3V\3V\3W\3W\3W\7W\u0367\nW\fW\16W\u036a\13W\5W\u036c\nW"+
		"\3W\3W\7W\u0370\nW\fW\16W\u0373\13W\5W\u0375\nW\3W\5W\u0378\nW\3W\3W\6"+
		"W\u037c\nW\rW\16W\u037d\3W\5W\u0381\nW\5W\u0383\nW\3X\3X\3X\7X\u0388\n"+
		"X\fX\16X\u038b\13X\3X\3X\3Y\3Y\3Y\3Y\3Y\3Y\3Y\3Y\3Y\5Y\u0398\nY\3Z\3Z"+
		"\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3[\3[\7[\u03a8\n[\f[\16[\u03ab\13[\3\\"+
		"\6\\\u03ae\n\\\r\\\16\\\u03af\3\\\3\\\3]\3]\5]\u03b6\n]\3]\6]\u03b9\n"+
		"]\r]\16]\u03ba\3^\3^\3_\3_\3`\3`\3a\3a\3b\3b\3b\3b\5b\u03c9\nb\3b\5b\u03cc"+
		"\nb\3b\3b\3b\6b\u03d1\nb\rb\16b\u03d2\3b\3b\3b\3b\3b\5b\u03da\nb\3c\3"+
		"c\2\2d\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17"+
		"\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\35"+
		"9\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66"+
		"k\67m8o9q:s;u<w=y>{?}@\177A\u0081B\u0083C\u0085D\u0087E\u0089F\u008bG"+
		"\u008dH\u008fI\u0091J\u0093K\u0095L\u0097M\u0099N\u009bO\u009dP\u009f"+
		"Q\u00a1R\u00a3S\u00a5T\u00a7U\u00a9V\u00abW\u00adX\u00afY\u00b1Z\u00b3"+
		"[\u00b5\\\u00b7]\u00b9\2\u00bb\2\u00bd\2\u00bf\2\u00c1\2\u00c3\2\u00c5"+
		"\2\3\2\20\3\2\63;\3\2\62;\6\2\f\f\17\17$$^^\5\2\13\f\17\17\"\"\4\2GGg"+
		"g\4\2--//\4\2C\\c|\5\2\62;C\\c|\6\2&&C\\aac|\7\2&&\62;C\\aac|\n\2$$))"+
		"^^ddhhppttvv\3\2\62\65\3\2\629\5\2\62;CHch\2\u03e9\2\3\3\2\2\2\2\5\3\2"+
		"\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2"+
		"\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3"+
		"\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3"+
		"\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3"+
		"\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2"+
		"\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2"+
		"Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3"+
		"\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2q\3\2\2"+
		"\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2\2\2\2}\3\2\2\2\2"+
		"\177\3\2\2\2\2\u0081\3\2\2\2\2\u0083\3\2\2\2\2\u0085\3\2\2\2\2\u0087\3"+
		"\2\2\2\2\u0089\3\2\2\2\2\u008b\3\2\2\2\2\u008d\3\2\2\2\2\u008f\3\2\2\2"+
		"\2\u0091\3\2\2\2\2\u0093\3\2\2\2\2\u0095\3\2\2\2\2\u0097\3\2\2\2\2\u0099"+
		"\3\2\2\2\2\u009b\3\2\2\2\2\u009d\3\2\2\2\2\u009f\3\2\2\2\2\u00a1\3\2\2"+
		"\2\2\u00a3\3\2\2\2\2\u00a5\3\2\2\2\2\u00a7\3\2\2\2\2\u00a9\3\2\2\2\2\u00ab"+
		"\3\2\2\2\2\u00ad\3\2\2\2\2\u00af\3\2\2\2\2\u00b1\3\2\2\2\2\u00b3\3\2\2"+
		"\2\2\u00b5\3\2\2\2\2\u00b7\3\2\2\2\3\u00c7\3\2\2\2\5\u00c9\3\2\2\2\7\u00cb"+
		"\3\2\2\2\t\u00cd\3\2\2\2\13\u00d0\3\2\2\2\r\u00d2\3\2\2\2\17\u00d6\3\2"+
		"\2\2\21\u00d8\3\2\2\2\23\u00db\3\2\2\2\25\u00de\3\2\2\2\27\u00e1\3\2\2"+
		"\2\31\u00e4\3\2\2\2\33\u00e7\3\2\2\2\35\u00ec\3\2\2\2\37\u00ef\3\2\2\2"+
		"!\u00f2\3\2\2\2#\u00f9\3\2\2\2%\u00fd\3\2\2\2\'\u00ff\3\2\2\2)\u0101\3"+
		"\2\2\2+\u0103\3\2\2\2-\u0105\3\2\2\2/\u0107\3\2\2\2\61\u0109\3\2\2\2\63"+
		"\u010b\3\2\2\2\65\u010e\3\2\2\2\67\u0111\3\2\2\29\u0113\3\2\2\2;\u0115"+
		"\3\2\2\2=\u0118\3\2\2\2?\u011b\3\2\2\2A\u011e\3\2\2\2C\u0121\3\2\2\2E"+
		"\u0124\3\2\2\2G\u012b\3\2\2\2I\u0132\3\2\2\2K\u013b\3\2\2\2M\u013f\3\2"+
		"\2\2O\u0147\3\2\2\2Q\u014d\3\2\2\2S\u0152\3\2\2\2U\u0158\3\2\2\2W\u015f"+
		"\3\2\2\2Y\u0168\3\2\2\2[\u016c\3\2\2\2]\u0170\3\2\2\2_\u0177\3\2\2\2a"+
		"\u0182\3\2\2\2c\u018b\3\2\2\2e\u0193\3\2\2\2g\u01a2\3\2\2\2i\u01b4\3\2"+
		"\2\2k\u01c7\3\2\2\2m\u01d5\3\2\2\2o\u01dc\3\2\2\2q\u01e7\3\2\2\2s\u01f4"+
		"\3\2\2\2u\u01fc\3\2\2\2w\u0203\3\2\2\2y\u020b\3\2\2\2{\u0215\3\2\2\2}"+
		"\u0220\3\2\2\2\177\u0229\3\2\2\2\u0081\u0237\3\2\2\2\u0083\u0247\3\2\2"+
		"\2\u0085\u0259\3\2\2\2\u0087\u0266\3\2\2\2\u0089\u0272\3\2\2\2\u008b\u0287"+
		"\3\2\2\2\u008d\u0293\3\2\2\2\u008f\u02a4\3\2\2\2\u0091\u02ae\3\2\2\2\u0093"+
		"\u02c1\3\2\2\2\u0095\u02d2\3\2\2\2\u0097\u02e9\3\2\2\2\u0099\u02fe\3\2"+
		"\2\2\u009b\u0307\3\2\2\2\u009d\u0316\3\2\2\2\u009f\u0322\3\2\2\2\u00a1"+
		"\u032f\3\2\2\2\u00a3\u033d\3\2\2\2\u00a5\u034a\3\2\2\2\u00a7\u0354\3\2"+
		"\2\2\u00a9\u0359\3\2\2\2\u00ab\u035e\3\2\2\2\u00ad\u0382\3\2\2\2\u00af"+
		"\u0384\3\2\2\2\u00b1\u0397\3\2\2\2\u00b3\u0399\3\2\2\2\u00b5\u03a5\3\2"+
		"\2\2\u00b7\u03ad\3\2\2\2\u00b9\u03b3\3\2\2\2\u00bb\u03bc\3\2\2\2\u00bd"+
		"\u03be\3\2\2\2\u00bf\u03c0\3\2\2\2\u00c1\u03c2\3\2\2\2\u00c3\u03d9\3\2"+
		"\2\2\u00c5\u03db\3\2\2\2\u00c7\u00c8\7*\2\2\u00c8\4\3\2\2\2\u00c9\u00ca"+
		"\7+\2\2\u00ca\6\3\2\2\2\u00cb\u00cc\7\60\2\2\u00cc\b\3\2\2\2\u00cd\u00ce"+
		"\7X\2\2\u00ce\u00cf\7}\2\2\u00cf\n\3\2\2\2\u00d0\u00d1\7\177\2\2\u00d1"+
		"\f\3\2\2\2\u00d2\u00d3\7f\2\2\u00d3\u00d4\7\64\2\2\u00d4\u00d5\7<\2\2"+
		"\u00d5\16\3\2\2\2\u00d6\u00d7\7.\2\2\u00d7\20\3\2\2\2\u00d8\u00d9\7%\2"+
		"\2\u00d9\u00da\7}\2\2\u00da\22\3\2\2\2\u00db\u00dc\7\60\2\2\u00dc\u00dd"+
		"\7,\2\2\u00dd\24\3\2\2\2\u00de\u00df\7F\2\2\u00df\u00e0\7}\2\2\u00e0\26"+
		"\3\2\2\2\u00e1\u00e2\7C\2\2\u00e2\u00e3\7}\2\2\u00e3\30\3\2\2\2\u00e4"+
		"\u00e5\7K\2\2\u00e5\u00e6\7}\2\2\u00e6\32\3\2\2\2\u00e7\u00e8\7Q\2\2\u00e8"+
		"\u00e9\7W\2\2\u00e9\u00ea\7I\2\2\u00ea\u00eb\7}\2\2\u00eb\34\3\2\2\2\u00ec"+
		"\u00ed\7T\2\2\u00ed\u00ee\7}\2\2\u00ee\36\3\2\2\2\u00ef\u00f0\7E\2\2\u00f0"+
		"\u00f1\7}\2\2\u00f1 \3\2\2\2\u00f2\u00f3\7]\2\2\u00f3\u00f4\7f\2\2\u00f4"+
		"\u00f5\7c\2\2\u00f5\u00f6\7{\2\2\u00f6\u00f7\7u\2\2\u00f7\u00f8\7_\2\2"+
		"\u00f8\"\3\2\2\2\u00f9\u00fa\7\60\2\2\u00fa\u00fb\7,\2\2\u00fb\u00fc\7"+
		"\60\2\2\u00fc$\3\2\2\2\u00fd\u00fe\7`\2\2\u00fe&\3\2\2\2\u00ff\u0100\7"+
		"/\2\2\u0100(\3\2\2\2\u0101\u0102\7-\2\2\u0102*\3\2\2\2\u0103\u0104\7#"+
		"\2\2\u0104,\3\2\2\2\u0105\u0106\7,\2\2\u0106.\3\2\2\2\u0107\u0108\7\61"+
		"\2\2\u0108\60\3\2\2\2\u0109\u010a\7\'\2\2\u010a\62\3\2\2\2\u010b\u010c"+
		"\7>\2\2\u010c\u010d\7?\2\2\u010d\64\3\2\2\2\u010e\u010f\7@\2\2\u010f\u0110"+
		"\7?\2\2\u0110\66\3\2\2\2\u0111\u0112\7>\2\2\u01128\3\2\2\2\u0113\u0114"+
		"\7@\2\2\u0114:\3\2\2\2\u0115\u0116\7?\2\2\u0116\u0117\7?\2\2\u0117<\3"+
		"\2\2\2\u0118\u0119\7#\2\2\u0119\u011a\7?\2\2\u011a>\3\2\2\2\u011b\u011c"+
		"\7(\2\2\u011c\u011d\7(\2\2\u011d@\3\2\2\2\u011e\u011f\7~\2\2\u011f\u0120"+
		"\7~\2\2\u0120B\3\2\2\2\u0121\u0122\7k\2\2\u0122\u0123\7h\2\2\u0123D\3"+
		"\2\2\2\u0124\u0125\7g\2\2\u0125\u0126\7z\2\2\u0126\u0127\7e\2\2\u0127"+
		"\u0128\7g\2\2\u0128\u0129\7r\2\2\u0129\u012a\7v\2\2\u012aF\3\2\2\2\u012b"+
		"\u012c\7k\2\2\u012c\u012d\7u\2\2\u012d\u012e\7P\2\2\u012e\u012f\7w\2\2"+
		"\u012f\u0130\7n\2\2\u0130\u0131\7n\2\2\u0131H\3\2\2\2\u0132\u0133\7e\2"+
		"\2\u0133\u0134\7q\2\2\u0134\u0135\7c\2\2\u0135\u0136\7n\2\2\u0136\u0137"+
		"\7g\2\2\u0137\u0138\7u\2\2\u0138\u0139\7e\2\2\u0139\u013a\7g\2\2\u013a"+
		"J\3\2\2\2\u013b\u013c\7u\2\2\u013c\u013d\7w\2\2\u013d\u013e\7o\2\2\u013e"+
		"L\3\2\2\2\u013f\u0140\7c\2\2\u0140\u0141\7x\2\2\u0141\u0142\7g\2\2\u0142"+
		"\u0143\7t\2\2\u0143\u0144\7c\2\2\u0144\u0145\7i\2\2\u0145\u0146\7g\2\2"+
		"\u0146N\3\2\2\2\u0147\u0148\7h\2\2\u0148\u0149\7k\2\2\u0149\u014a\7t\2"+
		"\2\u014a\u014b\7u\2\2\u014b\u014c\7v\2\2\u014cP\3\2\2\2\u014d\u014e\7"+
		"n\2\2\u014e\u014f\7c\2\2\u014f\u0150\7u\2\2\u0150\u0151\7v\2\2\u0151R"+
		"\3\2\2\2\u0152\u0153\7e\2\2\u0153\u0154\7q\2\2\u0154\u0155\7w\2\2\u0155"+
		"\u0156\7p\2\2\u0156\u0157\7v\2\2\u0157T\3\2\2\2\u0158\u0159\7u\2\2\u0159"+
		"\u015a\7v\2\2\u015a\u015b\7f\2\2\u015b\u015c\7f\2\2\u015c\u015d\7g\2\2"+
		"\u015d\u015e\7x\2\2\u015eV\3\2\2\2\u015f\u0160\7x\2\2\u0160\u0161\7c\2"+
		"\2\u0161\u0162\7t\2\2\u0162\u0163\7k\2\2\u0163\u0164\7c\2\2\u0164\u0165"+
		"\7p\2\2\u0165\u0166\7e\2\2\u0166\u0167\7g\2\2\u0167X\3\2\2\2\u0168\u0169"+
		"\7o\2\2\u0169\u016a\7k\2\2\u016a\u016b\7p\2\2\u016bZ\3\2\2\2\u016c\u016d"+
		"\7o\2\2\u016d\u016e\7c\2\2\u016e\u016f\7z\2\2\u016f\\\3\2\2\2\u0170\u0171"+
		"\7o\2\2\u0171\u0172\7g\2\2\u0172\u0173\7f\2\2\u0173\u0174\7k\2\2\u0174"+
		"\u0175\7c\2\2\u0175\u0176\7p\2\2\u0176^\3\2\2\2\u0177\u0178\7r\2\2\u0178"+
		"\u0179\7g\2\2\u0179\u017a\7t\2\2\u017a\u017b\7e\2\2\u017b\u017c\7g\2\2"+
		"\u017c\u017d\7p\2\2\u017d\u017e\7v\2\2\u017e\u017f\7k\2\2\u017f\u0180"+
		"\7n\2\2\u0180\u0181\7g\2\2\u0181`\3\2\2\2\u0182\u0183\7t\2\2\u0183\u0184"+
		"\7c\2\2\u0184\u0185\7p\2\2\u0185\u0186\7m\2\2\u0186\u0187\7J\2\2\u0187"+
		"\u0188\7k\2\2\u0188\u0189\7i\2\2\u0189\u018a\7j\2\2\u018ab\3\2\2\2\u018b"+
		"\u018c\7t\2\2\u018c\u018d\7c\2\2\u018d\u018e\7p\2\2\u018e\u018f\7m\2\2"+
		"\u018f\u0190\7N\2\2\u0190\u0191\7q\2\2\u0191\u0192\7y\2\2\u0192d\3\2\2"+
		"\2\u0193\u0194\7t\2\2\u0194\u0195\7c\2\2\u0195\u0196\7p\2\2\u0196\u0197"+
		"\7m\2\2\u0197\u0198\7R\2\2\u0198\u0199\7g\2\2\u0199\u019a\7t\2\2\u019a"+
		"\u019b\7e\2\2\u019b\u019c\7g\2\2\u019c\u019d\7p\2\2\u019d\u019e\7v\2\2"+
		"\u019e\u019f\7k\2\2\u019f\u01a0\7n\2\2\u01a0\u01a1\7g\2\2\u01a1f\3\2\2"+
		"\2\u01a2\u01a3\7c\2\2\u01a3\u01a4\7x\2\2\u01a4\u01a5\7g\2\2\u01a5\u01a6"+
		"\7t\2\2\u01a6\u01a7\7c\2\2\u01a7\u01a8\7i\2\2\u01a8\u01a9\7g\2\2\u01a9"+
		"\u01aa\7U\2\2\u01aa\u01ab\7w\2\2\u01ab\u01ac\7o\2\2\u01ac\u01ad\7Q\2\2"+
		"\u01ad\u01ae\7t\2\2\u01ae\u01af\7i\2\2\u01af\u01b0\7W\2\2\u01b0\u01b1"+
		"\7p\2\2\u01b1\u01b2\7k\2\2\u01b2\u01b3\7v\2\2\u01b3h\3\2\2\2\u01b4\u01b5"+
		"\7n\2\2\u01b5\u01b6\7c\2\2\u01b6\u01b7\7u\2\2\u01b7\u01b8\7v\2\2\u01b8"+
		"\u01b9\7C\2\2\u01b9\u01ba\7x\2\2\u01ba\u01bb\7g\2\2\u01bb\u01bc\7t\2\2"+
		"\u01bc\u01bd\7c\2\2\u01bd\u01be\7i\2\2\u01be\u01bf\7g\2\2\u01bf\u01c0"+
		"\7Q\2\2\u01c0\u01c1\7t\2\2\u01c1\u01c2\7i\2\2\u01c2\u01c3\7W\2\2\u01c3"+
		"\u01c4\7p\2\2\u01c4\u01c5\7k\2\2\u01c5\u01c6\7v\2\2\u01c6j\3\2\2\2\u01c7"+
		"\u01c8\7p\2\2\u01c8\u01c9\7q\2\2\u01c9\u01ca\7C\2\2\u01ca\u01cb\7i\2\2"+
		"\u01cb\u01cc\7i\2\2\u01cc\u01cd\7t\2\2\u01cd\u01ce\7g\2\2\u01ce\u01cf"+
		"\7i\2\2\u01cf\u01d0\7c\2\2\u01d0\u01d1\7v\2\2\u01d1\u01d2\7k\2\2\u01d2"+
		"\u01d3\7q\2\2\u01d3\u01d4\7p\2\2\u01d4l\3\2\2\2\u01d5\u01d6\7r\2\2\u01d6"+
		"\u01d7\7g\2\2\u01d7\u01d8\7t\2\2\u01d8\u01d9\7k\2\2\u01d9\u01da\7q\2\2"+
		"\u01da\u01db\7f\2\2\u01dbn\3\2\2\2\u01dc\u01dd\7q\2\2\u01dd\u01de\7w\2"+
		"\2\u01de\u01df\7C\2\2\u01df\u01e0\7p\2\2\u01e0\u01e1\7e\2\2\u01e1\u01e2"+
		"\7g\2\2\u01e2\u01e3\7u\2\2\u01e3\u01e4\7v\2\2\u01e4\u01e5\7q\2\2\u01e5"+
		"\u01e6\7t\2\2\u01e6p\3\2\2\2\u01e7\u01e8\7q\2\2\u01e8\u01e9\7w\2\2\u01e9"+
		"\u01ea\7F\2\2\u01ea\u01eb\7g\2\2\u01eb\u01ec\7u\2\2\u01ec\u01ed\7e\2\2"+
		"\u01ed\u01ee\7g\2\2\u01ee\u01ef\7p\2\2\u01ef\u01f0\7f\2\2\u01f0\u01f1"+
		"\7c\2\2\u01f1\u01f2\7p\2\2\u01f2\u01f3\7v\2\2\u01f3r\3\2\2\2\u01f4\u01f5"+
		"\7q\2\2\u01f5\u01f6\7w\2\2\u01f6\u01f7\7N\2\2\u01f7\u01f8\7g\2\2\u01f8"+
		"\u01f9\7x\2\2\u01f9\u01fa\7g\2\2\u01fa\u01fb\7n\2\2\u01fbt\3\2\2\2\u01fc"+
		"\u01fd\7q\2\2\u01fd\u01fe\7w\2\2\u01fe\u01ff\7R\2\2\u01ff\u0200\7g\2\2"+
		"\u0200\u0201\7g\2\2\u0201\u0202\7t\2\2\u0202v\3\2\2\2\u0203\u0204\7q\2"+
		"\2\u0204\u0205\7w\2\2\u0205\u0206\7I\2\2\u0206\u0207\7t\2\2\u0207\u0208"+
		"\7q\2\2\u0208\u0209\7w\2\2\u0209\u020a\7r\2\2\u020ax\3\2\2\2\u020b\u020c"+
		"\7q\2\2\u020c\u020d\7w\2\2\u020d\u020e\7F\2\2\u020e\u020f\7c\2\2\u020f"+
		"\u0210\7v\2\2\u0210\u0211\7c\2\2\u0211\u0212\7U\2\2\u0212\u0213\7g\2\2"+
		"\u0213\u0214\7v\2\2\u0214z\3\2\2\2\u0215\u0216\7g\2\2\u0216\u0217\7x\2"+
		"\2\u0217\u0218\7g\2\2\u0218\u0219\7p\2\2\u0219\u021a\7v\2\2\u021a\u021b"+
		"\7a\2\2\u021b\u021c\7f\2\2\u021c\u021d\7c\2\2\u021d\u021e\7v\2\2\u021e"+
		"\u021f\7g\2\2\u021f|\3\2\2\2\u0220\u0221\7f\2\2\u0221\u0222\7w\2\2\u0222"+
		"\u0223\7g\2\2\u0223\u0224\7a\2\2\u0224\u0225\7f\2\2\u0225\u0226\7c\2\2"+
		"\u0226\u0227\7v\2\2\u0227\u0228\7g\2\2\u0228~\3\2\2\2\u0229\u022a\7k\2"+
		"\2\u022a\u022b\7p\2\2\u022b\u022c\7e\2\2\u022c\u022d\7k\2\2\u022d\u022e"+
		"\7f\2\2\u022e\u022f\7g\2\2\u022f\u0230\7p\2\2\u0230\u0231\7v\2\2\u0231"+
		"\u0232\7a\2\2\u0232\u0233\7f\2\2\u0233\u0234\7c\2\2\u0234\u0235\7v\2\2"+
		"\u0235\u0236\7g\2\2\u0236\u0080\3\2\2\2\u0237\u0238\7g\2\2\u0238\u0239"+
		"\7p\2\2\u0239\u023a\7t\2\2\u023a\u023b\7q\2\2\u023b\u023c\7n\2\2\u023c"+
		"\u023d\7n\2\2\u023d\u023e\7o\2\2\u023e\u023f\7g\2\2\u023f\u0240\7p\2\2"+
		"\u0240\u0241\7v\2\2\u0241\u0242\7a\2\2\u0242\u0243\7f\2\2\u0243\u0244"+
		"\7c\2\2\u0244\u0245\7v\2\2\u0245\u0246\7g\2\2\u0246\u0082\3\2\2\2\u0247"+
		"\u0248\7g\2\2\u0248\u0249\7p\2\2\u0249\u024a\7t\2\2\u024a\u024b\7q\2\2"+
		"\u024b\u024c\7n\2\2\u024c\u024d\7n\2\2\u024d\u024e\7o\2\2\u024e\u024f"+
		"\7g\2\2\u024f\u0250\7p\2\2\u0250\u0251\7v\2\2\u0251\u0252\7a\2\2\u0252"+
		"\u0253\7u\2\2\u0253\u0254\7v\2\2\u0254\u0255\7c\2\2\u0255\u0256\7v\2\2"+
		"\u0256\u0257\7w\2\2\u0257\u0258\7u\2\2\u0258\u0084\3\2\2\2\u0259\u025a"+
		"\7e\2\2\u025a\u025b\7w\2\2\u025b\u025c\7t\2\2\u025c\u025d\7t\2\2\u025d"+
		"\u025e\7g\2\2\u025e\u025f\7p\2\2\u025f\u0260\7v\2\2\u0260\u0261\7a\2\2"+
		"\u0261\u0262\7f\2\2\u0262\u0263\7c\2\2\u0263\u0264\7v\2\2\u0264\u0265"+
		"\7g\2\2\u0265\u0086\3\2\2\2\u0266\u0267\7x\2\2\u0267\u0268\7c\2\2\u0268"+
		"\u0269\7n\2\2\u0269\u026a\7w\2\2\u026a\u026b\7g\2\2\u026b\u026c\7a\2\2"+
		"\u026c\u026d\7e\2\2\u026d\u026e\7q\2\2\u026e\u026f\7w\2\2\u026f\u0270"+
		"\7p\2\2\u0270\u0271\7v\2\2\u0271\u0088\3\2\2\2\u0272\u0273\7|\2\2\u0273"+
		"\u0274\7g\2\2\u0274\u0275\7t\2\2\u0275\u0276\7q\2\2\u0276\u0277\7a\2\2"+
		"\u0277\u0278\7r\2\2\u0278\u0279\7q\2\2\u0279\u027a\7u\2\2\u027a\u027b"+
		"\7a\2\2\u027b\u027c\7x\2\2\u027c\u027d\7c\2\2\u027d\u027e\7n\2\2\u027e"+
		"\u027f\7w\2\2\u027f\u0280\7g\2\2\u0280\u0281\7a\2\2\u0281\u0282\7e\2\2"+
		"\u0282\u0283\7q\2\2\u0283\u0284\7w\2\2\u0284\u0285\7p\2\2\u0285\u0286"+
		"\7v\2\2\u0286\u008a\3\2\2\2\u0287\u0288\7g\2\2\u0288\u0289\7x\2\2\u0289"+
		"\u028a\7g\2\2\u028a\u028b\7p\2\2\u028b\u028c\7v\2\2\u028c\u028d\7a\2\2"+
		"\u028d\u028e\7e\2\2\u028e\u028f\7q\2\2\u028f\u0290\7w\2\2\u0290\u0291"+
		"\7p\2\2\u0291\u0292\7v\2\2\u0292\u008c\3\2\2\2\u0293\u0294\7g\2\2\u0294"+
		"\u0295\7p\2\2\u0295\u0296\7t\2\2\u0296\u0297\7q\2\2\u0297\u0298\7n\2\2"+
		"\u0298\u0299\7n\2\2\u0299\u029a\7o\2\2\u029a\u029b\7g\2\2\u029b\u029c"+
		"\7p\2\2\u029c\u029d\7v\2\2\u029d\u029e\7a\2\2\u029e\u029f\7e\2\2\u029f"+
		"\u02a0\7q\2\2\u02a0\u02a1\7w\2\2\u02a1\u02a2\7p\2\2\u02a2\u02a3\7v\2\2"+
		"\u02a3\u008e\3\2\2\2\u02a4\u02a5\7v\2\2\u02a5\u02a6\7g\2\2\u02a6\u02a7"+
		"\7k\2\2\u02a7\u02a8\7a\2\2\u02a8\u02a9\7e\2\2\u02a9\u02aa\7q\2\2\u02aa"+
		"\u02ab\7w\2\2\u02ab\u02ac\7p\2\2\u02ac\u02ad\7v\2\2\u02ad\u0090\3\2\2"+
		"\2\u02ae\u02af\7r\2\2\u02af\u02b0\7t\2\2\u02b0\u02b1\7q\2\2\u02b1\u02b2"+
		"\7i\2\2\u02b2\u02b3\7t\2\2\u02b3\u02b4\7c\2\2\u02b4\u02b5\7o\2\2\u02b5"+
		"\u02b6\7a\2\2\u02b6\u02b7\7u\2\2\u02b7\u02b8\7v\2\2\u02b8\u02b9\7c\2\2"+
		"\u02b9\u02ba\7i\2\2\u02ba\u02bb\7g\2\2\u02bb\u02bc\7a\2\2\u02bc\u02bd"+
		"\7p\2\2\u02bd\u02be\7c\2\2\u02be\u02bf\7o\2\2\u02bf\u02c0\7g\2\2\u02c0"+
		"\u0092\3\2\2\2\u02c1\u02c2\7r\2\2\u02c2\u02c3\7t\2\2\u02c3\u02c4\7q\2"+
		"\2\u02c4\u02c5\7i\2\2\u02c5\u02c6\7t\2\2\u02c6\u02c7\7c\2\2\u02c7\u02c8"+
		"\7o\2\2\u02c8\u02c9\7a\2\2\u02c9\u02ca\7u\2\2\u02ca\u02cb\7v\2\2\u02cb"+
		"\u02cc\7c\2\2\u02cc\u02cd\7i\2\2\u02cd\u02ce\7g\2\2\u02ce\u02cf\7a\2\2"+
		"\u02cf\u02d0\7k\2\2\u02d0\u02d1\7f\2\2\u02d1\u0094\3\2\2\2\u02d2\u02d3"+
		"\7t\2\2\u02d3\u02d4\7g\2\2\u02d4\u02d5\7r\2\2\u02d5\u02d6\7q\2\2\u02d6"+
		"\u02d7\7t\2\2\u02d7\u02d8\7v\2\2\u02d8\u02d9\7k\2\2\u02d9\u02da\7p\2\2"+
		"\u02da\u02db\7i\2\2\u02db\u02dc\7a\2\2\u02dc\u02dd\7r\2\2\u02dd\u02de"+
		"\7g\2\2\u02de\u02df\7t\2\2\u02df\u02e0\7k\2\2\u02e0\u02e1\7q\2\2\u02e1"+
		"\u02e2\7f\2\2\u02e2\u02e3\7a\2\2\u02e3\u02e4\7u\2\2\u02e4\u02e5\7v\2\2"+
		"\u02e5\u02e6\7c\2\2\u02e6\u02e7\7t\2\2\u02e7\u02e8\7v\2\2\u02e8\u0096"+
		"\3\2\2\2\u02e9\u02ea\7t\2\2\u02ea\u02eb\7g\2\2\u02eb\u02ec\7r\2\2\u02ec"+
		"\u02ed\7q\2\2\u02ed\u02ee\7t\2\2\u02ee\u02ef\7v\2\2\u02ef\u02f0\7k\2\2"+
		"\u02f0\u02f1\7p\2\2\u02f1\u02f2\7i\2\2\u02f2\u02f3\7a\2\2\u02f3\u02f4"+
		"\7r\2\2\u02f4\u02f5\7g\2\2\u02f5\u02f6\7t\2\2\u02f6\u02f7\7k\2\2\u02f7"+
		"\u02f8\7q\2\2\u02f8\u02f9\7f\2\2\u02f9\u02fa\7a\2\2\u02fa\u02fb\7g\2\2"+
		"\u02fb\u02fc\7p\2\2\u02fc\u02fd\7f\2\2\u02fd\u0098\3\2\2\2\u02fe\u02ff"+
		"\7j\2\2\u02ff\u0300\7c\2\2\u0300\u0301\7u\2\2\u0301\u0302\7X\2\2\u0302"+
		"\u0303\7c\2\2\u0303\u0304\7n\2\2\u0304\u0305\7w\2\2\u0305\u0306\7g\2\2"+
		"\u0306\u009a\3\2\2\2\u0307\u0308\7o\2\2\u0308\u0309\7k\2\2\u0309\u030a"+
		"\7p\2\2\u030a\u030b\7w\2\2\u030b\u030c\7v\2\2\u030c\u030d\7g\2\2\u030d"+
		"\u030e\7u\2\2\u030e\u030f\7D\2\2\u030f\u0310\7g\2\2\u0310\u0311\7v\2\2"+
		"\u0311\u0312\7y\2\2\u0312\u0313\7g\2\2\u0313\u0314\7g\2\2\u0314\u0315"+
		"\7p\2\2\u0315\u009c\3\2\2\2\u0316\u0317\7f\2\2\u0317\u0318\7c\2\2\u0318"+
		"\u0319\7{\2\2\u0319\u031a\7u\2\2\u031a\u031b\7D\2\2\u031b\u031c\7g\2\2"+
		"\u031c\u031d\7v\2\2\u031d\u031e\7y\2\2\u031e\u031f\7g\2\2\u031f\u0320"+
		"\7g\2\2\u0320\u0321\7p\2\2\u0321\u009e\3\2\2\2\u0322\u0323\7y\2\2\u0323"+
		"\u0324\7g\2\2\u0324\u0325\7g\2\2\u0325\u0326\7m\2\2\u0326\u0327\7u\2\2"+
		"\u0327\u0328\7D\2\2\u0328\u0329\7g\2\2\u0329\u032a\7v\2\2\u032a\u032b"+
		"\7y\2\2\u032b\u032c\7g\2\2\u032c\u032d\7g\2\2\u032d\u032e\7p\2\2\u032e"+
		"\u00a0\3\2\2\2\u032f\u0330\7o\2\2\u0330\u0331\7q\2\2\u0331\u0332\7p\2"+
		"\2\u0332\u0333\7v\2\2\u0333\u0334\7j\2\2\u0334\u0335\7u\2\2\u0335\u0336"+
		"\7D\2\2\u0336\u0337\7g\2\2\u0337\u0338\7v\2\2\u0338\u0339\7y\2\2\u0339"+
		"\u033a\7g\2\2\u033a\u033b\7g\2\2\u033b\u033c\7p\2\2\u033c\u00a2\3\2\2"+
		"\2\u033d\u033e\7{\2\2\u033e\u033f\7g\2\2\u033f\u0340\7c\2\2\u0340\u0341"+
		"\7t\2\2\u0341\u0342\7u\2\2\u0342\u0343\7D\2\2\u0343\u0344\7g\2\2\u0344"+
		"\u0345\7v\2\2\u0345\u0346\7y\2\2\u0346\u0347\7g\2\2\u0347\u0348\7g\2\2"+
		"\u0348\u0349\7p\2\2\u0349\u00a4\3\2\2\2\u034a\u034b\7e\2\2\u034b\u034c"+
		"\7q\2\2\u034c\u034d\7p\2\2\u034d\u034e\7f\2\2\u034e\u034f\7k\2\2\u034f"+
		"\u0350\7v\2\2\u0350\u0351\7k\2\2\u0351\u0352\7q\2\2\u0352\u0353\7p\2\2"+
		"\u0353\u00a6\3\2\2\2\u0354\u0355\7|\2\2\u0355\u0356\7k\2\2\u0356\u0357"+
		"\7p\2\2\u0357\u0358\7i\2\2\u0358\u00a8\3\2\2\2\u0359\u035a\7q\2\2\u035a"+
		"\u035b\7k\2\2\u035b\u035c\7|\2\2\u035c\u035d\7r\2\2\u035d\u00aa\3\2\2"+
		"\2\u035e\u035f\7|\2\2\u035f\u0360\7r\2\2\u0360\u0361\7x\2\2\u0361\u0362"+
		"\7e\2\2\u0362\u00ac\3\2\2\2\u0363\u036c\7\62\2\2\u0364\u0368\t\2\2\2\u0365"+
		"\u0367\t\3\2\2\u0366\u0365\3\2\2\2\u0367\u036a\3\2\2\2\u0368\u0366\3\2"+
		"\2\2\u0368\u0369\3\2\2\2\u0369\u036c\3\2\2\2\u036a\u0368\3\2\2\2\u036b"+
		"\u0363\3\2\2\2\u036b\u0364\3\2\2\2\u036c\u0374\3\2\2\2\u036d\u0371\7\60"+
		"\2\2\u036e\u0370\t\3\2\2\u036f\u036e\3\2\2\2\u0370\u0373\3\2\2\2\u0371"+
		"\u036f\3\2\2\2\u0371\u0372\3\2\2\2\u0372\u0375\3\2\2\2\u0373\u0371\3\2"+
		"\2\2\u0374\u036d\3\2\2\2\u0374\u0375\3\2\2\2\u0375\u0377\3\2\2\2\u0376"+
		"\u0378\5\u00b9]\2\u0377\u0376\3\2\2\2\u0377\u0378\3\2\2\2\u0378\u0383"+
		"\3\2\2\2\u0379\u037b\7\60\2\2\u037a\u037c\t\3\2\2\u037b\u037a\3\2\2\2"+
		"\u037c\u037d\3\2\2\2\u037d\u037b\3\2\2\2\u037d\u037e\3\2\2\2\u037e\u0380"+
		"\3\2\2\2\u037f\u0381\5\u00b9]\2\u0380\u037f\3\2\2\2\u0380\u0381\3\2\2"+
		"\2\u0381\u0383\3\2\2\2\u0382\u036b\3\2\2\2\u0382\u0379\3\2\2\2\u0383\u00ae"+
		"\3\2\2\2\u0384\u0389\7$\2\2\u0385\u0388\n\4\2\2\u0386\u0388\5\u00c3b\2"+
		"\u0387\u0385\3\2\2\2\u0387\u0386\3\2\2\2\u0388\u038b\3\2\2\2\u0389\u0387"+
		"\3\2\2\2\u0389\u038a\3\2\2\2\u038a\u038c\3\2\2\2\u038b\u0389\3\2\2\2\u038c"+
		"\u038d\7$\2\2\u038d\u00b0\3\2\2\2\u038e\u038f\7v\2\2\u038f\u0390\7t\2"+
		"\2\u0390\u0391\7w\2\2\u0391\u0398\7g\2\2\u0392\u0393\7h\2\2\u0393\u0394"+
		"\7c\2\2\u0394\u0395\7n\2\2\u0395\u0396\7u\2\2\u0396\u0398\7g\2\2\u0397"+
		"\u038e\3\2\2\2\u0397\u0392\3\2\2\2\u0398\u00b2\3\2\2\2\u0399\u039a\5\u00bb"+
		"^\2\u039a\u039b\5\u00bd_\2\u039b\u039c\5\u00bd_\2\u039c\u039d\5\u00bd"+
		"_\2\u039d\u039e\5\u00bd_\2\u039e\u039f\5\u00bd_\2\u039f\u03a0\5\u00bd"+
		"_\2\u03a0\u03a1\5\u00bd_\2\u03a1\u03a2\5\u00bd_\2\u03a2\u03a3\5\u00bd"+
		"_\2\u03a3\u03a4\5\u00bd_\2\u03a4\u00b4\3\2\2\2\u03a5\u03a9\5\u00bf`\2"+
		"\u03a6\u03a8\5\u00c1a\2\u03a7\u03a6\3\2\2\2\u03a8\u03ab\3\2\2\2\u03a9"+
		"\u03a7\3\2\2\2\u03a9\u03aa\3\2\2\2\u03aa\u00b6\3\2\2\2\u03ab\u03a9\3\2"+
		"\2\2\u03ac\u03ae\t\5\2\2\u03ad\u03ac\3\2\2\2\u03ae\u03af\3\2\2\2\u03af"+
		"\u03ad\3\2\2\2\u03af\u03b0\3\2\2\2\u03b0\u03b1\3\2\2\2\u03b1\u03b2\b\\"+
		"\2\2\u03b2\u00b8\3\2\2\2\u03b3\u03b5\t\6\2\2\u03b4\u03b6\t\7\2\2\u03b5"+
		"\u03b4\3\2\2\2\u03b5\u03b6\3\2\2\2\u03b6\u03b8\3\2\2\2\u03b7\u03b9\t\3"+
		"\2\2\u03b8\u03b7\3\2\2\2\u03b9\u03ba\3\2\2\2\u03ba\u03b8\3\2\2\2\u03ba"+
		"\u03bb\3\2\2\2\u03bb\u00ba\3\2\2\2\u03bc\u03bd\t\b\2\2\u03bd\u00bc\3\2"+
		"\2\2\u03be\u03bf\t\t\2\2\u03bf\u00be\3\2\2\2\u03c0\u03c1\t\n\2\2\u03c1"+
		"\u00c0\3\2\2\2\u03c2\u03c3\t\13\2\2\u03c3\u00c2\3\2\2\2\u03c4\u03c5\7"+
		"^\2\2\u03c5\u03da\t\f\2\2\u03c6\u03cb\7^\2\2\u03c7\u03c9\t\r\2\2\u03c8"+
		"\u03c7\3\2\2\2\u03c8\u03c9\3\2\2\2\u03c9\u03ca\3\2\2\2\u03ca\u03cc\t\16"+
		"\2\2\u03cb\u03c8\3\2\2\2\u03cb\u03cc\3\2\2\2\u03cc\u03cd\3\2\2\2\u03cd"+
		"\u03da\t\16\2\2\u03ce\u03d0\7^\2\2\u03cf\u03d1\7w\2\2\u03d0\u03cf\3\2"+
		"\2\2\u03d1\u03d2\3\2\2\2\u03d2\u03d0\3\2\2\2\u03d2\u03d3\3\2\2\2\u03d3"+
		"\u03d4\3\2\2\2\u03d4\u03d5\5\u00c5c\2\u03d5\u03d6\5\u00c5c\2\u03d6\u03d7"+
		"\5\u00c5c\2\u03d7\u03d8\5\u00c5c\2\u03d8\u03da\3\2\2\2\u03d9\u03c4\3\2"+
		"\2\2\u03d9\u03c6\3\2\2\2\u03d9\u03ce\3\2\2\2\u03da\u00c4\3\2\2\2\u03db"+
		"\u03dc\t\17\2\2\u03dc\u00c6\3\2\2\2\26\2\u0368\u036b\u0371\u0374\u0377"+
		"\u037d\u0380\u0382\u0387\u0389\u0397\u03a9\u03af\u03b5\u03ba\u03c8\u03cb"+
		"\u03d2\u03d9\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}