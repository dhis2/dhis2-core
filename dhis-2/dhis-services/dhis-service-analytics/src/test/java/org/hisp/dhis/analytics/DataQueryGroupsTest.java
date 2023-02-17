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
package org.hisp.dhis.analytics;

import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DataQueryGroupsTest extends DhisConvenienceTest
{
    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private DataElement deE;

    private DataElement deF;

    private DataElement deG;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private OrganisationUnit ouD;

    private OrganisationUnit ouE;

    @BeforeEach
    void before()
    {
        // INTEGER, SUM
        deA = createDataElement( 'A', new CategoryCombo() );
        deB = createDataElement( 'B', new CategoryCombo() );
        deC = createDataElement( 'C', new CategoryCombo() );
        deD = createDataElement( 'D', new CategoryCombo() );
        deE = createDataElement( 'E', new CategoryCombo() );
        deF = createDataElement( 'F', new CategoryCombo() );
        deG = createDataElement( 'G', new CategoryCombo() );
        deF.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );
        deG.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        ouD = createOrganisationUnit( 'D' );
        ouE = createOrganisationUnit( 'E' );
    }

    @Test
    void planQueryA()
    {
        DataQueryParams paramsA = DataQueryParams.newBuilder()
            .withDataElements( getList( deA, deB ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( createPeriods( "2000Q1", "2000Q2", "2000Q3", "2000Q4", "2001Q1", "2001Q2" ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .build();
        DataQueryParams paramsB = DataQueryParams.newBuilder()
            .withDataElements( getList( deC, deD ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( createPeriods( "2000Q1", "2000Q2", "2000Q3", "2000Q4", "2001Q1", "2001Q2" ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .build();
        DataQueryParams paramsC = DataQueryParams.newBuilder()
            .withDataElements( getList( deE ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( createPeriods( "2000Q1", "2000Q2", "2000Q3", "2000Q4", "2001Q1", "2001Q2" ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .build();
        DataQueryParams paramsD = DataQueryParams.newBuilder()
            .withDataElements( getList( deF, deG ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( createPeriods( "2000Q1", "2000Q2", "2000Q3", "2000Q4", "2001Q1", "2001Q2" ) )
            .withAggregationType( AnalyticsAggregationType.AVERAGE )
            .build();

        List<DataQueryParams> queries = new ArrayList<>();
        queries.add( paramsA );
        queries.add( paramsB );
        queries.add( paramsC );
        queries.add( paramsD );

        DataQueryGroups queryGroups = DataQueryGroups.newBuilder().withQueries( queries ).build();
        assertEquals( 2, queryGroups.getSequentialQueries().size() );
        assertEquals( 4, queryGroups.getAllQueries().size() );
        assertEquals( 3, queryGroups.getLargestGroupSize() );
        assertTrue( queryGroups.isOptimal( 3 ) );
        assertTrue( queryGroups.isOptimal( 2 ) );
        assertFalse( queryGroups.isOptimal( 4 ) );
    }

    @Test
    void getQueryA()
    {
        DimensionalObject dimA = new BaseDimensionalObject(
            DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, List.of( deA, deB ) );
        DimensionalObject dimB = new BaseDimensionalObject(
            DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, List.of( ouA, ouB, ouC ) );
        DimensionalObject dimC = new BaseDimensionalObject(
            DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, createPeriods( "2000Q1" ) );

        DataQueryParams paramsA = DataQueryParams.newBuilder()
            .addDimension( dimA )
            .addDimension( dimB )
            .addFilter( dimC ).build();

        assertNotNull( paramsA.getDimension( DimensionalObject.DATA_X_DIM_ID ) );
        assertNotNull( paramsA.getDimension( DimensionalObject.ORGUNIT_DIM_ID ) );
        assertNotNull( paramsA.getFilter( DimensionalObject.PERIOD_DIM_ID ) );
        assertEquals( 2, paramsA.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        assertEquals( 3, paramsA.getDimension( DimensionalObject.ORGUNIT_DIM_ID ).getItems().size() );
        assertEquals( 1, paramsA.getFilter( DimensionalObject.PERIOD_DIM_ID ).getItems().size() );
    }
}
