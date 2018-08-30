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
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, POWER=17, 
		MINUS=18, PLUS=19, NOT=20, MUL=21, DIV=22, MOD=23, LEQ=24, GEQ=25, LT=26, 
		GT=27, EQ=28, NE=29, AND=30, OR=31, IF=32, EXCEPT=33, IS_NULL=34, COALESCE=35, 
		SUM=36, AVERAGE=37, FIRST=38, LAST=39, COUNT=40, STDDEV=41, VARIANCE=42, 
		MIN=43, MAX=44, MEDIAN=45, PERCENTILE=46, RANK_HIGH=47, RANK_LOW=48, RANK_PERCENTILE=49, 
		PERIOD=50, OU_LEVEL=51, OU_ANCESTOR=52, OU_DESCENDANT=53, OU_PEER=54, 
		OU_GROUP=55, EVENT_DATE=56, DUE_DATE=57, INCIDENT_DATE=58, ENROLLMENT_DATE=59, 
		ENROLLMENT_STATUS=60, CURRENT_STATUS=61, VALUE_COUNT=62, ZERO_POS_VALUE_COUNT=63, 
		EVENT_COUNT=64, ENROLLMENT_COUNT=65, TEI_COUNT=66, PROGRAM_STAGE_NAME=67, 
		PROGRAM_STAGE_ID=68, REPORTING_PERIOD_START=69, REPORTING_PERIOD_END=70, 
		HAS_VALUE=71, MINUTES_BETWEEN=72, DAYS_BETWEEN=73, WEEKS_BETWEEN=74, MONTHS_BETWEEN=75, 
		YEARS_BETWEEN=76, CONDITION=77, ZING=78, OIZP=79, ZPVC=80, NUMERIC_LITERAL=81, 
		STRING_LITERAL=82, BOOLEAN_LITERAL=83, UID=84, WS=85;
	public static final int
		RULE_expr = 0, RULE_value = 1, RULE_programIndicatorExpr = 2, RULE_programIndicatorVariable = 3, 
		RULE_programIndicatorFunction = 4, RULE_a0 = 5, RULE_a0_1 = 6, RULE_a1 = 7, 
		RULE_a1_2 = 8, RULE_a1_n = 9, RULE_a2 = 10, RULE_a3 = 11, RULE_numericLiteral = 12, 
		RULE_stringLiteral = 13, RULE_booleanLiteral = 14, RULE_dimensionItemObject = 15, 
		RULE_dataElementOperand = 16, RULE_programDataElement = 17, RULE_programTrackedEntityAttribute = 18, 
		RULE_programIndicator = 19, RULE_orgUnitCount = 20, RULE_reportingRate = 21, 
		RULE_constant = 22, RULE_days = 23;
	public static final String[] ruleNames = {
		"expr", "value", "programIndicatorExpr", "programIndicatorVariable", "programIndicatorFunction", 
		"a0", "a0_1", "a1", "a1_2", "a1_n", "a2", "a3", "numericLiteral", "stringLiteral", 
		"booleanLiteral", "dimensionItemObject", "dataElementOperand", "programDataElement", 
		"programTrackedEntityAttribute", "programIndicator", "orgUnitCount", "reportingRate", 
		"constant", "days"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'('", "')'", "'.'", "'V{'", "'}'", "'d2:'", "','", "'#{'", "'D{'", 
		"'A{'", "'I{'", "'OUG{'", "'R{'", "'.REPORTING_RATE}'", "'C{'", "'[days]'", 
		"'^'", "'-'", "'+'", "'!'", "'*'", "'/'", "'%'", "'<='", "'>='", "'<'", 
		"'>'", "'=='", "'!='", "'&&'", "'||'", "'if'", "'except'", "'isNull'", 
		"'coalesce'", "'sum'", "'average'", "'first'", "'last'", "'count'", "'stddev'", 
		"'variance'", "'min'", "'max'", "'median'", "'percentile'", "'rankHigh'", 
		"'rankLow'", "'rankPercentile'", "'period'", "'ouLevel'", "'ouAncestor'", 
		"'ouDescendant'", "'ouPeer'", "'ouGroup'", "'event_date'", "'due_date'", 
		"'incident_date'", "'enrollment_date'", "'enrollment_status'", "'current_date'", 
		"'value_count'", "'zero_pos_value_count'", "'event_count'", "'enrollment_count'", 
		"'tei_count'", "'program_stage_name'", "'program_stage_id'", "'reporting_period_start'", 
		"'reporting_period_end'", "'hasValue'", "'minutesBetween'", "'daysBetween'", 
		"'weeksBetween'", "'monthsBetween'", "'yearsBetween'", "'condition'", 
		"'zing'", "'oizp'", "'zpvc'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, "POWER", "MINUS", "PLUS", "NOT", "MUL", 
		"DIV", "MOD", "LEQ", "GEQ", "LT", "GT", "EQ", "NE", "AND", "OR", "IF", 
		"EXCEPT", "IS_NULL", "COALESCE", "SUM", "AVERAGE", "FIRST", "LAST", "COUNT", 
		"STDDEV", "VARIANCE", "MIN", "MAX", "MEDIAN", "PERCENTILE", "RANK_HIGH", 
		"RANK_LOW", "RANK_PERCENTILE", "PERIOD", "OU_LEVEL", "OU_ANCESTOR", "OU_DESCENDANT", 
		"OU_PEER", "OU_GROUP", "EVENT_DATE", "DUE_DATE", "INCIDENT_DATE", "ENROLLMENT_DATE", 
		"ENROLLMENT_STATUS", "CURRENT_STATUS", "VALUE_COUNT", "ZERO_POS_VALUE_COUNT", 
		"EVENT_COUNT", "ENROLLMENT_COUNT", "TEI_COUNT", "PROGRAM_STAGE_NAME", 
		"PROGRAM_STAGE_ID", "REPORTING_PERIOD_START", "REPORTING_PERIOD_END", 
		"HAS_VALUE", "MINUTES_BETWEEN", "DAYS_BETWEEN", "WEEKS_BETWEEN", "MONTHS_BETWEEN", 
		"YEARS_BETWEEN", "CONDITION", "ZING", "OIZP", "ZPVC", "NUMERIC_LITERAL", 
		"STRING_LITERAL", "BOOLEAN_LITERAL", "UID", "WS"
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
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ProgramIndicatorExprContext programIndicatorExpr() {
			return getRuleContext(ProgramIndicatorExprContext.class,0);
		}
		public A0Context a0() {
			return getRuleContext(A0Context.class,0);
		}
		public A0_1Context a0_1() {
			return getRuleContext(A0_1Context.class,0);
		}
		public A1_2Context a1_2() {
			return getRuleContext(A1_2Context.class,0);
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
		int _startState = 0;
		enterRecursionRule(_localctx, 0, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(65);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				{
				setState(49);
				match(T__0);
				setState(50);
				expr(0);
				setState(51);
				match(T__1);
				}
				break;
			case IF:
				{
				setState(53);
				((ExprContext)_localctx).fun = match(IF);
				setState(54);
				a3();
				}
				break;
			case IS_NULL:
				{
				setState(55);
				((ExprContext)_localctx).fun = match(IS_NULL);
				setState(56);
				a1();
				}
				break;
			case COALESCE:
				{
				setState(57);
				((ExprContext)_localctx).fun = match(COALESCE);
				setState(58);
				a1_n();
				}
				break;
			case MINUS:
			case NOT:
				{
				setState(59);
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
				setState(60);
				expr(10);
				}
				break;
			case PLUS:
				{
				setState(61);
				match(PLUS);
				setState(62);
				expr(9);
				}
				break;
			case T__7:
			case T__8:
			case T__9:
			case T__10:
			case T__11:
			case T__12:
			case T__14:
			case T__15:
			case NUMERIC_LITERAL:
			case STRING_LITERAL:
			case BOOLEAN_LITERAL:
				{
				setState(63);
				value();
				}
				break;
			case T__3:
			case T__5:
				{
				setState(64);
				programIndicatorExpr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(170);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(168);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(67);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(68);
						((ExprContext)_localctx).fun = match(POWER);
						setState(69);
						expr(11);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(70);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(71);
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
						setState(72);
						expr(9);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(73);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(74);
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
						setState(75);
						expr(8);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(76);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(77);
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
						setState(78);
						expr(7);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(79);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(80);
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
						setState(81);
						expr(6);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(82);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(83);
						((ExprContext)_localctx).fun = match(AND);
						setState(84);
						expr(5);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(85);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(86);
						((ExprContext)_localctx).fun = match(OR);
						setState(87);
						expr(4);
						}
						break;
					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(88);
						if (!(precpred(_ctx, 34))) throw new FailedPredicateException(this, "precpred(_ctx, 34)");
						setState(89);
						match(T__2);
						setState(90);
						((ExprContext)_localctx).fun = match(EXCEPT);
						setState(91);
						a1();
						}
						break;
					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(92);
						if (!(precpred(_ctx, 30))) throw new FailedPredicateException(this, "precpred(_ctx, 30)");
						setState(93);
						match(T__2);
						setState(94);
						((ExprContext)_localctx).fun = match(SUM);
						setState(95);
						a0();
						}
						break;
					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(96);
						if (!(precpred(_ctx, 29))) throw new FailedPredicateException(this, "precpred(_ctx, 29)");
						setState(97);
						match(T__2);
						setState(98);
						((ExprContext)_localctx).fun = match(MAX);
						setState(99);
						a0();
						}
						break;
					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(100);
						if (!(precpred(_ctx, 28))) throw new FailedPredicateException(this, "precpred(_ctx, 28)");
						setState(101);
						match(T__2);
						setState(102);
						((ExprContext)_localctx).fun = match(MIN);
						setState(103);
						a0();
						}
						break;
					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(104);
						if (!(precpred(_ctx, 27))) throw new FailedPredicateException(this, "precpred(_ctx, 27)");
						setState(105);
						match(T__2);
						setState(106);
						((ExprContext)_localctx).fun = match(AVERAGE);
						setState(107);
						a0();
						}
						break;
					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(108);
						if (!(precpred(_ctx, 26))) throw new FailedPredicateException(this, "precpred(_ctx, 26)");
						setState(109);
						match(T__2);
						setState(110);
						((ExprContext)_localctx).fun = match(STDDEV);
						setState(111);
						a0();
						}
						break;
					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(112);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(113);
						match(T__2);
						setState(114);
						((ExprContext)_localctx).fun = match(VARIANCE);
						setState(115);
						a0();
						}
						break;
					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(116);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(117);
						match(T__2);
						setState(118);
						((ExprContext)_localctx).fun = match(MEDIAN);
						setState(119);
						a0();
						}
						break;
					case 16:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(120);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(121);
						match(T__2);
						setState(122);
						((ExprContext)_localctx).fun = match(FIRST);
						setState(123);
						a0_1();
						}
						break;
					case 17:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(124);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(125);
						match(T__2);
						setState(126);
						((ExprContext)_localctx).fun = match(LAST);
						setState(127);
						a0_1();
						}
						break;
					case 18:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(128);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(129);
						match(T__2);
						setState(130);
						((ExprContext)_localctx).fun = match(PERCENTILE);
						setState(131);
						a1();
						}
						break;
					case 19:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(132);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(133);
						match(T__2);
						setState(134);
						((ExprContext)_localctx).fun = match(RANK_HIGH);
						setState(135);
						a1();
						}
						break;
					case 20:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(136);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(137);
						match(T__2);
						setState(138);
						((ExprContext)_localctx).fun = match(RANK_LOW);
						setState(139);
						a1();
						}
						break;
					case 21:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(140);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(141);
						match(T__2);
						setState(142);
						((ExprContext)_localctx).fun = match(RANK_PERCENTILE);
						setState(143);
						a1();
						}
						break;
					case 22:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(144);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(145);
						match(T__2);
						setState(146);
						((ExprContext)_localctx).fun = match(PERIOD);
						setState(147);
						a1_n();
						}
						break;
					case 23:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(148);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(149);
						match(T__2);
						setState(150);
						((ExprContext)_localctx).fun = match(OU_LEVEL);
						setState(151);
						a1_2();
						}
						break;
					case 24:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(152);
						if (!(precpred(_ctx, 15))) throw new FailedPredicateException(this, "precpred(_ctx, 15)");
						setState(153);
						match(T__2);
						setState(154);
						((ExprContext)_localctx).fun = match(OU_ANCESTOR);
						setState(155);
						a1();
						}
						break;
					case 25:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(156);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(157);
						match(T__2);
						setState(158);
						((ExprContext)_localctx).fun = match(OU_DESCENDANT);
						setState(159);
						a1_2();
						}
						break;
					case 26:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(160);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(161);
						match(T__2);
						setState(162);
						((ExprContext)_localctx).fun = match(OU_PEER);
						setState(163);
						a1_2();
						}
						break;
					case 27:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(164);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(165);
						match(T__2);
						setState(166);
						((ExprContext)_localctx).fun = match(OU_GROUP);
						setState(167);
						a1_n();
						}
						break;
					}
					} 
				}
				setState(172);
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

	public static class ValueContext extends ParserRuleContext {
		public DimensionItemObjectContext dimensionItemObject() {
			return getRuleContext(DimensionItemObjectContext.class,0);
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
		public NumericLiteralContext numericLiteral() {
			return getRuleContext(NumericLiteralContext.class,0);
		}
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public BooleanLiteralContext booleanLiteral() {
			return getRuleContext(BooleanLiteralContext.class,0);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_value);
		try {
			setState(181);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__7:
			case T__8:
			case T__9:
			case T__10:
				enterOuterAlt(_localctx, 1);
				{
				setState(173);
				dimensionItemObject();
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(174);
				orgUnitCount();
				}
				break;
			case T__12:
				enterOuterAlt(_localctx, 3);
				{
				setState(175);
				reportingRate();
				}
				break;
			case T__14:
				enterOuterAlt(_localctx, 4);
				{
				setState(176);
				constant();
				}
				break;
			case T__15:
				enterOuterAlt(_localctx, 5);
				{
				setState(177);
				days();
				}
				break;
			case NUMERIC_LITERAL:
				enterOuterAlt(_localctx, 6);
				{
				setState(178);
				numericLiteral();
				}
				break;
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 7);
				{
				setState(179);
				stringLiteral();
				}
				break;
			case BOOLEAN_LITERAL:
				enterOuterAlt(_localctx, 8);
				{
				setState(180);
				booleanLiteral();
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
			setState(189);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__3:
				enterOuterAlt(_localctx, 1);
				{
				setState(183);
				match(T__3);
				setState(184);
				programIndicatorVariable();
				setState(185);
				match(T__4);
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 2);
				{
				setState(187);
				match(T__5);
				setState(188);
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
			setState(206);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EVENT_DATE:
				enterOuterAlt(_localctx, 1);
				{
				setState(191);
				((ProgramIndicatorVariableContext)_localctx).var = match(EVENT_DATE);
				}
				break;
			case DUE_DATE:
				enterOuterAlt(_localctx, 2);
				{
				setState(192);
				((ProgramIndicatorVariableContext)_localctx).var = match(DUE_DATE);
				}
				break;
			case INCIDENT_DATE:
				enterOuterAlt(_localctx, 3);
				{
				setState(193);
				((ProgramIndicatorVariableContext)_localctx).var = match(INCIDENT_DATE);
				}
				break;
			case ENROLLMENT_DATE:
				enterOuterAlt(_localctx, 4);
				{
				setState(194);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_DATE);
				}
				break;
			case ENROLLMENT_STATUS:
				enterOuterAlt(_localctx, 5);
				{
				setState(195);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_STATUS);
				}
				break;
			case CURRENT_STATUS:
				enterOuterAlt(_localctx, 6);
				{
				setState(196);
				((ProgramIndicatorVariableContext)_localctx).var = match(CURRENT_STATUS);
				}
				break;
			case VALUE_COUNT:
				enterOuterAlt(_localctx, 7);
				{
				setState(197);
				((ProgramIndicatorVariableContext)_localctx).var = match(VALUE_COUNT);
				}
				break;
			case ZERO_POS_VALUE_COUNT:
				enterOuterAlt(_localctx, 8);
				{
				setState(198);
				((ProgramIndicatorVariableContext)_localctx).var = match(ZERO_POS_VALUE_COUNT);
				}
				break;
			case EVENT_COUNT:
				enterOuterAlt(_localctx, 9);
				{
				setState(199);
				((ProgramIndicatorVariableContext)_localctx).var = match(EVENT_COUNT);
				}
				break;
			case ENROLLMENT_COUNT:
				enterOuterAlt(_localctx, 10);
				{
				setState(200);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_COUNT);
				}
				break;
			case TEI_COUNT:
				enterOuterAlt(_localctx, 11);
				{
				setState(201);
				((ProgramIndicatorVariableContext)_localctx).var = match(TEI_COUNT);
				}
				break;
			case PROGRAM_STAGE_NAME:
				enterOuterAlt(_localctx, 12);
				{
				setState(202);
				((ProgramIndicatorVariableContext)_localctx).var = match(PROGRAM_STAGE_NAME);
				}
				break;
			case PROGRAM_STAGE_ID:
				enterOuterAlt(_localctx, 13);
				{
				setState(203);
				((ProgramIndicatorVariableContext)_localctx).var = match(PROGRAM_STAGE_ID);
				}
				break;
			case REPORTING_PERIOD_START:
				enterOuterAlt(_localctx, 14);
				{
				setState(204);
				((ProgramIndicatorVariableContext)_localctx).var = match(REPORTING_PERIOD_START);
				}
				break;
			case REPORTING_PERIOD_END:
				enterOuterAlt(_localctx, 15);
				{
				setState(205);
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
			setState(228);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HAS_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(208);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(HAS_VALUE);
				setState(209);
				a1();
				}
				break;
			case MINUTES_BETWEEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(210);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(MINUTES_BETWEEN);
				setState(211);
				a2();
				}
				break;
			case DAYS_BETWEEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(212);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(DAYS_BETWEEN);
				setState(213);
				a2();
				}
				break;
			case WEEKS_BETWEEN:
				enterOuterAlt(_localctx, 4);
				{
				setState(214);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(WEEKS_BETWEEN);
				setState(215);
				a2();
				}
				break;
			case MONTHS_BETWEEN:
				enterOuterAlt(_localctx, 5);
				{
				setState(216);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(MONTHS_BETWEEN);
				setState(217);
				a2();
				}
				break;
			case YEARS_BETWEEN:
				enterOuterAlt(_localctx, 6);
				{
				setState(218);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(YEARS_BETWEEN);
				setState(219);
				a2();
				}
				break;
			case CONDITION:
				enterOuterAlt(_localctx, 7);
				{
				setState(220);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(CONDITION);
				setState(221);
				a3();
				}
				break;
			case ZING:
				enterOuterAlt(_localctx, 8);
				{
				setState(222);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(ZING);
				setState(223);
				a1();
				}
				break;
			case OIZP:
				enterOuterAlt(_localctx, 9);
				{
				setState(224);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(OIZP);
				setState(225);
				a1();
				}
				break;
			case ZPVC:
				enterOuterAlt(_localctx, 10);
				{
				setState(226);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(ZPVC);
				setState(227);
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
			setState(230);
			match(T__0);
			setState(231);
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
			setState(233);
			match(T__0);
			setState(235);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__3) | (1L << T__5) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__14) | (1L << T__15) | (1L << MINUS) | (1L << PLUS) | (1L << NOT) | (1L << IF) | (1L << IS_NULL) | (1L << COALESCE))) != 0) || ((((_la - 81)) & ~0x3f) == 0 && ((1L << (_la - 81)) & ((1L << (NUMERIC_LITERAL - 81)) | (1L << (STRING_LITERAL - 81)) | (1L << (BOOLEAN_LITERAL - 81)))) != 0)) {
				{
				setState(234);
				expr(0);
				}
			}

			setState(237);
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
			setState(239);
			match(T__0);
			setState(240);
			expr(0);
			setState(241);
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
			setState(243);
			match(T__0);
			setState(244);
			expr(0);
			setState(247);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(245);
				match(T__6);
				setState(246);
				expr(0);
				}
			}

			setState(249);
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
			setState(251);
			match(T__0);
			setState(252);
			expr(0);
			setState(257);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__6) {
				{
				{
				setState(253);
				match(T__6);
				setState(254);
				expr(0);
				}
				}
				setState(259);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(260);
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
			setState(262);
			match(T__0);
			setState(263);
			expr(0);
			setState(264);
			match(T__6);
			setState(265);
			expr(0);
			setState(266);
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
			setState(268);
			match(T__0);
			setState(269);
			expr(0);
			setState(270);
			match(T__6);
			setState(271);
			expr(0);
			setState(272);
			match(T__6);
			setState(273);
			expr(0);
			setState(274);
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
		enterRule(_localctx, 24, RULE_numericLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(276);
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
		enterRule(_localctx, 26, RULE_stringLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(278);
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
		enterRule(_localctx, 28, RULE_booleanLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
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

	public static class DimensionItemObjectContext extends ParserRuleContext {
		public DataElementOperandContext dataElementOperand() {
			return getRuleContext(DataElementOperandContext.class,0);
		}
		public ProgramDataElementContext programDataElement() {
			return getRuleContext(ProgramDataElementContext.class,0);
		}
		public ProgramTrackedEntityAttributeContext programTrackedEntityAttribute() {
			return getRuleContext(ProgramTrackedEntityAttributeContext.class,0);
		}
		public ProgramIndicatorContext programIndicator() {
			return getRuleContext(ProgramIndicatorContext.class,0);
		}
		public DimensionItemObjectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dimensionItemObject; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDimensionItemObject(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DimensionItemObjectContext dimensionItemObject() throws RecognitionException {
		DimensionItemObjectContext _localctx = new DimensionItemObjectContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_dimensionItemObject);
		try {
			setState(286);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__7:
				enterOuterAlt(_localctx, 1);
				{
				setState(282);
				dataElementOperand();
				}
				break;
			case T__8:
				enterOuterAlt(_localctx, 2);
				{
				setState(283);
				programDataElement();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 3);
				{
				setState(284);
				programTrackedEntityAttribute();
				}
				break;
			case T__10:
				enterOuterAlt(_localctx, 4);
				{
				setState(285);
				programIndicator();
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

	public static class DataElementOperandContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public DataElementOperandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementOperand; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementOperand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementOperandContext dataElementOperand() throws RecognitionException {
		DataElementOperandContext _localctx = new DataElementOperandContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_dataElementOperand);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(288);
			match(T__7);
			setState(289);
			match(UID);
			setState(296);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__2) {
				{
				setState(290);
				match(T__2);
				setState(291);
				match(UID);
				setState(294);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__2) {
					{
					setState(292);
					match(T__2);
					setState(293);
					match(UID);
					}
				}

				}
			}

			setState(298);
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
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
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
		enterRule(_localctx, 34, RULE_programDataElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(300);
			match(T__8);
			setState(301);
			match(UID);
			setState(304);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__2) {
				{
				setState(302);
				match(T__2);
				setState(303);
				match(UID);
				}
			}

			setState(306);
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

	public static class ProgramTrackedEntityAttributeContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public ProgramTrackedEntityAttributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_programTrackedEntityAttribute; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitProgramTrackedEntityAttribute(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramTrackedEntityAttributeContext programTrackedEntityAttribute() throws RecognitionException {
		ProgramTrackedEntityAttributeContext _localctx = new ProgramTrackedEntityAttributeContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_programTrackedEntityAttribute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(308);
			match(T__9);
			setState(309);
			match(UID);
			setState(310);
			match(T__2);
			setState(311);
			match(UID);
			setState(312);
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
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
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
		enterRule(_localctx, 38, RULE_programIndicator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(314);
			match(T__10);
			setState(315);
			match(UID);
			setState(316);
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
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
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
		enterRule(_localctx, 40, RULE_orgUnitCount);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(318);
			match(T__11);
			setState(319);
			match(UID);
			setState(320);
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
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
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
		enterRule(_localctx, 42, RULE_reportingRate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(322);
			match(T__12);
			setState(323);
			match(UID);
			setState(324);
			match(T__13);
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
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
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
		enterRule(_localctx, 44, RULE_constant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(326);
			match(T__14);
			setState(327);
			match(UID);
			setState(328);
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
		enterRule(_localctx, 46, RULE_days);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(330);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 0:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 11);
		case 1:
			return precpred(_ctx, 8);
		case 2:
			return precpred(_ctx, 7);
		case 3:
			return precpred(_ctx, 6);
		case 4:
			return precpred(_ctx, 5);
		case 5:
			return precpred(_ctx, 4);
		case 6:
			return precpred(_ctx, 3);
		case 7:
			return precpred(_ctx, 34);
		case 8:
			return precpred(_ctx, 30);
		case 9:
			return precpred(_ctx, 29);
		case 10:
			return precpred(_ctx, 28);
		case 11:
			return precpred(_ctx, 27);
		case 12:
			return precpred(_ctx, 26);
		case 13:
			return precpred(_ctx, 25);
		case 14:
			return precpred(_ctx, 24);
		case 15:
			return precpred(_ctx, 23);
		case 16:
			return precpred(_ctx, 22);
		case 17:
			return precpred(_ctx, 21);
		case 18:
			return precpred(_ctx, 20);
		case 19:
			return precpred(_ctx, 19);
		case 20:
			return precpred(_ctx, 18);
		case 21:
			return precpred(_ctx, 17);
		case 22:
			return precpred(_ctx, 16);
		case 23:
			return precpred(_ctx, 15);
		case 24:
			return precpred(_ctx, 14);
		case 25:
			return precpred(_ctx, 13);
		case 26:
			return precpred(_ctx, 12);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3W\u014f\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\5"+
		"\2D\n\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3"+
		"\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2"+
		"\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3"+
		"\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2"+
		"\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3"+
		"\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\7\2\u00ab"+
		"\n\2\f\2\16\2\u00ae\13\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3\u00b8\n\3"+
		"\3\4\3\4\3\4\3\4\3\4\3\4\5\4\u00c0\n\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5\u00d1\n\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6\u00e7\n\6\3\7"+
		"\3\7\3\7\3\b\3\b\5\b\u00ee\n\b\3\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n"+
		"\5\n\u00fa\n\n\3\n\3\n\3\13\3\13\3\13\3\13\7\13\u0102\n\13\f\13\16\13"+
		"\u0105\13\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\21\3\21\5\21\u0121"+
		"\n\21\3\22\3\22\3\22\3\22\3\22\3\22\5\22\u0129\n\22\5\22\u012b\n\22\3"+
		"\22\3\22\3\23\3\23\3\23\3\23\5\23\u0133\n\23\3\23\3\23\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\27\3\27\3\27"+
		"\3\27\3\30\3\30\3\30\3\30\3\31\3\31\3\31\2\3\2\32\2\4\6\b\n\f\16\20\22"+
		"\24\26\30\32\34\36 \"$&(*,.\60\2\7\4\2\24\24\26\26\3\2\27\31\3\2\24\25"+
		"\3\2\32\35\3\2\36\37\2\u0180\2C\3\2\2\2\4\u00b7\3\2\2\2\6\u00bf\3\2\2"+
		"\2\b\u00d0\3\2\2\2\n\u00e6\3\2\2\2\f\u00e8\3\2\2\2\16\u00eb\3\2\2\2\20"+
		"\u00f1\3\2\2\2\22\u00f5\3\2\2\2\24\u00fd\3\2\2\2\26\u0108\3\2\2\2\30\u010e"+
		"\3\2\2\2\32\u0116\3\2\2\2\34\u0118\3\2\2\2\36\u011a\3\2\2\2 \u0120\3\2"+
		"\2\2\"\u0122\3\2\2\2$\u012e\3\2\2\2&\u0136\3\2\2\2(\u013c\3\2\2\2*\u0140"+
		"\3\2\2\2,\u0144\3\2\2\2.\u0148\3\2\2\2\60\u014c\3\2\2\2\62\63\b\2\1\2"+
		"\63\64\7\3\2\2\64\65\5\2\2\2\65\66\7\4\2\2\66D\3\2\2\2\678\7\"\2\28D\5"+
		"\30\r\29:\7$\2\2:D\5\20\t\2;<\7%\2\2<D\5\24\13\2=>\t\2\2\2>D\5\2\2\f?"+
		"@\7\25\2\2@D\5\2\2\13AD\5\4\3\2BD\5\6\4\2C\62\3\2\2\2C\67\3\2\2\2C9\3"+
		"\2\2\2C;\3\2\2\2C=\3\2\2\2C?\3\2\2\2CA\3\2\2\2CB\3\2\2\2D\u00ac\3\2\2"+
		"\2EF\f\r\2\2FG\7\23\2\2G\u00ab\5\2\2\rHI\f\n\2\2IJ\t\3\2\2J\u00ab\5\2"+
		"\2\13KL\f\t\2\2LM\t\4\2\2M\u00ab\5\2\2\nNO\f\b\2\2OP\t\5\2\2P\u00ab\5"+
		"\2\2\tQR\f\7\2\2RS\t\6\2\2S\u00ab\5\2\2\bTU\f\6\2\2UV\7 \2\2V\u00ab\5"+
		"\2\2\7WX\f\5\2\2XY\7!\2\2Y\u00ab\5\2\2\6Z[\f$\2\2[\\\7\5\2\2\\]\7#\2\2"+
		"]\u00ab\5\20\t\2^_\f \2\2_`\7\5\2\2`a\7&\2\2a\u00ab\5\f\7\2bc\f\37\2\2"+
		"cd\7\5\2\2de\7.\2\2e\u00ab\5\f\7\2fg\f\36\2\2gh\7\5\2\2hi\7-\2\2i\u00ab"+
		"\5\f\7\2jk\f\35\2\2kl\7\5\2\2lm\7\'\2\2m\u00ab\5\f\7\2no\f\34\2\2op\7"+
		"\5\2\2pq\7+\2\2q\u00ab\5\f\7\2rs\f\33\2\2st\7\5\2\2tu\7,\2\2u\u00ab\5"+
		"\f\7\2vw\f\32\2\2wx\7\5\2\2xy\7/\2\2y\u00ab\5\f\7\2z{\f\31\2\2{|\7\5\2"+
		"\2|}\7(\2\2}\u00ab\5\16\b\2~\177\f\30\2\2\177\u0080\7\5\2\2\u0080\u0081"+
		"\7)\2\2\u0081\u00ab\5\16\b\2\u0082\u0083\f\27\2\2\u0083\u0084\7\5\2\2"+
		"\u0084\u0085\7\60\2\2\u0085\u00ab\5\20\t\2\u0086\u0087\f\26\2\2\u0087"+
		"\u0088\7\5\2\2\u0088\u0089\7\61\2\2\u0089\u00ab\5\20\t\2\u008a\u008b\f"+
		"\25\2\2\u008b\u008c\7\5\2\2\u008c\u008d\7\62\2\2\u008d\u00ab\5\20\t\2"+
		"\u008e\u008f\f\24\2\2\u008f\u0090\7\5\2\2\u0090\u0091\7\63\2\2\u0091\u00ab"+
		"\5\20\t\2\u0092\u0093\f\23\2\2\u0093\u0094\7\5\2\2\u0094\u0095\7\64\2"+
		"\2\u0095\u00ab\5\24\13\2\u0096\u0097\f\22\2\2\u0097\u0098\7\5\2\2\u0098"+
		"\u0099\7\65\2\2\u0099\u00ab\5\22\n\2\u009a\u009b\f\21\2\2\u009b\u009c"+
		"\7\5\2\2\u009c\u009d\7\66\2\2\u009d\u00ab\5\20\t\2\u009e\u009f\f\20\2"+
		"\2\u009f\u00a0\7\5\2\2\u00a0\u00a1\7\67\2\2\u00a1\u00ab\5\22\n\2\u00a2"+
		"\u00a3\f\17\2\2\u00a3\u00a4\7\5\2\2\u00a4\u00a5\78\2\2\u00a5\u00ab\5\22"+
		"\n\2\u00a6\u00a7\f\16\2\2\u00a7\u00a8\7\5\2\2\u00a8\u00a9\79\2\2\u00a9"+
		"\u00ab\5\24\13\2\u00aaE\3\2\2\2\u00aaH\3\2\2\2\u00aaK\3\2\2\2\u00aaN\3"+
		"\2\2\2\u00aaQ\3\2\2\2\u00aaT\3\2\2\2\u00aaW\3\2\2\2\u00aaZ\3\2\2\2\u00aa"+
		"^\3\2\2\2\u00aab\3\2\2\2\u00aaf\3\2\2\2\u00aaj\3\2\2\2\u00aan\3\2\2\2"+
		"\u00aar\3\2\2\2\u00aav\3\2\2\2\u00aaz\3\2\2\2\u00aa~\3\2\2\2\u00aa\u0082"+
		"\3\2\2\2\u00aa\u0086\3\2\2\2\u00aa\u008a\3\2\2\2\u00aa\u008e\3\2\2\2\u00aa"+
		"\u0092\3\2\2\2\u00aa\u0096\3\2\2\2\u00aa\u009a\3\2\2\2\u00aa\u009e\3\2"+
		"\2\2\u00aa\u00a2\3\2\2\2\u00aa\u00a6\3\2\2\2\u00ab\u00ae\3\2\2\2\u00ac"+
		"\u00aa\3\2\2\2\u00ac\u00ad\3\2\2\2\u00ad\3\3\2\2\2\u00ae\u00ac\3\2\2\2"+
		"\u00af\u00b8\5 \21\2\u00b0\u00b8\5*\26\2\u00b1\u00b8\5,\27\2\u00b2\u00b8"+
		"\5.\30\2\u00b3\u00b8\5\60\31\2\u00b4\u00b8\5\32\16\2\u00b5\u00b8\5\34"+
		"\17\2\u00b6\u00b8\5\36\20\2\u00b7\u00af\3\2\2\2\u00b7\u00b0\3\2\2\2\u00b7"+
		"\u00b1\3\2\2\2\u00b7\u00b2\3\2\2\2\u00b7\u00b3\3\2\2\2\u00b7\u00b4\3\2"+
		"\2\2\u00b7\u00b5\3\2\2\2\u00b7\u00b6\3\2\2\2\u00b8\5\3\2\2\2\u00b9\u00ba"+
		"\7\6\2\2\u00ba\u00bb\5\b\5\2\u00bb\u00bc\7\7\2\2\u00bc\u00c0\3\2\2\2\u00bd"+
		"\u00be\7\b\2\2\u00be\u00c0\5\n\6\2\u00bf\u00b9\3\2\2\2\u00bf\u00bd\3\2"+
		"\2\2\u00c0\7\3\2\2\2\u00c1\u00d1\7:\2\2\u00c2\u00d1\7;\2\2\u00c3\u00d1"+
		"\7<\2\2\u00c4\u00d1\7=\2\2\u00c5\u00d1\7>\2\2\u00c6\u00d1\7?\2\2\u00c7"+
		"\u00d1\7@\2\2\u00c8\u00d1\7A\2\2\u00c9\u00d1\7B\2\2\u00ca\u00d1\7C\2\2"+
		"\u00cb\u00d1\7D\2\2\u00cc\u00d1\7E\2\2\u00cd\u00d1\7F\2\2\u00ce\u00d1"+
		"\7G\2\2\u00cf\u00d1\7H\2\2\u00d0\u00c1\3\2\2\2\u00d0\u00c2\3\2\2\2\u00d0"+
		"\u00c3\3\2\2\2\u00d0\u00c4\3\2\2\2\u00d0\u00c5\3\2\2\2\u00d0\u00c6\3\2"+
		"\2\2\u00d0\u00c7\3\2\2\2\u00d0\u00c8\3\2\2\2\u00d0\u00c9\3\2\2\2\u00d0"+
		"\u00ca\3\2\2\2\u00d0\u00cb\3\2\2\2\u00d0\u00cc\3\2\2\2\u00d0\u00cd\3\2"+
		"\2\2\u00d0\u00ce\3\2\2\2\u00d0\u00cf\3\2\2\2\u00d1\t\3\2\2\2\u00d2\u00d3"+
		"\7I\2\2\u00d3\u00e7\5\20\t\2\u00d4\u00d5\7J\2\2\u00d5\u00e7\5\26\f\2\u00d6"+
		"\u00d7\7K\2\2\u00d7\u00e7\5\26\f\2\u00d8\u00d9\7L\2\2\u00d9\u00e7\5\26"+
		"\f\2\u00da\u00db\7M\2\2\u00db\u00e7\5\26\f\2\u00dc\u00dd\7N\2\2\u00dd"+
		"\u00e7\5\26\f\2\u00de\u00df\7O\2\2\u00df\u00e7\5\30\r\2\u00e0\u00e1\7"+
		"P\2\2\u00e1\u00e7\5\20\t\2\u00e2\u00e3\7Q\2\2\u00e3\u00e7\5\20\t\2\u00e4"+
		"\u00e5\7R\2\2\u00e5\u00e7\5\24\13\2\u00e6\u00d2\3\2\2\2\u00e6\u00d4\3"+
		"\2\2\2\u00e6\u00d6\3\2\2\2\u00e6\u00d8\3\2\2\2\u00e6\u00da\3\2\2\2\u00e6"+
		"\u00dc\3\2\2\2\u00e6\u00de\3\2\2\2\u00e6\u00e0\3\2\2\2\u00e6\u00e2\3\2"+
		"\2\2\u00e6\u00e4\3\2\2\2\u00e7\13\3\2\2\2\u00e8\u00e9\7\3\2\2\u00e9\u00ea"+
		"\7\4\2\2\u00ea\r\3\2\2\2\u00eb\u00ed\7\3\2\2\u00ec\u00ee\5\2\2\2\u00ed"+
		"\u00ec\3\2\2\2\u00ed\u00ee\3\2\2\2\u00ee\u00ef\3\2\2\2\u00ef\u00f0\7\4"+
		"\2\2\u00f0\17\3\2\2\2\u00f1\u00f2\7\3\2\2\u00f2\u00f3\5\2\2\2\u00f3\u00f4"+
		"\7\4\2\2\u00f4\21\3\2\2\2\u00f5\u00f6\7\3\2\2\u00f6\u00f9\5\2\2\2\u00f7"+
		"\u00f8\7\t\2\2\u00f8\u00fa\5\2\2\2\u00f9\u00f7\3\2\2\2\u00f9\u00fa\3\2"+
		"\2\2\u00fa\u00fb\3\2\2\2\u00fb\u00fc\7\4\2\2\u00fc\23\3\2\2\2\u00fd\u00fe"+
		"\7\3\2\2\u00fe\u0103\5\2\2\2\u00ff\u0100\7\t\2\2\u0100\u0102\5\2\2\2\u0101"+
		"\u00ff\3\2\2\2\u0102\u0105\3\2\2\2\u0103\u0101\3\2\2\2\u0103\u0104\3\2"+
		"\2\2\u0104\u0106\3\2\2\2\u0105\u0103\3\2\2\2\u0106\u0107\7\4\2\2\u0107"+
		"\25\3\2\2\2\u0108\u0109\7\3\2\2\u0109\u010a\5\2\2\2\u010a\u010b\7\t\2"+
		"\2\u010b\u010c\5\2\2\2\u010c\u010d\7\4\2\2\u010d\27\3\2\2\2\u010e\u010f"+
		"\7\3\2\2\u010f\u0110\5\2\2\2\u0110\u0111\7\t\2\2\u0111\u0112\5\2\2\2\u0112"+
		"\u0113\7\t\2\2\u0113\u0114\5\2\2\2\u0114\u0115\7\4\2\2\u0115\31\3\2\2"+
		"\2\u0116\u0117\7S\2\2\u0117\33\3\2\2\2\u0118\u0119\7T\2\2\u0119\35\3\2"+
		"\2\2\u011a\u011b\7U\2\2\u011b\37\3\2\2\2\u011c\u0121\5\"\22\2\u011d\u0121"+
		"\5$\23\2\u011e\u0121\5&\24\2\u011f\u0121\5(\25\2\u0120\u011c\3\2\2\2\u0120"+
		"\u011d\3\2\2\2\u0120\u011e\3\2\2\2\u0120\u011f\3\2\2\2\u0121!\3\2\2\2"+
		"\u0122\u0123\7\n\2\2\u0123\u012a\7V\2\2\u0124\u0125\7\5\2\2\u0125\u0128"+
		"\7V\2\2\u0126\u0127\7\5\2\2\u0127\u0129\7V\2\2\u0128\u0126\3\2\2\2\u0128"+
		"\u0129\3\2\2\2\u0129\u012b\3\2\2\2\u012a\u0124\3\2\2\2\u012a\u012b\3\2"+
		"\2\2\u012b\u012c\3\2\2\2\u012c\u012d\7\7\2\2\u012d#\3\2\2\2\u012e\u012f"+
		"\7\13\2\2\u012f\u0132\7V\2\2\u0130\u0131\7\5\2\2\u0131\u0133\7V\2\2\u0132"+
		"\u0130\3\2\2\2\u0132\u0133\3\2\2\2\u0133\u0134\3\2\2\2\u0134\u0135\7\7"+
		"\2\2\u0135%\3\2\2\2\u0136\u0137\7\f\2\2\u0137\u0138\7V\2\2\u0138\u0139"+
		"\7\5\2\2\u0139\u013a\7V\2\2\u013a\u013b\7\7\2\2\u013b\'\3\2\2\2\u013c"+
		"\u013d\7\r\2\2\u013d\u013e\7V\2\2\u013e\u013f\7\7\2\2\u013f)\3\2\2\2\u0140"+
		"\u0141\7\16\2\2\u0141\u0142\7V\2\2\u0142\u0143\7\7\2\2\u0143+\3\2\2\2"+
		"\u0144\u0145\7\17\2\2\u0145\u0146\7V\2\2\u0146\u0147\7\20\2\2\u0147-\3"+
		"\2\2\2\u0148\u0149\7\21\2\2\u0149\u014a\7V\2\2\u014a\u014b\7\7\2\2\u014b"+
		"/\3\2\2\2\u014c\u014d\7\22\2\2\u014d\61\3\2\2\2\20C\u00aa\u00ac\u00b7"+
		"\u00bf\u00d0\u00e6\u00ed\u00f9\u0103\u0120\u0128\u012a\u0132";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}