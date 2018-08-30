package org.hisp.dhis.parsing;

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

import java.util.Map;

import static org.hisp.dhis.parsing.generated.ExpressionParser.*;

/**
 * Evaluates expressions using the ANTLR parser, using the ANTLR visitor pattern.
 *
 * @author Jim Grace
 */
public class ExpressionEvaluator extends ExpressionChecker
{
    private Map<String, Double> valueMap;

    private Map<String, Double> constantMap;

    //TODO: This goes away when we finish the TODOs.
    private final static Double PLACEHOLDER = Double.valueOf( 2. );

    public ExpressionEvaluator( Map<String, Double> valueMap, Map<String, Double> constantMap )
    {
        this.valueMap = valueMap;
        this.constantMap = constantMap;
    }

    @Override
    public Object visitConstant( ConstantContext ctx )
    {
        return constantMap.get( ctx.getToken( UID, 0 ).getText() );
    }

    @Override
    public Object visitOrgUnitCount( OrgUnitCountContext ctx )
    {
        //TODO: write the real OrgUnitCount code
        return PLACEHOLDER;
    }

    @Override
    public Object visitReportingRate( ReportingRateContext ctx )
    {
        //TODO: write the real ReportingRate code
        return PLACEHOLDER;
    }

    @Override
    public Object visitDays( DaysContext ctx )
    {
        //TODO: write the real Days-in-the-month code
        return 31.;
    }

    @Override
    protected Object functionAnd( ExprContext ctx )
    {
        return castBoolean( visit( ctx.expr( 0 ) ) )
            && castBoolean( visit( ctx.expr( 1 ) ) );
    }

    @Override
    protected Object functionOr( ExprContext ctx )
    {
        return castBoolean( visit( ctx.expr( 0 ) ) )
            || castBoolean( visit( ctx.expr( 1 ) ) );
    }

    @Override
    protected Object functionIf( ExprContext ctx )
    {
        return castBoolean( visit( ctx.a3().expr( 0 ) ) )
            ? visit( ctx.a3().expr( 1 ) )
            : visit( ctx.a3().expr( 2 ) );
    }

    @Override
    protected Object functionCoalesce( ExprContext ctx )
    {
        for ( ExprContext c : ctx.a1_n().expr() )
        {
            Object val = visit( c );
            if ( val != null )
            {
                return val;
            }
        }
        return null;
    }
}
