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

import static java.lang.Math.round;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link ItemPeriodInYear}.
 *
 * @author Jim Grace
 */
@ExtendWith( MockitoExtension.class )
class ItemPeriodInYearTest
    extends DhisConvenienceTest
{
    @Mock
    private ExprContext ctx;

    @Mock
    private CommonExpressionVisitor visitor;

    @Mock
    private ExpressionParams params;

    @Mock
    private ExpressionState expressionState;

    @Mock
    private QueryModifiers queryMods;

    private final ItemPeriodInYear target = new ItemPeriodInYear();

    @Test
    void testEvaluateDaily()
    {
        assertEquals( 1, evalWith( "20200101" ) );
        assertEquals( 61, evalWith( "20200301" ) );
        assertEquals( 366, evalWith( "20201231" ) );
        assertEquals( 1, evalWith( "20210101" ) );
        assertEquals( 60, evalWith( "20210301" ) );
        assertEquals( 365, evalWith( "20211231" ) );
    }

    @Test
    void testEvaluateWeekly()
    {
        assertEquals( 1, evalWith( "2020W1" ) );
        assertEquals( 53, evalWith( "2020W53" ) );
        assertEquals( 1, evalWith( "2021W1" ) );
        assertEquals( 52, evalWith( "2021W52" ) );
    }

    @Test
    void testEvaluateWeeklyThursday()
    {
        assertEquals( 1, evalWith( "2020ThuW1" ) );
        assertEquals( 52, evalWith( "2020ThuW52" ) );
        assertEquals( 1, evalWith( "2021ThuW1" ) );
        assertEquals( 52, evalWith( "2021ThuW52" ) );
    }

    @Test
    void testEvaluateBiWeekly()
    {
        assertEquals( 1, evalWith( "2020BiW1" ) );
        assertEquals( 27, evalWith( "2020BiW27" ) );
        assertEquals( 1, evalWith( "2021BiW1" ) );
        assertEquals( 26, evalWith( "2021BiW26" ) );
    }

    @Test
    void testEvaluateMonthly()
    {
        assertEquals( 9, evalWith( "202209" ) );
    }

    @Test
    void testEvaluateBiMonthly()
    {
        assertEquals( 5, evalWith( "202205B" ) );
    }

    @Test
    void testEvaluateQuarterly()
    {
        assertEquals( 1, evalWith( "2022Q1" ) );
    }

    @Test
    void testEvaluateYearly()
    {
        assertEquals( 1, evalWith( "2022" ) );
    }

    @Test
    void testEvaluateYearlyFinancial()
    {
        assertEquals( 1, evalWith( "2022Oct" ) );
    }

    @Test
    void testEvaluateMonthlyWithPeriodOffset()
    {
        assertEquals( 5, evalWith( "202209", -4 ) );
        assertEquals( 1, evalWith( "202209", 4 ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private long evalWith( String period )
    {
        return evalWith( period, 0 );
    }

    private long evalWith( String period, int periodOffset )
    {
        when( visitor.getParams() ).thenReturn( params );
        when( params.getPeriods() ).thenReturn( List.of( createPeriod( period ) ) );
        when( visitor.getState() ).thenReturn( expressionState );

        if ( periodOffset == 0 )
        {
            when( expressionState.getQueryMods() ).thenReturn( null );
        }
        else
        {
            when( expressionState.getQueryMods() ).thenReturn( queryMods );
            when( queryMods.getPeriodOffset() ).thenReturn( periodOffset );
        }

        return round( target.evaluate( ctx, visitor ) );
    }
}
