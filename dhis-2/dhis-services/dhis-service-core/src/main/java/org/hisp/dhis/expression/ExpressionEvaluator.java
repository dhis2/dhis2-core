package org.hisp.dhis.expression;

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

import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.parser.expression.CommonVisitor;
import org.hisp.dhis.parser.expression.ParserExceptionWithoutContext;

import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;

/**
 * ANTLR parse tree visitor to compute an expression value, once the expression
 * item values have been retrieved from the database.
 * <p/>
 * Uses the ANTLR visitor partern.
 *
 * @author Jim Grace
 */
public class ExpressionEvaluator
    extends CommonVisitor
{
    private Map<String, Double> keyValueMap;

    Map<String, Double> constantMap;

    Map<String, Integer> orgUnitCountMap;

    private int itemsFound = 0;

    private int itemValuesFound = 0;

    private Double days = null;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ExpressionEvaluator( Map<DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap,
        Integer days )
    {
        checkNotNull( valueMap );
        checkNotNull( constantMap );

        keyValueMap = getKeyValueMap( valueMap );

        this.constantMap = constantMap;
        this.orgUnitCountMap = orgUnitCountMap;

        if ( days != null )
        {
            this.days = new Double( days );
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int getItemsFound()
    {
        return itemsFound;
    }

    public int getItemValuesFound()
    {
        return itemValuesFound;
    }

    // -------------------------------------------------------------------------
    // Visitor methods
    // -------------------------------------------------------------------------

    @Override
    protected Object andOperator( ExprContext ctx )
    {
        return castBooleanVisit( ctx.expr( 0 ) )
            && castBooleanVisit( ctx.expr( 1 ) );
    }

    @Override
    protected Object orOperator( ExprContext ctx )
    {
        return castBooleanVisit( ctx.expr( 0 ) )
            || castBooleanVisit( ctx.expr( 1 ) );
    }

    @Override
    protected Object ifFunction( FunctionContext ctx )
    {
        return castBooleanVisit( ctx.expr( 0 ) )
            ? visit( ctx.expr( 1 ) )
            : visit( ctx.expr( 2 ) );
    }

    @Override
    public Object visitItem( ItemContext ctx )
    {
        switch ( ctx.it.getType() )
        {
            case HASH_BRACE:
                if ( isDataElementOperandSyntax( ctx ) )
                {
                    return getItemValue(
                        ctx.uid0.getText() + "." +
                            ( ctx.uid1 == null ? "*" : ctx.uid1.getText() ) +
                            ( ctx.uid2 == null ? "" : "." + ctx.uid2.getText() ) );
                }
                else // Data element:
                {
                    return getItemValue(
                        ctx.uid0.getText() );
                }

            case A_BRACE:
                if ( !isExpressionProgramAttribute( ctx ) )
                {
                    throw new ParserExceptionWithoutContext( "Program attribute must have two UIDs: " + ctx.getText() );
                }
                // Fall through
            case D_BRACE:
                return getItemValue(
                    ctx.uid0.getText() + "." +
                        ctx.uid1.getText() );

            case C_BRACE:
                return getConstant( ctx );

            case I_BRACE:
                return getItemValue(
                    ctx.uid0.getText() );

            case N_BRACE:
                return getItemValue( ctx.uid0.getText() );

            case OUG_BRACE:
                return getOrgUnitGroupCount( ctx );

            case R_BRACE:
                return getItemValue(
                    ctx.uid0.getText() + "." +
                        ctx.REPORTING_RATE_TYPE().getText() );

            case DAYS:
                return days;

            default:
                throw new ParserExceptionWithoutContext( "Item not recognized for this type of expression: " + ctx.getText() );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * From the initial valueMap containing expression items with full
     * DimensionalItemObjects, makes a hash map that can be used for fast
     * lookup from the identifier found in the expression. This allows the
     * DimensionalItemObject to be identified from the expression without
     * the overhead of calling the IdentifiableObjectManager.
     *
     * @param valueMap the given valueMap.
     */
    private Map<String, Double> getKeyValueMap( Map<DimensionalItemObject, Double> valueMap )
    {
        return valueMap.entrySet().stream().collect(
            Collectors.toMap( e -> e.getKey().getDimensionItem(), e -> e.getValue() ) );
    }

    /**
     * Gets an expression item's value from the keyValueMap.
     * <p/>
     * If we should replace nulls with the default value, then do so, and
     * remember how many items found, and how many of them had values, for
     * subsequent MissingValueStrategy analysis.
     * <p/>
     * If we should not replace nulls with the default value, then don't,
     * as this is likely for some function that is testing for nulls, and
     * a missing value should not count towards the MissingValueStrategy.
     *
     * @param itemId the DimensionalItemObject id.
     * @return the item's value.
     */
    private Double getItemValue( String itemId )
    {
        Double value = keyValueMap.get( itemId );

        if ( replaceNulls )
        {
            itemsFound++;

            if ( value == null )
            {
                value = DOUBLE_VALUE_IF_NULL;
            }
            else
            {
                itemValuesFound++;
            }
        }

        return value;
    }

    private Double getConstant( ItemContext ctx )
    {
        Double value = constantMap.get( ctx.uid0.getText() );

        if ( value == null ) // Shouldn't happen for a valid expression.
        {
            throw new ParserExceptionWithoutContext( "Can't find constant " + ctx.getText() );
        }

        return value;
    }

    private Double getOrgUnitGroupCount( ItemContext ctx )
    {
        if ( orgUnitCountMap == null )
        {
            return 0d;
        }

        Integer count = orgUnitCountMap.get( ctx.uid0.getText() );

        if ( count == null ) // Shouldn't happen for a valid expression.
        {
            throw new ParserExceptionWithoutContext( "Can't find count for organisation unit " + ctx.getText() );
        }

        return count.doubleValue();
    }
}
