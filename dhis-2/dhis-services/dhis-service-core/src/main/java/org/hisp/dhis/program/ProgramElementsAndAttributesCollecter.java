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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.*;

import java.util.Set;

import org.hisp.dhis.parser.expression.antlr.ExpressionBaseListener;

/**
 * Traverse the ANTLR4 parse tree for a program expression to collect the UIDs
 * for data elements and program attributes.
 * <p/>
 * Uses the ANTLR4 listener pattern.
 *
 * @author Jim Grace
 */
public class ProgramElementsAndAttributesCollecter
    extends ExpressionBaseListener
{
    private Set<String> items;

    private AnalyticsType analyticsType;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ProgramElementsAndAttributesCollecter( Set<String> items, AnalyticsType analyticsType )
    {
        checkNotNull( items );
        checkNotNull( analyticsType );

        this.items = items;
        this.analyticsType = analyticsType;
    }

    // -------------------------------------------------------------------------
    // ANTLR Listener methods
    // -------------------------------------------------------------------------

    @Override
    public void enterExpr( ExprContext ctx )
    {
        if ( ctx.it == null )
        {
            return;
        }

        switch ( ctx.it.getType() )
        {
            case HASH_BRACE:
                assumeStageElementSyntax( ctx );

                String programStageId = ctx.uid0.getText();
                String dataElementId = ctx.uid1.getText();

                if ( AnalyticsType.ENROLLMENT.equals( analyticsType ) )
                {
                    items.add( programStageId + "_" + dataElementId );
                }
                else
                {
                    items.add( dataElementId );
                }
                break;

            case A_BRACE:
                assumeProgramExpressionProgramAttribute( ctx );

                String attributeId = ctx.uid0.getText();

                items.add( attributeId );

                break;

            default:
                break;
        }
    }
}
