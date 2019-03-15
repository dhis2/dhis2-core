// Generated from Expression.g4 by ANTLR 4.7.2
package org.hisp.dhis.parser.generated;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ExpressionParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ExpressionVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ExpressionParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(ExpressionParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(ExpressionParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitItem(ExpressionParser.ItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#programVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramVariable(ExpressionParser.ProgramVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#programFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramFunction(ExpressionParser.ProgramFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn(ExpressionParser.ColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#stageDataElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStageDataElement(ExpressionParser.StageDataElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#programAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramAttribute(ExpressionParser.ProgramAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#compareDate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompareDate(ExpressionParser.CompareDateContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#itemNumStringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitItemNumStringLiteral(ExpressionParser.ItemNumStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#numStringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumStringLiteral(ExpressionParser.NumStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(ExpressionParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#numericLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(ExpressionParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpressionParser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(ExpressionParser.StringLiteralContext ctx);
}