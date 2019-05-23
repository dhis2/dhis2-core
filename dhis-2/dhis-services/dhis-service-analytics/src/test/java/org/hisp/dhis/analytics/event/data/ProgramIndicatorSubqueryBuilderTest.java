/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.event.data;

import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Luciano Fiandesio
 */
public class ProgramIndicatorSubqueryBuilderTest
{
    private final static String DUMMY_EXPRESSION = "#{1234567}";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ProgramIndicatorService programIndicatorService;

    private Program program;

    private Date startDate;

    private Date endDate;

    private DefaultProgramIndicatorSubqueryBuilder subject;

    @Before
    public void setUp()
    {
        program = createProgram( 'A' );
        startDate = getDate( 2018, 1, 1 );
        endDate = getDate( 2018, 6, 30 );
        subject = new DefaultProgramIndicatorSubqueryBuilder( programIndicatorService );
    }

    @Test
    public void t1()
    {
        ProgramIndicator pi = createProgramIndicator( 'A', program, DUMMY_EXPRESSION, "" );

        when( programIndicatorService.getAnalyticsSql( DUMMY_EXPRESSION, pi, startDate, endDate ) )
            .thenReturn( "distinct psi" );

        String sql = subject.getAggregateClauseForProgramIndicator( pi, AnalyticsType.ENROLLMENT, startDate, endDate );

        assertThat(sql, is("(SELECT avg (distinct psi) FROM analytics_event_" + program.getUid() +" WHERE pi = ax.pi)"));
    }

    @Test
    public void t2()
    {
        ProgramIndicator pi = createProgramIndicator( 'A', program, DUMMY_EXPRESSION, "" );
        pi.setAnalyticsType(AnalyticsType.EVENT);

        when( programIndicatorService.getAnalyticsSql( DUMMY_EXPRESSION, pi, startDate, endDate ) )
                .thenReturn( "distinct psi" );

        String sql = subject.getAggregateClauseForProgramIndicator( pi, AnalyticsType.EVENT, startDate, endDate );

        assertThat(sql, is("(SELECT avg (distinct psi) FROM analytics_event_" + program.getUid() +" WHERE psi = ax.psi)"));
    }
}