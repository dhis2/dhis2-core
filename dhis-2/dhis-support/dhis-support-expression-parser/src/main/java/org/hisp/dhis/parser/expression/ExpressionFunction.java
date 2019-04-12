package org.hisp.dhis.parser.expression;

import org.hisp.dhis.parser.expression.function.FunctionPower;

import java.util.HashMap;
import java.util.Map;

public interface ExpressionFunction
{
    Object evaluate( ExprContext ctx, AbstractVisitor visitor );

    Object getSql( ExprContext ctx, AbstractVisitor visitor );

    Map<Integer, ExpressionFunction> functionMap = new HashMap<Integer, ExpressionFunction>()
    {
        {
            put( POWER, new FunctionPower() );
            put( MUL, new functionMul() );
        }
    };

    static Object evaluate( int fun, ExpressionFunctionMode mode, ExprContext ctx, AbstractVisitor visitor )
    {
        ExpressionFunction function = functionMap.get( fun );

        switch( mode )
        {
            case EVAL:
                return function.evaluate( ctx, visitor );

            case SQL:
                return function.getSql( ctx, visitor );

            default:
                throw new RuntimeException( "Unsupported function mode " + mode.name() );
        }
    }
}
