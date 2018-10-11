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
		COALESCE=36, MAXIMUM=37, MINIMUM=38, SUM=39, AVERAGE=40, FIRST=41, LAST=42, 
		COUNT=43, STDDEV=44, VARIANCE=45, MIN=46, MAX=47, MEDIAN=48, PERCENTILE=49, 
		RANK_HIGH=50, RANK_LOW=51, RANK_PERCENTILE=52, AVERAGE_SUM_ORG_UNIT=53, 
		LAST_AVERAGE_ORG_UNIT=54, NO_AGGREGATION=55, PERIOD=56, OU_ANCESTOR=57, 
		OU_DESCENDANT=58, OU_LEVEL=59, OU_PEER=60, OU_GROUP=61, OU_DATA_SET=62, 
		EVENT_DATE=63, DUE_DATE=64, INCIDENT_DATE=65, ENROLLMENT_DATE=66, ENROLLMENT_STATUS=67, 
		CURRENT_STATUS=68, VALUE_COUNT=69, ZERO_POS_VALUE_COUNT=70, EVENT_COUNT=71, 
		ENROLLMENT_COUNT=72, TEI_COUNT=73, PROGRAM_STAGE_NAME=74, PROGRAM_STAGE_ID=75, 
		REPORTING_PERIOD_START=76, REPORTING_PERIOD_END=77, HAS_VALUE=78, MINUTES_BETWEEN=79, 
		DAYS_BETWEEN=80, WEEKS_BETWEEN=81, MONTHS_BETWEEN=82, YEARS_BETWEEN=83, 
		CONDITION=84, ZING=85, OIZP=86, ZPVC=87, NUMERIC_LITERAL=88, STRING_LITERAL=89, 
		BOOLEAN_LITERAL=90, UID=91, JAVA_IDENTIFIER=92, WS=93;
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
		"MAXIMUM", "MINIMUM", "SUM", "AVERAGE", "FIRST", "LAST", "COUNT", "STDDEV", 
		"VARIANCE", "MIN", "MAX", "MEDIAN", "PERCENTILE", "RANK_HIGH", "RANK_LOW", 
		"RANK_PERCENTILE", "AVERAGE_SUM_ORG_UNIT", "LAST_AVERAGE_ORG_UNIT", "NO_AGGREGATION", 
		"PERIOD", "OU_ANCESTOR", "OU_DESCENDANT", "OU_LEVEL", "OU_PEER", "OU_GROUP", 
		"OU_DATA_SET", "EVENT_DATE", "DUE_DATE", "INCIDENT_DATE", "ENROLLMENT_DATE", 
		"ENROLLMENT_STATUS", "CURRENT_STATUS", "VALUE_COUNT", "ZERO_POS_VALUE_COUNT", 
		"EVENT_COUNT", "ENROLLMENT_COUNT", "TEI_COUNT", "PROGRAM_STAGE_NAME", 
		"PROGRAM_STAGE_ID", "REPORTING_PERIOD_START", "REPORTING_PERIOD_END", 
		"HAS_VALUE", "MINUTES_BETWEEN", "DAYS_BETWEEN", "WEEKS_BETWEEN", "MONTHS_BETWEEN", 
		"YEARS_BETWEEN", "CONDITION", "ZING", "OIZP", "ZPVC", "NUMERIC_LITERAL", 
		"STRING_LITERAL", "BOOLEAN_LITERAL", "UID", "JAVA_IDENTIFIER", "WS", "Exponent", 
		"Alpha", "AlphaNum", "JavaIdentifierStart", "JavaIdentifierPart", "EscapeSequence", 
		"HexDigit"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'('", "')'", "'.'", "'V{'", "'}'", "'d2:'", "','", "'#{'", "'.*'", 
		"'D{'", "'A{'", "'I{'", "'OUG{'", "'R{'", "'C{'", "'[days]'", "'.*.'", 
		"'^'", "'-'", "'+'", "'!'", "'*'", "'/'", "'%'", "'<='", "'>='", "'<'", 
		"'>'", "'=='", "'!='", "'&&'", "'||'", "'if'", "'except'", "'isNull'", 
		"'coalesce'", "'maximum'", "'minimum'", "'sum'", "'average'", "'first'", 
		"'last'", "'count'", "'stddev'", "'variance'", "'min'", "'max'", "'median'", 
		"'percentile'", "'rankHigh'", "'rankLow'", "'rankPercentile'", "'averageSumOrgUnit'", 
		"'lastAverageOrgUnit'", "'noAggregation'", "'period'", "'ouAncestor'", 
		"'ouDescendant'", "'ouLevel'", "'ouPeer'", "'ouGroup'", "'ouDataSet'", 
		"'event_date'", "'due_date'", "'incident_date'", "'enrollment_date'", 
		"'enrollment_status'", "'current_date'", "'value_count'", "'zero_pos_value_count'", 
		"'event_count'", "'enrollment_count'", "'tei_count'", "'program_stage_name'", 
		"'program_stage_id'", "'reporting_period_start'", "'reporting_period_end'", 
		"'hasValue'", "'minutesBetween'", "'daysBetween'", "'weeksBetween'", "'monthsBetween'", 
		"'yearsBetween'", "'condition'", "'zing'", "'oizp'", "'zpvc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, "POWER", "MINUS", "PLUS", "NOT", "MUL", 
		"DIV", "MOD", "LEQ", "GEQ", "LT", "GT", "EQ", "NE", "AND", "OR", "IF", 
		"EXCEPT", "IS_NULL", "COALESCE", "MAXIMUM", "MINIMUM", "SUM", "AVERAGE", 
		"FIRST", "LAST", "COUNT", "STDDEV", "VARIANCE", "MIN", "MAX", "MEDIAN", 
		"PERCENTILE", "RANK_HIGH", "RANK_LOW", "RANK_PERCENTILE", "AVERAGE_SUM_ORG_UNIT", 
		"LAST_AVERAGE_ORG_UNIT", "NO_AGGREGATION", "PERIOD", "OU_ANCESTOR", "OU_DESCENDANT", 
		"OU_LEVEL", "OU_PEER", "OU_GROUP", "OU_DATA_SET", "EVENT_DATE", "DUE_DATE", 
		"INCIDENT_DATE", "ENROLLMENT_DATE", "ENROLLMENT_STATUS", "CURRENT_STATUS", 
		"VALUE_COUNT", "ZERO_POS_VALUE_COUNT", "EVENT_COUNT", "ENROLLMENT_COUNT", 
		"TEI_COUNT", "PROGRAM_STAGE_NAME", "PROGRAM_STAGE_ID", "REPORTING_PERIOD_START", 
		"REPORTING_PERIOD_END", "HAS_VALUE", "MINUTES_BETWEEN", "DAYS_BETWEEN", 
		"WEEKS_BETWEEN", "MONTHS_BETWEEN", "YEARS_BETWEEN", "CONDITION", "ZING", 
		"OIZP", "ZPVC", "NUMERIC_LITERAL", "STRING_LITERAL", "BOOLEAN_LITERAL", 
		"UID", "JAVA_IDENTIFIER", "WS"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2_\u03f1\b\1\4\2\t"+
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
		"`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\5"+
		"\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\13"+
		"\3\f\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\20"+
		"\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23"+
		"\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\32"+
		"\3\32\3\32\3\33\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\36\3\37\3\37"+
		"\3\37\3 \3 \3 \3!\3!\3!\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3"+
		"$\3$\3$\3%\3%\3%\3%\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3&\3&\3&\3\'\3\'\3\'"+
		"\3\'\3\'\3\'\3\'\3\'\3(\3(\3(\3(\3)\3)\3)\3)\3)\3)\3)\3)\3*\3*\3*\3*\3"+
		"*\3*\3+\3+\3+\3+\3+\3,\3,\3,\3,\3,\3,\3-\3-\3-\3-\3-\3-\3-\3.\3.\3.\3"+
		".\3.\3.\3.\3.\3.\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\61\3"+
		"\61\3\61\3\61\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3"+
		"\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\64\3"+
		"\64\3\64\3\64\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3"+
		"\65\3\65\3\65\3\65\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3"+
		"\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\3\67\3\67\3"+
		"\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\38\38"+
		"\38\38\38\38\38\38\38\38\38\38\38\38\39\39\39\39\39\39\39\3:\3:\3:\3:"+
		"\3:\3:\3:\3:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3;\3;\3;\3;\3;\3;\3;\3<\3<\3<"+
		"\3<\3<\3<\3<\3<\3=\3=\3=\3=\3=\3=\3=\3>\3>\3>\3>\3>\3>\3>\3>\3?\3?\3?"+
		"\3?\3?\3?\3?\3?\3?\3?\3@\3@\3@\3@\3@\3@\3@\3@\3@\3@\3@\3A\3A\3A\3A\3A"+
		"\3A\3A\3A\3A\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3C\3C\3C\3C\3C"+
		"\3C\3C\3C\3C\3C\3C\3C\3C\3C\3C\3C\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D"+
		"\3D\3D\3D\3D\3D\3D\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3F\3F\3F\3F"+
		"\3F\3F\3F\3F\3F\3F\3F\3F\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G\3G"+
		"\3G\3G\3G\3G\3G\3G\3H\3H\3H\3H\3H\3H\3H\3H\3H\3H\3H\3H\3I\3I\3I\3I\3I"+
		"\3I\3I\3I\3I\3I\3I\3I\3I\3I\3I\3I\3I\3J\3J\3J\3J\3J\3J\3J\3J\3J\3J\3K"+
		"\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3K\3L\3L\3L\3L\3L"+
		"\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3L\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M"+
		"\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3M\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N"+
		"\3N\3N\3N\3N\3N\3N\3N\3N\3N\3N\3O\3O\3O\3O\3O\3O\3O\3O\3O\3P\3P\3P\3P"+
		"\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q"+
		"\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3S\3S\3S\3S\3S\3S\3S\3S\3S\3S"+
		"\3S\3S\3S\3S\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3T\3U\3U\3U\3U\3U\3U"+
		"\3U\3U\3U\3U\3V\3V\3V\3V\3V\3W\3W\3W\3W\3W\3X\3X\3X\3X\3X\3Y\3Y\3Y\7Y"+
		"\u037b\nY\fY\16Y\u037e\13Y\5Y\u0380\nY\3Y\3Y\7Y\u0384\nY\fY\16Y\u0387"+
		"\13Y\5Y\u0389\nY\3Y\5Y\u038c\nY\3Y\3Y\6Y\u0390\nY\rY\16Y\u0391\3Y\5Y\u0395"+
		"\nY\5Y\u0397\nY\3Z\3Z\3Z\7Z\u039c\nZ\fZ\16Z\u039f\13Z\3Z\3Z\3[\3[\3[\3"+
		"[\3[\3[\3[\3[\3[\5[\u03ac\n[\3\\\3\\\3\\\3\\\3\\\3\\\3\\\3\\\3\\\3\\\3"+
		"\\\3\\\3]\3]\7]\u03bc\n]\f]\16]\u03bf\13]\3^\6^\u03c2\n^\r^\16^\u03c3"+
		"\3^\3^\3_\3_\5_\u03ca\n_\3_\6_\u03cd\n_\r_\16_\u03ce\3`\3`\3a\3a\3b\3"+
		"b\3c\3c\3d\3d\3d\3d\5d\u03dd\nd\3d\5d\u03e0\nd\3d\3d\3d\6d\u03e5\nd\r"+
		"d\16d\u03e6\3d\3d\3d\3d\3d\5d\u03ee\nd\3e\3e\2\2f\3\3\5\4\7\5\t\6\13\7"+
		"\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25"+
		")\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O"+
		")Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66k\67m8o9q:s;u<w=y>{?}@\177A\u0081"+
		"B\u0083C\u0085D\u0087E\u0089F\u008bG\u008dH\u008fI\u0091J\u0093K\u0095"+
		"L\u0097M\u0099N\u009bO\u009dP\u009fQ\u00a1R\u00a3S\u00a5T\u00a7U\u00a9"+
		"V\u00abW\u00adX\u00afY\u00b1Z\u00b3[\u00b5\\\u00b7]\u00b9^\u00bb_\u00bd"+
		"\2\u00bf\2\u00c1\2\u00c3\2\u00c5\2\u00c7\2\u00c9\2\3\2\20\3\2\63;\3\2"+
		"\62;\6\2\f\f\17\17$$^^\5\2\13\f\17\17\"\"\4\2GGgg\4\2--//\4\2C\\c|\5\2"+
		"\62;C\\c|\6\2&&C\\aac|\7\2&&\62;C\\aac|\n\2$$))^^ddhhppttvv\3\2\62\65"+
		"\3\2\629\5\2\62;CHch\2\u03fd\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3"+
		"\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2"+
		"\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37"+
		"\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3"+
		"\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2"+
		"\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C"+
		"\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2"+
		"\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2"+
		"\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i"+
		"\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2"+
		"\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081"+
		"\3\2\2\2\2\u0083\3\2\2\2\2\u0085\3\2\2\2\2\u0087\3\2\2\2\2\u0089\3\2\2"+
		"\2\2\u008b\3\2\2\2\2\u008d\3\2\2\2\2\u008f\3\2\2\2\2\u0091\3\2\2\2\2\u0093"+
		"\3\2\2\2\2\u0095\3\2\2\2\2\u0097\3\2\2\2\2\u0099\3\2\2\2\2\u009b\3\2\2"+
		"\2\2\u009d\3\2\2\2\2\u009f\3\2\2\2\2\u00a1\3\2\2\2\2\u00a3\3\2\2\2\2\u00a5"+
		"\3\2\2\2\2\u00a7\3\2\2\2\2\u00a9\3\2\2\2\2\u00ab\3\2\2\2\2\u00ad\3\2\2"+
		"\2\2\u00af\3\2\2\2\2\u00b1\3\2\2\2\2\u00b3\3\2\2\2\2\u00b5\3\2\2\2\2\u00b7"+
		"\3\2\2\2\2\u00b9\3\2\2\2\2\u00bb\3\2\2\2\3\u00cb\3\2\2\2\5\u00cd\3\2\2"+
		"\2\7\u00cf\3\2\2\2\t\u00d1\3\2\2\2\13\u00d4\3\2\2\2\r\u00d6\3\2\2\2\17"+
		"\u00da\3\2\2\2\21\u00dc\3\2\2\2\23\u00df\3\2\2\2\25\u00e2\3\2\2\2\27\u00e5"+
		"\3\2\2\2\31\u00e8\3\2\2\2\33\u00eb\3\2\2\2\35\u00f0\3\2\2\2\37\u00f3\3"+
		"\2\2\2!\u00f6\3\2\2\2#\u00fd\3\2\2\2%\u0101\3\2\2\2\'\u0103\3\2\2\2)\u0105"+
		"\3\2\2\2+\u0107\3\2\2\2-\u0109\3\2\2\2/\u010b\3\2\2\2\61\u010d\3\2\2\2"+
		"\63\u010f\3\2\2\2\65\u0112\3\2\2\2\67\u0115\3\2\2\29\u0117\3\2\2\2;\u0119"+
		"\3\2\2\2=\u011c\3\2\2\2?\u011f\3\2\2\2A\u0122\3\2\2\2C\u0125\3\2\2\2E"+
		"\u0128\3\2\2\2G\u012f\3\2\2\2I\u0136\3\2\2\2K\u013f\3\2\2\2M\u0147\3\2"+
		"\2\2O\u014f\3\2\2\2Q\u0153\3\2\2\2S\u015b\3\2\2\2U\u0161\3\2\2\2W\u0166"+
		"\3\2\2\2Y\u016c\3\2\2\2[\u0173\3\2\2\2]\u017c\3\2\2\2_\u0180\3\2\2\2a"+
		"\u0184\3\2\2\2c\u018b\3\2\2\2e\u0196\3\2\2\2g\u019f\3\2\2\2i\u01a7\3\2"+
		"\2\2k\u01b6\3\2\2\2m\u01c8\3\2\2\2o\u01db\3\2\2\2q\u01e9\3\2\2\2s\u01f0"+
		"\3\2\2\2u\u01fb\3\2\2\2w\u0208\3\2\2\2y\u0210\3\2\2\2{\u0217\3\2\2\2}"+
		"\u021f\3\2\2\2\177\u0229\3\2\2\2\u0081\u0234\3\2\2\2\u0083\u023d\3\2\2"+
		"\2\u0085\u024b\3\2\2\2\u0087\u025b\3\2\2\2\u0089\u026d\3\2\2\2\u008b\u027a"+
		"\3\2\2\2\u008d\u0286\3\2\2\2\u008f\u029b\3\2\2\2\u0091\u02a7\3\2\2\2\u0093"+
		"\u02b8\3\2\2\2\u0095\u02c2\3\2\2\2\u0097\u02d5\3\2\2\2\u0099\u02e6\3\2"+
		"\2\2\u009b\u02fd\3\2\2\2\u009d\u0312\3\2\2\2\u009f\u031b\3\2\2\2\u00a1"+
		"\u032a\3\2\2\2\u00a3\u0336\3\2\2\2\u00a5\u0343\3\2\2\2\u00a7\u0351\3\2"+
		"\2\2\u00a9\u035e\3\2\2\2\u00ab\u0368\3\2\2\2\u00ad\u036d\3\2\2\2\u00af"+
		"\u0372\3\2\2\2\u00b1\u0396\3\2\2\2\u00b3\u0398\3\2\2\2\u00b5\u03ab\3\2"+
		"\2\2\u00b7\u03ad\3\2\2\2\u00b9\u03b9\3\2\2\2\u00bb\u03c1\3\2\2\2\u00bd"+
		"\u03c7\3\2\2\2\u00bf\u03d0\3\2\2\2\u00c1\u03d2\3\2\2\2\u00c3\u03d4\3\2"+
		"\2\2\u00c5\u03d6\3\2\2\2\u00c7\u03ed\3\2\2\2\u00c9\u03ef\3\2\2\2\u00cb"+
		"\u00cc\7*\2\2\u00cc\4\3\2\2\2\u00cd\u00ce\7+\2\2\u00ce\6\3\2\2\2\u00cf"+
		"\u00d0\7\60\2\2\u00d0\b\3\2\2\2\u00d1\u00d2\7X\2\2\u00d2\u00d3\7}\2\2"+
		"\u00d3\n\3\2\2\2\u00d4\u00d5\7\177\2\2\u00d5\f\3\2\2\2\u00d6\u00d7\7f"+
		"\2\2\u00d7\u00d8\7\64\2\2\u00d8\u00d9\7<\2\2\u00d9\16\3\2\2\2\u00da\u00db"+
		"\7.\2\2\u00db\20\3\2\2\2\u00dc\u00dd\7%\2\2\u00dd\u00de\7}\2\2\u00de\22"+
		"\3\2\2\2\u00df\u00e0\7\60\2\2\u00e0\u00e1\7,\2\2\u00e1\24\3\2\2\2\u00e2"+
		"\u00e3\7F\2\2\u00e3\u00e4\7}\2\2\u00e4\26\3\2\2\2\u00e5\u00e6\7C\2\2\u00e6"+
		"\u00e7\7}\2\2\u00e7\30\3\2\2\2\u00e8\u00e9\7K\2\2\u00e9\u00ea\7}\2\2\u00ea"+
		"\32\3\2\2\2\u00eb\u00ec\7Q\2\2\u00ec\u00ed\7W\2\2\u00ed\u00ee\7I\2\2\u00ee"+
		"\u00ef\7}\2\2\u00ef\34\3\2\2\2\u00f0\u00f1\7T\2\2\u00f1\u00f2\7}\2\2\u00f2"+
		"\36\3\2\2\2\u00f3\u00f4\7E\2\2\u00f4\u00f5\7}\2\2\u00f5 \3\2\2\2\u00f6"+
		"\u00f7\7]\2\2\u00f7\u00f8\7f\2\2\u00f8\u00f9\7c\2\2\u00f9\u00fa\7{\2\2"+
		"\u00fa\u00fb\7u\2\2\u00fb\u00fc\7_\2\2\u00fc\"\3\2\2\2\u00fd\u00fe\7\60"+
		"\2\2\u00fe\u00ff\7,\2\2\u00ff\u0100\7\60\2\2\u0100$\3\2\2\2\u0101\u0102"+
		"\7`\2\2\u0102&\3\2\2\2\u0103\u0104\7/\2\2\u0104(\3\2\2\2\u0105\u0106\7"+
		"-\2\2\u0106*\3\2\2\2\u0107\u0108\7#\2\2\u0108,\3\2\2\2\u0109\u010a\7,"+
		"\2\2\u010a.\3\2\2\2\u010b\u010c\7\61\2\2\u010c\60\3\2\2\2\u010d\u010e"+
		"\7\'\2\2\u010e\62\3\2\2\2\u010f\u0110\7>\2\2\u0110\u0111\7?\2\2\u0111"+
		"\64\3\2\2\2\u0112\u0113\7@\2\2\u0113\u0114\7?\2\2\u0114\66\3\2\2\2\u0115"+
		"\u0116\7>\2\2\u01168\3\2\2\2\u0117\u0118\7@\2\2\u0118:\3\2\2\2\u0119\u011a"+
		"\7?\2\2\u011a\u011b\7?\2\2\u011b<\3\2\2\2\u011c\u011d\7#\2\2\u011d\u011e"+
		"\7?\2\2\u011e>\3\2\2\2\u011f\u0120\7(\2\2\u0120\u0121\7(\2\2\u0121@\3"+
		"\2\2\2\u0122\u0123\7~\2\2\u0123\u0124\7~\2\2\u0124B\3\2\2\2\u0125\u0126"+
		"\7k\2\2\u0126\u0127\7h\2\2\u0127D\3\2\2\2\u0128\u0129\7g\2\2\u0129\u012a"+
		"\7z\2\2\u012a\u012b\7e\2\2\u012b\u012c\7g\2\2\u012c\u012d\7r\2\2\u012d"+
		"\u012e\7v\2\2\u012eF\3\2\2\2\u012f\u0130\7k\2\2\u0130\u0131\7u\2\2\u0131"+
		"\u0132\7P\2\2\u0132\u0133\7w\2\2\u0133\u0134\7n\2\2\u0134\u0135\7n\2\2"+
		"\u0135H\3\2\2\2\u0136\u0137\7e\2\2\u0137\u0138\7q\2\2\u0138\u0139\7c\2"+
		"\2\u0139\u013a\7n\2\2\u013a\u013b\7g\2\2\u013b\u013c\7u\2\2\u013c\u013d"+
		"\7e\2\2\u013d\u013e\7g\2\2\u013eJ\3\2\2\2\u013f\u0140\7o\2\2\u0140\u0141"+
		"\7c\2\2\u0141\u0142\7z\2\2\u0142\u0143\7k\2\2\u0143\u0144\7o\2\2\u0144"+
		"\u0145\7w\2\2\u0145\u0146\7o\2\2\u0146L\3\2\2\2\u0147\u0148\7o\2\2\u0148"+
		"\u0149\7k\2\2\u0149\u014a\7p\2\2\u014a\u014b\7k\2\2\u014b\u014c\7o\2\2"+
		"\u014c\u014d\7w\2\2\u014d\u014e\7o\2\2\u014eN\3\2\2\2\u014f\u0150\7u\2"+
		"\2\u0150\u0151\7w\2\2\u0151\u0152\7o\2\2\u0152P\3\2\2\2\u0153\u0154\7"+
		"c\2\2\u0154\u0155\7x\2\2\u0155\u0156\7g\2\2\u0156\u0157\7t\2\2\u0157\u0158"+
		"\7c\2\2\u0158\u0159\7i\2\2\u0159\u015a\7g\2\2\u015aR\3\2\2\2\u015b\u015c"+
		"\7h\2\2\u015c\u015d\7k\2\2\u015d\u015e\7t\2\2\u015e\u015f\7u\2\2\u015f"+
		"\u0160\7v\2\2\u0160T\3\2\2\2\u0161\u0162\7n\2\2\u0162\u0163\7c\2\2\u0163"+
		"\u0164\7u\2\2\u0164\u0165\7v\2\2\u0165V\3\2\2\2\u0166\u0167\7e\2\2\u0167"+
		"\u0168\7q\2\2\u0168\u0169\7w\2\2\u0169\u016a\7p\2\2\u016a\u016b\7v\2\2"+
		"\u016bX\3\2\2\2\u016c\u016d\7u\2\2\u016d\u016e\7v\2\2\u016e\u016f\7f\2"+
		"\2\u016f\u0170\7f\2\2\u0170\u0171\7g\2\2\u0171\u0172\7x\2\2\u0172Z\3\2"+
		"\2\2\u0173\u0174\7x\2\2\u0174\u0175\7c\2\2\u0175\u0176\7t\2\2\u0176\u0177"+
		"\7k\2\2\u0177\u0178\7c\2\2\u0178\u0179\7p\2\2\u0179\u017a\7e\2\2\u017a"+
		"\u017b\7g\2\2\u017b\\\3\2\2\2\u017c\u017d\7o\2\2\u017d\u017e\7k\2\2\u017e"+
		"\u017f\7p\2\2\u017f^\3\2\2\2\u0180\u0181\7o\2\2\u0181\u0182\7c\2\2\u0182"+
		"\u0183\7z\2\2\u0183`\3\2\2\2\u0184\u0185\7o\2\2\u0185\u0186\7g\2\2\u0186"+
		"\u0187\7f\2\2\u0187\u0188\7k\2\2\u0188\u0189\7c\2\2\u0189\u018a\7p\2\2"+
		"\u018ab\3\2\2\2\u018b\u018c\7r\2\2\u018c\u018d\7g\2\2\u018d\u018e\7t\2"+
		"\2\u018e\u018f\7e\2\2\u018f\u0190\7g\2\2\u0190\u0191\7p\2\2\u0191\u0192"+
		"\7v\2\2\u0192\u0193\7k\2\2\u0193\u0194\7n\2\2\u0194\u0195\7g\2\2\u0195"+
		"d\3\2\2\2\u0196\u0197\7t\2\2\u0197\u0198\7c\2\2\u0198\u0199\7p\2\2\u0199"+
		"\u019a\7m\2\2\u019a\u019b\7J\2\2\u019b\u019c\7k\2\2\u019c\u019d\7i\2\2"+
		"\u019d\u019e\7j\2\2\u019ef\3\2\2\2\u019f\u01a0\7t\2\2\u01a0\u01a1\7c\2"+
		"\2\u01a1\u01a2\7p\2\2\u01a2\u01a3\7m\2\2\u01a3\u01a4\7N\2\2\u01a4\u01a5"+
		"\7q\2\2\u01a5\u01a6\7y\2\2\u01a6h\3\2\2\2\u01a7\u01a8\7t\2\2\u01a8\u01a9"+
		"\7c\2\2\u01a9\u01aa\7p\2\2\u01aa\u01ab\7m\2\2\u01ab\u01ac\7R\2\2\u01ac"+
		"\u01ad\7g\2\2\u01ad\u01ae\7t\2\2\u01ae\u01af\7e\2\2\u01af\u01b0\7g\2\2"+
		"\u01b0\u01b1\7p\2\2\u01b1\u01b2\7v\2\2\u01b2\u01b3\7k\2\2\u01b3\u01b4"+
		"\7n\2\2\u01b4\u01b5\7g\2\2\u01b5j\3\2\2\2\u01b6\u01b7\7c\2\2\u01b7\u01b8"+
		"\7x\2\2\u01b8\u01b9\7g\2\2\u01b9\u01ba\7t\2\2\u01ba\u01bb\7c\2\2\u01bb"+
		"\u01bc\7i\2\2\u01bc\u01bd\7g\2\2\u01bd\u01be\7U\2\2\u01be\u01bf\7w\2\2"+
		"\u01bf\u01c0\7o\2\2\u01c0\u01c1\7Q\2\2\u01c1\u01c2\7t\2\2\u01c2\u01c3"+
		"\7i\2\2\u01c3\u01c4\7W\2\2\u01c4\u01c5\7p\2\2\u01c5\u01c6\7k\2\2\u01c6"+
		"\u01c7\7v\2\2\u01c7l\3\2\2\2\u01c8\u01c9\7n\2\2\u01c9\u01ca\7c\2\2\u01ca"+
		"\u01cb\7u\2\2\u01cb\u01cc\7v\2\2\u01cc\u01cd\7C\2\2\u01cd\u01ce\7x\2\2"+
		"\u01ce\u01cf\7g\2\2\u01cf\u01d0\7t\2\2\u01d0\u01d1\7c\2\2\u01d1\u01d2"+
		"\7i\2\2\u01d2\u01d3\7g\2\2\u01d3\u01d4\7Q\2\2\u01d4\u01d5\7t\2\2\u01d5"+
		"\u01d6\7i\2\2\u01d6\u01d7\7W\2\2\u01d7\u01d8\7p\2\2\u01d8\u01d9\7k\2\2"+
		"\u01d9\u01da\7v\2\2\u01dan\3\2\2\2\u01db\u01dc\7p\2\2\u01dc\u01dd\7q\2"+
		"\2\u01dd\u01de\7C\2\2\u01de\u01df\7i\2\2\u01df\u01e0\7i\2\2\u01e0\u01e1"+
		"\7t\2\2\u01e1\u01e2\7g\2\2\u01e2\u01e3\7i\2\2\u01e3\u01e4\7c\2\2\u01e4"+
		"\u01e5\7v\2\2\u01e5\u01e6\7k\2\2\u01e6\u01e7\7q\2\2\u01e7\u01e8\7p\2\2"+
		"\u01e8p\3\2\2\2\u01e9\u01ea\7r\2\2\u01ea\u01eb\7g\2\2\u01eb\u01ec\7t\2"+
		"\2\u01ec\u01ed\7k\2\2\u01ed\u01ee\7q\2\2\u01ee\u01ef\7f\2\2\u01efr\3\2"+
		"\2\2\u01f0\u01f1\7q\2\2\u01f1\u01f2\7w\2\2\u01f2\u01f3\7C\2\2\u01f3\u01f4"+
		"\7p\2\2\u01f4\u01f5\7e\2\2\u01f5\u01f6\7g\2\2\u01f6\u01f7\7u\2\2\u01f7"+
		"\u01f8\7v\2\2\u01f8\u01f9\7q\2\2\u01f9\u01fa\7t\2\2\u01fat\3\2\2\2\u01fb"+
		"\u01fc\7q\2\2\u01fc\u01fd\7w\2\2\u01fd\u01fe\7F\2\2\u01fe\u01ff\7g\2\2"+
		"\u01ff\u0200\7u\2\2\u0200\u0201\7e\2\2\u0201\u0202\7g\2\2\u0202\u0203"+
		"\7p\2\2\u0203\u0204\7f\2\2\u0204\u0205\7c\2\2\u0205\u0206\7p\2\2\u0206"+
		"\u0207\7v\2\2\u0207v\3\2\2\2\u0208\u0209\7q\2\2\u0209\u020a\7w\2\2\u020a"+
		"\u020b\7N\2\2\u020b\u020c\7g\2\2\u020c\u020d\7x\2\2\u020d\u020e\7g\2\2"+
		"\u020e\u020f\7n\2\2\u020fx\3\2\2\2\u0210\u0211\7q\2\2\u0211\u0212\7w\2"+
		"\2\u0212\u0213\7R\2\2\u0213\u0214\7g\2\2\u0214\u0215\7g\2\2\u0215\u0216"+
		"\7t\2\2\u0216z\3\2\2\2\u0217\u0218\7q\2\2\u0218\u0219\7w\2\2\u0219\u021a"+
		"\7I\2\2\u021a\u021b\7t\2\2\u021b\u021c\7q\2\2\u021c\u021d\7w\2\2\u021d"+
		"\u021e\7r\2\2\u021e|\3\2\2\2\u021f\u0220\7q\2\2\u0220\u0221\7w\2\2\u0221"+
		"\u0222\7F\2\2\u0222\u0223\7c\2\2\u0223\u0224\7v\2\2\u0224\u0225\7c\2\2"+
		"\u0225\u0226\7U\2\2\u0226\u0227\7g\2\2\u0227\u0228\7v\2\2\u0228~\3\2\2"+
		"\2\u0229\u022a\7g\2\2\u022a\u022b\7x\2\2\u022b\u022c\7g\2\2\u022c\u022d"+
		"\7p\2\2\u022d\u022e\7v\2\2\u022e\u022f\7a\2\2\u022f\u0230\7f\2\2\u0230"+
		"\u0231\7c\2\2\u0231\u0232\7v\2\2\u0232\u0233\7g\2\2\u0233\u0080\3\2\2"+
		"\2\u0234\u0235\7f\2\2\u0235\u0236\7w\2\2\u0236\u0237\7g\2\2\u0237\u0238"+
		"\7a\2\2\u0238\u0239\7f\2\2\u0239\u023a\7c\2\2\u023a\u023b\7v\2\2\u023b"+
		"\u023c\7g\2\2\u023c\u0082\3\2\2\2\u023d\u023e\7k\2\2\u023e\u023f\7p\2"+
		"\2\u023f\u0240\7e\2\2\u0240\u0241\7k\2\2\u0241\u0242\7f\2\2\u0242\u0243"+
		"\7g\2\2\u0243\u0244\7p\2\2\u0244\u0245\7v\2\2\u0245\u0246\7a\2\2\u0246"+
		"\u0247\7f\2\2\u0247\u0248\7c\2\2\u0248\u0249\7v\2\2\u0249\u024a\7g\2\2"+
		"\u024a\u0084\3\2\2\2\u024b\u024c\7g\2\2\u024c\u024d\7p\2\2\u024d\u024e"+
		"\7t\2\2\u024e\u024f\7q\2\2\u024f\u0250\7n\2\2\u0250\u0251\7n\2\2\u0251"+
		"\u0252\7o\2\2\u0252\u0253\7g\2\2\u0253\u0254\7p\2\2\u0254\u0255\7v\2\2"+
		"\u0255\u0256\7a\2\2\u0256\u0257\7f\2\2\u0257\u0258\7c\2\2\u0258\u0259"+
		"\7v\2\2\u0259\u025a\7g\2\2\u025a\u0086\3\2\2\2\u025b\u025c\7g\2\2\u025c"+
		"\u025d\7p\2\2\u025d\u025e\7t\2\2\u025e\u025f\7q\2\2\u025f\u0260\7n\2\2"+
		"\u0260\u0261\7n\2\2\u0261\u0262\7o\2\2\u0262\u0263\7g\2\2\u0263\u0264"+
		"\7p\2\2\u0264\u0265\7v\2\2\u0265\u0266\7a\2\2\u0266\u0267\7u\2\2\u0267"+
		"\u0268\7v\2\2\u0268\u0269\7c\2\2\u0269\u026a\7v\2\2\u026a\u026b\7w\2\2"+
		"\u026b\u026c\7u\2\2\u026c\u0088\3\2\2\2\u026d\u026e\7e\2\2\u026e\u026f"+
		"\7w\2\2\u026f\u0270\7t\2\2\u0270\u0271\7t\2\2\u0271\u0272\7g\2\2\u0272"+
		"\u0273\7p\2\2\u0273\u0274\7v\2\2\u0274\u0275\7a\2\2\u0275\u0276\7f\2\2"+
		"\u0276\u0277\7c\2\2\u0277\u0278\7v\2\2\u0278\u0279\7g\2\2\u0279\u008a"+
		"\3\2\2\2\u027a\u027b\7x\2\2\u027b\u027c\7c\2\2\u027c\u027d\7n\2\2\u027d"+
		"\u027e\7w\2\2\u027e\u027f\7g\2\2\u027f\u0280\7a\2\2\u0280\u0281\7e\2\2"+
		"\u0281\u0282\7q\2\2\u0282\u0283\7w\2\2\u0283\u0284\7p\2\2\u0284\u0285"+
		"\7v\2\2\u0285\u008c\3\2\2\2\u0286\u0287\7|\2\2\u0287\u0288\7g\2\2\u0288"+
		"\u0289\7t\2\2\u0289\u028a\7q\2\2\u028a\u028b\7a\2\2\u028b\u028c\7r\2\2"+
		"\u028c\u028d\7q\2\2\u028d\u028e\7u\2\2\u028e\u028f\7a\2\2\u028f\u0290"+
		"\7x\2\2\u0290\u0291\7c\2\2\u0291\u0292\7n\2\2\u0292\u0293\7w\2\2\u0293"+
		"\u0294\7g\2\2\u0294\u0295\7a\2\2\u0295\u0296\7e\2\2\u0296\u0297\7q\2\2"+
		"\u0297\u0298\7w\2\2\u0298\u0299\7p\2\2\u0299\u029a\7v\2\2\u029a\u008e"+
		"\3\2\2\2\u029b\u029c\7g\2\2\u029c\u029d\7x\2\2\u029d\u029e\7g\2\2\u029e"+
		"\u029f\7p\2\2\u029f\u02a0\7v\2\2\u02a0\u02a1\7a\2\2\u02a1\u02a2\7e\2\2"+
		"\u02a2\u02a3\7q\2\2\u02a3\u02a4\7w\2\2\u02a4\u02a5\7p\2\2\u02a5\u02a6"+
		"\7v\2\2\u02a6\u0090\3\2\2\2\u02a7\u02a8\7g\2\2\u02a8\u02a9\7p\2\2\u02a9"+
		"\u02aa\7t\2\2\u02aa\u02ab\7q\2\2\u02ab\u02ac\7n\2\2\u02ac\u02ad\7n\2\2"+
		"\u02ad\u02ae\7o\2\2\u02ae\u02af\7g\2\2\u02af\u02b0\7p\2\2\u02b0\u02b1"+
		"\7v\2\2\u02b1\u02b2\7a\2\2\u02b2\u02b3\7e\2\2\u02b3\u02b4\7q\2\2\u02b4"+
		"\u02b5\7w\2\2\u02b5\u02b6\7p\2\2\u02b6\u02b7\7v\2\2\u02b7\u0092\3\2\2"+
		"\2\u02b8\u02b9\7v\2\2\u02b9\u02ba\7g\2\2\u02ba\u02bb\7k\2\2\u02bb\u02bc"+
		"\7a\2\2\u02bc\u02bd\7e\2\2\u02bd\u02be\7q\2\2\u02be\u02bf\7w\2\2\u02bf"+
		"\u02c0\7p\2\2\u02c0\u02c1\7v\2\2\u02c1\u0094\3\2\2\2\u02c2\u02c3\7r\2"+
		"\2\u02c3\u02c4\7t\2\2\u02c4\u02c5\7q\2\2\u02c5\u02c6\7i\2\2\u02c6\u02c7"+
		"\7t\2\2\u02c7\u02c8\7c\2\2\u02c8\u02c9\7o\2\2\u02c9\u02ca\7a\2\2\u02ca"+
		"\u02cb\7u\2\2\u02cb\u02cc\7v\2\2\u02cc\u02cd\7c\2\2\u02cd\u02ce\7i\2\2"+
		"\u02ce\u02cf\7g\2\2\u02cf\u02d0\7a\2\2\u02d0\u02d1\7p\2\2\u02d1\u02d2"+
		"\7c\2\2\u02d2\u02d3\7o\2\2\u02d3\u02d4\7g\2\2\u02d4\u0096\3\2\2\2\u02d5"+
		"\u02d6\7r\2\2\u02d6\u02d7\7t\2\2\u02d7\u02d8\7q\2\2\u02d8\u02d9\7i\2\2"+
		"\u02d9\u02da\7t\2\2\u02da\u02db\7c\2\2\u02db\u02dc\7o\2\2\u02dc\u02dd"+
		"\7a\2\2\u02dd\u02de\7u\2\2\u02de\u02df\7v\2\2\u02df\u02e0\7c\2\2\u02e0"+
		"\u02e1\7i\2\2\u02e1\u02e2\7g\2\2\u02e2\u02e3\7a\2\2\u02e3\u02e4\7k\2\2"+
		"\u02e4\u02e5\7f\2\2\u02e5\u0098\3\2\2\2\u02e6\u02e7\7t\2\2\u02e7\u02e8"+
		"\7g\2\2\u02e8\u02e9\7r\2\2\u02e9\u02ea\7q\2\2\u02ea\u02eb\7t\2\2\u02eb"+
		"\u02ec\7v\2\2\u02ec\u02ed\7k\2\2\u02ed\u02ee\7p\2\2\u02ee\u02ef\7i\2\2"+
		"\u02ef\u02f0\7a\2\2\u02f0\u02f1\7r\2\2\u02f1\u02f2\7g\2\2\u02f2\u02f3"+
		"\7t\2\2\u02f3\u02f4\7k\2\2\u02f4\u02f5\7q\2\2\u02f5\u02f6\7f\2\2\u02f6"+
		"\u02f7\7a\2\2\u02f7\u02f8\7u\2\2\u02f8\u02f9\7v\2\2\u02f9\u02fa\7c\2\2"+
		"\u02fa\u02fb\7t\2\2\u02fb\u02fc\7v\2\2\u02fc\u009a\3\2\2\2\u02fd\u02fe"+
		"\7t\2\2\u02fe\u02ff\7g\2\2\u02ff\u0300\7r\2\2\u0300\u0301\7q\2\2\u0301"+
		"\u0302\7t\2\2\u0302\u0303\7v\2\2\u0303\u0304\7k\2\2\u0304\u0305\7p\2\2"+
		"\u0305\u0306\7i\2\2\u0306\u0307\7a\2\2\u0307\u0308\7r\2\2\u0308\u0309"+
		"\7g\2\2\u0309\u030a\7t\2\2\u030a\u030b\7k\2\2\u030b\u030c\7q\2\2\u030c"+
		"\u030d\7f\2\2\u030d\u030e\7a\2\2\u030e\u030f\7g\2\2\u030f\u0310\7p\2\2"+
		"\u0310\u0311\7f\2\2\u0311\u009c\3\2\2\2\u0312\u0313\7j\2\2\u0313\u0314"+
		"\7c\2\2\u0314\u0315\7u\2\2\u0315\u0316\7X\2\2\u0316\u0317\7c\2\2\u0317"+
		"\u0318\7n\2\2\u0318\u0319\7w\2\2\u0319\u031a\7g\2\2\u031a\u009e\3\2\2"+
		"\2\u031b\u031c\7o\2\2\u031c\u031d\7k\2\2\u031d\u031e\7p\2\2\u031e\u031f"+
		"\7w\2\2\u031f\u0320\7v\2\2\u0320\u0321\7g\2\2\u0321\u0322\7u\2\2\u0322"+
		"\u0323\7D\2\2\u0323\u0324\7g\2\2\u0324\u0325\7v\2\2\u0325\u0326\7y\2\2"+
		"\u0326\u0327\7g\2\2\u0327\u0328\7g\2\2\u0328\u0329\7p\2\2\u0329\u00a0"+
		"\3\2\2\2\u032a\u032b\7f\2\2\u032b\u032c\7c\2\2\u032c\u032d\7{\2\2\u032d"+
		"\u032e\7u\2\2\u032e\u032f\7D\2\2\u032f\u0330\7g\2\2\u0330\u0331\7v\2\2"+
		"\u0331\u0332\7y\2\2\u0332\u0333\7g\2\2\u0333\u0334\7g\2\2\u0334\u0335"+
		"\7p\2\2\u0335\u00a2\3\2\2\2\u0336\u0337\7y\2\2\u0337\u0338\7g\2\2\u0338"+
		"\u0339\7g\2\2\u0339\u033a\7m\2\2\u033a\u033b\7u\2\2\u033b\u033c\7D\2\2"+
		"\u033c\u033d\7g\2\2\u033d\u033e\7v\2\2\u033e\u033f\7y\2\2\u033f\u0340"+
		"\7g\2\2\u0340\u0341\7g\2\2\u0341\u0342\7p\2\2\u0342\u00a4\3\2\2\2\u0343"+
		"\u0344\7o\2\2\u0344\u0345\7q\2\2\u0345\u0346\7p\2\2\u0346\u0347\7v\2\2"+
		"\u0347\u0348\7j\2\2\u0348\u0349\7u\2\2\u0349\u034a\7D\2\2\u034a\u034b"+
		"\7g\2\2\u034b\u034c\7v\2\2\u034c\u034d\7y\2\2\u034d\u034e\7g\2\2\u034e"+
		"\u034f\7g\2\2\u034f\u0350\7p\2\2\u0350\u00a6\3\2\2\2\u0351\u0352\7{\2"+
		"\2\u0352\u0353\7g\2\2\u0353\u0354\7c\2\2\u0354\u0355\7t\2\2\u0355\u0356"+
		"\7u\2\2\u0356\u0357\7D\2\2\u0357\u0358\7g\2\2\u0358\u0359\7v\2\2\u0359"+
		"\u035a\7y\2\2\u035a\u035b\7g\2\2\u035b\u035c\7g\2\2\u035c\u035d\7p\2\2"+
		"\u035d\u00a8\3\2\2\2\u035e\u035f\7e\2\2\u035f\u0360\7q\2\2\u0360\u0361"+
		"\7p\2\2\u0361\u0362\7f\2\2\u0362\u0363\7k\2\2\u0363\u0364\7v\2\2\u0364"+
		"\u0365\7k\2\2\u0365\u0366\7q\2\2\u0366\u0367\7p\2\2\u0367\u00aa\3\2\2"+
		"\2\u0368\u0369\7|\2\2\u0369\u036a\7k\2\2\u036a\u036b\7p\2\2\u036b\u036c"+
		"\7i\2\2\u036c\u00ac\3\2\2\2\u036d\u036e\7q\2\2\u036e\u036f\7k\2\2\u036f"+
		"\u0370\7|\2\2\u0370\u0371\7r\2\2\u0371\u00ae\3\2\2\2\u0372\u0373\7|\2"+
		"\2\u0373\u0374\7r\2\2\u0374\u0375\7x\2\2\u0375\u0376\7e\2\2\u0376\u00b0"+
		"\3\2\2\2\u0377\u0380\7\62\2\2\u0378\u037c\t\2\2\2\u0379\u037b\t\3\2\2"+
		"\u037a\u0379\3\2\2\2\u037b\u037e\3\2\2\2\u037c\u037a\3\2\2\2\u037c\u037d"+
		"\3\2\2\2\u037d\u0380\3\2\2\2\u037e\u037c\3\2\2\2\u037f\u0377\3\2\2\2\u037f"+
		"\u0378\3\2\2\2\u0380\u0388\3\2\2\2\u0381\u0385\7\60\2\2\u0382\u0384\t"+
		"\3\2\2\u0383\u0382\3\2\2\2\u0384\u0387\3\2\2\2\u0385\u0383\3\2\2\2\u0385"+
		"\u0386\3\2\2\2\u0386\u0389\3\2\2\2\u0387\u0385\3\2\2\2\u0388\u0381\3\2"+
		"\2\2\u0388\u0389\3\2\2\2\u0389\u038b\3\2\2\2\u038a\u038c\5\u00bd_\2\u038b"+
		"\u038a\3\2\2\2\u038b\u038c\3\2\2\2\u038c\u0397\3\2\2\2\u038d\u038f\7\60"+
		"\2\2\u038e\u0390\t\3\2\2\u038f\u038e\3\2\2\2\u0390\u0391\3\2\2\2\u0391"+
		"\u038f\3\2\2\2\u0391\u0392\3\2\2\2\u0392\u0394\3\2\2\2\u0393\u0395\5\u00bd"+
		"_\2\u0394\u0393\3\2\2\2\u0394\u0395\3\2\2\2\u0395\u0397\3\2\2\2\u0396"+
		"\u037f\3\2\2\2\u0396\u038d\3\2\2\2\u0397\u00b2\3\2\2\2\u0398\u039d\7$"+
		"\2\2\u0399\u039c\n\4\2\2\u039a\u039c\5\u00c7d\2\u039b\u0399\3\2\2\2\u039b"+
		"\u039a\3\2\2\2\u039c\u039f\3\2\2\2\u039d\u039b\3\2\2\2\u039d\u039e\3\2"+
		"\2\2\u039e\u03a0\3\2\2\2\u039f\u039d\3\2\2\2\u03a0\u03a1\7$\2\2\u03a1"+
		"\u00b4\3\2\2\2\u03a2\u03a3\7v\2\2\u03a3\u03a4\7t\2\2\u03a4\u03a5\7w\2"+
		"\2\u03a5\u03ac\7g\2\2\u03a6\u03a7\7h\2\2\u03a7\u03a8\7c\2\2\u03a8\u03a9"+
		"\7n\2\2\u03a9\u03aa\7u\2\2\u03aa\u03ac\7g\2\2\u03ab\u03a2\3\2\2\2\u03ab"+
		"\u03a6\3\2\2\2\u03ac\u00b6\3\2\2\2\u03ad\u03ae\5\u00bf`\2\u03ae\u03af"+
		"\5\u00c1a\2\u03af\u03b0\5\u00c1a\2\u03b0\u03b1\5\u00c1a\2\u03b1\u03b2"+
		"\5\u00c1a\2\u03b2\u03b3\5\u00c1a\2\u03b3\u03b4\5\u00c1a\2\u03b4\u03b5"+
		"\5\u00c1a\2\u03b5\u03b6\5\u00c1a\2\u03b6\u03b7\5\u00c1a\2\u03b7\u03b8"+
		"\5\u00c1a\2\u03b8\u00b8\3\2\2\2\u03b9\u03bd\5\u00c3b\2\u03ba\u03bc\5\u00c5"+
		"c\2\u03bb\u03ba\3\2\2\2\u03bc\u03bf\3\2\2\2\u03bd\u03bb\3\2\2\2\u03bd"+
		"\u03be\3\2\2\2\u03be\u00ba\3\2\2\2\u03bf\u03bd\3\2\2\2\u03c0\u03c2\t\5"+
		"\2\2\u03c1\u03c0\3\2\2\2\u03c2\u03c3\3\2\2\2\u03c3\u03c1\3\2\2\2\u03c3"+
		"\u03c4\3\2\2\2\u03c4\u03c5\3\2\2\2\u03c5\u03c6\b^\2\2\u03c6\u00bc\3\2"+
		"\2\2\u03c7\u03c9\t\6\2\2\u03c8\u03ca\t\7\2\2\u03c9\u03c8\3\2\2\2\u03c9"+
		"\u03ca\3\2\2\2\u03ca\u03cc\3\2\2\2\u03cb\u03cd\t\3\2\2\u03cc\u03cb\3\2"+
		"\2\2\u03cd\u03ce\3\2\2\2\u03ce\u03cc\3\2\2\2\u03ce\u03cf\3\2\2\2\u03cf"+
		"\u00be\3\2\2\2\u03d0\u03d1\t\b\2\2\u03d1\u00c0\3\2\2\2\u03d2\u03d3\t\t"+
		"\2\2\u03d3\u00c2\3\2\2\2\u03d4\u03d5\t\n\2\2\u03d5\u00c4\3\2\2\2\u03d6"+
		"\u03d7\t\13\2\2\u03d7\u00c6\3\2\2\2\u03d8\u03d9\7^\2\2\u03d9\u03ee\t\f"+
		"\2\2\u03da\u03df\7^\2\2\u03db\u03dd\t\r\2\2\u03dc\u03db\3\2\2\2\u03dc"+
		"\u03dd\3\2\2\2\u03dd\u03de\3\2\2\2\u03de\u03e0\t\16\2\2\u03df\u03dc\3"+
		"\2\2\2\u03df\u03e0\3\2\2\2\u03e0\u03e1\3\2\2\2\u03e1\u03ee\t\16\2\2\u03e2"+
		"\u03e4\7^\2\2\u03e3\u03e5\7w\2\2\u03e4\u03e3\3\2\2\2\u03e5\u03e6\3\2\2"+
		"\2\u03e6\u03e4\3\2\2\2\u03e6\u03e7\3\2\2\2\u03e7\u03e8\3\2\2\2\u03e8\u03e9"+
		"\5\u00c9e\2\u03e9\u03ea\5\u00c9e\2\u03ea\u03eb\5\u00c9e\2\u03eb\u03ec"+
		"\5\u00c9e\2\u03ec\u03ee\3\2\2\2\u03ed\u03d8\3\2\2\2\u03ed\u03da\3\2\2"+
		"\2\u03ed\u03e2\3\2\2\2\u03ee\u00c8\3\2\2\2\u03ef\u03f0\t\17\2\2\u03f0"+
		"\u00ca\3\2\2\2\26\2\u037c\u037f\u0385\u0388\u038b\u0391\u0394\u0396\u039b"+
		"\u039d\u03ab\u03bd\u03c3\u03c9\u03ce\u03dc\u03df\u03e6\u03ed\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}