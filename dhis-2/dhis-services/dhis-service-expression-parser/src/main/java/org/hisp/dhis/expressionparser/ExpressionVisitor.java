package org.hisp.dhis.expressionparser;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expressionparser.generated.ExpressionBaseVisitor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.Math.pow;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.hisp.dhis.expressionparser.generated.ExpressionParser.*;

/**
 * Common ANTLR parsed expression code, using the ANTLR visitor pattern.
 * <p/>
 * ANTLR provides two patterns for traversing a tree that represents a parsed
 * expression: With the "listener" pattern, ANTRL traverses the parsed tree
 * but allows a subclass to listen at each node as it is processed. With the
 * "visitor" pattern, the subclass (such as this class) is called for each node,
 * and it is up to the code in this pattern to make explicit calls to descendant
 * nodes. To process DHIS2 expressions the visitor pattern is used, because the
 * visiting of descendant nodes can be used to advantage as described here.
 * <p/>
 * A left-recursive descent parser such as ANTLR is designed to evaluate
 * expressions left-to-right, but the nodes are actually visited initially in
 * the order right-to-left. For example, if the expression is "1 + 2 - 3", the
 * higher-level node "(subexpression) - 3", is visited first, then it would
 * visit the lower-level subexpression node "1 + 2" to evaluate that
 * subexpression. After the subexpression returns its value, to the higher-level
 * node, it finally subtracts 3 from that returned subexpression value.
 * <p/>
 * As an example of how this class makes use of the right-to-left visiting,
 * consider the expression #{uid}.period(-3,-1).sum(). The sum() function is
 * visited first as (subexpression).sum(). The sum() function will expect its
 * subexpression to return multiple values to be summed, and so evaluates its
 * subexpresssion. In doing so, the period() function is next visited as
 * (subexpression).period(-3,-1). The period function then iterates over periods
 * -3 to -1, and calls multiple times to evaluate its (subexpression) for each
 * of these periods in turn. Finally, it returns an object with the multiple
 * values from these evaluations to the sum() function, which then sums them.
 * <p/>
 * If the ANTLR listener pattern were used in the above example, the
 * subexpression to the left of the period() function would be called only
 * once, as determined by ANTLR. But by using the visitor period, this class
 * is able to do things like evaluate the subexpression multiple times.
 * <p/>
 * This is an abstract class, which is intended to be extended for two different
 * purposes: (1) an "items visitor" which collects the expression items as
 * visited before data is fetched from the database, to know which values need
 * to be fetched, and which periods and organisaiton units they need fetching
 * for, and (2) an "value visitor" once the database values have been found, to
 * plug them into the expression and compute the total value of the expression.
 *
 * @author Jim Grace
 */
