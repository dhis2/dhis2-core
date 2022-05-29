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
package org.hisp.dhis.program;

import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.program.dataitem.ProgramItemAttribute;
import org.hisp.dhis.program.dataitem.ProgramItemPsEventdate;
import org.hisp.dhis.program.dataitem.ProgramItemStageElement;
import org.hisp.dhis.program.variable.ProgramVariableItem;
import org.hisp.dhis.system.util.ValidationUtils;

/**
 * Program indicator expression item
 * <p/>
 * The only two methods that are used by program indicator-only items are
 * {@link ExpressionItem#getDescription} and {@link ExpressionItem#getSql}.
 * <p/>
 * getDescription checks the expression item syntax, and returns the expected
 * return data type. For data items, it also registers the translation of any
 * UIDs into human-readable object names.
 *
 * @author Jim Grace
 */
public abstract class ProgramExpressionItem
    implements ExpressionItem
{
    @Override
    public final Object getExpressionInfo( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        throw new ParserExceptionWithoutContext(
            "Internal parsing error: getExpressionInfo called for program indicator item " + ctx.getText() );
    }

    @Override
    public final Object evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        throw new ParserExceptionWithoutContext(
            "Internal parsing error: evaluate called for program indicator item " + ctx.getText() );
    }

    /**
     * Get the program expression item that matches the parsed arguments
     *
     * @param ctx the expression context
     * @return the program expression item that can handle the parsed arguments
     */
    protected ProgramExpressionItem getProgramArgType( ExprContext ctx )
    {
        if ( ctx.psEventDate != null )
        {
            return new ProgramItemPsEventdate();
        }

        if ( ctx.uid1 != null )
        {
            return new ProgramItemStageElement();
        }

        if ( ctx.uid0 != null )
        {
            return new ProgramItemAttribute();
        }

        if ( ctx.programVariable() != null )
        {
            return new ProgramVariableItem();
        }

        throw new ParserExceptionWithoutContext( "Illegal argument in program indicator expression: " + ctx.getText() );
    }

    /**
     * Get a null replacement value, but if the type is boolean get a number.
     *
     * @param valueType type to get a null replacement value for
     * @return the replacement value
     */
    protected Object getNullReplacementValue( ValueType valueType )
    {
        return ValidationUtils.getNullReplacementValue( (valueType == BOOLEAN)
            ? NUMBER
            : valueType );
    }

    /**
     * Replace null SQL query values with 0 or '', depending on the value type.
     *
     * @param column the column (may be a subquery)
     * @param valueType the type of value that might be null
     * @return SQL to replace a null value with 0 or '' depending on type
     */
    protected String replaceNullSqlValues( String column, CommonExpressionVisitor visitor, ValueType valueType )
    {
        if ( valueType.isNumeric() || valueType.isBoolean() )
        {
            if ( visitor.getParams().getDataType() == DataType.BOOLEAN )
            {
                return "coalesce(" + column + "::numeric!=0,false)";
            }
            else
            {
                return "coalesce(" + column + "::numeric,0)";
            }
        }

        return "coalesce(" + column + ",'')";
    }
}
