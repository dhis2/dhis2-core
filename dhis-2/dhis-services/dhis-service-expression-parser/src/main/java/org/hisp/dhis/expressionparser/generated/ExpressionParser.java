// Generated from Expression.g4 by ANTLR 4.7.2
package org.hisp.dhis.expressionparser.generated;
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
		RULE_keyword = 34;
	private static String[] makeRuleNames() {
		return new String[] {
			"expression", "expr", "programIndicatorExpr", "programIndicatorVariable", 
			"programIndicatorFunction", "a0", "a0_1", "a1", "a1_2", "a1_n", "a2", 
			"a3", "dataElement", "dataElementOperandWithoutAoc", "dataElementOperandWithAoc", 
			"programDataElement", "programAttribute", "programIndicator", "orgUnitCount", 
			"reportingRate", "constant", "days", "dataElementId", "dataElementOperandIdWithoutAoc", 
			"dataElementOperandIdWithAoc", "programDataElementId", "programAttributeId", 
			"programIndicatorId", "orgUnitCountId", "reportingRateId", "constantId", 
			"numericLiteral", "stringLiteral", "booleanLiteral", "keyword"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'V{'", "'}'", "'d2:'", "','", "'#{'", "'.*'", "'D{'", 
			"'A{'", "'I{'", "'OUG{'", "'R{'", "'C{'", "'[days]'", "'.'", "'.*.'", 
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
		public TerminalNode IF() { return getToken(ExpressionParser.IF, 0); }
		public A1Context a1() {
			return getRuleContext(A1Context.class,0);
		}
		public TerminalNode IS_NULL() { return getToken(ExpressionParser.IS_NULL, 0); }
		public A1_nContext a1_n() {
			return getRuleContext(A1_nContext.class,0);
		}
		public TerminalNode COALESCE() { return getToken(ExpressionParser.COALESCE, 0); }
		public TerminalNode MAXIMUM() { return getToken(ExpressionParser.MAXIMUM, 0); }
		public TerminalNode MINIMUM() { return getToken(ExpressionParser.MINIMUM, 0); }
		public TerminalNode MINUS() { return getToken(ExpressionParser.MINUS, 0); }
		public TerminalNode NOT() { return getToken(ExpressionParser.NOT, 0); }
		public TerminalNode PLUS() { return getToken(ExpressionParser.PLUS, 0); }
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
		public TerminalNode POWER() { return getToken(ExpressionParser.POWER, 0); }
		public TerminalNode MUL() { return getToken(ExpressionParser.MUL, 0); }
		public TerminalNode DIV() { return getToken(ExpressionParser.DIV, 0); }
		public TerminalNode MOD() { return getToken(ExpressionParser.MOD, 0); }
		public TerminalNode LT() { return getToken(ExpressionParser.LT, 0); }
		public TerminalNode GT() { return getToken(ExpressionParser.GT, 0); }
		public TerminalNode LEQ() { return getToken(ExpressionParser.LEQ, 0); }
		public TerminalNode GEQ() { return getToken(ExpressionParser.GEQ, 0); }
		public TerminalNode EQ() { return getToken(ExpressionParser.EQ, 0); }
		public TerminalNode NE() { return getToken(ExpressionParser.NE, 0); }
		public TerminalNode AND() { return getToken(ExpressionParser.AND, 0); }
		public TerminalNode OR() { return getToken(ExpressionParser.OR, 0); }
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
			setState(131);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(129);
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
					}
					} 
				}
				setState(133);
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
			setState(140);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__2:
				enterOuterAlt(_localctx, 1);
				{
				setState(134);
				match(T__2);
				setState(135);
				programIndicatorVariable();
				setState(136);
				match(T__3);
				}
				break;
			case T__4:
				enterOuterAlt(_localctx, 2);
				{
				setState(138);
				match(T__4);
				setState(139);
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
		public TerminalNode EVENT_DATE() { return getToken(ExpressionParser.EVENT_DATE, 0); }
		public TerminalNode DUE_DATE() { return getToken(ExpressionParser.DUE_DATE, 0); }
		public TerminalNode INCIDENT_DATE() { return getToken(ExpressionParser.INCIDENT_DATE, 0); }
		public TerminalNode ENROLLMENT_DATE() { return getToken(ExpressionParser.ENROLLMENT_DATE, 0); }
		public TerminalNode ENROLLMENT_STATUS() { return getToken(ExpressionParser.ENROLLMENT_STATUS, 0); }
		public TerminalNode CURRENT_STATUS() { return getToken(ExpressionParser.CURRENT_STATUS, 0); }
		public TerminalNode VALUE_COUNT() { return getToken(ExpressionParser.VALUE_COUNT, 0); }
		public TerminalNode ZERO_POS_VALUE_COUNT() { return getToken(ExpressionParser.ZERO_POS_VALUE_COUNT, 0); }
		public TerminalNode EVENT_COUNT() { return getToken(ExpressionParser.EVENT_COUNT, 0); }
		public TerminalNode ENROLLMENT_COUNT() { return getToken(ExpressionParser.ENROLLMENT_COUNT, 0); }
		public TerminalNode TEI_COUNT() { return getToken(ExpressionParser.TEI_COUNT, 0); }
		public TerminalNode PROGRAM_STAGE_NAME() { return getToken(ExpressionParser.PROGRAM_STAGE_NAME, 0); }
		public TerminalNode PROGRAM_STAGE_ID() { return getToken(ExpressionParser.PROGRAM_STAGE_ID, 0); }
		public TerminalNode REPORTING_PERIOD_START() { return getToken(ExpressionParser.REPORTING_PERIOD_START, 0); }
		public TerminalNode REPORTING_PERIOD_END() { return getToken(ExpressionParser.REPORTING_PERIOD_END, 0); }
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
			setState(157);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EVENT_DATE:
				enterOuterAlt(_localctx, 1);
				{
				setState(142);
				((ProgramIndicatorVariableContext)_localctx).var = match(EVENT_DATE);
				}
				break;
			case DUE_DATE:
				enterOuterAlt(_localctx, 2);
				{
				setState(143);
				((ProgramIndicatorVariableContext)_localctx).var = match(DUE_DATE);
				}
				break;
			case INCIDENT_DATE:
				enterOuterAlt(_localctx, 3);
				{
				setState(144);
				((ProgramIndicatorVariableContext)_localctx).var = match(INCIDENT_DATE);
				}
				break;
			case ENROLLMENT_DATE:
				enterOuterAlt(_localctx, 4);
				{
				setState(145);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_DATE);
				}
				break;
			case ENROLLMENT_STATUS:
				enterOuterAlt(_localctx, 5);
				{
				setState(146);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_STATUS);
				}
				break;
			case CURRENT_STATUS:
				enterOuterAlt(_localctx, 6);
				{
				setState(147);
				((ProgramIndicatorVariableContext)_localctx).var = match(CURRENT_STATUS);
				}
				break;
			case VALUE_COUNT:
				enterOuterAlt(_localctx, 7);
				{
				setState(148);
				((ProgramIndicatorVariableContext)_localctx).var = match(VALUE_COUNT);
				}
				break;
			case ZERO_POS_VALUE_COUNT:
				enterOuterAlt(_localctx, 8);
				{
				setState(149);
				((ProgramIndicatorVariableContext)_localctx).var = match(ZERO_POS_VALUE_COUNT);
				}
				break;
			case EVENT_COUNT:
				enterOuterAlt(_localctx, 9);
				{
				setState(150);
				((ProgramIndicatorVariableContext)_localctx).var = match(EVENT_COUNT);
				}
				break;
			case ENROLLMENT_COUNT:
				enterOuterAlt(_localctx, 10);
				{
				setState(151);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_COUNT);
				}
				break;
			case TEI_COUNT:
				enterOuterAlt(_localctx, 11);
				{
				setState(152);
				((ProgramIndicatorVariableContext)_localctx).var = match(TEI_COUNT);
				}
				break;
			case PROGRAM_STAGE_NAME:
				enterOuterAlt(_localctx, 12);
				{
				setState(153);
				((ProgramIndicatorVariableContext)_localctx).var = match(PROGRAM_STAGE_NAME);
				}
				break;
			case PROGRAM_STAGE_ID:
				enterOuterAlt(_localctx, 13);
				{
				setState(154);
				((ProgramIndicatorVariableContext)_localctx).var = match(PROGRAM_STAGE_ID);
				}
				break;
			case REPORTING_PERIOD_START:
				enterOuterAlt(_localctx, 14);
				{
				setState(155);
				((ProgramIndicatorVariableContext)_localctx).var = match(REPORTING_PERIOD_START);
				}
				break;
			case REPORTING_PERIOD_END:
				enterOuterAlt(_localctx, 15);
				{
				setState(156);
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
		public TerminalNode HAS_VALUE() { return getToken(ExpressionParser.HAS_VALUE, 0); }
		public A2Context a2() {
			return getRuleContext(A2Context.class,0);
		}
		public TerminalNode MINUTES_BETWEEN() { return getToken(ExpressionParser.MINUTES_BETWEEN, 0); }
		public TerminalNode DAYS_BETWEEN() { return getToken(ExpressionParser.DAYS_BETWEEN, 0); }
		public TerminalNode WEEKS_BETWEEN() { return getToken(ExpressionParser.WEEKS_BETWEEN, 0); }
		public TerminalNode MONTHS_BETWEEN() { return getToken(ExpressionParser.MONTHS_BETWEEN, 0); }
		public TerminalNode YEARS_BETWEEN() { return getToken(ExpressionParser.YEARS_BETWEEN, 0); }
		public A3Context a3() {
			return getRuleContext(A3Context.class,0);
		}
		public TerminalNode CONDITION() { return getToken(ExpressionParser.CONDITION, 0); }
		public TerminalNode ZING() { return getToken(ExpressionParser.ZING, 0); }
		public TerminalNode OIZP() { return getToken(ExpressionParser.OIZP, 0); }
		public A1_nContext a1_n() {
			return getRuleContext(A1_nContext.class,0);
		}
		public TerminalNode ZPVC() { return getToken(ExpressionParser.ZPVC, 0); }
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
			setState(179);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HAS_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(159);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(HAS_VALUE);
				setState(160);
				a1();
				}
				break;
			case MINUTES_BETWEEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(161);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(MINUTES_BETWEEN);
				setState(162);
				a2();
				}
				break;
			case DAYS_BETWEEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(163);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(DAYS_BETWEEN);
				setState(164);
				a2();
				}
				break;
			case WEEKS_BETWEEN:
				enterOuterAlt(_localctx, 4);
				{
				setState(165);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(WEEKS_BETWEEN);
				setState(166);
				a2();
				}
				break;
			case MONTHS_BETWEEN:
				enterOuterAlt(_localctx, 5);
				{
				setState(167);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(MONTHS_BETWEEN);
				setState(168);
				a2();
				}
				break;
			case YEARS_BETWEEN:
				enterOuterAlt(_localctx, 6);
				{
				setState(169);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(YEARS_BETWEEN);
				setState(170);
				a2();
				}
				break;
			case CONDITION:
				enterOuterAlt(_localctx, 7);
				{
				setState(171);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(CONDITION);
				setState(172);
				a3();
				}
				break;
			case ZING:
				enterOuterAlt(_localctx, 8);
				{
				setState(173);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(ZING);
				setState(174);
				a1();
				}
				break;
			case OIZP:
				enterOuterAlt(_localctx, 9);
				{
				setState(175);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(OIZP);
				setState(176);
				a1();
				}
				break;
			case ZPVC:
				enterOuterAlt(_localctx, 10);
				{
				setState(177);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(ZPVC);
				setState(178);
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
			setState(181);
			match(T__0);
			setState(182);
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
			setState(184);
			match(T__0);
			setState(186);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__2) | (1L << T__4) | (1L << T__6) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << MINUS) | (1L << PLUS) | (1L << NOT) | (1L << IF) | (1L << IS_NULL) | (1L << COALESCE) | (1L << MAXIMUM) | (1L << MINIMUM) | (1L << NUMERIC_LITERAL))) != 0) || _la==STRING_LITERAL || _la==BOOLEAN_LITERAL) {
				{
				setState(185);
				expr(0);
				}
			}

			setState(188);
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
			setState(190);
			match(T__0);
			setState(191);
			expr(0);
			setState(192);
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
			setState(194);
			match(T__0);
			setState(195);
			expr(0);
			setState(198);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(196);
				match(T__5);
				setState(197);
				expr(0);
				}
			}

			setState(200);
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
			setState(202);
			match(T__0);
			setState(203);
			expr(0);
			setState(208);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__5) {
				{
				{
				setState(204);
				match(T__5);
				setState(205);
				expr(0);
				}
				}
				setState(210);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(211);
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
			setState(213);
			match(T__0);
			setState(214);
			expr(0);
			setState(215);
			match(T__5);
			setState(216);
			expr(0);
			setState(217);
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
			setState(219);
			match(T__0);
			setState(220);
			expr(0);
			setState(221);
			match(T__5);
			setState(222);
			expr(0);
			setState(223);
			match(T__5);
			setState(224);
			expr(0);
			setState(225);
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
			setState(227);
			match(T__6);
			setState(228);
			dataElementId();
			setState(229);
			match(T__3);
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
			setState(231);
			match(T__6);
			setState(232);
			dataElementOperandIdWithoutAoc();
			setState(234);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__7) {
				{
				setState(233);
				match(T__7);
				}
			}

			setState(236);
			match(T__3);
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
			setState(238);
			match(T__6);
			setState(239);
			dataElementOperandIdWithAoc();
			setState(240);
			match(T__3);
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
			setState(242);
			match(T__8);
			setState(243);
			programDataElementId();
			setState(244);
			match(T__3);
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
			setState(246);
			match(T__9);
			setState(247);
			programAttributeId();
			setState(248);
			match(T__3);
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
			setState(250);
			match(T__10);
			setState(251);
			programIndicatorId();
			setState(252);
			match(T__3);
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
			setState(254);
			match(T__11);
			setState(255);
			orgUnitCountId();
			setState(256);
			match(T__3);
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
			setState(258);
			match(T__12);
			setState(259);
			reportingRateId();
			setState(260);
			match(T__3);
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
			setState(262);
			match(T__13);
			setState(263);
			constantId();
			setState(264);
			match(T__3);
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
			setState(266);
			match(T__14);
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
			setState(268);
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
			setState(270);
			match(UID);
			setState(271);
			match(T__15);
			setState(272);
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
			setState(274);
			match(UID);
			setState(279);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__15:
				{
				setState(275);
				match(T__15);
				setState(276);
				match(UID);
				setState(277);
				match(T__15);
				}
				break;
			case T__16:
				{
				setState(278);
				match(T__16);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(281);
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
			setState(283);
			match(UID);
			setState(284);
			match(T__15);
			setState(285);
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
			setState(287);
			match(UID);
			setState(288);
			match(T__15);
			setState(289);
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
			setState(291);
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
			setState(293);
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
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
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
			setState(295);
			match(UID);
			setState(296);
			match(T__15);
			setState(297);
			keyword();
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
			setState(299);
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
			setState(301);
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
			setState(303);
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
			setState(305);
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

	public static class KeywordContext extends ParserRuleContext {
		public TerminalNode KEYWORD() { return getToken(ExpressionParser.KEYWORD, 0); }
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
		public KeywordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyword; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitKeyword(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeywordContext keyword() throws RecognitionException {
		KeywordContext _localctx = new KeywordContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_keyword);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(307);
			_la = _input.LA(1);
			if ( !(_la==UID || _la==KEYWORD) ) {
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
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3F\u0138\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3m\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3\u0084\n\3\f\3"+
		"\16\3\u0087\13\3\3\4\3\4\3\4\3\4\3\4\3\4\5\4\u008f\n\4\3\5\3\5\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5\u00a0\n\5\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5"+
		"\6\u00b6\n\6\3\7\3\7\3\7\3\b\3\b\5\b\u00bd\n\b\3\b\3\b\3\t\3\t\3\t\3\t"+
		"\3\n\3\n\3\n\3\n\5\n\u00c9\n\n\3\n\3\n\3\13\3\13\3\13\3\13\7\13\u00d1"+
		"\n\13\f\13\16\13\u00d4\13\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\5\17\u00ed"+
		"\n\17\3\17\3\17\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22\3\22"+
		"\3\22\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\26"+
		"\3\26\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\31\3\31\3\32\3\32\3\32"+
		"\3\32\3\32\5\32\u011a\n\32\3\32\3\32\3\33\3\33\3\33\3\33\3\34\3\34\3\34"+
		"\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3"+
		"#\3$\3$\3$\2\3\4%\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62"+
		"\64\668:<>@BDF\2\b\4\2\25\25\27\27\3\2\30\32\3\2\25\26\3\2\33\36\3\2\37"+
		" \3\2DE\2\u014d\2H\3\2\2\2\4l\3\2\2\2\6\u008e\3\2\2\2\b\u009f\3\2\2\2"+
		"\n\u00b5\3\2\2\2\f\u00b7\3\2\2\2\16\u00ba\3\2\2\2\20\u00c0\3\2\2\2\22"+
		"\u00c4\3\2\2\2\24\u00cc\3\2\2\2\26\u00d7\3\2\2\2\30\u00dd\3\2\2\2\32\u00e5"+
		"\3\2\2\2\34\u00e9\3\2\2\2\36\u00f0\3\2\2\2 \u00f4\3\2\2\2\"\u00f8\3\2"+
		"\2\2$\u00fc\3\2\2\2&\u0100\3\2\2\2(\u0104\3\2\2\2*\u0108\3\2\2\2,\u010c"+
		"\3\2\2\2.\u010e\3\2\2\2\60\u0110\3\2\2\2\62\u0114\3\2\2\2\64\u011d\3\2"+
		"\2\2\66\u0121\3\2\2\28\u0125\3\2\2\2:\u0127\3\2\2\2<\u0129\3\2\2\2>\u012d"+
		"\3\2\2\2@\u012f\3\2\2\2B\u0131\3\2\2\2D\u0133\3\2\2\2F\u0135\3\2\2\2H"+
		"I\5\4\3\2IJ\7\2\2\3J\3\3\2\2\2KL\b\3\1\2LM\7\3\2\2MN\5\4\3\2NO\7\4\2\2"+
		"Om\3\2\2\2PQ\7#\2\2Qm\5\30\r\2RS\7$\2\2Sm\5\20\t\2TU\7%\2\2Um\5\24\13"+
		"\2VW\7&\2\2Wm\5\24\13\2XY\7\'\2\2Ym\5\24\13\2Z[\t\2\2\2[m\5\4\3\30\\]"+
		"\7\26\2\2]m\5\4\3\27^m\5\32\16\2_m\5\34\17\2`m\5\36\20\2am\5 \21\2bm\5"+
		"\"\22\2cm\5$\23\2dm\5&\24\2em\5(\25\2fm\5*\26\2gm\5,\27\2hm\5\6\4\2im"+
		"\5@!\2jm\5B\"\2km\5D#\2lK\3\2\2\2lP\3\2\2\2lR\3\2\2\2lT\3\2\2\2lV\3\2"+
		"\2\2lX\3\2\2\2lZ\3\2\2\2l\\\3\2\2\2l^\3\2\2\2l_\3\2\2\2l`\3\2\2\2la\3"+
		"\2\2\2lb\3\2\2\2lc\3\2\2\2ld\3\2\2\2le\3\2\2\2lf\3\2\2\2lg\3\2\2\2lh\3"+
		"\2\2\2li\3\2\2\2lj\3\2\2\2lk\3\2\2\2m\u0085\3\2\2\2no\f\31\2\2op\7\24"+
		"\2\2p\u0084\5\4\3\31qr\f\26\2\2rs\t\3\2\2s\u0084\5\4\3\27tu\f\25\2\2u"+
		"v\t\4\2\2v\u0084\5\4\3\26wx\f\24\2\2xy\t\5\2\2y\u0084\5\4\3\25z{\f\23"+
		"\2\2{|\t\6\2\2|\u0084\5\4\3\24}~\f\22\2\2~\177\7!\2\2\177\u0084\5\4\3"+
		"\23\u0080\u0081\f\21\2\2\u0081\u0082\7\"\2\2\u0082\u0084\5\4\3\22\u0083"+
		"n\3\2\2\2\u0083q\3\2\2\2\u0083t\3\2\2\2\u0083w\3\2\2\2\u0083z\3\2\2\2"+
		"\u0083}\3\2\2\2\u0083\u0080\3\2\2\2\u0084\u0087\3\2\2\2\u0085\u0083\3"+
		"\2\2\2\u0085\u0086\3\2\2\2\u0086\5\3\2\2\2\u0087\u0085\3\2\2\2\u0088\u0089"+
		"\7\5\2\2\u0089\u008a\5\b\5\2\u008a\u008b\7\6\2\2\u008b\u008f\3\2\2\2\u008c"+
		"\u008d\7\7\2\2\u008d\u008f\5\n\6\2\u008e\u0088\3\2\2\2\u008e\u008c\3\2"+
		"\2\2\u008f\7\3\2\2\2\u0090\u00a0\7(\2\2\u0091\u00a0\7)\2\2\u0092\u00a0"+
		"\7*\2\2\u0093\u00a0\7+\2\2\u0094\u00a0\7,\2\2\u0095\u00a0\7-\2\2\u0096"+
		"\u00a0\7.\2\2\u0097\u00a0\7/\2\2\u0098\u00a0\7\60\2\2\u0099\u00a0\7\61"+
		"\2\2\u009a\u00a0\7\62\2\2\u009b\u00a0\7\63\2\2\u009c\u00a0\7\64\2\2\u009d"+
		"\u00a0\7\65\2\2\u009e\u00a0\7\66\2\2\u009f\u0090\3\2\2\2\u009f\u0091\3"+
		"\2\2\2\u009f\u0092\3\2\2\2\u009f\u0093\3\2\2\2\u009f\u0094\3\2\2\2\u009f"+
		"\u0095\3\2\2\2\u009f\u0096\3\2\2\2\u009f\u0097\3\2\2\2\u009f\u0098\3\2"+
		"\2\2\u009f\u0099\3\2\2\2\u009f\u009a\3\2\2\2\u009f\u009b\3\2\2\2\u009f"+
		"\u009c\3\2\2\2\u009f\u009d\3\2\2\2\u009f\u009e\3\2\2\2\u00a0\t\3\2\2\2"+
		"\u00a1\u00a2\7\67\2\2\u00a2\u00b6\5\20\t\2\u00a3\u00a4\78\2\2\u00a4\u00b6"+
		"\5\26\f\2\u00a5\u00a6\79\2\2\u00a6\u00b6\5\26\f\2\u00a7\u00a8\7:\2\2\u00a8"+
		"\u00b6\5\26\f\2\u00a9\u00aa\7;\2\2\u00aa\u00b6\5\26\f\2\u00ab\u00ac\7"+
		"<\2\2\u00ac\u00b6\5\26\f\2\u00ad\u00ae\7=\2\2\u00ae\u00b6\5\30\r\2\u00af"+
		"\u00b0\7>\2\2\u00b0\u00b6\5\20\t\2\u00b1\u00b2\7?\2\2\u00b2\u00b6\5\20"+
		"\t\2\u00b3\u00b4\7@\2\2\u00b4\u00b6\5\24\13\2\u00b5\u00a1\3\2\2\2\u00b5"+
		"\u00a3\3\2\2\2\u00b5\u00a5\3\2\2\2\u00b5\u00a7\3\2\2\2\u00b5\u00a9\3\2"+
		"\2\2\u00b5\u00ab\3\2\2\2\u00b5\u00ad\3\2\2\2\u00b5\u00af\3\2\2\2\u00b5"+
		"\u00b1\3\2\2\2\u00b5\u00b3\3\2\2\2\u00b6\13\3\2\2\2\u00b7\u00b8\7\3\2"+
		"\2\u00b8\u00b9\7\4\2\2\u00b9\r\3\2\2\2\u00ba\u00bc\7\3\2\2\u00bb\u00bd"+
		"\5\4\3\2\u00bc\u00bb\3\2\2\2\u00bc\u00bd\3\2\2\2\u00bd\u00be\3\2\2\2\u00be"+
		"\u00bf\7\4\2\2\u00bf\17\3\2\2\2\u00c0\u00c1\7\3\2\2\u00c1\u00c2\5\4\3"+
		"\2\u00c2\u00c3\7\4\2\2\u00c3\21\3\2\2\2\u00c4\u00c5\7\3\2\2\u00c5\u00c8"+
		"\5\4\3\2\u00c6\u00c7\7\b\2\2\u00c7\u00c9\5\4\3\2\u00c8\u00c6\3\2\2\2\u00c8"+
		"\u00c9\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca\u00cb\7\4\2\2\u00cb\23\3\2\2"+
		"\2\u00cc\u00cd\7\3\2\2\u00cd\u00d2\5\4\3\2\u00ce\u00cf\7\b\2\2\u00cf\u00d1"+
		"\5\4\3\2\u00d0\u00ce\3\2\2\2\u00d1\u00d4\3\2\2\2\u00d2\u00d0\3\2\2\2\u00d2"+
		"\u00d3\3\2\2\2\u00d3\u00d5\3\2\2\2\u00d4\u00d2\3\2\2\2\u00d5\u00d6\7\4"+
		"\2\2\u00d6\25\3\2\2\2\u00d7\u00d8\7\3\2\2\u00d8\u00d9\5\4\3\2\u00d9\u00da"+
		"\7\b\2\2\u00da\u00db\5\4\3\2\u00db\u00dc\7\4\2\2\u00dc\27\3\2\2\2\u00dd"+
		"\u00de\7\3\2\2\u00de\u00df\5\4\3\2\u00df\u00e0\7\b\2\2\u00e0\u00e1\5\4"+
		"\3\2\u00e1\u00e2\7\b\2\2\u00e2\u00e3\5\4\3\2\u00e3\u00e4\7\4\2\2\u00e4"+
		"\31\3\2\2\2\u00e5\u00e6\7\t\2\2\u00e6\u00e7\5.\30\2\u00e7\u00e8\7\6\2"+
		"\2\u00e8\33\3\2\2\2\u00e9\u00ea\7\t\2\2\u00ea\u00ec\5\60\31\2\u00eb\u00ed"+
		"\7\n\2\2\u00ec\u00eb\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\u00ee\3\2\2\2\u00ee"+
		"\u00ef\7\6\2\2\u00ef\35\3\2\2\2\u00f0\u00f1\7\t\2\2\u00f1\u00f2\5\62\32"+
		"\2\u00f2\u00f3\7\6\2\2\u00f3\37\3\2\2\2\u00f4\u00f5\7\13\2\2\u00f5\u00f6"+
		"\5\64\33\2\u00f6\u00f7\7\6\2\2\u00f7!\3\2\2\2\u00f8\u00f9\7\f\2\2\u00f9"+
		"\u00fa\5\66\34\2\u00fa\u00fb\7\6\2\2\u00fb#\3\2\2\2\u00fc\u00fd\7\r\2"+
		"\2\u00fd\u00fe\58\35\2\u00fe\u00ff\7\6\2\2\u00ff%\3\2\2\2\u0100\u0101"+
		"\7\16\2\2\u0101\u0102\5:\36\2\u0102\u0103\7\6\2\2\u0103\'\3\2\2\2\u0104"+
		"\u0105\7\17\2\2\u0105\u0106\5<\37\2\u0106\u0107\7\6\2\2\u0107)\3\2\2\2"+
		"\u0108\u0109\7\20\2\2\u0109\u010a\5> \2\u010a\u010b\7\6\2\2\u010b+\3\2"+
		"\2\2\u010c\u010d\7\21\2\2\u010d-\3\2\2\2\u010e\u010f\7D\2\2\u010f/\3\2"+
		"\2\2\u0110\u0111\7D\2\2\u0111\u0112\7\22\2\2\u0112\u0113\7D\2\2\u0113"+
		"\61\3\2\2\2\u0114\u0119\7D\2\2\u0115\u0116\7\22\2\2\u0116\u0117\7D\2\2"+
		"\u0117\u011a\7\22\2\2\u0118\u011a\7\23\2\2\u0119\u0115\3\2\2\2\u0119\u0118"+
		"\3\2\2\2\u011a\u011b\3\2\2\2\u011b\u011c\7D\2\2\u011c\63\3\2\2\2\u011d"+
		"\u011e\7D\2\2\u011e\u011f\7\22\2\2\u011f\u0120\7D\2\2\u0120\65\3\2\2\2"+
		"\u0121\u0122\7D\2\2\u0122\u0123\7\22\2\2\u0123\u0124\7D\2\2\u0124\67\3"+
		"\2\2\2\u0125\u0126\7D\2\2\u01269\3\2\2\2\u0127\u0128\7D\2\2\u0128;\3\2"+
		"\2\2\u0129\u012a\7D\2\2\u012a\u012b\7\22\2\2\u012b\u012c\5F$\2\u012c="+
		"\3\2\2\2\u012d\u012e\7D\2\2\u012e?\3\2\2\2\u012f\u0130\7A\2\2\u0130A\3"+
		"\2\2\2\u0131\u0132\7B\2\2\u0132C\3\2\2\2\u0133\u0134\7C\2\2\u0134E\3\2"+
		"\2\2\u0135\u0136\t\7\2\2\u0136G\3\2\2\2\rl\u0083\u0085\u008e\u009f\u00b5"+
		"\u00bc\u00c8\u00d2\u00ec\u0119";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}