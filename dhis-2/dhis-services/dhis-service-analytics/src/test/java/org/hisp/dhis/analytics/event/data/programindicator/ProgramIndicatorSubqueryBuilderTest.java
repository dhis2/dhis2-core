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
package org.hisp.dhis.analytics.event.data.programindicator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramIndicator;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class ProgramIndicatorSubqueryBuilderTest
{
    private final static String DUMMY_EXPRESSION = "#{1234567}";

    private final static String DUMMY_FILTER_EXPRESSION = "#{1234567.filter}";

    private final static BeanRandomizer rnd = BeanRandomizer.create();

    @Mock
    private ProgramIndicatorService programIndicatorService;

    private Program program;

    private Date startDate;

    private Date endDate;

    private DefaultProgramIndicatorSubqueryBuilder subject;

    @BeforeEach
    public void setUp()
    {
        program = createProgram( 'A' );
        startDate = getDate( 2018, 1, 1 );
        endDate = getDate( 2018, 6, 30 );
        subject = new DefaultProgramIndicatorSubqueryBuilder( programIndicatorService );
    }

    @Test
    void verifyProgramIndicatorSubQueryWithAliasTable()
    {
        ProgramIndicator pi = createProgramIndicator( 'A', program, DUMMY_EXPRESSION, "" );

        when( programIndicatorService.getAnalyticsSql( DUMMY_EXPRESSION, NUMERIC, pi, startDate, endDate, "subax" ) )
            .thenReturn( "distinct psi" );

        String sql = subject.getAggregateClauseForProgramIndicator( pi, AnalyticsType.ENROLLMENT, startDate, endDate );

        assertThat( sql, is( "(SELECT avg (distinct psi) FROM analytics_event_" + program.getUid().toLowerCase()
            + " as subax WHERE pi = ax.pi)" ) );
    }

    /**
     * This tests also verify that the join after WHERE is changing when outer
     * join is type EVENT
     */
    @Test
    void verifyProgramIndicatorWithoutAggregationTypeReturnsAvg()
    {
        ProgramIndicator pi = createProgramIndicator( 'A', program, DUMMY_EXPRESSION, "" );
        pi.setAggregationType( null );

        when( programIndicatorService.getAnalyticsSql( DUMMY_EXPRESSION, NUMERIC, pi, startDate, endDate, "subax" ) )
            .thenReturn( "distinct psi" );

        String sql = subject.getAggregateClauseForProgramIndicator( pi, AnalyticsType.EVENT, startDate, endDate );

        assertThat( sql, is( "(SELECT avg (distinct psi) FROM analytics_event_" + program.getUid().toLowerCase()
            + " as subax WHERE psi = ax.psi)" ) );
    }

    @Test
    void verifyJoinWhenOuterQueryIsEnrollment()
    {
        ProgramIndicator pi = createProgramIndicator( 'A', program, DUMMY_EXPRESSION, "" );

        when( programIndicatorService.getAnalyticsSql( DUMMY_EXPRESSION, NUMERIC, pi, startDate, endDate, "subax" ) )
            .thenReturn( "distinct psi" );

        String sql = subject.getAggregateClauseForProgramIndicator( pi, AnalyticsType.ENROLLMENT, startDate, endDate );

        assertThat( sql, is( "(SELECT avg (distinct psi) FROM analytics_event_" + program.getUid().toLowerCase()
            + " as subax WHERE pi = ax.pi)" ) );
    }

    @Test
    void verifyJoinWhenRelationshipTypeIsPresent()
    {
        ProgramIndicator pi = createProgramIndicator( 'A', program, DUMMY_EXPRESSION, "" );

        // Create a TEI to TEI relationship
        RelationshipType relationshipType = rnd.nextObject( RelationshipType.class );
        relationshipType.getFromConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipType.getToConstraint().setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );

        when( programIndicatorService.getAnalyticsSql( DUMMY_EXPRESSION, NUMERIC, pi, startDate, endDate, "subax" ) )
            .thenReturn( "distinct psi" );

        String sql = subject.getAggregateClauseForProgramIndicator( pi, relationshipType, AnalyticsType.ENROLLMENT,
            startDate, endDate );

        assertThat( sql, is( "(SELECT avg (distinct psi) FROM analytics_event_" + program.getUid().toLowerCase()
            + " as subax WHERE  subax.tei in (select tei.uid from trackedentityinstance tei " +
            "LEFT JOIN relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid  " +
            "LEFT JOIN relationship r on r.from_relationshipitemid = ri.relationshipitemid " +
            "LEFT JOIN relationshipitem ri2 on r.to_relationshipitemid = ri2.relationshipitemid " +
            "LEFT JOIN relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid " +
            "LEFT JOIN trackedentityinstance tei on tei.trackedentityinstanceid = ri2.trackedentityinstanceid " +
            "WHERE rty.relationshiptypeid = " + relationshipType.getId() + " AND tei.uid = ax.tei ))" ) );
    }

    @Test
    void verifyProgramIndicatorWithFilter()
    {
        ProgramIndicator pi = createProgramIndicator( 'A', program, DUMMY_EXPRESSION, "" );
        pi.setFilter( DUMMY_FILTER_EXPRESSION );

        when( programIndicatorService.getAnalyticsSql( DUMMY_EXPRESSION, NUMERIC, pi, startDate, endDate,
            "subax" ) ).thenReturn( "distinct psi" );
        when( programIndicatorService.getAnalyticsSql( DUMMY_FILTER_EXPRESSION, BOOLEAN, pi, startDate, endDate,
            "subax" ) ).thenReturn( "a = b" );

        String sql = subject.getAggregateClauseForProgramIndicator( pi, AnalyticsType.ENROLLMENT, startDate, endDate );

        assertThat( sql, is( "(SELECT avg (distinct psi) FROM analytics_event_" + program.getUid().toLowerCase()
            + " as subax WHERE pi = ax.pi AND a = b)" ) );
    }

}