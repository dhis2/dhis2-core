package org.hisp.dhis.parser.expression.function;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.period.Period;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

/**
 * Aggregates a vector of samples (base class).
 *
 * @author Jim Grace
 */
public abstract class VectorFunction
    implements ExpressionItem
{
    @Override
    public Object getItemId( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        // ItemIds in all but last expr (if any) are from current period.

        for ( int i = 0; i < ctx.expr().size() - 1; i++ )
        {
            visitor.visitExpr( ctx.expr().get( i ) );
        }

        // ItemIds in the last (or only) expr are from sampled periods.

        Set<DimensionalItemId> savedItemIds = visitor.getItemIds();
        visitor.setItemIds( visitor.getSampleItemIds() );

        Object result = visitor.visitExpr( ctx.expr().get( ctx.expr().size() - 1 ) );

        visitor.setItemIds( savedItemIds );

        return castDouble( result );
    }

    @Override
    public Object evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        List<Double> args = new ArrayList<>();

        // All but last expr (if any) are from current period.

        for ( int i = 0; i < ctx.expr().size() - 1; i++ )
        {
            args.add( castDouble( visitor.visitExpr( ctx.expr().get( i ) ) ) );
        }

        // Last (or only) expr is from sampled periods.

        ExprContext lastExpr = ctx.expr().get( ctx.expr().size() - 1 );

        List<Double> values = getSampleValues( lastExpr, visitor );

        return vectorHandleNulls( aggregate( values, args ), visitor );
    }

    /**
     * By default, if there is a null value, count it as a value found for
     * the purpose of missingValueStrategy. This can be overridden by a
     * vector function (like count) that returns a non-null value (0) if
     * no actual values are found.
     *
     * @param value   the value to count (might be null)
     * @param visitor the tree visitor
     * @return the value to return (null might be replaced)
     */
    public Object vectorHandleNulls( Object value, CommonExpressionVisitor visitor )
    {
        return visitor.handleNulls( value );
    }

    /**
     * Aggregates the values, using arguments (if any)
     *
     * @param args the values to aggregate.
     * @param args the arguments (if any) for aggregating the values.
     * @return the aggregated value.
     */
    public abstract Object aggregate( List<Double> values, List<Double> args );

    /**
     * Gets a list of sample values to aggregate.
     *
     * @param ctx the sample expression context
     * @param visitor the tree visitor
     * @return the list of sample values
     *
     * The missingValueStrategy is handled as follows: for each sample expression
     * inside the aggregation function, if there are any sample values missing
     * and the strategy is SKIP_IF_ANY_VALUE_MISSING, then that sample is skipped.
     * Also if all the values are missing and the strategy is
     * SKIP_IF_ALL_VALUES_MISSING, then that sample is skipped.
     *
     * Finally, if there were any items in the sample expression, the count of
     * items in the main expression is incremented. And if there was at least
     * one sample value, the count of item values in the main expression is
     * incremented. This means that if the vector is empty, it counts as a
     * missing value in the main expression.
     */
    private List<Double> getSampleValues( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        int savedItemsFound = visitor.getItemsFound();
        int savedItemValuesFound = visitor.getItemValuesFound();
        Map<String, Double> savedItemValueMap = visitor.getItemValueMap();

        List<Double> values = new ArrayList<>();

        for ( Period p : visitor.getSamplePeriods() )
        {
            visitor.setItemsFound( 0 );
            visitor.setItemValuesFound( 0 );

            if ( visitor.getPeriodItemValueMap() != null &&
                visitor.getPeriodItemValueMap().get( p ) != null )
            {
                visitor.setItemValueMap( visitor.getPeriodItemValueMap().get( p ) );
            }
            else // No samples found in this period:
            {
                visitor.setItemValueMap( new HashMap<>() );
            }

            Double value = castDouble( visitor.visit( ctx ) );

            if ( ( visitor.getMissingValueStrategy() == SKIP_IF_ANY_VALUE_MISSING && visitor.getItemValuesFound() < visitor.getItemsFound() )
                || ( visitor.getMissingValueStrategy() == SKIP_IF_ALL_VALUES_MISSING && visitor.getItemsFound() != 0 && visitor.getItemValuesFound() == 0 ) )
            {
                value = null;
            }

            if ( value != null )
            {
                values.add( value );
            }
        }

        if ( visitor.getItemsFound() > 0 )
        {
            savedItemsFound++;

            if ( !values.isEmpty() )
            {
                savedItemValuesFound++;
            }
        }

        visitor.setItemsFound( savedItemsFound );
        visitor.setItemValuesFound( savedItemValuesFound );
        visitor.setItemValueMap( savedItemValueMap );

        return values;
    }
}