public abstract class ExpressionVisitor
    extends ExpressionBaseVisitor<Object>
{
    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected ConstantService constantService;

    protected Map<String, Double> constantMap = null;

    protected Map<String, Integer> orgUnitCountMap = null;

    protected Double days = DEFAULT_ITEM_VALUE;

    protected Map<String, String> itemDescriptions = null;

    /**
     * Default item value to use when the value is not present.
     */
    protected final static Double DEFAULT_ITEM_VALUE = 0.0;


    // -------------------------------------------------------------------------
    // Visitor methods that are implemented here
    // -------------------------------------------------------------------------

    @Override
    public final Object visitExpression( ExpressionContext ctx )
    {
        return visit( ctx.expr() );
    }

    @Override
    public final Object visitExpr( ExprContext ctx )
    {
        if ( ctx.fun != null ) // Invoke a function
        {
            try
            {
                return function( ctx );
            }
            catch ( ExpressionParserExceptionWithoutContext ex )
            {
                throw new ExpressionParserException( ex.getMessage() + " while evaluating '" + ctx.fun.getText() + "'" );
            }
        }
        else if ( ctx.expr(0) != null ) // Pass through the expression
        {
            return visit( ctx.expr( 0 ) );
        }
        else // Visit the type of expression defined
        {
            return visit( ctx.getChild( 0 ) );
        }
    }

    @Override
    public final Object visitNumericLiteral( NumericLiteralContext ctx )
    {
        return Double.valueOf( ctx.getText() );
    }

    @Override
    public final Object visitStringLiteral( StringLiteralContext ctx )
    {
        return unescapeJava( ctx.getText().substring( 1, ctx.getText().length() - 1 ) );
    }

    @Override
    public final Object visitBooleanLiteral( BooleanLiteralContext ctx )
    {
        return Boolean.valueOf( ctx.getText() );
    }

    @Override
    public final Object visitConstant( ConstantContext ctx )
    {
        String constantId = ctx.constantId().getText();

        Double value = constantMap == null ? DEFAULT_ITEM_VALUE : constantMap.get( constantId );

        if ( value == null )
        {
            throw new ExpressionParserException( "No constant defined for " + constantId );
        }

        if ( itemDescriptions != null )
        {
            Constant constant = constantService.getConstant( constantId );

            itemDescriptions.put( ctx.getText(), constant.getDisplayName() );
        }

        return value;
    }

    @Override
    public final Object visitDays( DaysContext ctx )
    {
        return days;
    }

    // -------------------------------------------------------------------------
    // Visitor methods to override when the syntax is supported.
    // -------------------------------------------------------------------------

    @Override
    public Object visitProgramIndicatorVariable( ProgramIndicatorVariableContext ctx )
    {
        throw new ExpressionParserException( "Program indicator variable is not valid in this expression." );
    }

    @Override
    public Object visitProgramIndicatorFunction( ProgramIndicatorFunctionContext ctx )
    {
        throw new ExpressionParserException( "Program indicator function is not valid in this expression." );
    }

    // -------------------------------------------------------------------------
    // Subclasses must implement the following logical methods.
    //
    // When getting ExpresssionItems and description, visit all the items
    // beause we don't know what the data values will be. Assume that all
    // values will be dummy and no values will be null.
    //
    // When finding the expression value based on data values, only evaluate
    // those exprssions we need to. This not only saves time but is important
    // when implementing skip strategy. For example, if a value is explicitly
    // tested to see if it is null, then it doesn't apply against the skip
    // strategy if it is missing. Also, assume that any value returned might
    // be null, and protect yourself accordingly.
    // -------------------------------------------------------------------------

    /**
     * Finds the logical AND of two boolean expressions.
     *
     * @param ctx the parsing context.
     * @return the logical AND.
     */
    protected abstract Object functionAnd( ExprContext ctx );

    /**
     * Finds the logical OR of two boolean expressions.
     *
     * @param ctx the parsing context.
     * @return the logical OR.
     */
    protected abstract Object functionOr( ExprContext ctx );

    /**
     * If the test expression is true, returns the first expression value,
     * else returns the second expression value.
     *
     * @param ctx the parsing context.
     * @return the first or second expression value, depending on the test.
     */
    protected abstract Object functionIf( ExprContext ctx );

    /**
     * If the test expression is true, skips this value (returns null).
     *
     * @param ctx the parsing context.
     * @return the expression, or null, depending on the test.
     */
    protected abstract Object functionExcept( ExprContext ctx );

    /**
     * Returns the first non-null argument.
     *
     * @param ctx the parsing context.
     * @return the first non-null argument.
     */
    protected abstract Object functionCoalesce( ExprContext ctx );

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Evaluates common functions and operators.
     *
     * @param ctx expression context
     * @return evaluated expression object
     */
    protected Object function( ExprContext ctx )
    {
        switch ( ctx.fun.getType() ) {
        // -----------------------------------------------------------------
        // Arithmetic Operators (return Double)
        // -----------------------------------------------------------------

        case MINUS:
            if ( ctx.expr().size() > 1 ) // Subtract operator
            {
                return double2( ctx, (Double a, Double b) -> a - b );
            }
            else // Unary Negative operator
            {
                return double1( ctx, (Double a) -> -a );
            }

        case PLUS: // String concatenation or numeric addition
            Object arg1 = visit( ctx.expr(0) );
            Object arg2 = visit( ctx.expr(1) );

            if (arg1 instanceof String)
            {
                return arg2 == null ? null : arg1 + castString(arg2);
            }

            return double2( castDouble(arg1), castDouble(arg2), (Double a, Double b) -> a + b );

        case POWER:
            return double2( ctx, (Double a, Double b) -> pow(a, b) );

        case MUL:
            return double2( ctx, (Double a, Double b) -> a * b );

        case DIV:
            return double2( ctx, (Double a, Double b) -> a / b );

        case MOD:
            return double2( ctx, (Double a, Double b) -> a % b );

        // -----------------------------------------------------------------
        // Logical Operators (return Boolean)
        // -----------------------------------------------------------------

        case NOT:
            return boolean1( ctx, (Boolean a) -> !a );

        case LEQ:
            return compare( ctx, (Integer a) -> a <= 0 );

        case GEQ:
            return compare( ctx, (Integer a) -> a >= 0 );

        case LT:
            return compare( ctx, (Integer a) -> a < 0 );

        case GT:
            return compare( ctx, (Integer a) -> a > 0 );

        case EQ:
            return compare( ctx, (Integer a) -> a == 0 );

        case NE:
            return compare( ctx, (Integer a) -> a != 0 );

        case AND:
            return functionAnd( ctx );

        case OR:
            return functionOr( ctx );

        // -----------------------------------------------------------------
        // Logical functions
        // -----------------------------------------------------------------

        case IF:
            return functionIf( ctx );

        case IS_NULL:
            return visit( ctx.a1().expr() ) == null;

        case COALESCE:
            return functionCoalesce( ctx);

        case MAXIMUM:
            return functionMinMax( ctx, 1.0 );

        case MINIMUM:
            return functionMinMax( ctx, -1.0 );

        default: // (Shouldn't happen, mismatch between expression grammer and here.)
            throw new ExpressionParserExceptionWithoutContext( "Fun=" + ctx.fun.getType() + " not recognized." );
        }
    }

    /**
     * Returns the minimum or maximum value.
     *
     * @param ctx    the parsing context.
     * @param minmax -1.0 for minimum, 1.0 for maximum.
     * @return the minimum or maximum value.
     */
    private Object functionMinMax( ExprContext ctx, double minmax )
    {
        Double returnVal = null;

        for ( ExprContext c : ctx.a1_n().expr() )
        {
            Double val = castDouble( visit( c ) );

            if ( returnVal == null || val != null && ( val - returnVal ) * minmax > 0 )
            {
                returnVal = val;
            }
        }
        return returnVal;
    }

    // -------------------------------------------------------------------------
    // Other supportive functions
    // -------------------------------------------------------------------------

    /**
     * Evaluates a one-argument double function, but returns null if the
     * input value is null.
     *
     * @param ctx the parsing context with one expression.
     * @param fn the function to evaluate.
     * @return the function result, or null if input was null.
     */
    protected Double double1( ExprContext ctx, Function<Double, Double> fn )
    {
        Double d1 = castDouble( visit( ctx.expr( 0 ) ) );

        return fn.apply( d1 == null ? DEFAULT_ITEM_VALUE : d1 );
    }

    /**
     * Evaluates a two-argument double function, but returns null if either of
     * the input values is null.
     *
     * @param ctx the parsing context with two expressions.
     * @param fn the function to evaluate.
     * @return the function result, or null if either input was null.
     */
    protected Double double2( ExprContext ctx, BiFunction<Double, Double, Double> fn )
    {
        Double d1 = castDouble( visit( ctx.expr( 0 ) ) );
        Double d2 = castDouble( visit( ctx.expr( 1 ) ) );

        return double2( d1, d2, fn);
    }

    /**
     * Evaluates a two-argument double function, but returns null if either of
     * the input values is null.
     *
     * @param d1 the first function argument (could be null).
     * @param d2 the second function argument (could be null).
     * @param fn the function to evaluate.
     * @return the function result, or null if either input was null.
     */
    protected Double double2( Double d1, Double d2, BiFunction<Double, Double, Double> fn )
    {
        return fn.apply(
            d1 == null ? DEFAULT_ITEM_VALUE : d1,
            d2 == null ? DEFAULT_ITEM_VALUE : d2 );
    }

    /**
     * Evaluates a two-argument boolean function, but returns null if either of
     * the input values is null.
     *
     * @param ctx the parsing context with two expressions.
     * @param fn the function to evaluate.
     * @return the function result, or null if either input was null.
     */
    protected Boolean boolean1( ExprContext ctx, Function<Boolean, Boolean> fn )
    {
        Boolean b1 = castBoolean( visit( ctx.expr( 0 ) ) );

        return b1 == null
            ? null
            : fn.apply( b1 );
    }

    /**
     * Casts object as Double, or throws exception.
     * <p/>
     * If the object is null, return null.
     *
     * @param object the value to cast as a Double.
     * @return Double value.
     */
    protected Double castDouble( Object object )
    {
        return (Double) cast( Double.class, object );
    }

    /**
     * Casts object as Boolean, or throws exception.
     *
     * @param object the value to cast as a Boolean.
     * @return Boolean value.
     */
    protected Boolean castBoolean( Object object )
    {
        return (Boolean) cast( Boolean.class, object );
    }

    /**
     * Casts object as String, or throws exception.
     *
     * @param object the value to cast as a String.
     * @return String value.
     */
    protected String castString( Object object )
    {
        return (String) cast( String.class, object );
    }

    /**
     * Checks to see whether object can be cast to the class specified,
     * or throws exception if it can't.
     *
     * @param clazz the class: Double, Boolean, or String
     * @param object the value to cast
     * @return object (if it can be cast to that class.)
     */
    protected Object cast( Class<?> clazz, Object object )
    {
        if ( object instanceof Double && clazz != Double.class )
        {
            throw new ExpressionParserExceptionWithoutContext( "Found number when expecting " + clazz.getSimpleName() );
        }

        if ( object instanceof String && clazz != String.class )
        {
            throw new ExpressionParserExceptionWithoutContext( "Found string when expecting " + clazz.getSimpleName() );
        }

        if ( object instanceof Boolean && clazz != Boolean.class )
        {
            throw new ExpressionParserExceptionWithoutContext( "Found boolean value when expecting " + clazz.getSimpleName() );
        }

        try
        {
            return clazz.cast( object );
        }
        catch ( Exception e )
        {
            throw new ExpressionParserExceptionWithoutContext( "Could not cast value to " + clazz.getSimpleName() );
        }
    }

    /**
     * Compares two Doubles, Strings or Booleans applying the given function
     * to the integer result of the comparison. Returns null if either value
     * is null.
     *
     * @param ctx the expression context with two sub-expressions.
     * @param fn the function to evaluate the compare result.
     * @return the results of the comparision.
     */
    private Boolean compare( ExprContext ctx, Function<Integer, Boolean> fn )
    {
        Object o1 = visit( ctx.expr( 0 ) );
        Object o2 = visit( ctx.expr( 1 ) );

        if ( o1 == null || o2 == null )
        {
            return null;
        }

        int compare;

        if ( o1 instanceof Double )
        {
            compare = ( (Double) o1).compareTo( castDouble( o2 ) );
        }
        else if ( o1 instanceof String )
        {
            compare = ( (String) o1).compareTo( castString( o2 ) );
        }
        else if ( o1 instanceof Boolean )
        {
            compare = ( (Boolean) o1).compareTo( castBoolean( o2 ) );
        }
        else // (Shouldn't happen)
        {
            throw new ExpressionParserExceptionWithoutContext( "Magnitude of " + o1.getClass().getSimpleName() + " '" + o1.toString() +
                "' cannot be compared to: " + o2.getClass().getSimpleName() + " '" + o1.toString() + "'" );
        }

        return fn.apply( compare );
    }
}
