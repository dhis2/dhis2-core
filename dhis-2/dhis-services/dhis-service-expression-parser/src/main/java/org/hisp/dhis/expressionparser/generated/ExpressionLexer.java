// Generated from /Users/jim/dev/dhis2/dhis2-core/dhis-2/dhis-services/dhis-service-expression-parser/src/main/resources/org/hisp/dhis/expressionparser/Expression.g4 by ANTLR 4.7.2
package org.hisp.dhis.expressionparser.generated;
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
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		POWER=18, MINUS=19, PLUS=20, NOT=21, MUL=22, DIV=23, MOD=24, LEQ=25, GEQ=26, 
		LT=27, GT=28, EQ=29, NE=30, AND=31, OR=32, IF=33, IS_NULL=34, COALESCE=35, 
		MAXIMUM=36, MINIMUM=37, EVENT_DATE=38, DUE_DATE=39, INCIDENT_DATE=40, 
		ENROLLMENT_DATE=41, ENROLLMENT_STATUS=42, CURRENT_STATUS=43, VALUE_COUNT=44, 
		ZERO_POS_VALUE_COUNT=45, EVENT_COUNT=46, ENROLLMENT_COUNT=47, TEI_COUNT=48, 
		PROGRAM_STAGE_NAME=49, PROGRAM_STAGE_ID=50, REPORTING_PERIOD_START=51, 
		REPORTING_PERIOD_END=52, HAS_VALUE=53, MINUTES_BETWEEN=54, DAYS_BETWEEN=55, 
		WEEKS_BETWEEN=56, MONTHS_BETWEEN=57, YEARS_BETWEEN=58, CONDITION=59, ZING=60, 
		OIZP=61, ZPVC=62, NUMERIC_LITERAL=63, STRING_LITERAL=64, BOOLEAN_LITERAL=65, 
		UID=66, KEYWORD=67, WS=68;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
			"POWER", "MINUS", "PLUS", "NOT", "MUL", "DIV", "MOD", "LEQ", "GEQ", "LT", 
			"GT", "EQ", "NE", "AND", "OR", "IF", "IS_NULL", "COALESCE", "MAXIMUM", 
			"MINIMUM", "EVENT_DATE", "DUE_DATE", "INCIDENT_DATE", "ENROLLMENT_DATE", 
			"ENROLLMENT_STATUS", "CURRENT_STATUS", "VALUE_COUNT", "ZERO_POS_VALUE_COUNT", 
			"EVENT_COUNT", "ENROLLMENT_COUNT", "TEI_COUNT", "PROGRAM_STAGE_NAME", 
			"PROGRAM_STAGE_ID", "REPORTING_PERIOD_START", "REPORTING_PERIOD_END", 
			"HAS_VALUE", "MINUTES_BETWEEN", "DAYS_BETWEEN", "WEEKS_BETWEEN", "MONTHS_BETWEEN", 
			"YEARS_BETWEEN", "CONDITION", "ZING", "OIZP", "ZPVC", "NUMERIC_LITERAL", 
			"STRING_LITERAL", "BOOLEAN_LITERAL", "UID", "KEYWORD", "WS", "Exponent", 
			"Alpha", "AlphaNum", "KeywordStart", "KeywordPart", "EscapeSequence", 
			"HexDigit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'V{'", "'}'", "'d2:'", "','", "'#{'", "'.'", "'.*'", 
			"'.*.'", "'D{'", "'A{'", "'I{'", "'OUG{'", "'R{'", "'C{'", "'[days]'", 
			"'^'", "'-'", "'+'", "'!'", "'*'", "'/'", "'%'", "'<='", "'>='", "'<'", 
			"'>'", "'=='", "'!='", "'&&'", "'||'", "'if'", "'isNull'", "'coalesce'", 
			"'maximum'", "'minimum'", "'event_date'", "'due_date'", "'incident_date'", 
			"'enrollment_date'", "'enrollment_status'", "'current_date'", "'value_count'", 
			"'zero_pos_value_count'", "'event_count'", "'enrollment_count'", "'tei_count'", 
			"'program_stage_name'", "'program_stage_id'", "'reporting_period_start'", 
			"'reporting_period_end'", "'hasValue'", "'minutesBetween'", "'daysBetween'", 
			"'weeksBetween'", "'monthsBetween'", "'yearsBetween'", "'condition'", 
			"'zing'", "'oizp'", "'zpvc'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, "POWER", "MINUS", "PLUS", "NOT", 
			"MUL", "DIV", "MOD", "LEQ", "GEQ", "LT", "GT", "EQ", "NE", "AND", "OR", 
			"IF", "IS_NULL", "COALESCE", "MAXIMUM", "MINIMUM", "EVENT_DATE", "DUE_DATE", 
			"INCIDENT_DATE", "ENROLLMENT_DATE", "ENROLLMENT_STATUS", "CURRENT_STATUS", 
			"VALUE_COUNT", "ZERO_POS_VALUE_COUNT", "EVENT_COUNT", "ENROLLMENT_COUNT", 
			"TEI_COUNT", "PROGRAM_STAGE_NAME", "PROGRAM_STAGE_ID", "REPORTING_PERIOD_START", 
			"REPORTING_PERIOD_END", "HAS_VALUE", "MINUTES_BETWEEN", "DAYS_BETWEEN", 
			"WEEKS_BETWEEN", "MONTHS_BETWEEN", "YEARS_BETWEEN", "CONDITION", "ZING", 
			"OIZP", "ZPVC", "NUMERIC_LITERAL", "STRING_LITERAL", "BOOLEAN_LITERAL", 
			"UID", "KEYWORD", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2F\u02de\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3\5\3\5\3\6\3\6\3\6"+
		"\3\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\f\3"+
		"\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3"+
		"\20\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\24\3"+
		"\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32\3\32\3"+
		"\33\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\36\3\37\3\37\3\37\3 \3 "+
		"\3 \3!\3!\3!\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3$\3$\3$"+
		"\3$\3%\3%\3%\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3&\3&\3&\3\'\3\'\3\'\3\'\3"+
		"\'\3\'\3\'\3\'\3\'\3\'\3\'\3(\3(\3(\3(\3(\3(\3(\3(\3(\3)\3)\3)\3)\3)\3"+
		")\3)\3)\3)\3)\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3*\3"+
		"*\3*\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3+\3,\3,\3,\3"+
		",\3,\3,\3,\3,\3,\3,\3,\3,\3,\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3-\3.\3"+
		".\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3/\3/\3/\3"+
		"/\3/\3/\3/\3/\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60"+
		"\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\61\3\61\3\61"+
		"\3\61\3\61\3\61\3\61\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62"+
		"\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\63\3\63\3\63\3\63\3\63"+
		"\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\64\3\64"+
		"\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\64"+
		"\3\64\3\64\3\64\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3\65\3\65\3\65"+
		"\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65\3\65"+
		"\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\3\67"+
		"\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\3\67\38\38\38\38\38\38\3"+
		"8\38\38\38\38\38\39\39\39\39\39\39\39\39\39\39\39\39\39\3:\3:\3:\3:\3"+
		":\3:\3:\3:\3:\3:\3:\3:\3:\3:\3;\3;\3;\3;\3;\3;\3;\3;\3;\3;\3;\3;\3;\3"+
		"<\3<\3<\3<\3<\3<\3<\3<\3<\3<\3=\3=\3=\3=\3=\3>\3>\3>\3>\3>\3?\3?\3?\3"+
		"?\3?\3@\3@\3@\7@\u0268\n@\f@\16@\u026b\13@\5@\u026d\n@\3@\3@\7@\u0271"+
		"\n@\f@\16@\u0274\13@\5@\u0276\n@\3@\5@\u0279\n@\3@\3@\6@\u027d\n@\r@\16"+
		"@\u027e\3@\5@\u0282\n@\5@\u0284\n@\3A\3A\3A\7A\u0289\nA\fA\16A\u028c\13"+
		"A\3A\3A\3B\3B\3B\3B\3B\3B\3B\3B\3B\5B\u0299\nB\3C\3C\3C\3C\3C\3C\3C\3"+
		"C\3C\3C\3C\3C\3D\3D\7D\u02a9\nD\fD\16D\u02ac\13D\3E\6E\u02af\nE\rE\16"+
		"E\u02b0\3E\3E\3F\3F\5F\u02b7\nF\3F\6F\u02ba\nF\rF\16F\u02bb\3G\3G\3H\3"+
		"H\3I\3I\3J\3J\3K\3K\3K\3K\5K\u02ca\nK\3K\5K\u02cd\nK\3K\3K\3K\6K\u02d2"+
		"\nK\rK\16K\u02d3\3K\3K\3K\3K\3K\5K\u02db\nK\3L\3L\2\2M\3\3\5\4\7\5\t\6"+
		"\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24"+
		"\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K"+
		"\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66k\67m8o9q:s;u<w=y>{?}@\177"+
		"A\u0081B\u0083C\u0085D\u0087E\u0089F\u008b\2\u008d\2\u008f\2\u0091\2\u0093"+
		"\2\u0095\2\u0097\2\3\2\20\3\2\63;\3\2\62;\6\2\f\f\17\17$$^^\5\2\13\f\17"+
		"\17\"\"\4\2GGgg\4\2--//\4\2C\\c|\5\2\62;C\\c|\6\2&&C\\aac|\7\2&&\62;C"+
		"\\aac|\n\2$$))^^ddhhppttvv\3\2\62\65\3\2\629\5\2\62;CHch\2\u02ea\2\3\3"+
		"\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2"+
		"\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3"+
		"\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2"+
		"%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61"+
		"\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2"+
		"\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I"+
		"\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2"+
		"\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2"+
		"\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o"+
		"\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2"+
		"\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081\3\2\2\2\2\u0083\3\2\2\2\2\u0085"+
		"\3\2\2\2\2\u0087\3\2\2\2\2\u0089\3\2\2\2\3\u0099\3\2\2\2\5\u009b\3\2\2"+
		"\2\7\u009d\3\2\2\2\t\u00a0\3\2\2\2\13\u00a2\3\2\2\2\r\u00a6\3\2\2\2\17"+
		"\u00a8\3\2\2\2\21\u00ab\3\2\2\2\23\u00ad\3\2\2\2\25\u00b0\3\2\2\2\27\u00b4"+
		"\3\2\2\2\31\u00b7\3\2\2\2\33\u00ba\3\2\2\2\35\u00bd\3\2\2\2\37\u00c2\3"+
		"\2\2\2!\u00c5\3\2\2\2#\u00c8\3\2\2\2%\u00cf\3\2\2\2\'\u00d1\3\2\2\2)\u00d3"+
		"\3\2\2\2+\u00d5\3\2\2\2-\u00d7\3\2\2\2/\u00d9\3\2\2\2\61\u00db\3\2\2\2"+
		"\63\u00dd\3\2\2\2\65\u00e0\3\2\2\2\67\u00e3\3\2\2\29\u00e5\3\2\2\2;\u00e7"+
		"\3\2\2\2=\u00ea\3\2\2\2?\u00ed\3\2\2\2A\u00f0\3\2\2\2C\u00f3\3\2\2\2E"+
		"\u00f6\3\2\2\2G\u00fd\3\2\2\2I\u0106\3\2\2\2K\u010e\3\2\2\2M\u0116\3\2"+
		"\2\2O\u0121\3\2\2\2Q\u012a\3\2\2\2S\u0138\3\2\2\2U\u0148\3\2\2\2W\u015a"+
		"\3\2\2\2Y\u0167\3\2\2\2[\u0173\3\2\2\2]\u0188\3\2\2\2_\u0194\3\2\2\2a"+
		"\u01a5\3\2\2\2c\u01af\3\2\2\2e\u01c2\3\2\2\2g\u01d3\3\2\2\2i\u01ea\3\2"+
		"\2\2k\u01ff\3\2\2\2m\u0208\3\2\2\2o\u0217\3\2\2\2q\u0223\3\2\2\2s\u0230"+
		"\3\2\2\2u\u023e\3\2\2\2w\u024b\3\2\2\2y\u0255\3\2\2\2{\u025a\3\2\2\2}"+
		"\u025f\3\2\2\2\177\u0283\3\2\2\2\u0081\u0285\3\2\2\2\u0083\u0298\3\2\2"+
		"\2\u0085\u029a\3\2\2\2\u0087\u02a6\3\2\2\2\u0089\u02ae\3\2\2\2\u008b\u02b4"+
		"\3\2\2\2\u008d\u02bd\3\2\2\2\u008f\u02bf\3\2\2\2\u0091\u02c1\3\2\2\2\u0093"+
		"\u02c3\3\2\2\2\u0095\u02da\3\2\2\2\u0097\u02dc\3\2\2\2\u0099\u009a\7*"+
		"\2\2\u009a\4\3\2\2\2\u009b\u009c\7+\2\2\u009c\6\3\2\2\2\u009d\u009e\7"+
		"X\2\2\u009e\u009f\7}\2\2\u009f\b\3\2\2\2\u00a0\u00a1\7\177\2\2\u00a1\n"+
		"\3\2\2\2\u00a2\u00a3\7f\2\2\u00a3\u00a4\7\64\2\2\u00a4\u00a5\7<\2\2\u00a5"+
		"\f\3\2\2\2\u00a6\u00a7\7.\2\2\u00a7\16\3\2\2\2\u00a8\u00a9\7%\2\2\u00a9"+
		"\u00aa\7}\2\2\u00aa\20\3\2\2\2\u00ab\u00ac\7\60\2\2\u00ac\22\3\2\2\2\u00ad"+
		"\u00ae\7\60\2\2\u00ae\u00af\7,\2\2\u00af\24\3\2\2\2\u00b0\u00b1\7\60\2"+
		"\2\u00b1\u00b2\7,\2\2\u00b2\u00b3\7\60\2\2\u00b3\26\3\2\2\2\u00b4\u00b5"+
		"\7F\2\2\u00b5\u00b6\7}\2\2\u00b6\30\3\2\2\2\u00b7\u00b8\7C\2\2\u00b8\u00b9"+
		"\7}\2\2\u00b9\32\3\2\2\2\u00ba\u00bb\7K\2\2\u00bb\u00bc\7}\2\2\u00bc\34"+
		"\3\2\2\2\u00bd\u00be\7Q\2\2\u00be\u00bf\7W\2\2\u00bf\u00c0\7I\2\2\u00c0"+
		"\u00c1\7}\2\2\u00c1\36\3\2\2\2\u00c2\u00c3\7T\2\2\u00c3\u00c4\7}\2\2\u00c4"+
		" \3\2\2\2\u00c5\u00c6\7E\2\2\u00c6\u00c7\7}\2\2\u00c7\"\3\2\2\2\u00c8"+
		"\u00c9\7]\2\2\u00c9\u00ca\7f\2\2\u00ca\u00cb\7c\2\2\u00cb\u00cc\7{\2\2"+
		"\u00cc\u00cd\7u\2\2\u00cd\u00ce\7_\2\2\u00ce$\3\2\2\2\u00cf\u00d0\7`\2"+
		"\2\u00d0&\3\2\2\2\u00d1\u00d2\7/\2\2\u00d2(\3\2\2\2\u00d3\u00d4\7-\2\2"+
		"\u00d4*\3\2\2\2\u00d5\u00d6\7#\2\2\u00d6,\3\2\2\2\u00d7\u00d8\7,\2\2\u00d8"+
		".\3\2\2\2\u00d9\u00da\7\61\2\2\u00da\60\3\2\2\2\u00db\u00dc\7\'\2\2\u00dc"+
		"\62\3\2\2\2\u00dd\u00de\7>\2\2\u00de\u00df\7?\2\2\u00df\64\3\2\2\2\u00e0"+
		"\u00e1\7@\2\2\u00e1\u00e2\7?\2\2\u00e2\66\3\2\2\2\u00e3\u00e4\7>\2\2\u00e4"+
		"8\3\2\2\2\u00e5\u00e6\7@\2\2\u00e6:\3\2\2\2\u00e7\u00e8\7?\2\2\u00e8\u00e9"+
		"\7?\2\2\u00e9<\3\2\2\2\u00ea\u00eb\7#\2\2\u00eb\u00ec\7?\2\2\u00ec>\3"+
		"\2\2\2\u00ed\u00ee\7(\2\2\u00ee\u00ef\7(\2\2\u00ef@\3\2\2\2\u00f0\u00f1"+
		"\7~\2\2\u00f1\u00f2\7~\2\2\u00f2B\3\2\2\2\u00f3\u00f4\7k\2\2\u00f4\u00f5"+
		"\7h\2\2\u00f5D\3\2\2\2\u00f6\u00f7\7k\2\2\u00f7\u00f8\7u\2\2\u00f8\u00f9"+
		"\7P\2\2\u00f9\u00fa\7w\2\2\u00fa\u00fb\7n\2\2\u00fb\u00fc\7n\2\2\u00fc"+
		"F\3\2\2\2\u00fd\u00fe\7e\2\2\u00fe\u00ff\7q\2\2\u00ff\u0100\7c\2\2\u0100"+
		"\u0101\7n\2\2\u0101\u0102\7g\2\2\u0102\u0103\7u\2\2\u0103\u0104\7e\2\2"+
		"\u0104\u0105\7g\2\2\u0105H\3\2\2\2\u0106\u0107\7o\2\2\u0107\u0108\7c\2"+
		"\2\u0108\u0109\7z\2\2\u0109\u010a\7k\2\2\u010a\u010b\7o\2\2\u010b\u010c"+
		"\7w\2\2\u010c\u010d\7o\2\2\u010dJ\3\2\2\2\u010e\u010f\7o\2\2\u010f\u0110"+
		"\7k\2\2\u0110\u0111\7p\2\2\u0111\u0112\7k\2\2\u0112\u0113\7o\2\2\u0113"+
		"\u0114\7w\2\2\u0114\u0115\7o\2\2\u0115L\3\2\2\2\u0116\u0117\7g\2\2\u0117"+
		"\u0118\7x\2\2\u0118\u0119\7g\2\2\u0119\u011a\7p\2\2\u011a\u011b\7v\2\2"+
		"\u011b\u011c\7a\2\2\u011c\u011d\7f\2\2\u011d\u011e\7c\2\2\u011e\u011f"+
		"\7v\2\2\u011f\u0120\7g\2\2\u0120N\3\2\2\2\u0121\u0122\7f\2\2\u0122\u0123"+
		"\7w\2\2\u0123\u0124\7g\2\2\u0124\u0125\7a\2\2\u0125\u0126\7f\2\2\u0126"+
		"\u0127\7c\2\2\u0127\u0128\7v\2\2\u0128\u0129\7g\2\2\u0129P\3\2\2\2\u012a"+
		"\u012b\7k\2\2\u012b\u012c\7p\2\2\u012c\u012d\7e\2\2\u012d\u012e\7k\2\2"+
		"\u012e\u012f\7f\2\2\u012f\u0130\7g\2\2\u0130\u0131\7p\2\2\u0131\u0132"+
		"\7v\2\2\u0132\u0133\7a\2\2\u0133\u0134\7f\2\2\u0134\u0135\7c\2\2\u0135"+
		"\u0136\7v\2\2\u0136\u0137\7g\2\2\u0137R\3\2\2\2\u0138\u0139\7g\2\2\u0139"+
		"\u013a\7p\2\2\u013a\u013b\7t\2\2\u013b\u013c\7q\2\2\u013c\u013d\7n\2\2"+
		"\u013d\u013e\7n\2\2\u013e\u013f\7o\2\2\u013f\u0140\7g\2\2\u0140\u0141"+
		"\7p\2\2\u0141\u0142\7v\2\2\u0142\u0143\7a\2\2\u0143\u0144\7f\2\2\u0144"+
		"\u0145\7c\2\2\u0145\u0146\7v\2\2\u0146\u0147\7g\2\2\u0147T\3\2\2\2\u0148"+
		"\u0149\7g\2\2\u0149\u014a\7p\2\2\u014a\u014b\7t\2\2\u014b\u014c\7q\2\2"+
		"\u014c\u014d\7n\2\2\u014d\u014e\7n\2\2\u014e\u014f\7o\2\2\u014f\u0150"+
		"\7g\2\2\u0150\u0151\7p\2\2\u0151\u0152\7v\2\2\u0152\u0153\7a\2\2\u0153"+
		"\u0154\7u\2\2\u0154\u0155\7v\2\2\u0155\u0156\7c\2\2\u0156\u0157\7v\2\2"+
		"\u0157\u0158\7w\2\2\u0158\u0159\7u\2\2\u0159V\3\2\2\2\u015a\u015b\7e\2"+
		"\2\u015b\u015c\7w\2\2\u015c\u015d\7t\2\2\u015d\u015e\7t\2\2\u015e\u015f"+
		"\7g\2\2\u015f\u0160\7p\2\2\u0160\u0161\7v\2\2\u0161\u0162\7a\2\2\u0162"+
		"\u0163\7f\2\2\u0163\u0164\7c\2\2\u0164\u0165\7v\2\2\u0165\u0166\7g\2\2"+
		"\u0166X\3\2\2\2\u0167\u0168\7x\2\2\u0168\u0169\7c\2\2\u0169\u016a\7n\2"+
		"\2\u016a\u016b\7w\2\2\u016b\u016c\7g\2\2\u016c\u016d\7a\2\2\u016d\u016e"+
		"\7e\2\2\u016e\u016f\7q\2\2\u016f\u0170\7w\2\2\u0170\u0171\7p\2\2\u0171"+
		"\u0172\7v\2\2\u0172Z\3\2\2\2\u0173\u0174\7|\2\2\u0174\u0175\7g\2\2\u0175"+
		"\u0176\7t\2\2\u0176\u0177\7q\2\2\u0177\u0178\7a\2\2\u0178\u0179\7r\2\2"+
		"\u0179\u017a\7q\2\2\u017a\u017b\7u\2\2\u017b\u017c\7a\2\2\u017c\u017d"+
		"\7x\2\2\u017d\u017e\7c\2\2\u017e\u017f\7n\2\2\u017f\u0180\7w\2\2\u0180"+
		"\u0181\7g\2\2\u0181\u0182\7a\2\2\u0182\u0183\7e\2\2\u0183\u0184\7q\2\2"+
		"\u0184\u0185\7w\2\2\u0185\u0186\7p\2\2\u0186\u0187\7v\2\2\u0187\\\3\2"+
		"\2\2\u0188\u0189\7g\2\2\u0189\u018a\7x\2\2\u018a\u018b\7g\2\2\u018b\u018c"+
		"\7p\2\2\u018c\u018d\7v\2\2\u018d\u018e\7a\2\2\u018e\u018f\7e\2\2\u018f"+
		"\u0190\7q\2\2\u0190\u0191\7w\2\2\u0191\u0192\7p\2\2\u0192\u0193\7v\2\2"+
		"\u0193^\3\2\2\2\u0194\u0195\7g\2\2\u0195\u0196\7p\2\2\u0196\u0197\7t\2"+
		"\2\u0197\u0198\7q\2\2\u0198\u0199\7n\2\2\u0199\u019a\7n\2\2\u019a\u019b"+
		"\7o\2\2\u019b\u019c\7g\2\2\u019c\u019d\7p\2\2\u019d\u019e\7v\2\2\u019e"+
		"\u019f\7a\2\2\u019f\u01a0\7e\2\2\u01a0\u01a1\7q\2\2\u01a1\u01a2\7w\2\2"+
		"\u01a2\u01a3\7p\2\2\u01a3\u01a4\7v\2\2\u01a4`\3\2\2\2\u01a5\u01a6\7v\2"+
		"\2\u01a6\u01a7\7g\2\2\u01a7\u01a8\7k\2\2\u01a8\u01a9\7a\2\2\u01a9\u01aa"+
		"\7e\2\2\u01aa\u01ab\7q\2\2\u01ab\u01ac\7w\2\2\u01ac\u01ad\7p\2\2\u01ad"+
		"\u01ae\7v\2\2\u01aeb\3\2\2\2\u01af\u01b0\7r\2\2\u01b0\u01b1\7t\2\2\u01b1"+
		"\u01b2\7q\2\2\u01b2\u01b3\7i\2\2\u01b3\u01b4\7t\2\2\u01b4\u01b5\7c\2\2"+
		"\u01b5\u01b6\7o\2\2\u01b6\u01b7\7a\2\2\u01b7\u01b8\7u\2\2\u01b8\u01b9"+
		"\7v\2\2\u01b9\u01ba\7c\2\2\u01ba\u01bb\7i\2\2\u01bb\u01bc\7g\2\2\u01bc"+
		"\u01bd\7a\2\2\u01bd\u01be\7p\2\2\u01be\u01bf\7c\2\2\u01bf\u01c0\7o\2\2"+
		"\u01c0\u01c1\7g\2\2\u01c1d\3\2\2\2\u01c2\u01c3\7r\2\2\u01c3\u01c4\7t\2"+
		"\2\u01c4\u01c5\7q\2\2\u01c5\u01c6\7i\2\2\u01c6\u01c7\7t\2\2\u01c7\u01c8"+
		"\7c\2\2\u01c8\u01c9\7o\2\2\u01c9\u01ca\7a\2\2\u01ca\u01cb\7u\2\2\u01cb"+
		"\u01cc\7v\2\2\u01cc\u01cd\7c\2\2\u01cd\u01ce\7i\2\2\u01ce\u01cf\7g\2\2"+
		"\u01cf\u01d0\7a\2\2\u01d0\u01d1\7k\2\2\u01d1\u01d2\7f\2\2\u01d2f\3\2\2"+
		"\2\u01d3\u01d4\7t\2\2\u01d4\u01d5\7g\2\2\u01d5\u01d6\7r\2\2\u01d6\u01d7"+
		"\7q\2\2\u01d7\u01d8\7t\2\2\u01d8\u01d9\7v\2\2\u01d9\u01da\7k\2\2\u01da"+
		"\u01db\7p\2\2\u01db\u01dc\7i\2\2\u01dc\u01dd\7a\2\2\u01dd\u01de\7r\2\2"+
		"\u01de\u01df\7g\2\2\u01df\u01e0\7t\2\2\u01e0\u01e1\7k\2\2\u01e1\u01e2"+
		"\7q\2\2\u01e2\u01e3\7f\2\2\u01e3\u01e4\7a\2\2\u01e4\u01e5\7u\2\2\u01e5"+
		"\u01e6\7v\2\2\u01e6\u01e7\7c\2\2\u01e7\u01e8\7t\2\2\u01e8\u01e9\7v\2\2"+
		"\u01e9h\3\2\2\2\u01ea\u01eb\7t\2\2\u01eb\u01ec\7g\2\2\u01ec\u01ed\7r\2"+
		"\2\u01ed\u01ee\7q\2\2\u01ee\u01ef\7t\2\2\u01ef\u01f0\7v\2\2\u01f0\u01f1"+
		"\7k\2\2\u01f1\u01f2\7p\2\2\u01f2\u01f3\7i\2\2\u01f3\u01f4\7a\2\2\u01f4"+
		"\u01f5\7r\2\2\u01f5\u01f6\7g\2\2\u01f6\u01f7\7t\2\2\u01f7\u01f8\7k\2\2"+
		"\u01f8\u01f9\7q\2\2\u01f9\u01fa\7f\2\2\u01fa\u01fb\7a\2\2\u01fb\u01fc"+
		"\7g\2\2\u01fc\u01fd\7p\2\2\u01fd\u01fe\7f\2\2\u01fej\3\2\2\2\u01ff\u0200"+
		"\7j\2\2\u0200\u0201\7c\2\2\u0201\u0202\7u\2\2\u0202\u0203\7X\2\2\u0203"+
		"\u0204\7c\2\2\u0204\u0205\7n\2\2\u0205\u0206\7w\2\2\u0206\u0207\7g\2\2"+
		"\u0207l\3\2\2\2\u0208\u0209\7o\2\2\u0209\u020a\7k\2\2\u020a\u020b\7p\2"+
		"\2\u020b\u020c\7w\2\2\u020c\u020d\7v\2\2\u020d\u020e\7g\2\2\u020e\u020f"+
		"\7u\2\2\u020f\u0210\7D\2\2\u0210\u0211\7g\2\2\u0211\u0212\7v\2\2\u0212"+
		"\u0213\7y\2\2\u0213\u0214\7g\2\2\u0214\u0215\7g\2\2\u0215\u0216\7p\2\2"+
		"\u0216n\3\2\2\2\u0217\u0218\7f\2\2\u0218\u0219\7c\2\2\u0219\u021a\7{\2"+
		"\2\u021a\u021b\7u\2\2\u021b\u021c\7D\2\2\u021c\u021d\7g\2\2\u021d\u021e"+
		"\7v\2\2\u021e\u021f\7y\2\2\u021f\u0220\7g\2\2\u0220\u0221\7g\2\2\u0221"+
		"\u0222\7p\2\2\u0222p\3\2\2\2\u0223\u0224\7y\2\2\u0224\u0225\7g\2\2\u0225"+
		"\u0226\7g\2\2\u0226\u0227\7m\2\2\u0227\u0228\7u\2\2\u0228\u0229\7D\2\2"+
		"\u0229\u022a\7g\2\2\u022a\u022b\7v\2\2\u022b\u022c\7y\2\2\u022c\u022d"+
		"\7g\2\2\u022d\u022e\7g\2\2\u022e\u022f\7p\2\2\u022fr\3\2\2\2\u0230\u0231"+
		"\7o\2\2\u0231\u0232\7q\2\2\u0232\u0233\7p\2\2\u0233\u0234\7v\2\2\u0234"+
		"\u0235\7j\2\2\u0235\u0236\7u\2\2\u0236\u0237\7D\2\2\u0237\u0238\7g\2\2"+
		"\u0238\u0239\7v\2\2\u0239\u023a\7y\2\2\u023a\u023b\7g\2\2\u023b\u023c"+
		"\7g\2\2\u023c\u023d\7p\2\2\u023dt\3\2\2\2\u023e\u023f\7{\2\2\u023f\u0240"+
		"\7g\2\2\u0240\u0241\7c\2\2\u0241\u0242\7t\2\2\u0242\u0243\7u\2\2\u0243"+
		"\u0244\7D\2\2\u0244\u0245\7g\2\2\u0245\u0246\7v\2\2\u0246\u0247\7y\2\2"+
		"\u0247\u0248\7g\2\2\u0248\u0249\7g\2\2\u0249\u024a\7p\2\2\u024av\3\2\2"+
		"\2\u024b\u024c\7e\2\2\u024c\u024d\7q\2\2\u024d\u024e\7p\2\2\u024e\u024f"+
		"\7f\2\2\u024f\u0250\7k\2\2\u0250\u0251\7v\2\2\u0251\u0252\7k\2\2\u0252"+
		"\u0253\7q\2\2\u0253\u0254\7p\2\2\u0254x\3\2\2\2\u0255\u0256\7|\2\2\u0256"+
		"\u0257\7k\2\2\u0257\u0258\7p\2\2\u0258\u0259\7i\2\2\u0259z\3\2\2\2\u025a"+
		"\u025b\7q\2\2\u025b\u025c\7k\2\2\u025c\u025d\7|\2\2\u025d\u025e\7r\2\2"+
		"\u025e|\3\2\2\2\u025f\u0260\7|\2\2\u0260\u0261\7r\2\2\u0261\u0262\7x\2"+
		"\2\u0262\u0263\7e\2\2\u0263~\3\2\2\2\u0264\u026d\7\62\2\2\u0265\u0269"+
		"\t\2\2\2\u0266\u0268\t\3\2\2\u0267\u0266\3\2\2\2\u0268\u026b\3\2\2\2\u0269"+
		"\u0267\3\2\2\2\u0269\u026a\3\2\2\2\u026a\u026d\3\2\2\2\u026b\u0269\3\2"+
		"\2\2\u026c\u0264\3\2\2\2\u026c\u0265\3\2\2\2\u026d\u0275\3\2\2\2\u026e"+
		"\u0272\7\60\2\2\u026f\u0271\t\3\2\2\u0270\u026f\3\2\2\2\u0271\u0274\3"+
		"\2\2\2\u0272\u0270\3\2\2\2\u0272\u0273\3\2\2\2\u0273\u0276\3\2\2\2\u0274"+
		"\u0272\3\2\2\2\u0275\u026e\3\2\2\2\u0275\u0276\3\2\2\2\u0276\u0278\3\2"+
		"\2\2\u0277\u0279\5\u008bF\2\u0278\u0277\3\2\2\2\u0278\u0279\3\2\2\2\u0279"+
		"\u0284\3\2\2\2\u027a\u027c\7\60\2\2\u027b\u027d\t\3\2\2\u027c\u027b\3"+
		"\2\2\2\u027d\u027e\3\2\2\2\u027e\u027c\3\2\2\2\u027e\u027f\3\2\2\2\u027f"+
		"\u0281\3\2\2\2\u0280\u0282\5\u008bF\2\u0281\u0280\3\2\2\2\u0281\u0282"+
		"\3\2\2\2\u0282\u0284\3\2\2\2\u0283\u026c\3\2\2\2\u0283\u027a\3\2\2\2\u0284"+
		"\u0080\3\2\2\2\u0285\u028a\7$\2\2\u0286\u0289\n\4\2\2\u0287\u0289\5\u0095"+
		"K\2\u0288\u0286\3\2\2\2\u0288\u0287\3\2\2\2\u0289\u028c\3\2\2\2\u028a"+
		"\u0288\3\2\2\2\u028a\u028b\3\2\2\2\u028b\u028d\3\2\2\2\u028c\u028a\3\2"+
		"\2\2\u028d\u028e\7$\2\2\u028e\u0082\3\2\2\2\u028f\u0290\7v\2\2\u0290\u0291"+
		"\7t\2\2\u0291\u0292\7w\2\2\u0292\u0299\7g\2\2\u0293\u0294\7h\2\2\u0294"+
		"\u0295\7c\2\2\u0295\u0296\7n\2\2\u0296\u0297\7u\2\2\u0297\u0299\7g\2\2"+
		"\u0298\u028f\3\2\2\2\u0298\u0293\3\2\2\2\u0299\u0084\3\2\2\2\u029a\u029b"+
		"\5\u008dG\2\u029b\u029c\5\u008fH\2\u029c\u029d\5\u008fH\2\u029d\u029e"+
		"\5\u008fH\2\u029e\u029f\5\u008fH\2\u029f\u02a0\5\u008fH\2\u02a0\u02a1"+
		"\5\u008fH\2\u02a1\u02a2\5\u008fH\2\u02a2\u02a3\5\u008fH\2\u02a3\u02a4"+
		"\5\u008fH\2\u02a4\u02a5\5\u008fH\2\u02a5\u0086\3\2\2\2\u02a6\u02aa\5\u0091"+
		"I\2\u02a7\u02a9\5\u0093J\2\u02a8\u02a7\3\2\2\2\u02a9\u02ac\3\2\2\2\u02aa"+
		"\u02a8\3\2\2\2\u02aa\u02ab\3\2\2\2\u02ab\u0088\3\2\2\2\u02ac\u02aa\3\2"+
		"\2\2\u02ad\u02af\t\5\2\2\u02ae\u02ad\3\2\2\2\u02af\u02b0\3\2\2\2\u02b0"+
		"\u02ae\3\2\2\2\u02b0\u02b1\3\2\2\2\u02b1\u02b2\3\2\2\2\u02b2\u02b3\bE"+
		"\2\2\u02b3\u008a\3\2\2\2\u02b4\u02b6\t\6\2\2\u02b5\u02b7\t\7\2\2\u02b6"+
		"\u02b5\3\2\2\2\u02b6\u02b7\3\2\2\2\u02b7\u02b9\3\2\2\2\u02b8\u02ba\t\3"+
		"\2\2\u02b9\u02b8\3\2\2\2\u02ba\u02bb\3\2\2\2\u02bb\u02b9\3\2\2\2\u02bb"+
		"\u02bc\3\2\2\2\u02bc\u008c\3\2\2\2\u02bd\u02be\t\b\2\2\u02be\u008e\3\2"+
		"\2\2\u02bf\u02c0\t\t\2\2\u02c0\u0090\3\2\2\2\u02c1\u02c2\t\n\2\2\u02c2"+
		"\u0092\3\2\2\2\u02c3\u02c4\t\13\2\2\u02c4\u0094\3\2\2\2\u02c5\u02c6\7"+
		"^\2\2\u02c6\u02db\t\f\2\2\u02c7\u02cc\7^\2\2\u02c8\u02ca\t\r\2\2\u02c9"+
		"\u02c8\3\2\2\2\u02c9\u02ca\3\2\2\2\u02ca\u02cb\3\2\2\2\u02cb\u02cd\t\16"+
		"\2\2\u02cc\u02c9\3\2\2\2\u02cc\u02cd\3\2\2\2\u02cd\u02ce\3\2\2\2\u02ce"+
		"\u02db\t\16\2\2\u02cf\u02d1\7^\2\2\u02d0\u02d2\7w\2\2\u02d1\u02d0\3\2"+
		"\2\2\u02d2\u02d3\3\2\2\2\u02d3\u02d1\3\2\2\2\u02d3\u02d4\3\2\2\2\u02d4"+
		"\u02d5\3\2\2\2\u02d5\u02d6\5\u0097L\2\u02d6\u02d7\5\u0097L\2\u02d7\u02d8"+
		"\5\u0097L\2\u02d8\u02d9\5\u0097L\2\u02d9\u02db\3\2\2\2\u02da\u02c5\3\2"+
		"\2\2\u02da\u02c7\3\2\2\2\u02da\u02cf\3\2\2\2\u02db\u0096\3\2\2\2\u02dc"+
		"\u02dd\t\17\2\2\u02dd\u0098\3\2\2\2\26\2\u0269\u026c\u0272\u0275\u0278"+
		"\u027e\u0281\u0283\u0288\u028a\u0298\u02aa\u02b0\u02b6\u02bb\u02c9\u02cc"+
		"\u02d3\u02da\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}