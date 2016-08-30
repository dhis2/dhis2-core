package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.hisp.dhis.common.DimensionalObjectUtils.getList;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElement;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class EventQueryPlannerTest
    extends DhisSpringTest
{
    private Program prA;
    
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    
    private ProgramDataElement pdeA;
    private ProgramDataElement pdeB;
    private ProgramDataElement pdeC;
    private ProgramDataElement pdeD;
    
    private TrackedEntityAttribute atA;
    private TrackedEntityAttribute atB;
    
    private ProgramTrackedEntityAttribute patA;
    private ProgramTrackedEntityAttribute patB;
    
    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    
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
        deC = createDataElement( 'C', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT, DataElementDomain.TRACKER );
        deD = createDataElement( 'D', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT, DataElementDomain.TRACKER );
        
        idObjectManager.save( deA );
        idObjectManager.save( deB );
        idObjectManager.save( deC );
        idObjectManager.save( deD );
        
        pdeA = new ProgramDataElement( prA, deA );
        pdeB = new ProgramDataElement( prA, deB );
        pdeC = new ProgramDataElement( prA, deC );
        pdeD = new ProgramDataElement( prA, deD );
        
        idObjectManager.save( pdeA );
        idObjectManager.save( pdeB );
        idObjectManager.save( pdeC );
        idObjectManager.save( pdeD );
        
        atA = createTrackedEntityAttribute( 'A' );
        atB = createTrackedEntityAttribute( 'B' );
        
        idObjectManager.save( atA );
        idObjectManager.save( atB );
        
        patA = new ProgramTrackedEntityAttribute( prA, atA );
        patB = new ProgramTrackedEntityAttribute( prA, atB );
        
        idObjectManager.save( patA );
        idObjectManager.save( patB );
        
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C', ouA );
        
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
    }
    
    @Test
    public void testPlanAggregateQueryA()
    {        
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();
        
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        
        assertEquals( 1, queries.size() );
        
        EventQueryParams query = queries.get( 0 );
        
        assertEquals( new DateTime( 2010, 6, 1, 0, 0 ).toDate(), query.getStartDate() );
        assertEquals( new DateTime( 2012, 3, 20, 0, 0 ).toDate(), query.getEndDate() );
        
        Partitions partitions = query.getPartitions();
        
        assertEquals( 3, partitions.getPartitions().size() );
        assertEquals( "analytics_event_2010_programuida", partitions.getPartitions().get( 0 ) );
        assertEquals( "analytics_event_2011_programuida", partitions.getPartitions().get( 1 ) );
        assertEquals( "analytics_event_2012_programuida", partitions.getPartitions().get( 2 ) );
    }

    @Test
    public void testPlanAggregateQueryB()
    {        
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 3, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2010, 9, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();
        
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );

        assertEquals( 1, queries.size() );

        EventQueryParams query = queries.get( 0 );
        
        assertEquals( new DateTime( 2010, 3, 1, 0, 0 ).toDate(), query.getStartDate() );
        assertEquals( new DateTime( 2010, 9, 20, 0, 0 ).toDate(), query.getEndDate() );

        Partitions partitions = query.getPartitions();

        assertEquals( 1, partitions.getPartitions().size() );
        assertEquals( "analytics_event_2010_programuida", partitions.getSinglePartition() );
    }

    @Test
    public void testPlanAggregateQueryC()
    {        
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA, ouB ) ).build();
        
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        
        assertEquals( 2, queries.size() );
        assertEquals( ouA, queries.get( 0 ).getOrganisationUnits().get( 0 ) );
        assertEquals( ouB, queries.get( 1 ).getOrganisationUnits().get( 0 ) );
    }

    @Test
    public void testPlanEventQueryA()
    {        
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();
        
        params = queryPlanner.planEventQuery( params );
        
        assertEquals( new DateTime( 2010, 6, 1, 0, 0 ).toDate(), params.getStartDate() );
        assertEquals( new DateTime( 2012, 3, 20, 0, 0 ).toDate(), params.getEndDate() );
        
        Partitions partitions = params.getPartitions();
        
        assertEquals( 3, partitions.getPartitions().size() );
        assertEquals( "analytics_event_2010_programuida", partitions.getPartitions().get( 0 ) );
        assertEquals( "analytics_event_2011_programuida", partitions.getPartitions().get( 1 ) );
        assertEquals( "analytics_event_2012_programuida", partitions.getPartitions().get( 2 ) );
    }

    @Test
    public void testPlanEventQueryB()
    {        
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 3, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2010, 9, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();
        
        params = queryPlanner.planEventQuery( params );

        assertEquals( new DateTime( 2010, 3, 1, 0, 0 ).toDate(), params.getStartDate() );
        assertEquals( new DateTime( 2010, 9, 20, 0, 0 ).toDate(), params.getEndDate() );

        Partitions partitions = params.getPartitions();

        assertEquals( 1, partitions.getPartitions().size() );
        assertEquals( "analytics_event_2010_programuida", partitions.getSinglePartition() );
    }

    @Test
    public void testFromDataQueryParams()
    {
        DataQueryParams dataQueryParams = DataQueryParams.newBuilder()
            .withProgramDataElements( getList( pdeA, pdeB, pdeC, pdeD ) )
            .withProgramAttributes( getList( patA, patB ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC ) )
            .withPeriods( getList( createPeriod( "200101" ), createPeriod( "200103" ), createPeriod( "200105" ), createPeriod( "200107" ) ) ).build();

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
    public void testPlanAggregateDataQueryA()
    {
        DataQueryParams dataQueryParams = DataQueryParams.newBuilder()
            .withProgramDataElements( getList( pdeA, pdeB, pdeC, pdeD ) )
            .withProgramAttributes( getList( patA, patB ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC ) )
            .withPeriods( getList( createPeriod( "200101" ), createPeriod( "200103" ), createPeriod( "200105" ), createPeriod( "200107" ) ) ).build();
        
        EventQueryParams params = EventQueryParams.fromDataQueryParams( dataQueryParams );
        
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        
        assertEquals( 12, queries.size() );
        
        for ( EventQueryParams query : queries )
        {
            assertTrue( query.hasValueDimension() );
        }
    }

    @Test
    public void testPlanAggregateDataQueryCollapseDataItems()
    {
        DataQueryParams dataQueryParams = DataQueryParams.newBuilder()
            .withProgramDataElements( getList( pdeA, pdeB, pdeC, pdeD ) )
            .withProgramAttributes( getList( patA, patB ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC ) )
            .withPeriods( getList( createPeriod( "200101" ), createPeriod( "200103" ), createPeriod( "200105" ), createPeriod( "200107" ) ) ).build();
        
        EventQueryParams params = new EventQueryParams.Builder( dataQueryParams )
            .withCollapseDataDimensions( true )
            .build();
        
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        
        assertEquals( 12, queries.size() );
        
        for ( EventQueryParams query : queries )
        {
            assertTrue( query.hasValueDimension() );
            assertTrue( query.isCollapseDataDimensions() );
        }
    }
}
