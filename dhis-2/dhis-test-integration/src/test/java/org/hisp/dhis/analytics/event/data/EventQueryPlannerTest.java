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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class EventQueryPlannerTest extends NonTransactionalIntegrationTest
{

    private Program prA;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private ProgramDataElementDimensionItem pdeA;

    private ProgramDataElementDimensionItem pdeB;

    private ProgramDataElementDimensionItem pdeC;

    private ProgramDataElementDimensionItem pdeD;

    private TrackedEntityAttribute atA;

    private TrackedEntityAttribute atB;

    private ProgramTrackedEntityAttributeDimensionItem patA;

    private ProgramTrackedEntityAttributeDimensionItem patB;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private LegendSet lsA;

    private OptionSet osA;

    @Autowired
    private EventQueryPlanner queryPlanner;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Override
    public void setUpTest()
    {
        prA = createProgram( 'A' );
        prA.setUid( "programuida" );
        idObjectManager.save( prA );
        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER );
        deC = createDataElement( 'C', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT,
            DataElementDomain.TRACKER );
        deD = createDataElement( 'D', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT,
            DataElementDomain.TRACKER );
        idObjectManager.save( deA );
        idObjectManager.save( deB );
        idObjectManager.save( deC );
        idObjectManager.save( deD );
        pdeA = new ProgramDataElementDimensionItem( prA, deA );
        pdeB = new ProgramDataElementDimensionItem( prA, deB );
        pdeC = new ProgramDataElementDimensionItem( prA, deC );
        pdeD = new ProgramDataElementDimensionItem( prA, deD );
        atA = createTrackedEntityAttribute( 'A' );
        atB = createTrackedEntityAttribute( 'B' );
        idObjectManager.save( atA );
        idObjectManager.save( atB );
        patA = new ProgramTrackedEntityAttributeDimensionItem( prA, atA );
        patB = new ProgramTrackedEntityAttributeDimensionItem( prA, atB );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C', ouA );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        lsA = createLegendSet( 'A' );
        idObjectManager.save( lsA );
        osA = new OptionSet( "OptionSetA", ValueType.TEXT );
        idObjectManager.save( osA );
    }

    @Test
    void testPlanAggregateQueryA()
    {
        EventQueryParams params = new EventQueryParams.Builder().withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        assertEquals( 1, queries.size() );
        EventQueryParams query = queries.get( 0 );
        assertEquals( new DateTime( 2010, 6, 1, 0, 0 ).toDate(), query.getStartDate() );
        assertEquals( new DateTime( 2012, 3, 20, 0, 0 ).toDate(), query.getEndDate() );
        Partitions partitions = query.getPartitions();
        Partitions expected = new Partitions( Sets.newHashSet( 2010, 2011, 2012 ) );
        assertEquals( 3, partitions.getPartitions().size() );
        assertEquals( expected, partitions );
        assertEquals( PartitionUtils.getTableName( AnalyticsTableType.EVENT.getTableName(), prA ),
            query.getTableName() );
    }

    @Test
    void testPlanAggregateQueryB()
    {
        EventQueryParams params = new EventQueryParams.Builder().withProgram( prA )
            .withStartDate( new DateTime( 2010, 3, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2010, 9, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        assertEquals( 1, queries.size() );
        EventQueryParams query = queries.get( 0 );
        assertEquals( new DateTime( 2010, 3, 1, 0, 0 ).toDate(), query.getStartDate() );
        assertEquals( new DateTime( 2010, 9, 20, 0, 0 ).toDate(), query.getEndDate() );
        Partitions partitions = query.getPartitions();
        Partitions expected = new Partitions( Sets.newHashSet( 2010 ) );
        assertEquals( 1, partitions.getPartitions().size() );
        assertEquals( expected, partitions );
        assertEquals( PartitionUtils.getTableName( AnalyticsTableType.EVENT.getTableName(), prA ),
            query.getTableName() );
    }

    @Test
    void testPlanAggregateQueryC()
    {
        EventQueryParams params = new EventQueryParams.Builder().withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA, ouB ) ).build();
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        assertEquals( 2, queries.size() );
        assertEquals( ouA, queries.get( 0 ).getOrganisationUnits().get( 0 ) );
        assertEquals( ouB, queries.get( 1 ).getOrganisationUnits().get( 0 ) );
        EventQueryParams query = queries.get( 0 );
        Partitions partitions = query.getPartitions();
        Partitions expected = new Partitions( Sets.newHashSet( 2010, 2011, 2012 ) );
        assertEquals( 3, partitions.getPartitions().size() );
        assertEquals( expected, partitions );
        assertEquals( PartitionUtils.getTableName( AnalyticsTableType.EVENT.getTableName(), prA ),
            query.getTableName() );
    }

    @Test
    void testPlanEventQueryA()
    {
        EventQueryParams params = new EventQueryParams.Builder().withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();
        params = queryPlanner.planEventQuery( params );
        assertEquals( new DateTime( 2010, 6, 1, 0, 0 ).toDate(), params.getStartDate() );
        assertEquals( new DateTime( 2012, 3, 20, 0, 0 ).toDate(), params.getEndDate() );
        Partitions partitions = params.getPartitions();
        Partitions expected = new Partitions( Sets.newHashSet( 2010, 2011, 2012 ) );
        assertEquals( 3, partitions.getPartitions().size() );
        assertEquals( expected, partitions );
        assertEquals( PartitionUtils.getTableName( AnalyticsTableType.EVENT.getTableName(), prA ),
            params.getTableName() );
    }

    @Test
    void testPlanEventQueryB()
    {
        EventQueryParams params = new EventQueryParams.Builder().withProgram( prA )
            .withStartDate( new DateTime( 2010, 3, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2010, 9, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();
        params = queryPlanner.planEventQuery( params );
        assertEquals( new DateTime( 2010, 3, 1, 0, 0 ).toDate(), params.getStartDate() );
        assertEquals( new DateTime( 2010, 9, 20, 0, 0 ).toDate(), params.getEndDate() );
        Partitions partitions = params.getPartitions();
        Partitions expected = new Partitions( Sets.newHashSet( 2010 ) );
        assertEquals( 1, partitions.getPartitions().size() );
        assertEquals( expected, partitions );
        assertEquals( PartitionUtils.getTableName( AnalyticsTableType.EVENT.getTableName(), prA ),
            params.getTableName() );
    }

    @Test
    void testFromDataQueryParams()
    {
        DataQueryParams dataQueryParams = DataQueryParams.newBuilder()
            .withProgramDataElements( getList( pdeA, pdeB, pdeC, pdeD ) ).withProgramAttributes( getList( patA, patB ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC ) ).withPeriods( getList( createPeriod( "200101" ),
                createPeriod( "200103" ), createPeriod( "200105" ), createPeriod( "200107" ) ) )
            .build();
        EventQueryParams params = EventQueryParams.fromDataQueryParams( dataQueryParams );
        assertEquals( 6, params.getItems().size() );
        assertNull( params.getDimension( DimensionalObject.DATA_X_DIM_ID ) );
        assertTrue( params.isAggregateData() );
        for ( QueryItem item : params.getItems() )
        {
            assertEquals( prA, item.getProgram() );
        }
    }

    @Test
    void testPlanAggregateDataQueryA()
    {
        DataQueryParams dataQueryParams = DataQueryParams.newBuilder()
            .withProgramDataElements( getList( pdeA, pdeB, pdeC, pdeD ) ).withProgramAttributes( getList( patA, patB ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC ) ).withPeriods( getList( createPeriod( "200101" ),
                createPeriod( "200103" ), createPeriod( "200105" ), createPeriod( "200107" ) ) )
            .build();
        EventQueryParams params = EventQueryParams.fromDataQueryParams( dataQueryParams );
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        assertEquals( 12, queries.size() );
        for ( EventQueryParams query : queries )
        {
            assertTrue( query.hasValueDimension() );
        }
    }

    @Test
    void testPlanAggregateDataQueryCollapseDataItems()
    {
        DataQueryParams dataQueryParams = DataQueryParams.newBuilder()
            .withProgramDataElements( getList( pdeA, pdeB, pdeC, pdeD ) ).withProgramAttributes( getList( patA, patB ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC ) ).withPeriods( getList( createPeriod( "200101" ),
                createPeriod( "200103" ), createPeriod( "200105" ), createPeriod( "200107" ) ) )
            .build();
        EventQueryParams params = new EventQueryParams.Builder( dataQueryParams ).withCollapseDataDimensions( true )
            .build();
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        assertEquals( 12, queries.size() );
        for ( EventQueryParams query : queries )
        {
            assertTrue( query.hasValueDimension() );
            assertTrue( query.isCollapseDataDimensions() );
        }
    }

    @Test
    void testPlanAggregateDataQueryFirstValue()
    {
        testPlanAggregateDataQueryFirstOrLastValue( AnalyticsAggregationType.FIRST );
    }

    @Test
    void testPlanAggregateDataQueryLastValue()
    {
        testPlanAggregateDataQueryFirstOrLastValue( AnalyticsAggregationType.LAST );
    }

    private void testPlanAggregateDataQueryFirstOrLastValue( AnalyticsAggregationType analyticsAggregationType )
    {
        DataQueryParams dataQueryParams = DataQueryParams.newBuilder().withProgramDataElements( getList( pdeA ) )
            .withOrganisationUnits( getList( ouA ) ).withPeriods( getList( createPeriod( "200101" ),
                createPeriod( "200103" ), createPeriod( "200105" ), createPeriod( "200107" ) ) )
            .withAggregationType( analyticsAggregationType ).build();
        EventQueryParams params = EventQueryParams.fromDataQueryParams( dataQueryParams );
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        assertEquals( 4, queries.size() );
        for ( EventQueryParams query : queries )
        {
            assertEquals( 1, query.getPeriods().size() );
            assertNotNull( query.getDimension( PERIOD_DIM_ID ) );
            assertEquals( MonthlyPeriodType.NAME.toLowerCase(),
                query.getDimension( PERIOD_DIM_ID ).getDimensionName() );
            assertTrue( query.hasValueDimension() );
        }
    }
}
