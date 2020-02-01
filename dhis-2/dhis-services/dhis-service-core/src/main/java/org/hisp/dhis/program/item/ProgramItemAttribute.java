package org.hisp.dhis.program.item;

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

import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ParserExceptionWithoutContext;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ItemContext;

/**
 * Program indicator expression item ProgramAttribute
 *
 * @author Jim Grace
 */
public class ProgramItemAttribute
    extends ProgramItem
{
    @Override
    public Object getDescription( ItemContext ctx, CommonExpressionVisitor visitor )
    {
        assumeProgramExpressionProgramAttribute( ctx );

        String attributeId = ctx.uid0.getText();

        TrackedEntityAttribute attribute = visitor.getAttributeService().getTrackedEntityAttribute( attributeId );

        if ( attribute == null )
        {
            throw new ParserExceptionWithoutContext( "Tracked entity attribute " + attributeId + " not found." );
        }

        visitor.getItemDescriptions().put( ctx.getText(), attribute.getDisplayName() );

        return ValidationUtils.getSubstitutionValue( attribute.getValueType() );
    }

    @Override
    public Object getSql( ItemContext ctx, CommonExpressionVisitor visitor )
    {
        assumeProgramExpressionProgramAttribute( ctx );

        String attributeId = ctx.uid0.getText();

        String column = visitor.getStatementBuilder().columnQuote( attributeId );

        if ( visitor.getReplaceNulls() )
        {
            TrackedEntityAttribute attribute = visitor.getAttributeService().getTrackedEntityAttribute( attributeId );

            if ( attribute == null )
            {
                throw new ParserExceptionWithoutContext( "Tracked entity attribute " + attributeId + " not found during SQL generation." );
            }

            column = replaceNullValues( column, attribute.getValueType() );
        }

        return column;
    }
}
