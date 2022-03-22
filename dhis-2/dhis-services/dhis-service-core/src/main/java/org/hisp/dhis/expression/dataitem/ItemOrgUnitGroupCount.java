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

import static org.hisp.dhis.parser.expression.ParserUtils.DOUBLE_VALUE_IF_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;

/**
 * Expression item OrganisationUnitGroup
 *
 * @author Jim Grace
 */
public class ItemOrgUnitGroupCount
    implements ExpressionItem
{
    @Override
    public Object getDescription( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        OrganisationUnitGroup orgUnitGroup = visitor.getIdObjectManager()
            .get( OrganisationUnitGroup.class, ctx.uid0.getText() );

        if ( orgUnitGroup == null )
        {
            throw new ParserExceptionWithoutContext( "No organization unit group defined for " + ctx.uid0.getText() );
        }

        visitor.getItemDescriptions().put( ctx.getText(), orgUnitGroup.getDisplayName() );

        return DOUBLE_VALUE_IF_NULL;
    }

    @Override
    public Object getExpressionInfo( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        visitor.getInfo().getOrgUnitGroupCountIds().add( ctx.uid0.getText() );

        return DOUBLE_VALUE_IF_NULL;
    }

    @Override
    public Object evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        Integer count = visitor.getParams().getOrgUnitCountMap().get( ctx.uid0.getText() );

        if ( count == null ) // Shouldn't happen for a valid expression.
        {
            throw new ParserExceptionWithoutContext( "Can't find count for organisation unit " + ctx.uid0.getText() );
        }

        return count.doubleValue();
    }
}
