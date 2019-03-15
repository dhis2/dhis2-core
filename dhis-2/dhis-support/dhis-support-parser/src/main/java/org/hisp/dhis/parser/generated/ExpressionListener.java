// Generated from Expression.g4 by ANTLR 4.7.2
package org.hisp.dhis.parser.generated;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ExpressionParser}.
 */
public interface ExpressionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ExpressionParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ExpressionParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(ExpressionParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(ExpressionParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#function}.
	 * @param ctx the parse tree
	 */
	void enterFunction(ExpressionParser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#function}.
	 * @param ctx the parse tree
	 */
	void exitFunction(ExpressionParser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#item}.
	 * @param ctx the parse tree
	 */
	void enterItem(ExpressionParser.ItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#item}.
	 * @param ctx the parse tree
	 */
	void exitItem(ExpressionParser.ItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#programVariable}.
	 * @param ctx the parse tree
	 */
	void enterProgramVariable(ExpressionParser.ProgramVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#programVariable}.
	 * @param ctx the parse tree
	 */
	void exitProgramVariable(ExpressionParser.ProgramVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#programFunction}.
	 * @param ctx the parse tree
	 */
	void enterProgramFunction(ExpressionParser.ProgramFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#programFunction}.
	 * @param ctx the parse tree
	 */
	void exitProgramFunction(ExpressionParser.ProgramFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#column}.
	 * @param ctx the parse tree
	 */
	void enterColumn(ExpressionParser.ColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#column}.
	 * @param ctx the parse tree
	 */
	void exitColumn(ExpressionParser.ColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#stageDataElement}.
	 * @param ctx the parse tree
	 */
	void enterStageDataElement(ExpressionParser.StageDataElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#stageDataElement}.
	 * @param ctx the parse tree
	 */
	void exitStageDataElement(ExpressionParser.StageDataElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#programAttribute}.
	 * @param ctx the parse tree
	 */
	void enterProgramAttribute(ExpressionParser.ProgramAttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#programAttribute}.
	 * @param ctx the parse tree
	 */
	void exitProgramAttribute(ExpressionParser.ProgramAttributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#compareDate}.
	 * @param ctx the parse tree
	 */
	void enterCompareDate(ExpressionParser.CompareDateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#compareDate}.
	 * @param ctx the parse tree
	 */
	void exitCompareDate(ExpressionParser.CompareDateContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#itemNumStringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterItemNumStringLiteral(ExpressionParser.ItemNumStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#itemNumStringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitItemNumStringLiteral(ExpressionParser.ItemNumStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#numStringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterNumStringLiteral(ExpressionParser.NumStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#numStringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitNumStringLiteral(ExpressionParser.NumStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(ExpressionParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(ExpressionParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#numericLiteral}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(ExpressionParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#numericLiteral}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(ExpressionParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpressionParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(ExpressionParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpressionParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(ExpressionParser.StringLiteralContext ctx);
}