package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
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
    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    
    @Autowired
    private EventQueryPlanner queryPlanner;
    
    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    @Override
    public void setUpTest()
    {
        prA = new Program();
        prA.setUid( "programuida" );
        
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
    }
    
    @Test
    public void testPlanAggregateQueryA()
    {        
        EventQueryParams params = new EventQueryParams();
        params.setProgram( prA );
        params.setStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() );
        params.setEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() );
        params.setOrganisationUnits( Lists.newArrayList( ouA ) );
        
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
        EventQueryParams params = new EventQueryParams();
        params.setProgram( prA );
        params.setStartDate( new DateTime( 2010, 3, 1, 0, 0 ).toDate() );
        params.setEndDate( new DateTime( 2010, 9, 20, 0, 0 ).toDate() );
        params.setOrganisationUnits( Lists.newArrayList( ouA ) );
        
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
        EventQueryParams params = new EventQueryParams();
        params.setProgram( prA );
        params.setStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() );
        params.setEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() );
        params.setOrganisationUnits( Lists.newArrayList( ouA, ouB ) );
        
        List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
        
        assertEquals( 2, queries.size() );
        assertEquals( ouA, queries.get( 0 ).getOrganisationUnits().get( 0 ) );
        assertEquals( ouB, queries.get( 1 ).getOrganisationUnits().get( 0 ) );
    }

    @Test
    public void testPlanEventQueryA()
    {        
        EventQueryParams params = new EventQueryParams();
        params.setProgram( prA );
        params.setStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() );
        params.setEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() );
        params.setOrganisationUnits( Lists.newArrayList( ouA ) );
        
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
        EventQueryParams params = new EventQueryParams();
        params.setProgram( prA );
        params.setStartDate( new DateTime( 2010, 3, 1, 0, 0 ).toDate() );
        params.setEndDate( new DateTime( 2010, 9, 20, 0, 0 ).toDate() );
        params.setOrganisationUnits( Lists.newArrayList( ouA ) );
        
        params = queryPlanner.planEventQuery( params );

        assertEquals( new DateTime( 2010, 3, 1, 0, 0 ).toDate(), params.getStartDate() );
        assertEquals( new DateTime( 2010, 9, 20, 0, 0 ).toDate(), params.getEndDate() );

        Partitions partitions = params.getPartitions();

        assertEquals( 1, partitions.getPartitions().size() );
        assertEquals( "analytics_event_2010_programuida", partitions.getSinglePartition() );
    }
}
