// Generated from /Users/jim/dev/dhis2/dhis2-core/dhis-2/dhis-services/dhis-service-expression-parser/src/main/resources/org/hisp/dhis/expressionparser/Expression.g4 by ANTLR 4.7.2
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
		RULE_dataElementOperandCoc = 13, RULE_dataElementOperandAoc = 14, RULE_dataElementOperandCocAndAoc = 15, 
		RULE_programDataElement = 16, RULE_programAttribute = 17, RULE_programIndicator = 18, 
		RULE_orgUnitCount = 19, RULE_reportingRate = 20, RULE_constant = 21, RULE_days = 22, 
		RULE_numericLiteral = 23, RULE_stringLiteral = 24, RULE_booleanLiteral = 25, 
		RULE_keyword = 26;
	private static String[] makeRuleNames() {
		return new String[] {
			"expression", "expr", "programIndicatorExpr", "programIndicatorVariable", 
			"programIndicatorFunction", "a0", "a0_1", "a1", "a1_2", "a1_n", "a2", 
			"a3", "dataElement", "dataElementOperandCoc", "dataElementOperandAoc", 
			"dataElementOperandCocAndAoc", "programDataElement", "programAttribute", 
			"programIndicator", "orgUnitCount", "reportingRate", "constant", "days", 
			"numericLiteral", "stringLiteral", "booleanLiteral", "keyword"
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
			setState(54);
			expr(0);
			setState(55);
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
		public DataElementOperandCocContext dataElementOperandCoc() {
			return getRuleContext(DataElementOperandCocContext.class,0);
		}
		public DataElementOperandAocContext dataElementOperandAoc() {
			return getRuleContext(DataElementOperandAocContext.class,0);
		}
		public DataElementOperandCocAndAocContext dataElementOperandCocAndAoc() {
			return getRuleContext(DataElementOperandCocAndAocContext.class,0);
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
			setState(91);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				{
				setState(58);
				match(T__0);
				setState(59);
				expr(0);
				setState(60);
				match(T__1);
				}
				break;
			case 2:
				{
				setState(62);
				((ExprContext)_localctx).fun = match(IF);
				setState(63);
				a3();
				}
				break;
			case 3:
				{
				setState(64);
				((ExprContext)_localctx).fun = match(IS_NULL);
				setState(65);
				a1();
				}
				break;
			case 4:
				{
				setState(66);
				((ExprContext)_localctx).fun = match(COALESCE);
				setState(67);
				a1_n();
				}
				break;
			case 5:
				{
				setState(68);
				((ExprContext)_localctx).fun = match(MAXIMUM);
				setState(69);
				a1_n();
				}
				break;
			case 6:
				{
				setState(70);
				((ExprContext)_localctx).fun = match(MINIMUM);
				setState(71);
				a1_n();
				}
				break;
			case 7:
				{
				setState(72);
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
				setState(73);
				expr(23);
				}
				break;
			case 8:
				{
				setState(74);
				match(PLUS);
				setState(75);
				expr(22);
				}
				break;
			case 9:
				{
				setState(76);
				dataElement();
				}
				break;
			case 10:
				{
				setState(77);
				dataElementOperandCoc();
				}
				break;
			case 11:
				{
				setState(78);
				dataElementOperandAoc();
				}
				break;
			case 12:
				{
				setState(79);
				dataElementOperandCocAndAoc();
				}
				break;
			case 13:
				{
				setState(80);
				programDataElement();
				}
				break;
			case 14:
				{
				setState(81);
				programAttribute();
				}
				break;
			case 15:
				{
				setState(82);
				programIndicator();
				}
				break;
			case 16:
				{
				setState(83);
				orgUnitCount();
				}
				break;
			case 17:
				{
				setState(84);
				reportingRate();
				}
				break;
			case 18:
				{
				setState(85);
				constant();
				}
				break;
			case 19:
				{
				setState(86);
				days();
				}
				break;
			case 20:
				{
				setState(87);
				programIndicatorExpr();
				}
				break;
			case 21:
				{
				setState(88);
				numericLiteral();
				}
				break;
			case 22:
				{
				setState(89);
				stringLiteral();
				}
				break;
			case 23:
				{
				setState(90);
				booleanLiteral();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(116);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(114);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(93);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(94);
						((ExprContext)_localctx).fun = match(POWER);
						setState(95);
						expr(24);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(96);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(97);
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
						setState(98);
						expr(22);
						}
						break;
					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(99);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(100);
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
						setState(101);
						expr(21);
						}
						break;
					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(102);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(103);
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
						setState(104);
						expr(20);
						}
						break;
					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(105);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(106);
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
						setState(107);
						expr(19);
						}
						break;
					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(108);
						if (!(precpred(_ctx, 17))) throw new FailedPredicateException(this, "precpred(_ctx, 17)");
						setState(109);
						((ExprContext)_localctx).fun = match(AND);
						setState(110);
						expr(18);
						}
						break;
					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(111);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(112);
						((ExprContext)_localctx).fun = match(OR);
						setState(113);
						expr(17);
						}
						break;
					}
					} 
				}
				setState(118);
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
			setState(125);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__2:
				enterOuterAlt(_localctx, 1);
				{
				setState(119);
				match(T__2);
				setState(120);
				programIndicatorVariable();
				setState(121);
				match(T__3);
				}
				break;
			case T__4:
				enterOuterAlt(_localctx, 2);
				{
				setState(123);
				match(T__4);
				setState(124);
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
			setState(142);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EVENT_DATE:
				enterOuterAlt(_localctx, 1);
				{
				setState(127);
				((ProgramIndicatorVariableContext)_localctx).var = match(EVENT_DATE);
				}
				break;
			case DUE_DATE:
				enterOuterAlt(_localctx, 2);
				{
				setState(128);
				((ProgramIndicatorVariableContext)_localctx).var = match(DUE_DATE);
				}
				break;
			case INCIDENT_DATE:
				enterOuterAlt(_localctx, 3);
				{
				setState(129);
				((ProgramIndicatorVariableContext)_localctx).var = match(INCIDENT_DATE);
				}
				break;
			case ENROLLMENT_DATE:
				enterOuterAlt(_localctx, 4);
				{
				setState(130);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_DATE);
				}
				break;
			case ENROLLMENT_STATUS:
				enterOuterAlt(_localctx, 5);
				{
				setState(131);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_STATUS);
				}
				break;
			case CURRENT_STATUS:
				enterOuterAlt(_localctx, 6);
				{
				setState(132);
				((ProgramIndicatorVariableContext)_localctx).var = match(CURRENT_STATUS);
				}
				break;
			case VALUE_COUNT:
				enterOuterAlt(_localctx, 7);
				{
				setState(133);
				((ProgramIndicatorVariableContext)_localctx).var = match(VALUE_COUNT);
				}
				break;
			case ZERO_POS_VALUE_COUNT:
				enterOuterAlt(_localctx, 8);
				{
				setState(134);
				((ProgramIndicatorVariableContext)_localctx).var = match(ZERO_POS_VALUE_COUNT);
				}
				break;
			case EVENT_COUNT:
				enterOuterAlt(_localctx, 9);
				{
				setState(135);
				((ProgramIndicatorVariableContext)_localctx).var = match(EVENT_COUNT);
				}
				break;
			case ENROLLMENT_COUNT:
				enterOuterAlt(_localctx, 10);
				{
				setState(136);
				((ProgramIndicatorVariableContext)_localctx).var = match(ENROLLMENT_COUNT);
				}
				break;
			case TEI_COUNT:
				enterOuterAlt(_localctx, 11);
				{
				setState(137);
				((ProgramIndicatorVariableContext)_localctx).var = match(TEI_COUNT);
				}
				break;
			case PROGRAM_STAGE_NAME:
				enterOuterAlt(_localctx, 12);
				{
				setState(138);
				((ProgramIndicatorVariableContext)_localctx).var = match(PROGRAM_STAGE_NAME);
				}
				break;
			case PROGRAM_STAGE_ID:
				enterOuterAlt(_localctx, 13);
				{
				setState(139);
				((ProgramIndicatorVariableContext)_localctx).var = match(PROGRAM_STAGE_ID);
				}
				break;
			case REPORTING_PERIOD_START:
				enterOuterAlt(_localctx, 14);
				{
				setState(140);
				((ProgramIndicatorVariableContext)_localctx).var = match(REPORTING_PERIOD_START);
				}
				break;
			case REPORTING_PERIOD_END:
				enterOuterAlt(_localctx, 15);
				{
				setState(141);
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
			setState(164);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HAS_VALUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(144);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(HAS_VALUE);
				setState(145);
				a1();
				}
				break;
			case MINUTES_BETWEEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(146);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(MINUTES_BETWEEN);
				setState(147);
				a2();
				}
				break;
			case DAYS_BETWEEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(148);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(DAYS_BETWEEN);
				setState(149);
				a2();
				}
				break;
			case WEEKS_BETWEEN:
				enterOuterAlt(_localctx, 4);
				{
				setState(150);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(WEEKS_BETWEEN);
				setState(151);
				a2();
				}
				break;
			case MONTHS_BETWEEN:
				enterOuterAlt(_localctx, 5);
				{
				setState(152);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(MONTHS_BETWEEN);
				setState(153);
				a2();
				}
				break;
			case YEARS_BETWEEN:
				enterOuterAlt(_localctx, 6);
				{
				setState(154);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(YEARS_BETWEEN);
				setState(155);
				a2();
				}
				break;
			case CONDITION:
				enterOuterAlt(_localctx, 7);
				{
				setState(156);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(CONDITION);
				setState(157);
				a3();
				}
				break;
			case ZING:
				enterOuterAlt(_localctx, 8);
				{
				setState(158);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(ZING);
				setState(159);
				a1();
				}
				break;
			case OIZP:
				enterOuterAlt(_localctx, 9);
				{
				setState(160);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(OIZP);
				setState(161);
				a1();
				}
				break;
			case ZPVC:
				enterOuterAlt(_localctx, 10);
				{
				setState(162);
				((ProgramIndicatorFunctionContext)_localctx).fun = match(ZPVC);
				setState(163);
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
			setState(166);
			match(T__0);
			setState(167);
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
			setState(169);
			match(T__0);
			setState(171);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__2) | (1L << T__4) | (1L << T__6) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << MINUS) | (1L << PLUS) | (1L << NOT) | (1L << IF) | (1L << IS_NULL) | (1L << COALESCE) | (1L << MAXIMUM) | (1L << MINIMUM) | (1L << NUMERIC_LITERAL))) != 0) || _la==STRING_LITERAL || _la==BOOLEAN_LITERAL) {
				{
				setState(170);
				expr(0);
				}
			}

			setState(173);
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
			setState(175);
			match(T__0);
			setState(176);
			expr(0);
			setState(177);
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
			setState(179);
			match(T__0);
			setState(180);
			expr(0);
			setState(183);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__5) {
				{
				setState(181);
				match(T__5);
				setState(182);
				expr(0);
				}
			}

			setState(185);
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
			setState(187);
			match(T__0);
			setState(188);
			expr(0);
			setState(193);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__5) {
				{
				{
				setState(189);
				match(T__5);
				setState(190);
				expr(0);
				}
				}
				setState(195);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(196);
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
			setState(198);
			match(T__0);
			setState(199);
			expr(0);
			setState(200);
			match(T__5);
			setState(201);
			expr(0);
			setState(202);
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
			setState(204);
			match(T__0);
			setState(205);
			expr(0);
			setState(206);
			match(T__5);
			setState(207);
			expr(0);
			setState(208);
			match(T__5);
			setState(209);
			expr(0);
			setState(210);
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
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
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
			setState(212);
			match(T__6);
			setState(213);
			match(UID);
			setState(214);
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

	public static class DataElementOperandCocContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public DataElementOperandCocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementOperandCoc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementOperandCoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementOperandCocContext dataElementOperandCoc() throws RecognitionException {
		DataElementOperandCocContext _localctx = new DataElementOperandCocContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_dataElementOperandCoc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(216);
			match(T__6);
			setState(217);
			match(UID);
			setState(218);
			match(T__7);
			setState(219);
			match(UID);
			setState(221);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__8) {
				{
				setState(220);
				match(T__8);
				}
			}

			setState(223);
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

	public static class DataElementOperandAocContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public DataElementOperandAocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementOperandAoc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementOperandAoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementOperandAocContext dataElementOperandAoc() throws RecognitionException {
		DataElementOperandAocContext _localctx = new DataElementOperandAocContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_dataElementOperandAoc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(225);
			match(T__6);
			setState(226);
			match(UID);
			setState(227);
			match(T__9);
			setState(228);
			match(UID);
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

	public static class DataElementOperandCocAndAocContext extends ParserRuleContext {
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
		}
		public DataElementOperandCocAndAocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataElementOperandCocAndAoc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof ExpressionVisitor ) return ((ExpressionVisitor<? extends T>)visitor).visitDataElementOperandCocAndAoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DataElementOperandCocAndAocContext dataElementOperandCocAndAoc() throws RecognitionException {
		DataElementOperandCocAndAocContext _localctx = new DataElementOperandCocAndAocContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_dataElementOperandCocAndAoc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(231);
			match(T__6);
			setState(232);
			match(UID);
			setState(233);
			match(T__7);
			setState(234);
			match(UID);
			setState(235);
			match(T__7);
			setState(236);
			match(UID);
			setState(237);
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
		enterRule(_localctx, 32, RULE_programDataElement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(239);
			match(T__10);
			setState(240);
			match(UID);
			setState(241);
			match(T__7);
			setState(242);
			match(UID);
			setState(243);
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
		public List<TerminalNode> UID() { return getTokens(ExpressionParser.UID); }
		public TerminalNode UID(int i) {
			return getToken(ExpressionParser.UID, i);
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
		enterRule(_localctx, 34, RULE_programAttribute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(245);
			match(T__11);
			setState(246);
			match(UID);
			setState(247);
			match(T__7);
			setState(248);
			match(UID);
			setState(249);
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
		enterRule(_localctx, 36, RULE_programIndicator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(251);
			match(T__12);
			setState(252);
			match(UID);
			setState(253);
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
		enterRule(_localctx, 38, RULE_orgUnitCount);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			match(T__13);
			setState(256);
			match(UID);
			setState(257);
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
		public TerminalNode UID() { return getToken(ExpressionParser.UID, 0); }
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
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
		enterRule(_localctx, 40, RULE_reportingRate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(259);
			match(T__14);
			setState(260);
			match(UID);
			setState(261);
			match(T__7);
			setState(262);
			keyword();
			setState(263);
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
		enterRule(_localctx, 42, RULE_constant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
			match(T__15);
			setState(266);
			match(UID);
			setState(267);
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
		enterRule(_localctx, 44, RULE_days);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			match(T__16);
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
		enterRule(_localctx, 46, RULE_numericLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
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
		enterRule(_localctx, 48, RULE_stringLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
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
		enterRule(_localctx, 50, RULE_booleanLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(275);
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
		enterRule(_localctx, 52, RULE_keyword);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(277);
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
			return precpred(_ctx, 24);
		case 1:
			return precpred(_ctx, 21);
		case 2:
			return precpred(_ctx, 20);
		case 3:
			return precpred(_ctx, 19);
		case 4:
			return precpred(_ctx, 18);
		case 5:
			return precpred(_ctx, 17);
		case 6:
			return precpred(_ctx, 16);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3F\u011a\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3^\n\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3u\n"+
		"\3\f\3\16\3x\13\3\3\4\3\4\3\4\3\4\3\4\3\4\5\4\u0080\n\4\3\5\3\5\3\5\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5\u0091\n\5\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\5\6\u00a7\n\6\3\7\3\7\3\7\3\b\3\b\5\b\u00ae\n\b\3\b\3\b\3\t\3\t\3\t\3"+
		"\t\3\n\3\n\3\n\3\n\5\n\u00ba\n\n\3\n\3\n\3\13\3\13\3\13\3\13\7\13\u00c2"+
		"\n\13\f\13\16\13\u00c5\13\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17"+
		"\5\17\u00e0\n\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\26\3\26\3\26"+
		"\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32\3\33"+
		"\3\33\3\34\3\34\3\34\2\3\4\35\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 "+
		"\"$&(*,.\60\62\64\66\2\b\4\2\25\25\27\27\3\2\30\32\3\2\25\26\3\2\33\36"+
		"\3\2\37 \3\2DE\2\u0137\28\3\2\2\2\4]\3\2\2\2\6\177\3\2\2\2\b\u0090\3\2"+
		"\2\2\n\u00a6\3\2\2\2\f\u00a8\3\2\2\2\16\u00ab\3\2\2\2\20\u00b1\3\2\2\2"+
		"\22\u00b5\3\2\2\2\24\u00bd\3\2\2\2\26\u00c8\3\2\2\2\30\u00ce\3\2\2\2\32"+
		"\u00d6\3\2\2\2\34\u00da\3\2\2\2\36\u00e3\3\2\2\2 \u00e9\3\2\2\2\"\u00f1"+
		"\3\2\2\2$\u00f7\3\2\2\2&\u00fd\3\2\2\2(\u0101\3\2\2\2*\u0105\3\2\2\2,"+
		"\u010b\3\2\2\2.\u010f\3\2\2\2\60\u0111\3\2\2\2\62\u0113\3\2\2\2\64\u0115"+
		"\3\2\2\2\66\u0117\3\2\2\289\5\4\3\29:\7\2\2\3:\3\3\2\2\2;<\b\3\1\2<=\7"+
		"\3\2\2=>\5\4\3\2>?\7\4\2\2?^\3\2\2\2@A\7#\2\2A^\5\30\r\2BC\7$\2\2C^\5"+
		"\20\t\2DE\7%\2\2E^\5\24\13\2FG\7&\2\2G^\5\24\13\2HI\7\'\2\2I^\5\24\13"+
		"\2JK\t\2\2\2K^\5\4\3\31LM\7\26\2\2M^\5\4\3\30N^\5\32\16\2O^\5\34\17\2"+
		"P^\5\36\20\2Q^\5 \21\2R^\5\"\22\2S^\5$\23\2T^\5&\24\2U^\5(\25\2V^\5*\26"+
		"\2W^\5,\27\2X^\5.\30\2Y^\5\6\4\2Z^\5\60\31\2[^\5\62\32\2\\^\5\64\33\2"+
		"];\3\2\2\2]@\3\2\2\2]B\3\2\2\2]D\3\2\2\2]F\3\2\2\2]H\3\2\2\2]J\3\2\2\2"+
		"]L\3\2\2\2]N\3\2\2\2]O\3\2\2\2]P\3\2\2\2]Q\3\2\2\2]R\3\2\2\2]S\3\2\2\2"+
		"]T\3\2\2\2]U\3\2\2\2]V\3\2\2\2]W\3\2\2\2]X\3\2\2\2]Y\3\2\2\2]Z\3\2\2\2"+
		"][\3\2\2\2]\\\3\2\2\2^v\3\2\2\2_`\f\32\2\2`a\7\24\2\2au\5\4\3\32bc\f\27"+
		"\2\2cd\t\3\2\2du\5\4\3\30ef\f\26\2\2fg\t\4\2\2gu\5\4\3\27hi\f\25\2\2i"+
		"j\t\5\2\2ju\5\4\3\26kl\f\24\2\2lm\t\6\2\2mu\5\4\3\25no\f\23\2\2op\7!\2"+
		"\2pu\5\4\3\24qr\f\22\2\2rs\7\"\2\2su\5\4\3\23t_\3\2\2\2tb\3\2\2\2te\3"+
		"\2\2\2th\3\2\2\2tk\3\2\2\2tn\3\2\2\2tq\3\2\2\2ux\3\2\2\2vt\3\2\2\2vw\3"+
		"\2\2\2w\5\3\2\2\2xv\3\2\2\2yz\7\5\2\2z{\5\b\5\2{|\7\6\2\2|\u0080\3\2\2"+
		"\2}~\7\7\2\2~\u0080\5\n\6\2\177y\3\2\2\2\177}\3\2\2\2\u0080\7\3\2\2\2"+
		"\u0081\u0091\7(\2\2\u0082\u0091\7)\2\2\u0083\u0091\7*\2\2\u0084\u0091"+
		"\7+\2\2\u0085\u0091\7,\2\2\u0086\u0091\7-\2\2\u0087\u0091\7.\2\2\u0088"+
		"\u0091\7/\2\2\u0089\u0091\7\60\2\2\u008a\u0091\7\61\2\2\u008b\u0091\7"+
		"\62\2\2\u008c\u0091\7\63\2\2\u008d\u0091\7\64\2\2\u008e\u0091\7\65\2\2"+
		"\u008f\u0091\7\66\2\2\u0090\u0081\3\2\2\2\u0090\u0082\3\2\2\2\u0090\u0083"+
		"\3\2\2\2\u0090\u0084\3\2\2\2\u0090\u0085\3\2\2\2\u0090\u0086\3\2\2\2\u0090"+
		"\u0087\3\2\2\2\u0090\u0088\3\2\2\2\u0090\u0089\3\2\2\2\u0090\u008a\3\2"+
		"\2\2\u0090\u008b\3\2\2\2\u0090\u008c\3\2\2\2\u0090\u008d\3\2\2\2\u0090"+
		"\u008e\3\2\2\2\u0090\u008f\3\2\2\2\u0091\t\3\2\2\2\u0092\u0093\7\67\2"+
		"\2\u0093\u00a7\5\20\t\2\u0094\u0095\78\2\2\u0095\u00a7\5\26\f\2\u0096"+
		"\u0097\79\2\2\u0097\u00a7\5\26\f\2\u0098\u0099\7:\2\2\u0099\u00a7\5\26"+
		"\f\2\u009a\u009b\7;\2\2\u009b\u00a7\5\26\f\2\u009c\u009d\7<\2\2\u009d"+
		"\u00a7\5\26\f\2\u009e\u009f\7=\2\2\u009f\u00a7\5\30\r\2\u00a0\u00a1\7"+
		">\2\2\u00a1\u00a7\5\20\t\2\u00a2\u00a3\7?\2\2\u00a3\u00a7\5\20\t\2\u00a4"+
		"\u00a5\7@\2\2\u00a5\u00a7\5\24\13\2\u00a6\u0092\3\2\2\2\u00a6\u0094\3"+
		"\2\2\2\u00a6\u0096\3\2\2\2\u00a6\u0098\3\2\2\2\u00a6\u009a\3\2\2\2\u00a6"+
		"\u009c\3\2\2\2\u00a6\u009e\3\2\2\2\u00a6\u00a0\3\2\2\2\u00a6\u00a2\3\2"+
		"\2\2\u00a6\u00a4\3\2\2\2\u00a7\13\3\2\2\2\u00a8\u00a9\7\3\2\2\u00a9\u00aa"+
		"\7\4\2\2\u00aa\r\3\2\2\2\u00ab\u00ad\7\3\2\2\u00ac\u00ae\5\4\3\2\u00ad"+
		"\u00ac\3\2\2\2\u00ad\u00ae\3\2\2\2\u00ae\u00af\3\2\2\2\u00af\u00b0\7\4"+
		"\2\2\u00b0\17\3\2\2\2\u00b1\u00b2\7\3\2\2\u00b2\u00b3\5\4\3\2\u00b3\u00b4"+
		"\7\4\2\2\u00b4\21\3\2\2\2\u00b5\u00b6\7\3\2\2\u00b6\u00b9\5\4\3\2\u00b7"+
		"\u00b8\7\b\2\2\u00b8\u00ba\5\4\3\2\u00b9\u00b7\3\2\2\2\u00b9\u00ba\3\2"+
		"\2\2\u00ba\u00bb\3\2\2\2\u00bb\u00bc\7\4\2\2\u00bc\23\3\2\2\2\u00bd\u00be"+
		"\7\3\2\2\u00be\u00c3\5\4\3\2\u00bf\u00c0\7\b\2\2\u00c0\u00c2\5\4\3\2\u00c1"+
		"\u00bf\3\2\2\2\u00c2\u00c5\3\2\2\2\u00c3\u00c1\3\2\2\2\u00c3\u00c4\3\2"+
		"\2\2\u00c4\u00c6\3\2\2\2\u00c5\u00c3\3\2\2\2\u00c6\u00c7\7\4\2\2\u00c7"+
		"\25\3\2\2\2\u00c8\u00c9\7\3\2\2\u00c9\u00ca\5\4\3\2\u00ca\u00cb\7\b\2"+
		"\2\u00cb\u00cc\5\4\3\2\u00cc\u00cd\7\4\2\2\u00cd\27\3\2\2\2\u00ce\u00cf"+
		"\7\3\2\2\u00cf\u00d0\5\4\3\2\u00d0\u00d1\7\b\2\2\u00d1\u00d2\5\4\3\2\u00d2"+
		"\u00d3\7\b\2\2\u00d3\u00d4\5\4\3\2\u00d4\u00d5\7\4\2\2\u00d5\31\3\2\2"+
		"\2\u00d6\u00d7\7\t\2\2\u00d7\u00d8\7D\2\2\u00d8\u00d9\7\6\2\2\u00d9\33"+
		"\3\2\2\2\u00da\u00db\7\t\2\2\u00db\u00dc\7D\2\2\u00dc\u00dd\7\n\2\2\u00dd"+
		"\u00df\7D\2\2\u00de\u00e0\7\13\2\2\u00df\u00de\3\2\2\2\u00df\u00e0\3\2"+
		"\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00e2\7\6\2\2\u00e2\35\3\2\2\2\u00e3\u00e4"+
		"\7\t\2\2\u00e4\u00e5\7D\2\2\u00e5\u00e6\7\f\2\2\u00e6\u00e7\7D\2\2\u00e7"+
		"\u00e8\7\6\2\2\u00e8\37\3\2\2\2\u00e9\u00ea\7\t\2\2\u00ea\u00eb\7D\2\2"+
		"\u00eb\u00ec\7\n\2\2\u00ec\u00ed\7D\2\2\u00ed\u00ee\7\n\2\2\u00ee\u00ef"+
		"\7D\2\2\u00ef\u00f0\7\6\2\2\u00f0!\3\2\2\2\u00f1\u00f2\7\r\2\2\u00f2\u00f3"+
		"\7D\2\2\u00f3\u00f4\7\n\2\2\u00f4\u00f5\7D\2\2\u00f5\u00f6\7\6\2\2\u00f6"+
		"#\3\2\2\2\u00f7\u00f8\7\16\2\2\u00f8\u00f9\7D\2\2\u00f9\u00fa\7\n\2\2"+
		"\u00fa\u00fb\7D\2\2\u00fb\u00fc\7\6\2\2\u00fc%\3\2\2\2\u00fd\u00fe\7\17"+
		"\2\2\u00fe\u00ff\7D\2\2\u00ff\u0100\7\6\2\2\u0100\'\3\2\2\2\u0101\u0102"+
		"\7\20\2\2\u0102\u0103\7D\2\2\u0103\u0104\7\6\2\2\u0104)\3\2\2\2\u0105"+
		"\u0106\7\21\2\2\u0106\u0107\7D\2\2\u0107\u0108\7\n\2\2\u0108\u0109\5\66"+
		"\34\2\u0109\u010a\7\6\2\2\u010a+\3\2\2\2\u010b\u010c\7\22\2\2\u010c\u010d"+
		"\7D\2\2\u010d\u010e\7\6\2\2\u010e-\3\2\2\2\u010f\u0110\7\23\2\2\u0110"+
		"/\3\2\2\2\u0111\u0112\7A\2\2\u0112\61\3\2\2\2\u0113\u0114\7B\2\2\u0114"+
		"\63\3\2\2\2\u0115\u0116\7C\2\2\u0116\65\3\2\2\2\u0117\u0118\t\7\2\2\u0118"+
		"\67\3\2\2\2\f]tv\177\u0090\u00a6\u00ad\u00b9\u00c3\u00df";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}