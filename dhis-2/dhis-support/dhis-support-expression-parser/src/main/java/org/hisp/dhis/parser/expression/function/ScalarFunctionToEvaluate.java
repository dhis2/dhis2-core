package org.hisp.dhis.parser.expression.function;

import org.hisp.dhis.antlr.AntlrExpressionVisitor;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;

/**
 * Scalar function that needs to be evaluated using the specific CommonExpressionVisitor.
 *
 * @author Enrico Colasante
 */
public interface ScalarFunctionToEvaluate extends SimpleScalarFunction
{
    /**
     * Finds the value of an expression function, evaluating arguments only
     * when necessary.
     *
     * @param ctx the expression context
     * @param visitor the specific tree visitor
     * @return the value of the function, evaluating necessary args
     */
    Object evaluate( ExpressionParser.ExprContext ctx, CommonExpressionVisitor visitor );

    /**
     * Override the default evaluate method using the specific visitor CommonExpressionVisitor.
     *
     * @param ctx the expression context
     * @param visitor the specific tree visitor
     * @return the value of the function, evaluating necessary args
     */
    default Object evaluate( ExpressionParser.ExprContext ctx, AntlrExpressionVisitor visitor )
    {
        return evaluate( ctx, (CommonExpressionVisitor) visitor );
    }
}
