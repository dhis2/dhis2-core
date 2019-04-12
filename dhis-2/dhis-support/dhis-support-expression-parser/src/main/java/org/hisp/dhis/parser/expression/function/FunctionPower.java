package org.hisp.dhis.parser.expression.function;

import org.hisp.dhis.parser.expression.AbstractVisitor;
import org.hisp.dhis.parser.expression.ExpressionFunction;

import static java.lang.Math.pow;

public class FunctionPower
    implements ExpressionFunction
{
    public Object evaluate( ExprContext ctx,  AbstractVisitor visitor )
    {
        return pow( visitor.castDoubleVisit( ctx.expr( 0 ) ),
            visitor.castDoubleVisit( ctx.expr( 1 ) ) );
    }

    public Object getSql( ExprContext ctx, AbstractVisitor visitor )
    {
        return visitor.castStringVisit( ctx.expr( 0 ) )
            + "^" + visitor.castStringVisit( ctx.expr( 1 ) );
    }
}
