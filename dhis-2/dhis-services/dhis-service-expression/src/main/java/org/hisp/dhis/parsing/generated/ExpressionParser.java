// Generated from /Users/jim/Documents/dev/dhis2/dhis2-core/dhis-2/dhis-services/dhis-service-expression/src/main/resources/org/hisp/dhis/parsing/Expression.g4 by ANTLR 4.7.1
package org.hisp.dhis.parsing.generated;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ExpressionParser extends Parser {
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
	public static final int
		RULE_expression = 0, RULE_expr = 1, RULE_programIndicatorExpr = 2, RULE_programIndicatorVariable = 3, 
		RULE_programIndicatorFunction = 4, RULE_a0 = 5, RULE_a0_1 = 6, RULE_a1 = 7, 
		RULE_a1_2 = 8, RULE_a1_n = 9, RULE_a2 = 10, RULE_a3 = 11, RULE_dataElement = 12, 
		RULE_dataElementOperandWithoutAoc = 13, RULE_dataElementOperandWithAoc = 14, 
		RULE_programDataElement = 15, RULE_programAttribute = 16, RULE_programIndicator = 17, 
		RULE_orgUnitCount = 18, RULE_reportingRate = 19, RULE_constant = 20, RULE_days = 21, 
		RULE_dataElementId = 22, RULE_dataElementOperandIdWithoutAoc = 23, RULE_dataElementOperandIdWithAoc = 24, 
		RULE_programDataElementId = 25, RULE_programAttributeId = 26, RULE_programIndicatorId = 27, 
		RULE_orgUnitCountId = 28, RULE_reportingRateId = 29, RULE_constantId = 30, 
		RULE_numericLiteral = 31, RULE_stringLiteral = 32, RULE_booleanLiteral = 33, 
		RULE_javaIdentifier = 34;
	public static final String[] ruleNames = {
		"expression", "expr", "programIndicatorExpr", "programIndicatorVariable", 
		"programIndicatorFunction", "a0", "a0_1", "a1", "a1_2", "a1_n", "a2", 
		"a3", "dataElement", "dataElementOperandWithoutAoc", "dataElementOperandWithAoc", 
		"programDataElement", "programAttribute", "programIndicator", "orgUnitCount", 
		"reportingRate", "constant", "days", "dataElementId", "dataElementOperandIdWithoutAoc", 
		"dataElementOperandIdWithAoc", "programDataElementId", "programAttributeId", 
		"programIndicatorId", "orgUnitCountId", "reportingRateId", "constantId", 
		"numericLiteral", "stringLiteral", "booleanLiteral", "javaIdentifier"
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

	@Override
	public String getGrammarFileName() { return "Expression.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public ExpressionParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ExpressionContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode EOF() { return getToken(ExpressionParser.EOF, 0); }
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(70);
			expr(0);
			setState(71);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public Token fun;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public A3Context a3() {
			return getRuleContext(A3Context.class,0);
		}
		public A1Context a1() {
			return getRuleContext(A1Context.class,0);
		}
		public A1_nContext a1_n() {
			return getRuleContext(A1_nContext.class,0);
		}
		public DataElementContext dataElement() {
			return getRuleContext(DataElementContext.class,0);
		}
		public DataElementOperandWithoutAocContext dataElementOperandWithoutAoc() {
			return getRuleContext(DataElementOperandWithoutAocContext.class,0);
		}
		public DataElementOperandWithAocContext dataElementOperandWithAoc() {
			return getRuleContext(DataElementOperandWithAocContext.class,0);
		}
		public ProgramDataElementContext programDataElement() {
			return getRuleContext(ProgramDataElementContext.class,0);
		}
		public ProgramAttributeContext programAttribute() {
			return getRuleContext(ProgramAttributeContext.class,0);
		}
		public ProgramIndicatorContext programIndicator() {
			return getRuleContext(ProgramIndicatorContext.class,0);
		}
		public OrgUnitCountContext orgUnitCount() {
			return getRuleContext(OrgUnitCountContext.class,0);
		}
		public ReportingRateContext reportingRate() {
			return getRuleContext(ReportingRateContext.class,0);
		}
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public DaysContext days() {
			return getRuleContext(DaysContext.class,0);
		}
		public ProgramIndicatorExprContext programIndicatorExpr() {
			return getRuleContext(ProgramIndicatorExprContext.class,0);
		}
		public NumericLiteralContext numericLiteral() {
			return getRuleContext(NumericLiteralContext.class,0);
		}
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public BooleanLiteralContext booleanLiteral() {
			return getRuleContext(BooleanLiteralContext.class,0);
		}
		public A0Context a0() {
			return getRuleContext(A0Context.class,0);
		}
		public A0_1Context a0_1() {
			return getRuleContext(A0_1Context.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(106);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				{
				setState(74);
				match(T__0);
				setState(75);
				expr(0);
				setState(76);
				match(T__1);
				}
				break;
			case 2:
				{
				setState(78);
				((ExprContext)_localctx).fun = match(IF);
				setState(79);
				a3();
				}
				break;
			case 3:
				{
				setState(80);
				((ExprContext)_localctx).fun = match(IS_NULL);
				setState(81);
				a1();
				}
				break;
			case 4:
				{
				setState(82);
				((ExprContext)_localctx).fun = match(COALESCE);
				setState(83);
				a1_n();
				}
				break;
			case 5:
				{
				setState(84);
				((ExprContext)_localctx).fun = match(MAXIMUM);
				setState(85);
				a1_n();
				}
				break;
			case 6:
				{
				setState(86);
				((ExprContext)_localctx).fun = match(MINIMUM);
				setState(87);
				a1_n();
				}
				break;
			case 7:
				{
				setState(88);
				((ExprContext)_localctx).fun = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==MINUS || _la==NOT) ) {
					((ExprContext)_localctx).fun = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(89);
				expr(22);
				}
				break;
			case 8:
				{
				setState(90);
				match(PLUS);
				setState(91);
				expr(21);
				}
				break;
			case 9:
				{
				setState(92);
				dataElement();
				}
				break;
			case 10:
				{
				setState(93);
				dataElementOperandWithoutAoc();
				}
				break;
			case 11:
				{
				setState(94);
				dataElementOperandWithAoc();
				}
				break;
			case 12:
				{
				setState(95);
				programDataElement();
				}
				break;
			case 13:
				{
				setState(96);
				programAttribute();
				}
				break;
			case 14:
				{
				setState(97);
				programIndicator();
				}
				break;
			case 15:
				{
				setState(98);
				orgUnitCount();
				}
				break;
			case 16:
				{
				setState(99);
				reportingRate();
				}
				break;
			case 17:
				{
				setState(100);
				constant();
				}
				break;
			case 18:
				{
				setState(101);
				days();
				}
				break;
			case 19:
				{
				setState(102);
				programIndicatorExpr();
				}
				break;
			case 20:
				{
				setState(103);
				numericLiteral();
				}
				break;
			case 21:
				{
				setState(104);
				stringLiteral();
				}
				break;
			case 22:
				{
				setState(105);
				booleanLiteral();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(227);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(225);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(108);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(109);
						((ExprContext)_localctx).fun = match(POWER);
						setState(110);
						expr(23);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(111);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(112);
						((ExprContext)_localctx).fun = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << MUL) | (1L << DIV) | (1L << MOD))) != 0)) ) {
							((ExprContext)_localctx).fun = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(113);
						expr(21);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(114);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(115);
						((ExprContext)_localctx).fun = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==MINUS || _la==PLUS) ) {
							((ExprContext)_localctx).fun = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(116);
						expr(20);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(117);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(118);
						((ExprContext)_localctx).fun = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LEQ) | (1L << GEQ) | (1L << LT) | (1L << GT))) != 0)) ) {
							((ExprContext)_localctx).fun = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(119);
						expr(19);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(120);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(121);
						((ExprContext)_localctx).fun = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==EQ || _la==NE) ) {
							((ExprContext)_localctx).fun = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(122);
						expr(18);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(123);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(124);
						((ExprContext)_localctx).fun = match(AND);
						setState(125);
						expr(17);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(126);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(127);
						((ExprContext)_localctx).fun = match(OR);
						setState(128);
						expr(16);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(129);
						if (!(precpred(_ctx, 52))) throw new FailedPredicateException(this, "precpred(_ctx, 52)");
						setState(130);
						match(T__2);
						setState(131);
						((ExprContext)_localctx).fun = match(EXCEPT);
						setState(132);
						a1();
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(133);
						if (!(precpred(_ctx, 46))) throw new FailedPredicateException(this, "precpred(_ctx, 46)");
						setState(134);
						match(T__2);
						setState(135);
						((ExprContext)_localctx).fun = match(SUM);
						setState(136);
						a0();
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(137);
						if (!(precpred(_ctx, 45))) throw new FailedPredicateException(this, "precpred(_ctx, 45)");
						setState(138);
						match(T__2);
						setState(139);
						((ExprContext)_localctx).fun = match(MAX);
						setState(140);
						a0();
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(141);
						if (!(precpred(_ctx, 44))) throw new FailedPredicateException(this, "precpred(_ctx, 44)");
						setState(142);
						match(T__2);
						setState(143);
						((ExprContext)_localctx).fun = match(MIN);
						setState(144);
						a0();
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(145);
						if (!(precpred(_ctx, 43))) throw new FailedPredicateException(this, "precpred(_ctx, 43)");
						setState(146);
						match(T__2);
						setState(147);
						((ExprContext)_localctx).fun = match(AVERAGE);
						setState(148);
						a0();
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(149);
						if (!(precpred(_ctx, 42))) throw new FailedPredicateException(this, "precpred(_ctx, 42)");
						setState(150);
						match(T__2);
						setState(151);
						((ExprContext)_localctx).fun = match(STDDEV);
						setState(152);
						a0();
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(153);
						if (!(precpred(_ctx, 41))) throw new FailedPredicateException(this, "precpred(_ctx, 41)");
						setState(154);
						match(T__2);
						setState(155);
						((ExprContext)_localctx).fun = match(VARIANCE);
						setState(156);
						a0();
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(157);
						if (!(precpred(_ctx, 40))) throw new FailedPredicateException(this, "precpred(_ctx, 40)");
						setState(158);
						match(T__2);
						setState(159);
						((ExprContext)_localctx).fun = match(MEDIAN);
						setState(160);
						a0();
						}
						break;
					case 16:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(161);
						if (!(precpred(_ctx, 39))) throw new FailedPredicateException(this, "precpred(_ctx, 39)");
						setState(162);
						match(T__2);
						setState(163);
						((ExprContext)_localctx).fun = match(FIRST);
						setState(164);
						a0_1();
						}
						break;
					case 17:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(165);
						if (!(precpred(_ctx, 38))) throw new FailedPredicateException(this, "precpred(_ctx, 38)");
						setState(166);
						match(T__2);
						setState(167);
						((ExprContext)_localctx).fun = match(LAST);
						setState(168);
						a0_1();
						}
						break;
					case 18:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(169);
						if (!(precpred(_ctx, 37))) throw new FailedPredicateException(this, "precpred(_ctx, 37)");
						setState(170);
						match(T__2);
						setState(171);
						((ExprContext)_localctx).fun = match(PERCENTILE);
						setState(172);
						a1();
						}
						break;
					case 19:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(173);
						if (!(precpred(_ctx, 36))) throw new FailedPredicateException(this, "precpred(_ctx, 36)");
						setState(174);
						match(T__2);
						setState(175);
						((ExprContext)_localctx).fun = match(RANK_HIGH);
						setState(176);
						a1();
						}
						break;
					case 20:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(177);
						if (!(precpred(_ctx, 35))) throw new FailedPredicateException(this, "precpred(_ctx, 35)");
						setState(178);
						match(T__2);
						setState(179);
						((ExprContext)_localctx).fun = match(RANK_LOW);
						setState(180);
						a1();
						}
						break;
					case 21:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(181);
						if (!(precpred(_ctx, 34))) throw new FailedPredicateException(this, "precpred(_ctx, 34)");
						setState(182);
						match(T__2);
						setState(183);
						((ExprContext)_localctx).fun = match(RANK_PERCENTILE);
						setState(184);
						a1();
						}
						break;
					case 22:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(185);
						if (!(precpred(_ctx, 33))) throw new FailedPredicateException(this, "precpred(_ctx, 33)");
						setState(186);
						match(T__2);
						setState(187);
						((ExprContext)_localctx).fun = match(AVERAGE_SUM_ORG_UNIT);
						setState(188);
						a0();
						}
						break;
					case 23:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(189);
						if (!(precpred(_ctx, 32))) throw new FailedPredicateException(this, "precpred(_ctx, 32)");
						setState(190);
						match(T__2);
						setState(191);
						((ExprContext)_localctx).fun = match(LAST_AVERAGE_ORG_UNIT);
						setState(192);
						a0();
						}
						break;
					case 24:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(193);
						if (!(precpred(_ctx, 31))) throw new FailedPredicateException(this, "precpred(_ctx, 31)");
						setState(194);
						match(T__2);
						setState(195);
						((ExprContext)_localctx).fun = match(NO_AGGREGATION);
						setState(196);
						a0();
						}
						break;
					case 25:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(197);
						if (!(precpred(_ctx, 30))) throw new FailedPredicateException(this, "precpred(_ctx, 30)");
						setState(198);
						match(T__2);
						setState(199);
						((ExprContext)_localctx).fun = match(PERIOD);
						setState(200);
						a1_n();
						}
						break;
					case 26:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(201);
						if (!(precpred(_ctx, 29))) throw new FailedPredicateException(this, "precpred(_ctx, 29)");
						setState(202);
						match(T__2);
						setState(203);
						((ExprContext)_localctx).fun = match(OU_ANCESTOR);
						setState(204);
						a1();
						}
						break;
					case 27:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(205);
						if (!(precpred(_ctx, 28))) throw new FailedPredicateException(this, "precpred(_ctx, 28)");
						setState(206);
						match(T__2);
						setState(207);
						((ExprContext)_localctx).fun = match(OU_DESCENDANT);
						setState(208);
						a1();
						}
						break;
					case 28:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(209);
						if (!(precpred(_ctx, 27))) throw new FailedPredicateException(this, "precpred(_ctx, 27)");
						setState(210);
						match(T__2);
						setState(211);
						((ExprContext)_localctx).fun = match(OU_LEVEL);
						setState(212);
						a1_n();
						}
						break;
					case 29:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(213);
						if (!(precpred(_ctx, 26))) throw new FailedPredicateException(this, "precpred(_ctx, 26)");
						setState(214);
						match(T__2);
						setState(215);
						((ExprContext)_localctx).fun = match(OU_PEER);
						setState(216);
						a1();
						}
						break;
					case 30:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(217);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(218);
						match(T__2);
						setState(219);
						((ExprContext)_localctx).fun = match(OU_GROUP);
						setState(220);
						a1_n();
						}
						break;
					case 31:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(221);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(222);
						match(T__2);
						setState(223);
						((ExprContext)_localctx).fun = match(OU_DATA_SET);
						setState(224);
						a1_n();
						}
						break;
					}
					} 
				}
				setState(229);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class ProgramIndicatorExprContext extends ParserRuleContext {
		public ProgramIndicatorVariableContext programIndicatorVariable() {
			return getRuleContext(ProgramIndicatorVariableContext.class,0);
		}
		public ProgramIndicatorFunctionContext programIndicatorFunction() {
			return getRuleContext(ProgramIndicatorFunctionContext.class,0);
		}
		public ProgramIndicatorExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programIndicatorExpr; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramIndicatorExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramIndicatorExprContext programIndicatorExpr() throws RecognitionException {
		ProgramIndicatorExprContext _localctx = new ProgramIndicatorExprContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_programIndicatorExpr);
		try {
			setState(236);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__3:
				enterOuterAlt(_localctx, 1);
				{
				setState(230);
				match(T__3);
				setState(231);
				programIndicatorVariable();
				setState(232);
				match(T__4);
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 2);
				{
				setState(234);
				match(T__5);
				setState(235);
				programIndicatorFunction();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramIndicatorVariableContext extends ParserRuleContext {
		public Token var;
		public ProgramIndicatorVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programIndicatorVariable; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramIndicatorVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramIndicatorVariableContext programIndicatorVariable() throws RecognitionException {
		ProgramIndicatorVariableContext _localctx = new ProgramIndicatorVariableContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_programIndicatorVariable);
		try {
			setState(253);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EVENT_DATE:
				enterOuterAlt(_localctx, 1);
				{
				setState(238);
				((ProgramIndicatorVariableContext)_localctx).var = match(EVENT_DATE);
				}
				break;
			case DUE_DATE:
				enterOuterAlt(_localctx, 2);
				{
				setState(239);
				((ProgramIndicatorVariableContext)_localctx).var = match(DUE_DATE);
				}
				break;
			case INCIDENT_DATE:
				enterOuterAlt(_localctx, 3);
				{
				setState(240);
				((ProgramIndicatorVariableContext)_localctx).var = match(INCIDENT_DATE);
				}
				break;
			case ENROLLMENT_DATE:
				enterOuterAlt(_localctx, 4);
				{
				setState(241);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_DATE);
				}
				break;
			case ENROLLMENT_STATUS:
				enterOuterAlt(_localctx, 5);
				{
				setState(242);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_STATUS);
				}
				break;
			case CURRENT_STATUS:
				enterOuterAlt(_localctx, 6);
				{
				setState(243);
				((ProgramIndicatorVariableContext)_localctx).var = match(CURRENT_STATUS);
				}
				break;
			case VALUE_COUNT:
				enterOuterAlt(_localctx, 7);
				{
				setState(244);
				((ProgramIndicatorVariableContext)_localctx).var = match(VALUE_COUNT);
				}
				break;
			case ZERO_POS_VALUE_COUNT:
				enterOuterAlt(_localctx, 8);
				{
				setState(245);
				((ProgramIndicatorVariableContext)_localctx).var = match(ZERO_POS_VALUE_COUNT);
				}
				break;
			case EVENT_COUNT:
				enterOuterAlt(_localctx, 9);
				{
				setState(246);
				((ProgramIndicatorVariableContext)_localctx).var = match(EVENT_COUNT);
				}
				break;
			case ENROLLMENT_COUNT:
				enterOuterAlt(_localctx, 10);
				{
				setState(247);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_COUNT);
				}
				break;
			case TEI_COUNT:
				enterOuterAlt(_localctx, 11);
				{
				setState(248);
				((ProgramIndicatorVariableContext)_localctx).var = match(TEI_COUNT);
				}
				break;
			case PROGRAM_STAGE_NAME:
				enterOuterAlt(_localctx, 12);
				{
				setState(249);
				((ProgramIndicatorVariableContext)_localctx).var = match(PROGRAM_STAGE_NAME);
				}
				break;
			case PROGRAM_STAGE_ID:
				enterOuterAlt(_localctx, 13);
				{
				setState(250);
				((ProgramIndicatorVariableContext)_localctx).var = match(PROGRAM_STAGE_ID);
				}
				break;
			case REPORTING_PERIOD_START:
				enterOuterAlt(_localctx, 14);
				{
				setState(251);
				((ProgramIndicatorVariableContext)_localctx).var = match(REPORTING_PERIOD_START);
				}
				break;
			case REPORTING_PERIOD_END:
				enterOuterAlt(_localctx, 15);
				{
				setState(252);
				((ProgramIndicatorVariableContext)_localctx).var = match(REPORTING_PERIOD_END);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramIndicatorFunctionContext extends ParserRuleContext {
		public Token fun;
		public A1Context a1() {
			return getRuleContext(A1Context.class,0);
		}
		public A2Context a2() {
			return getRuleContext(A2Context.class,0);
		}
		public A3Context a3() {
			return getRuleContext(A3Context.class,0);
		}
		public A1_nContext a1_n() {
			return getRuleContext(A1_nContext.class,0);
		}
		public ProgramIndicatorFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programIndicatorFunction; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramIndicatorFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramIndicatorFunctionContext programIndicatorFunction() throws RecognitionException {
		ProgramIndicatorFunctionContext _localctx = new ProgramIndicatorFunctionContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_programIndicatorFunction);
		try {
			setState(275);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HAS_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(255);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(HAS_VALUE);
				setState(256);
				a1();
				}
				break;
			case MINUTES_BETWEEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(257);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(MINUTES_BETWEEN);
				setState(258);
				a2();
				}
				break;
			case DAYS_BETWEEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(259);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(DAYS_BETWEEN);
				setState(260);
				a2();
				}
				break;
			case WEEKS_BETWEEN:
				enterOuterAlt(_localctx, 4);
				{
				setState(261);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(WEEKS_BETWEEN);
				setState(262);
				a2();
				}
				break;
			case MONTHS_BETWEEN:
				enterOuterAlt(_localctx, 5);
				{
				setState(263);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(MONTHS_BETWEEN);
				setState(264);
				a2();
				}
				break;
			case YEARS_BETWEEN:
				enterOuterAlt(_localctx, 6);
				{
				setState(265);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(YEARS_BETWEEN);
				setState(266);
				a2();
				}
				break;
			case CONDITION:
				enterOuterAlt(_localctx, 7);
				{
				setState(267);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(CONDITION);
				setState(268);
				a3();
				}
				break;
			case ZING:
				enterOuterAlt(_localctx, 8);
				{
				setState(269);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(ZING);
				setState(270);
				a1();
				}
				break;
			case OIZP:
				enterOuterAlt(_localctx, 9);
				{
				setState(271);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(OIZP);
				setState(272);
				a1();
				}
				break;
			case ZPVC:
				enterOuterAlt(_localctx, 10);
				{
				setState(273);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(ZPVC);
				setState(274);
				a1_n();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class A0Context extends ParserRuleContext {
		public A0Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_a0; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitA0(this);
			else return visitor.visitChildren(this);
		}
	}

	public final A0Context a0() throws RecognitionException {
		A0Context _localctx = new A0Context(_ctx, getState());
		enterRule(_localctx, 10, RULE_a0);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(277);
			match(T__0);
			setState(278);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class A0_1Context extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public A0_1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_a0_1; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitA0_1(this);
			else return visitor.visitChildren(this);
		}
	}

	public final A0_1Context a0_1() throws RecognitionException {
		A0_1Context _localctx = new A0_1Context(_ctx, getState());
		enterRule(_localctx, 12, RULE_a0_1);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			match(T__0);
			setState(282);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__3) | (1L << T__5) | (1L << T__7) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << MINUS) | (1L << PLUS) | (1L << NOT) | (1L << IF) | (1L << IS_NULL) | (1L << COALESCE) | (1L << MAXIMUM) | (1L << MINIMUM))) != 0) || ((((_la - 88)) & ~0x3f) == 0 && ((1L << (_la - 88)) & ((1L << (NUMERIC_LITERAL - 88)) | (1L << (STRING_LITERAL - 88)) | (1L << (BOOLEAN_LITERAL - 88)))) != 0)) {
				{
				setState(281);
				expr(0);
				}
			}

			setState(284);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class A1Context extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public A1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_a1; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitA1(this);
			else return visitor.visitChildren(this);
		}
	}

	public final A1Context a1() throws RecognitionException {
		A1Context _localctx = new A1Context(_ctx, getState());
		enterRule(_localctx, 14, RULE_a1);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(286);
			match(T__0);
			setState(287);
			expr(0);
			setState(288);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class A1_2Context extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public A1_2Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_a1_2; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitA1_2(this);
			else return visitor.visitChildren(this);
		}
	}

	public final A1_2Context a1_2() throws RecognitionException {
		A1_2Context _localctx = new A1_2Context(_ctx, getState());
		enterRule(_localctx, 16, RULE_a1_2);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(290);
			match(T__0);
			setState(291);
			expr(0);
			setState(294);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(292);
				match(T__6);
				setState(293);
				expr(0);
				}
			}

			setState(296);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class A1_nContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public A1_nContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_a1_n; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitA1_n(this);
			else return visitor.visitChildren(this);
		}
	}

	public final A1_nContext a1_n() throws RecognitionException {
		A1_nContext _localctx = new A1_nContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_a1_n);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
			match(T__0);
			setState(299);
			expr(0);
			setState(304);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__6) {
				{
				{
				setState(300);
				match(T__6);
				setState(301);
				expr(0);
				}
				}
				setState(306);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(307);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class A2Context extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public A2Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_a2; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitA2(this);
			else return visitor.visitChildren(this);
		}
	}

	public final A2Context a2() throws RecognitionException {
		A2Context _localctx = new A2Context(_ctx, getState());
		enterRule(_localctx, 20, RULE_a2);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(309);
			match(T__0);
			setState(310);
			expr(0);
			setState(311);
			match(T__6);
			setState(312);
			expr(0);
			setState(313);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class A3Context extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public A3Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_a3; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitA3(this);
			else return visitor.visitChildren(this);
		}
	}

	public final A3Context a3() throws RecognitionException {
		A3Context _localctx = new A3Context(_ctx, getState());
		enterRule(_localctx, 22, RULE_a3);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(315);
			match(T__0);
			setState(316);
			expr(0);
			setState(317);
			match(T__6);
			setState(318);
			expr(0);
			setState(319);
			match(T__6);
			setState(320);
			expr(0);
			setState(321);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataElementContext extends ParserRuleContext {
		public DataElementIdContext dataElementId() {
			return getRuleContext(DataElementIdContext.class,0);
		}
		public DataElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementContext dataElement() throws RecognitionException {
		DataElementContext _localctx = new DataElementContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_dataElement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(323);
			match(T__7);
			setState(324);
			dataElementId();
			setState(325);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataElementOperandWithoutAocContext extends ParserRuleContext {
		public DataElementOperandIdWithoutAocContext dataElementOperandIdWithoutAoc() {
			return getRuleContext(DataElementOperandIdWithoutAocContext.class,0);
		}
		public DataElementOperandWithoutAocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementOperandWithoutAoc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementOperandWithoutAoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementOperandWithoutAocContext dataElementOperandWithoutAoc() throws RecognitionException {
		DataElementOperandWithoutAocContext _localctx = new DataElementOperandWithoutAocContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_dataElementOperandWithoutAoc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(327);
			match(T__7);
			setState(328);
			dataElementOperandIdWithoutAoc();
			setState(330);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__8) {
				{
				setState(329);
				match(T__8);
				}
			}

			setState(332);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataElementOperandWithAocContext extends ParserRuleContext {
		public DataElementOperandIdWithAocContext dataElementOperandIdWithAoc() {
			return getRuleContext(DataElementOperandIdWithAocContext.class,0);
		}
		public DataElementOperandWithAocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementOperandWithAoc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementOperandWithAoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementOperandWithAocContext dataElementOperandWithAoc() throws RecognitionException {
		DataElementOperandWithAocContext _localctx = new DataElementOperandWithAocContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_dataElementOperandWithAoc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(334);
			match(T__7);
			setState(335);
			dataElementOperandIdWithAoc();
			setState(336);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramDataElementContext extends ParserRuleContext {
		public ProgramDataElementIdContext programDataElementId() {
			return getRuleContext(ProgramDataElementIdContext.class,0);
		}
		public ProgramDataElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programDataElement; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramDataElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramDataElementContext programDataElement() throws RecognitionException {
		ProgramDataElementContext _localctx = new ProgramDataElementContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_programDataElement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(338);
			match(T__9);
			setState(339);
			programDataElementId();
			setState(340);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramAttributeContext extends ParserRuleContext {
		public ProgramAttributeIdContext programAttributeId() {
			return getRuleContext(ProgramAttributeIdContext.class,0);
		}
		public ProgramAttributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programAttribute; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramAttribute(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramAttributeContext programAttribute() throws RecognitionException {
		ProgramAttributeContext _localctx = new ProgramAttributeContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_programAttribute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(342);
			match(T__10);
			setState(343);
			programAttributeId();
			setState(344);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramIndicatorContext extends ParserRuleContext {
		public ProgramIndicatorIdContext programIndicatorId() {
			return getRuleContext(ProgramIndicatorIdContext.class,0);
		}
		public ProgramIndicatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programIndicator; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramIndicator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramIndicatorContext programIndicator() throws RecognitionException {
		ProgramIndicatorContext _localctx = new ProgramIndicatorContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_programIndicator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(346);
			match(T__11);
			setState(347);
			programIndicatorId();
			setState(348);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrgUnitCountContext extends ParserRuleContext {
		public OrgUnitCountIdContext orgUnitCountId() {
			return getRuleContext(OrgUnitCountIdContext.class,0);
		}
		public OrgUnitCountContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orgUnitCount; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitOrgUnitCount(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrgUnitCountContext orgUnitCount() throws RecognitionException {
		OrgUnitCountContext _localctx = new OrgUnitCountContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_orgUnitCount);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(350);
			match(T__12);
			setState(351);
			orgUnitCountId();
			setState(352);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ReportingRateContext extends ParserRuleContext {
		public ReportingRateIdContext reportingRateId() {
			return getRuleContext(ReportingRateIdContext.class,0);
		}
		public ReportingRateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reportingRate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitReportingRate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReportingRateContext reportingRate() throws RecognitionException {
		ReportingRateContext _localctx = new ReportingRateContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_reportingRate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(354);
			match(T__13);
			setState(355);
			reportingRateId();
			setState(356);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstantContext extends ParserRuleContext {
		public ConstantIdContext constantId() {
			return getRuleContext(ConstantIdContext.class,0);
		}
		public ConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitConstant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_constant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(358);
			match(T__14);
			setState(359);
			constantId();
			setState(360);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DaysContext extends ParserRuleContext {
		public DaysContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_days; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDays(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DaysContext days() throws RecognitionException {
		DaysContext _localctx = new DaysContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_days);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(362);
			match(T__15);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataElementIdContext extends ParserRuleContext {
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
		public DataElementIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementId; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementIdContext dataElementId() throws RecognitionException {
		DataElementIdContext _localctx = new DataElementIdContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_dataElementId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(364);
			match(UID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataElementOperandIdWithoutAocContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public DataElementOperandIdWithoutAocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementOperandIdWithoutAoc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementOperandIdWithoutAoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementOperandIdWithoutAocContext dataElementOperandIdWithoutAoc() throws RecognitionException {
		DataElementOperandIdWithoutAocContext _localctx = new DataElementOperandIdWithoutAocContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_dataElementOperandIdWithoutAoc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(366);
			match(UID);
			setState(367);
			match(T__2);
			setState(368);
			match(UID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DataElementOperandIdWithAocContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public DataElementOperandIdWithAocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementOperandIdWithAoc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementOperandIdWithAoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementOperandIdWithAocContext dataElementOperandIdWithAoc() throws RecognitionException {
		DataElementOperandIdWithAocContext _localctx = new DataElementOperandIdWithAocContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_dataElementOperandIdWithAoc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(370);
			match(UID);
			setState(375);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__2:
				{
				setState(371);
				match(T__2);
				setState(372);
				match(UID);
				setState(373);
				match(T__2);
				}
				break;
			case T__16:
				{
				setState(374);
				match(T__16);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(377);
			match(UID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramDataElementIdContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public ProgramDataElementIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programDataElementId; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramDataElementId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramDataElementIdContext programDataElementId() throws RecognitionException {
		ProgramDataElementIdContext _localctx = new ProgramDataElementIdContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_programDataElementId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(379);
			match(UID);
			setState(380);
			match(T__2);
			setState(381);
			match(UID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramAttributeIdContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public ProgramAttributeIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programAttributeId; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramAttributeId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramAttributeIdContext programAttributeId() throws RecognitionException {
		ProgramAttributeIdContext _localctx = new ProgramAttributeIdContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_programAttributeId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(383);
			match(UID);
			setState(384);
			match(T__2);
			setState(385);
			match(UID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramIndicatorIdContext extends ParserRuleContext {
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
		public ProgramIndicatorIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programIndicatorId; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramIndicatorId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramIndicatorIdContext programIndicatorId() throws RecognitionException {
		ProgramIndicatorIdContext _localctx = new ProgramIndicatorIdContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_programIndicatorId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(387);
			match(UID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrgUnitCountIdContext extends ParserRuleContext {
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
		public OrgUnitCountIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orgUnitCountId; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitOrgUnitCountId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrgUnitCountIdContext orgUnitCountId() throws RecognitionException {
		OrgUnitCountIdContext _localctx = new OrgUnitCountIdContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_orgUnitCountId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(389);
			match(UID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ReportingRateIdContext extends ParserRuleContext {
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
		public JavaIdentifierContext javaIdentifier() {
			return getRuleContext(JavaIdentifierContext.class,0);
		}
		public ReportingRateIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reportingRateId; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitReportingRateId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReportingRateIdContext reportingRateId() throws RecognitionException {
		ReportingRateIdContext _localctx = new ReportingRateIdContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_reportingRateId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(391);
			match(UID);
			setState(392);
			match(T__2);
			setState(393);
			javaIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstantIdContext extends ParserRuleContext {
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
		public ConstantIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constantId; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitConstantId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantIdContext constantId() throws RecognitionException {
		ConstantIdContext _localctx = new ConstantIdContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_constantId);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(395);
			match(UID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumericLiteralContext extends ParserRuleContext {
		public TerminalNode NUMERIC_LITERAL() { return getToken(ExpressionParser.NUMERIC_LITERAL, 0); }
		public NumericLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numericLiteral; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitNumericLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumericLiteralContext numericLiteral() throws RecognitionException {
		NumericLiteralContext _localctx = new NumericLiteralContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_numericLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(397);
			match(NUMERIC_LITERAL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StringLiteralContext extends ParserRuleContext {
		public TerminalNode STRING_LITERAL() { return getToken(ExpressionParser.STRING_LITERAL, 0); }
		public StringLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringLiteral; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringLiteralContext stringLiteral() throws RecognitionException {
		StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_stringLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(399);
			match(STRING_LITERAL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BooleanLiteralContext extends ParserRuleContext {
		public TerminalNode BOOLEAN_LITERAL() { return getToken(ExpressionParser.BOOLEAN_LITERAL, 0); }
		public BooleanLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanLiteral; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanLiteralContext booleanLiteral() throws RecognitionException {
		BooleanLiteralContext _localctx = new BooleanLiteralContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_booleanLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(401);
			match(BOOLEAN_LITERAL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JavaIdentifierContext extends ParserRuleContext {
		public TerminalNode JAVA_IDENTIFIER() { return getToken(ExpressionParser.JAVA_IDENTIFIER, 0); }
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
		public JavaIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_javaIdentifier; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitJavaIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JavaIdentifierContext javaIdentifier() throws RecognitionException {
		JavaIdentifierContext _localctx = new JavaIdentifierContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_javaIdentifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(403);
			_la = _input.LA(1);
			if ( !(_la==UID || _la==JAVA_IDENTIFIER) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 23);
		case 1:
			return precpred(_ctx, 20);
		case 2:
			return precpred(_ctx, 19);
		case 3:
			return precpred(_ctx, 18);
		case 4:
			return precpred(_ctx, 17);
		case 5:
			return precpred(_ctx, 16);
		case 6:
			return precpred(_ctx, 15);
		case 7:
			return precpred(_ctx, 52);
		case 8:
			return precpred(_ctx, 46);
		case 9:
			return precpred(_ctx, 45);
		case 10:
			return precpred(_ctx, 44);
		case 11:
			return precpred(_ctx, 43);
		case 12:
			return precpred(_ctx, 42);
		case 13:
			return precpred(_ctx, 41);
		case 14:
			return precpred(_ctx, 40);
		case 15:
			return precpred(_ctx, 39);
		case 16:
			return precpred(_ctx, 38);
		case 17:
			return precpred(_ctx, 37);
		case 18:
			return precpred(_ctx, 36);
		case 19:
			return precpred(_ctx, 35);
		case 20:
			return precpred(_ctx, 34);
		case 21:
			return precpred(_ctx, 33);
		case 22:
			return precpred(_ctx, 32);
		case 23:
			return precpred(_ctx, 31);
		case 24:
			return precpred(_ctx, 30);
		case 25:
			return precpred(_ctx, 29);
		case 26:
			return precpred(_ctx, 28);
		case 27:
			return precpred(_ctx, 27);
		case 28:
			return precpred(_ctx, 26);
		case 29:
			return precpred(_ctx, 25);
		case 30:
			return precpred(_ctx, 24);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3_\u0198\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3m\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\7\3\u00e4\n\3\f\3\16\3\u00e7\13\3\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\5\4\u00ef\n\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\5\5\u0100\n\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6\u0116\n\6\3\7\3\7\3\7\3\b\3\b\5\b"+
		"\u011d\n\b\3\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\5\n\u0129\n\n\3\n\3"+
		"\n\3\13\3\13\3\13\3\13\7\13\u0131\n\13\f\13\16\13\u0134\13\13\3\13\3\13"+
		"\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16"+
		"\3\16\3\17\3\17\3\17\5\17\u014d\n\17\3\17\3\17\3\20\3\20\3\20\3\20\3\21"+
		"\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\24\3\24\3\24"+
		"\3\24\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\27\3\27\3\30\3\30\3\31"+
		"\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\5\32\u017a\n\32\3\32\3\32\3\33"+
		"\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3\37"+
		"\3\37\3 \3 \3!\3!\3\"\3\"\3#\3#\3$\3$\3$\2\3\4%\2\4\6\b\n\f\16\20\22\24"+
		"\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDF\2\b\4\2\25\25\27\27\3\2\30"+
		"\32\3\2\25\26\3\2\33\36\3\2\37 \3\2]^\2\u01c5\2H\3\2\2\2\4l\3\2\2\2\6"+
		"\u00ee\3\2\2\2\b\u00ff\3\2\2\2\n\u0115\3\2\2\2\f\u0117\3\2\2\2\16\u011a"+
		"\3\2\2\2\20\u0120\3\2\2\2\22\u0124\3\2\2\2\24\u012c\3\2\2\2\26\u0137\3"+
		"\2\2\2\30\u013d\3\2\2\2\32\u0145\3\2\2\2\34\u0149\3\2\2\2\36\u0150\3\2"+
		"\2\2 \u0154\3\2\2\2\"\u0158\3\2\2\2$\u015c\3\2\2\2&\u0160\3\2\2\2(\u0164"+
		"\3\2\2\2*\u0168\3\2\2\2,\u016c\3\2\2\2.\u016e\3\2\2\2\60\u0170\3\2\2\2"+
		"\62\u0174\3\2\2\2\64\u017d\3\2\2\2\66\u0181\3\2\2\28\u0185\3\2\2\2:\u0187"+
		"\3\2\2\2<\u0189\3\2\2\2>\u018d\3\2\2\2@\u018f\3\2\2\2B\u0191\3\2\2\2D"+
		"\u0193\3\2\2\2F\u0195\3\2\2\2HI\5\4\3\2IJ\7\2\2\3J\3\3\2\2\2KL\b\3\1\2"+
		"LM\7\3\2\2MN\5\4\3\2NO\7\4\2\2Om\3\2\2\2PQ\7#\2\2Qm\5\30\r\2RS\7%\2\2"+
		"Sm\5\20\t\2TU\7&\2\2Um\5\24\13\2VW\7\'\2\2Wm\5\24\13\2XY\7(\2\2Ym\5\24"+
		"\13\2Z[\t\2\2\2[m\5\4\3\30\\]\7\26\2\2]m\5\4\3\27^m\5\32\16\2_m\5\34\17"+
		"\2`m\5\36\20\2am\5 \21\2bm\5\"\22\2cm\5$\23\2dm\5&\24\2em\5(\25\2fm\5"+
		"*\26\2gm\5,\27\2hm\5\6\4\2im\5@!\2jm\5B\"\2km\5D#\2lK\3\2\2\2lP\3\2\2"+
		"\2lR\3\2\2\2lT\3\2\2\2lV\3\2\2\2lX\3\2\2\2lZ\3\2\2\2l\\\3\2\2\2l^\3\2"+
		"\2\2l_\3\2\2\2l`\3\2\2\2la\3\2\2\2lb\3\2\2\2lc\3\2\2\2ld\3\2\2\2le\3\2"+
		"\2\2lf\3\2\2\2lg\3\2\2\2lh\3\2\2\2li\3\2\2\2lj\3\2\2\2lk\3\2\2\2m\u00e5"+
		"\3\2\2\2no\f\31\2\2op\7\24\2\2p\u00e4\5\4\3\31qr\f\26\2\2rs\t\3\2\2s\u00e4"+
		"\5\4\3\27tu\f\25\2\2uv\t\4\2\2v\u00e4\5\4\3\26wx\f\24\2\2xy\t\5\2\2y\u00e4"+
		"\5\4\3\25z{\f\23\2\2{|\t\6\2\2|\u00e4\5\4\3\24}~\f\22\2\2~\177\7!\2\2"+
		"\177\u00e4\5\4\3\23\u0080\u0081\f\21\2\2\u0081\u0082\7\"\2\2\u0082\u00e4"+
		"\5\4\3\22\u0083\u0084\f\66\2\2\u0084\u0085\7\5\2\2\u0085\u0086\7$\2\2"+
		"\u0086\u00e4\5\20\t\2\u0087\u0088\f\60\2\2\u0088\u0089\7\5\2\2\u0089\u008a"+
		"\7)\2\2\u008a\u00e4\5\f\7\2\u008b\u008c\f/\2\2\u008c\u008d\7\5\2\2\u008d"+
		"\u008e\7\61\2\2\u008e\u00e4\5\f\7\2\u008f\u0090\f.\2\2\u0090\u0091\7\5"+
		"\2\2\u0091\u0092\7\60\2\2\u0092\u00e4\5\f\7\2\u0093\u0094\f-\2\2\u0094"+
		"\u0095\7\5\2\2\u0095\u0096\7*\2\2\u0096\u00e4\5\f\7\2\u0097\u0098\f,\2"+
		"\2\u0098\u0099\7\5\2\2\u0099\u009a\7.\2\2\u009a\u00e4\5\f\7\2\u009b\u009c"+
		"\f+\2\2\u009c\u009d\7\5\2\2\u009d\u009e\7/\2\2\u009e\u00e4\5\f\7\2\u009f"+
		"\u00a0\f*\2\2\u00a0\u00a1\7\5\2\2\u00a1\u00a2\7\62\2\2\u00a2\u00e4\5\f"+
		"\7\2\u00a3\u00a4\f)\2\2\u00a4\u00a5\7\5\2\2\u00a5\u00a6\7+\2\2\u00a6\u00e4"+
		"\5\16\b\2\u00a7\u00a8\f(\2\2\u00a8\u00a9\7\5\2\2\u00a9\u00aa\7,\2\2\u00aa"+
		"\u00e4\5\16\b\2\u00ab\u00ac\f\'\2\2\u00ac\u00ad\7\5\2\2\u00ad\u00ae\7"+
		"\63\2\2\u00ae\u00e4\5\20\t\2\u00af\u00b0\f&\2\2\u00b0\u00b1\7\5\2\2\u00b1"+
		"\u00b2\7\64\2\2\u00b2\u00e4\5\20\t\2\u00b3\u00b4\f%\2\2\u00b4\u00b5\7"+
		"\5\2\2\u00b5\u00b6\7\65\2\2\u00b6\u00e4\5\20\t\2\u00b7\u00b8\f$\2\2\u00b8"+
		"\u00b9\7\5\2\2\u00b9\u00ba\7\66\2\2\u00ba\u00e4\5\20\t\2\u00bb\u00bc\f"+
		"#\2\2\u00bc\u00bd\7\5\2\2\u00bd\u00be\7\67\2\2\u00be\u00e4\5\f\7\2\u00bf"+
		"\u00c0\f\"\2\2\u00c0\u00c1\7\5\2\2\u00c1\u00c2\78\2\2\u00c2\u00e4\5\f"+
		"\7\2\u00c3\u00c4\f!\2\2\u00c4\u00c5\7\5\2\2\u00c5\u00c6\79\2\2\u00c6\u00e4"+
		"\5\f\7\2\u00c7\u00c8\f \2\2\u00c8\u00c9\7\5\2\2\u00c9\u00ca\7:\2\2\u00ca"+
		"\u00e4\5\24\13\2\u00cb\u00cc\f\37\2\2\u00cc\u00cd\7\5\2\2\u00cd\u00ce"+
		"\7;\2\2\u00ce\u00e4\5\20\t\2\u00cf\u00d0\f\36\2\2\u00d0\u00d1\7\5\2\2"+
		"\u00d1\u00d2\7<\2\2\u00d2\u00e4\5\20\t\2\u00d3\u00d4\f\35\2\2\u00d4\u00d5"+
		"\7\5\2\2\u00d5\u00d6\7=\2\2\u00d6\u00e4\5\24\13\2\u00d7\u00d8\f\34\2\2"+
		"\u00d8\u00d9\7\5\2\2\u00d9\u00da\7>\2\2\u00da\u00e4\5\20\t\2\u00db\u00dc"+
		"\f\33\2\2\u00dc\u00dd\7\5\2\2\u00dd\u00de\7?\2\2\u00de\u00e4\5\24\13\2"+
		"\u00df\u00e0\f\32\2\2\u00e0\u00e1\7\5\2\2\u00e1\u00e2\7@\2\2\u00e2\u00e4"+
		"\5\24\13\2\u00e3n\3\2\2\2\u00e3q\3\2\2\2\u00e3t\3\2\2\2\u00e3w\3\2\2\2"+
		"\u00e3z\3\2\2\2\u00e3}\3\2\2\2\u00e3\u0080\3\2\2\2\u00e3\u0083\3\2\2\2"+
		"\u00e3\u0087\3\2\2\2\u00e3\u008b\3\2\2\2\u00e3\u008f\3\2\2\2\u00e3\u0093"+
		"\3\2\2\2\u00e3\u0097\3\2\2\2\u00e3\u009b\3\2\2\2\u00e3\u009f\3\2\2\2\u00e3"+
		"\u00a3\3\2\2\2\u00e3\u00a7\3\2\2\2\u00e3\u00ab\3\2\2\2\u00e3\u00af\3\2"+
		"\2\2\u00e3\u00b3\3\2\2\2\u00e3\u00b7\3\2\2\2\u00e3\u00bb\3\2\2\2\u00e3"+
		"\u00bf\3\2\2\2\u00e3\u00c3\3\2\2\2\u00e3\u00c7\3\2\2\2\u00e3\u00cb\3\2"+
		"\2\2\u00e3\u00cf\3\2\2\2\u00e3\u00d3\3\2\2\2\u00e3\u00d7\3\2\2\2\u00e3"+
		"\u00db\3\2\2\2\u00e3\u00df\3\2\2\2\u00e4\u00e7\3\2\2\2\u00e5\u00e3\3\2"+
		"\2\2\u00e5\u00e6\3\2\2\2\u00e6\5\3\2\2\2\u00e7\u00e5\3\2\2\2\u00e8\u00e9"+
		"\7\6\2\2\u00e9\u00ea\5\b\5\2\u00ea\u00eb\7\7\2\2\u00eb\u00ef\3\2\2\2\u00ec"+
		"\u00ed\7\b\2\2\u00ed\u00ef\5\n\6\2\u00ee\u00e8\3\2\2\2\u00ee\u00ec\3\2"+
		"\2\2\u00ef\7\3\2\2\2\u00f0\u0100\7A\2\2\u00f1\u0100\7B\2\2\u00f2\u0100"+
		"\7C\2\2\u00f3\u0100\7D\2\2\u00f4\u0100\7E\2\2\u00f5\u0100\7F\2\2\u00f6"+
		"\u0100\7G\2\2\u00f7\u0100\7H\2\2\u00f8\u0100\7I\2\2\u00f9\u0100\7J\2\2"+
		"\u00fa\u0100\7K\2\2\u00fb\u0100\7L\2\2\u00fc\u0100\7M\2\2\u00fd\u0100"+
		"\7N\2\2\u00fe\u0100\7O\2\2\u00ff\u00f0\3\2\2\2\u00ff\u00f1\3\2\2\2\u00ff"+
		"\u00f2\3\2\2\2\u00ff\u00f3\3\2\2\2\u00ff\u00f4\3\2\2\2\u00ff\u00f5\3\2"+
		"\2\2\u00ff\u00f6\3\2\2\2\u00ff\u00f7\3\2\2\2\u00ff\u00f8\3\2\2\2\u00ff"+
		"\u00f9\3\2\2\2\u00ff\u00fa\3\2\2\2\u00ff\u00fb\3\2\2\2\u00ff\u00fc\3\2"+
		"\2\2\u00ff\u00fd\3\2\2\2\u00ff\u00fe\3\2\2\2\u0100\t\3\2\2\2\u0101\u0102"+
		"\7P\2\2\u0102\u0116\5\20\t\2\u0103\u0104\7Q\2\2\u0104\u0116\5\26\f\2\u0105"+
		"\u0106\7R\2\2\u0106\u0116\5\26\f\2\u0107\u0108\7S\2\2\u0108\u0116\5\26"+
		"\f\2\u0109\u010a\7T\2\2\u010a\u0116\5\26\f\2\u010b\u010c\7U\2\2\u010c"+
		"\u0116\5\26\f\2\u010d\u010e\7V\2\2\u010e\u0116\5\30\r\2\u010f\u0110\7"+
		"W\2\2\u0110\u0116\5\20\t\2\u0111\u0112\7X\2\2\u0112\u0116\5\20\t\2\u0113"+
		"\u0114\7Y\2\2\u0114\u0116\5\24\13\2\u0115\u0101\3\2\2\2\u0115\u0103\3"+
		"\2\2\2\u0115\u0105\3\2\2\2\u0115\u0107\3\2\2\2\u0115\u0109\3\2\2\2\u0115"+
		"\u010b\3\2\2\2\u0115\u010d\3\2\2\2\u0115\u010f\3\2\2\2\u0115\u0111\3\2"+
		"\2\2\u0115\u0113\3\2\2\2\u0116\13\3\2\2\2\u0117\u0118\7\3\2\2\u0118\u0119"+
		"\7\4\2\2\u0119\r\3\2\2\2\u011a\u011c\7\3\2\2\u011b\u011d\5\4\3\2\u011c"+
		"\u011b\3\2\2\2\u011c\u011d\3\2\2\2\u011d\u011e\3\2\2\2\u011e\u011f\7\4"+
		"\2\2\u011f\17\3\2\2\2\u0120\u0121\7\3\2\2\u0121\u0122\5\4\3\2\u0122\u0123"+
		"\7\4\2\2\u0123\21\3\2\2\2\u0124\u0125\7\3\2\2\u0125\u0128\5\4\3\2\u0126"+
		"\u0127\7\t\2\2\u0127\u0129\5\4\3\2\u0128\u0126\3\2\2\2\u0128\u0129\3\2"+
		"\2\2\u0129\u012a\3\2\2\2\u012a\u012b\7\4\2\2\u012b\23\3\2\2\2\u012c\u012d"+
		"\7\3\2\2\u012d\u0132\5\4\3\2\u012e\u012f\7\t\2\2\u012f\u0131\5\4\3\2\u0130"+
		"\u012e\3\2\2\2\u0131\u0134\3\2\2\2\u0132\u0130\3\2\2\2\u0132\u0133\3\2"+
		"\2\2\u0133\u0135\3\2\2\2\u0134\u0132\3\2\2\2\u0135\u0136\7\4\2\2\u0136"+
		"\25\3\2\2\2\u0137\u0138\7\3\2\2\u0138\u0139\5\4\3\2\u0139\u013a\7\t\2"+
		"\2\u013a\u013b\5\4\3\2\u013b\u013c\7\4\2\2\u013c\27\3\2\2\2\u013d\u013e"+
		"\7\3\2\2\u013e\u013f\5\4\3\2\u013f\u0140\7\t\2\2\u0140\u0141\5\4\3\2\u0141"+
		"\u0142\7\t\2\2\u0142\u0143\5\4\3\2\u0143\u0144\7\4\2\2\u0144\31\3\2\2"+
		"\2\u0145\u0146\7\n\2\2\u0146\u0147\5.\30\2\u0147\u0148\7\7\2\2\u0148\33"+
		"\3\2\2\2\u0149\u014a\7\n\2\2\u014a\u014c\5\60\31\2\u014b\u014d\7\13\2"+
		"\2\u014c\u014b\3\2\2\2\u014c\u014d\3\2\2\2\u014d\u014e\3\2\2\2\u014e\u014f"+
		"\7\7\2\2\u014f\35\3\2\2\2\u0150\u0151\7\n\2\2\u0151\u0152\5\62\32\2\u0152"+
		"\u0153\7\7\2\2\u0153\37\3\2\2\2\u0154\u0155\7\f\2\2\u0155\u0156\5\64\33"+
		"\2\u0156\u0157\7\7\2\2\u0157!\3\2\2\2\u0158\u0159\7\r\2\2\u0159\u015a"+
		"\5\66\34\2\u015a\u015b\7\7\2\2\u015b#\3\2\2\2\u015c\u015d\7\16\2\2\u015d"+
		"\u015e\58\35\2\u015e\u015f\7\7\2\2\u015f%\3\2\2\2\u0160\u0161\7\17\2\2"+
		"\u0161\u0162\5:\36\2\u0162\u0163\7\7\2\2\u0163\'\3\2\2\2\u0164\u0165\7"+
		"\20\2\2\u0165\u0166\5<\37\2\u0166\u0167\7\7\2\2\u0167)\3\2\2\2\u0168\u0169"+
		"\7\21\2\2\u0169\u016a\5> \2\u016a\u016b\7\7\2\2\u016b+\3\2\2\2\u016c\u016d"+
		"\7\22\2\2\u016d-\3\2\2\2\u016e\u016f\7]\2\2\u016f/\3\2\2\2\u0170\u0171"+
		"\7]\2\2\u0171\u0172\7\5\2\2\u0172\u0173\7]\2\2\u0173\61\3\2\2\2\u0174"+
		"\u0179\7]\2\2\u0175\u0176\7\5\2\2\u0176\u0177\7]\2\2\u0177\u017a\7\5\2"+
		"\2\u0178\u017a\7\23\2\2\u0179\u0175\3\2\2\2\u0179\u0178\3\2\2\2\u017a"+
		"\u017b\3\2\2\2\u017b\u017c\7]\2\2\u017c\63\3\2\2\2\u017d\u017e\7]\2\2"+
		"\u017e\u017f\7\5\2\2\u017f\u0180\7]\2\2\u0180\65\3\2\2\2\u0181\u0182\7"+
		"]\2\2\u0182\u0183\7\5\2\2\u0183\u0184\7]\2\2\u0184\67\3\2\2\2\u0185\u0186"+
		"\7]\2\2\u01869\3\2\2\2\u0187\u0188\7]\2\2\u0188;\3\2\2\2\u0189\u018a\7"+
		"]\2\2\u018a\u018b\7\5\2\2\u018b\u018c\5F$\2\u018c=\3\2\2\2\u018d\u018e"+
		"\7]\2\2\u018e?\3\2\2\2\u018f\u0190\7Z\2\2\u0190A\3\2\2\2\u0191\u0192\7"+
		"[\2\2\u0192C\3\2\2\2\u0193\u0194\7\\\2\2\u0194E\3\2\2\2\u0195\u0196\t"+
		"\7\2\2\u0196G\3\2\2\2\rl\u00e3\u00e5\u00ee\u00ff\u0115\u011c\u0128\u0132"+
		"\u014c\u0179";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}