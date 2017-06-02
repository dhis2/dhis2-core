package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.utils.AnalyticsTestUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests aggregation of data in event analytics tables.
 * <p>
 * To create a new test:
 * <p>
 * <ul>
 * <li>Make new EventQueryParam.</li>
 * <li>Add to 'eventQueryParams' map.</li>
 * <li>Add HashMap<String, Double> with expected output to results map.</li>
 * </ul>
 *
 * @author Henning Haakonsen
 */
@Category( IntegrationTest.class )
public class EventAnalyticsServiceTest
    extends DhisTest
{
    private Map<String, EventQueryParams> eventQueryParams = new HashMap<>();

    private Map<String, Map<String, Double>> results = new HashMap<>();

    @Autowired
    private EventAnalyticsService eventAnalyticsService;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Override
    public void setUpTest()
        throws IOException
    {
        Period peJan = createPeriod( "2017-01" );
        Period peFeb = createPeriod( "2017-02" );
        Period peMar = createPeriod( "2017-03" );
        Period peApril = createPeriod( "2017-04" );

        idObjectManager.save( peJan );
        idObjectManager.save( peFeb );
        idObjectManager.save( peMar );
        idObjectManager.save( peApril );

        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        DataElement deC = createDataElement( 'C' );
        DataElement deD = createDataElement( 'D' );

        idObjectManager.save( deA );
        idObjectManager.save( deB );
        idObjectManager.save( deC );
        idObjectManager.save( deD );

        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        OrganisationUnit ouC = createOrganisationUnit( 'C' );
        ouC.setOpeningDate( getDate( 2016, 4, 10 ) );
        ouC.setClosedDate( null );

        OrganisationUnit ouD = createOrganisationUnit( 'D' );
        ouD.setOpeningDate( getDate( 2016, 12, 10 ) );
        ouD.setClosedDate( null );

        OrganisationUnit ouE = createOrganisationUnit( 'E' );

        AnalyticsTestUtils.configureHierarchy( ouA, ouB, ouC, ouD, ouE );

        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );
        idObjectManager.save( ouD );
        idObjectManager.save( ouE );

        Program programA = createProgram( 'A', null, null, Sets.newHashSet( ouA, ouB ), null );
        programA.setUid( "programA123" );
        idObjectManager.save( programA );

        ProgramStage psA = createProgramStage( 'A', 0 );
        psA.setUid( "programStgA" );
        psA.addDataElement( deA, 0 );
        psA.addDataElement( deB, 1 );
        idObjectManager.save( psA );

        ProgramStage psB = createProgramStage( 'B', 0 );
        psB.setUid( "programStgB" );
        psB.addDataElement( deA, 0 );
        psB.addDataElement( deB, 1 );
        idObjectManager.save( psB );

        ProgramStage psC = createProgramStage( 'C', 0 );
        psC.setUid( "programStgC" );
        psC.addDataElement( deA, 0 );
        psC.addDataElement( deB, 1 );
        idObjectManager.save( psC );

        programA.getProgramStages().add( psA );

        TrackedEntity trackedEntity = createTrackedEntity( 'A' );
        idObjectManager.save( trackedEntity );

        org.hisp.dhis.trackedentity.TrackedEntityInstance maleA = createTrackedEntityInstance( 'A', ouA );
        maleA.setUid( "person1234A" );
        org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB = createTrackedEntityInstance( 'B', ouB );
        femaleB.setUid( "person1234B" );

        maleA.setTrackedEntity( trackedEntity );
        femaleB.setTrackedEntity( trackedEntity );

        idObjectManager.save( maleA );
        idObjectManager.save( femaleB );

        programInstanceService.enrollTrackedEntityInstance( maleA, programA, null, null, ouA );
        programInstanceService.enrollTrackedEntityInstance( femaleB, programA, null, null, ouA );

        // Read event data from CSV file
        // --------------------------------------------------------------------
        ArrayList<String[]> eventDataLines = AnalyticsTestUtils.readInputFile( "csv/eventData.csv" );
        parseEventData( eventDataLines );

        // Generate analytics tables
        // --------------------------------------------------------------------
        analyticsTableGenerator.generateTables( null, null, null, false );

        // Set parameters
        // --------------------------------------------------------------------

        // all events in program A - 2017
        EventQueryParams events_2017_params = new EventQueryParams.Builder()
            .withOrganisationUnits( Lists.newArrayList( ouA ) )
            .withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 12, 31 ) )
            .withProgram( programA )
            .build();

        eventQueryParams.put( "events_2017", events_2017_params );

        // Set results
        // --------------------------------------------------------------------

        Map<String, Double> events_2017_keyValue = new HashMap<>();
        events_2017_keyValue.put( "ouabcdefghA", 6.0 );

        results.put( "events_2017", events_2017_keyValue );

    }

    @Override
    public void tearDownTest()
    {
        analyticsTableGenerator.dropTables();
    }

    @Test
    public void testGridAggregation()
    {
        Grid aggregatedDataValueGrid;
        for ( Map.Entry<String, EventQueryParams> entry : eventQueryParams.entrySet() )
        {
            String key = entry.getKey();
            EventQueryParams params = entry.getValue();

            aggregatedDataValueGrid = eventAnalyticsService.getAggregatedEventData( params );

            AnalyticsTestUtils.assertResultGrid( aggregatedDataValueGrid, results.get( key ) );
        }
    }

    // -------------------------------------------------------------------------
    // Internal Logic
    // -------------------------------------------------------------------------
    private void parseEventData( ArrayList<String[]> lines )
    {
        String storedBy = "johndoe";

        for ( String[] line : lines )
        {
            Event event = new Event();
            event.setProgram( line[0] );
            event.setProgramStage( line[1] );

            DataValue dataValue = new DataValue();
            dataValue.setDataElement( line[2] );
            dataValue.setValue( line[6] );
            dataValue.setStoredBy( storedBy );

            event.setEventDate( line[3] );
            event.setOrgUnit( line[4] );

            event.setDataValues( Lists.newArrayList( dataValue ) );

            event.setCompletedDate( line[3] );
            event.setTrackedEntityInstance( line[5] );

            event.setStatus( EventStatus.COMPLETED );
        }
    }
}
