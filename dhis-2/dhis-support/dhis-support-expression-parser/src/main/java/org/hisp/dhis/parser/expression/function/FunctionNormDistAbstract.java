/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.parser.expression.function;

import static java.lang.Double.NaN;
import static java.util.Collections.emptyList;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_DOUBLE_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;

/**
 * Parent class for normalized distribution functions.
 *
 * @author Jim Grace
 */
public abstract class FunctionNormDistAbstract
    implements ExpressionItem
{
    private static final VectorStddevSamp vectorStddevSamp = new VectorStddevSamp();

    private static final VectorAvg vectorAvg = new VectorAvg();

    @Override
    public Object getExpressionInfo( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        // Visit each argument
        ctx.expr().forEach( visitor::visit );

        // If we need to get the average and/or stddev of the first argument,
        // then evaluate it also in the sample context so it will be sampled
        if ( ctx.expr().size() < 3 )
        {
            visitor.visitSamples( ctx.expr( 0 ) );
        }

        return DEFAULT_DOUBLE_VALUE;
    }

    @Override
    public Object evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        ExprContext expr0 = ctx.expr( 0 );

        Double x = visitor.castDoubleVisit( expr0 );

        // If the second argument is specified, use it as the mean of the normal
        // distribution. Otherwise, compute the mean of the first argument.
        Double mean = (ctx.expr().size() > 1)
            ? visitor.castDoubleVisit( ctx.expr( 1 ) )
            : castDouble( vectorAvg.compute( expr0, visitor, emptyList() ) );

        // If the third argument is specified, use it as the standard deviation
        // of the normal distribution. Otherwise, compute the standard deviation
        // of the first argument.
        Double stddev = (ctx.expr().size() > 2)
            ? visitor.castDoubleVisit( ctx.expr( 2 ) )
            : castDouble( vectorStddevSamp.compute( expr0, visitor, emptyList() ) );

        if ( stddev <= 0 )
        {
            return NaN; // stddev <=0 is not allowed, result is undefined
        }

        NormalDistribution dist = new NormalDistribution( mean, stddev );

        return getDistributionValue( dist, x );
    }

    /**
     * Returns the desired measure from the normal distribution. The measure
     * desired is supplied by the subclass.
     *
     * @param dist The normal distribution
     * @param x The value which to evaluate
     * @return The desired result
     */
    protected abstract Double getDistributionValue( NormalDistribution dist, Double x );
}
