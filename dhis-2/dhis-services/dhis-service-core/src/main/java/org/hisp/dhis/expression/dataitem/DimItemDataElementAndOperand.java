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
package org.hisp.dhis.expression.dataitem;

import static org.apache.commons.lang3.ObjectUtils.anyNotNull;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;

/**
 * Expression items DataElement and DataElementOperand
 *
 * @author Jim Grace
 */
public class DimItemDataElementAndOperand
    extends DimensionalItem
{
    @Override
    public DimensionalItemId getDimensionalItemId( ExprContext ctx,
        CommonExpressionVisitor visitor )
    {
        if ( isDataElementOperandSyntax( ctx ) )
        {
            return new DimensionalItemId( DATA_ELEMENT_OPERAND,
                ctx.uid0.getText(),
                ctx.uid1 == null ? null : ctx.uid1.getText(),
                ctx.uid2 == null ? null : ctx.uid2.getText(),
                ctx.getText(), visitor.getState().getQueryMods() );
        }
        else
        {
            return new DimensionalItemId( DATA_ELEMENT,
                ctx.uid0.getText(), null, null,
                ctx.getText(), visitor.getState().getQueryMods() );
        }
    }

    @Override
    public Object getSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        if ( !visitor.getState().isInSubexpression() )
        {
            throw new ParserExceptionWithoutContext(
                "Not valid to generate DataElement or DataElementOperand SQL here: " + ctx.getText() );
        }

        DataElement dataElement = visitor.getIdObjectManager().getNoAcl( DataElement.class, ctx.uid0.getText() );

        if ( dataElement == null )
        {
            throw new ParserExceptionWithoutContext( "DataElement not found: " + ctx.uid0.getText() );
        }

        // Boolean is stored as 1 or 0. Convert to SQL bool in subexpression:
        if ( dataElement.getValueType().isBoolean() )
        {
            return dataElement.getValueColumn() + "::int::bool";
        }

        return dataElement.getValueColumn();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Does an item of the form #{...} have the syntax of a data element operand
     * (as opposed to a data element)?
     *
     * @param ctx the item context
     * @return true if data element operand syntax
     */
    private boolean isDataElementOperandSyntax( ExprContext ctx )
    {
        if ( ctx.uid0 == null )
        {
            throw new ParserExceptionWithoutContext(
                "Data Element or DataElementOperand must have a uid " + ctx.getText() );
        }

        return anyNotNull( ctx.uid1, ctx.uid2 );
    }
}
