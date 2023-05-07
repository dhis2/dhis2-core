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
package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createPeriod;
import static org.hisp.dhis.analytics.AggregationType.MIN;
import static org.hisp.dhis.analytics.AnalyticsTableType.DATA_VALUE;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.subexpression.SubexpressionDimensionItem.getItemColumnName;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.regex.Pattern;

import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.subexpression.SubexpressionDimensionItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Jim Grace
 */
@ExtendWith( MockitoExtension.class )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
class JdbcSubexpressionQueryGeneratorTest
{
    @Mock
    private PartitionManager partitionManager;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcAnalyticsManager jam;

    @Mock
    private ExecutionPlanStore executionPlanStore;

    /**
     * Matches a UID with an initial single quote.
     */
    private final static Pattern QUOTED_UID = Pattern.compile( "'\\w{11}'" );

    @BeforeAll
    public void setUp()
    {
        QueryPlanner queryPlanner = new DefaultQueryPlanner( partitionManager );

        jam = new JdbcAnalyticsManager( queryPlanner, jdbcTemplate, executionPlanStore );
    }

    @Test
    void testGetSql()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );

        Period peA = createPeriod( "202305" );

        QueryModifiers queryModsMin = QueryModifiers.builder().aggregationType( MIN ).build();

        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        DataElement deC = createDataElement( 'C' );
        DataElement deD = createDataElement( 'D' );
        DataElement deE = createDataElement( 'E' );

        deE.setQueryMods( queryModsMin );

        CategoryOptionCombo cocA = createCategoryOptionCombo( 'A' );
        CategoryOptionCombo cocB = createCategoryOptionCombo( 'B' );
        CategoryOptionCombo cocC = createCategoryOptionCombo( 'C' );
        CategoryOptionCombo cocD = createCategoryOptionCombo( 'D' );

        DataElementOperand deoA = new DataElementOperand( deB, cocA );
        DataElementOperand deoB = new DataElementOperand( deC, cocB, cocC );
        DataElementOperand deoC = new DataElementOperand( deD, null, cocD );

        List<DimensionalItemObject> items = List.of( deA, deoA, deoB, deoC, deE );

        String deACol = getItemColumnName( deA.getUid(), null, null, null );
        String deoACol = getItemColumnName( deB.getUid(), cocA.getUid(), null, null );
        String deoBCol = getItemColumnName( deC.getUid(), cocB.getUid(), cocC.getUid(), null );
        String deoCCol = getItemColumnName( deD.getUid(), null, cocD.getUid(), null );
        String deECol = getItemColumnName( deD.getUid(), null, null, queryModsMin );

        String subexSql = deACol + "*(" + deoACol + "+" + deoBCol + ")+" + deoCCol + "-" + deECol;

        SubexpressionDimensionItem subex = new SubexpressionDimensionItem( subexSql, items, null );

        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataType( DataType.NUMERIC )
            .withTableName( "analytics" )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( subex ) ) )
            .addFilter( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA ) ) ).build();

        JdbcSubexpressionQueryGenerator target = new JdbcSubexpressionQueryGenerator( jam, params, DATA_VALUE );

        String expected = "select ax.\"pe\",'subexprxUID' as \"dx\"," +
            "sum(\"deabcdefghA\"*(\"deabcdefghB_cuabcdefghA\"+\"deabcdefghC_cuabcdefghB_cuabcdefghC\")+\"deabcdefghD__cuabcdefghD\"-\"deabcdefghDMIN\") as \"value\" "
            +
            "from (select ax.\"pe\", " +
            "sum(case when ax.\"dx\"='deabcdefghA' then \"value\" else null end) as \"deabcdefghA\"," +
            "sum(case when ax.\"dx\"='deabcdefghB' and ax.\"co\"='cuabcdefghA' and ax.\"ao\"='' then \"value\" else null end) as \"deabcdefghB_cuabcdefghA\","
            +
            "sum(case when ax.\"dx\"='deabcdefghC' and ax.\"co\"='cuabcdefghB' and ax.\"ao\"='cuabcdefghC' then \"value\" else null end) as \"deabcdefghC_cuabcdefghB_cuabcdefghC\","
            +
            "sum(case when ax.\"dx\"='deabcdefghD' and ax.\"co\"='' and ax.\"ao\"='cuabcdefghD' then \"value\" else null end) as \"deabcdefghD__cuabcdefghD\","
            +
            "min(case when ax.\"dx\"='deabcdefghE' then \"value\" else null end) as \"deabcdefghEMIN\" " +
            "from analytics as ax " +
            "where ax.\"pe\" in ('202305') " +
            "and ( ax.\"ou\" in ('ouabcdefghA') ) " +
            "and ax.\"dx\" in ('deabcdefghA','deabcdefghB','deabcdefghC','deabcdefghD','deabcdefghE')  " +
            "group by ax.\"pe\",ax.\"ou\") as ax " +
            "where \"deabcdefghA\"*(\"deabcdefghB_cuabcdefghA\"+\"deabcdefghC_cuabcdefghB_cuabcdefghC\")+\"deabcdefghD__cuabcdefghD\"-\"deabcdefghDMIN\" is not null "
            +
            "group by ax.\"pe\" ";

        String actual = anonymize( target.getSql() );

        assertEquals( expected, actual );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * The first UID in the generated query is randomly-generated for the
     * subexpression. Replace this with a known label so se can compare.
     */
    private String anonymize( String sql )
    {
        return QUOTED_UID.matcher( sql ).replaceFirst( "'subexprxUID'" );
    }
}
