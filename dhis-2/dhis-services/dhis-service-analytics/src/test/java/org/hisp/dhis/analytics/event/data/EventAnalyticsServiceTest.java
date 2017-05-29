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
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.utils.AnalyticsTestUtils;
import org.hisp.dhis.analytics.utils.DefaultAnalyticsTestUtils;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dbms.HibernateDbmsManager;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.*;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Henning Haakonsen
 */
public class EventAnalyticsServiceTest
    extends DhisTest
{
    private Map<String, EventQueryParams> eventQueryParams = new HashMap<>();

    private Map<String, AnalyticalObject> analyticalObjectHashMap = new HashMap<>();

    private Map<String, Map<String, Double>> results = new HashMap<>();


    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleA;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleB;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleA;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB;

    private Program prA;
    private ProgramStage psA;

    private TrackedEntityAttribute atA;
    private TrackedEntityAttribute atB;

    private LegendSet legendSetA;

    private Legend legendA;
    private Legend legendB;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private EventAnalyticsService eventAnalyticsService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private EventService eventService;

    private AnalyticsTestUtils analyticsTestUtils = new DefaultAnalyticsTestUtils();

    @Override
    public void setUpTest()
        throws IOException
    {
        Period peJan = createPeriod( "2017-01" );
        Period peFeb = createPeriod( "2017-02" );
        Period peMar = createPeriod( "2017-03" );
        Period peApril = createPeriod( "2017-04" );
        Period quarter = createPeriod( "2017Q1" );

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

        analyticsTestUtils.configureHierarchy( ouA, ouB, ouC, ouD, ouE );

        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );
        idObjectManager.save( ouD );
        idObjectManager.save( ouE );

        atA = createTrackedEntityAttribute( 'A' );
        atB = createTrackedEntityAttribute( 'B' );

        idObjectManager.save( atA );
        idObjectManager.save( atB );

        prA = createProgram( 'A', null, Sets.newHashSet( atA, atB ), Sets.newHashSet( ouA, ouB ), null );
        prA.setUid( "programA123" );
        idObjectManager.save( prA );

        psA = createProgramStage( 'A', 0 );
        psA.setUid( "programStgA" );
        psA.addDataElement( deA, 0 );
        psA.addDataElement( deB, 1 );
        prA.getProgramStages().add( psA );

        legendA = createLegend( 'A', 0d, 10d );
        legendB = createLegend( 'B', 10d, 20d );

        legendSetA = createLegendSet( 'A' );

        legendSetA.getLegends().add( legendA );
        legendSetA.getLegends().add( legendB );

        idObjectManager.save( legendSetA );


        /*Program programA = createProgram( 'A' );
        programA.setLastUpdated( getDate( 2017, 5, 23 ) );
        programA.setUid( "programA123" );
        Program programB = createProgram( 'B' );
        programB.setUid( "programB123" );
        Program programC = createProgram( 'C' );
        programC.setUid( "programC123" );
        Program programD = createProgram( 'D' );
        programD.setUid( "programD123" );

        idObjectManager.save( programA );
        idObjectManager.save( programB );
        idObjectManager.save( programC );
        idObjectManager.save( programD );

        ProgramStage programStageA = createProgramStage( 'A', programA );
        programA.addOrganisationUnit( ouA );
        programStageA.setUid( "programStgA" );
        ProgramStage programStageB = createProgramStage( 'B', programA );
        programA.addOrganisationUnit( ouA );
        programStageA.setUid( "programStgB" );

        idObjectManager.save( programStageA );
        idObjectManager.save( programStageB );*/



        TrackedEntity trackedEntity = createTrackedEntity( 'A' );
        trackedEntityService.addTrackedEntity( trackedEntity );

        maleA = createTrackedEntityInstance( 'A', ouA );
        maleB = createTrackedEntityInstance( 'B', ouB );
        femaleA = createTrackedEntityInstance( 'C', ouA );
        femaleB = createTrackedEntityInstance( 'D', ouB );

        maleA.setTrackedEntity( trackedEntity );
        maleB.setTrackedEntity( trackedEntity );
        femaleA.setTrackedEntity( trackedEntity );
        femaleB.setTrackedEntity( trackedEntity );

        idObjectManager.save( maleA );
        idObjectManager.save( maleB );
        idObjectManager.save( femaleA );
        idObjectManager.save( femaleB );



        programInstanceService.enrollTrackedEntityInstance( maleA, prA, null, null, ouA );
        programInstanceService.enrollTrackedEntityInstance( femaleA, prA, null, null, ouA );

        // Read event data from CSV file
        // --------------------------------------------------------------------
        ArrayList<String[]> eventDataLines = analyticsTestUtils.readInputFile( "csv/eventData.csv" );
        parseEventData( eventDataLines );


        // Generate analytics tables
        // --------------------------------------------------------------------
        analyticsTableGenerator.generateTables( null, null, null, false );

        // Set parameters
        // --------------------------------------------------------------------

        // all events - 2017
        Period y2017 = createPeriod( "2017" );
        EventQueryParams events_2017_params =  new EventQueryParams.Builder()
            .withOrganisationUnits( Lists.newArrayList( ouA ) )
            .withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 12, 31 ) )
            .withProgram( prA )
            .build();

        eventQueryParams.put( "events_2017", events_2017_params );


        // Set results
        // --------------------------------------------------------------------

        Map<String, Double> events_2017_keyValue = new HashMap<>();
        events_2017_keyValue.put( "0.0", 0.0 );

        results.put( "events_2017", events_2017_keyValue );

    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void tearDownTest()
    {
        analyticsTableGenerator.dropTables();
    }

    @Autowired
    private HibernateDbmsManager hibernateDbmsManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Test
    public void testGetAggregatedEvents()
    {
        List<List<Object>> tableContent = new ArrayList<>();

        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet( "SHOW TABLES;" );
        int cols = sqlRowSet.getMetaData().getColumnCount() + 1;

        List<Object> headers = new ArrayList<>();

        for ( int i = 1; i < cols; i++ )
        {
            headers.add( sqlRowSet.getMetaData().getColumnName( i ) );
        }

        tableContent.add( headers );

        while ( sqlRowSet.next() )
        {
            List<Object> row = new ArrayList<>();

            for ( int i = 1; i < cols; i++ )
            {
                row.add( sqlRowSet.getObject( i ) );

            }

            tableContent.add( row );
        }

        for ( List<Object> row : tableContent )
        {
            for ( Object line : row )
            {
                System.out.print(line + ", ");
            }
            System.out.println();
        }

        Grid aggregatedEventData;
        for ( Map.Entry<String, EventQueryParams> entry : eventQueryParams.entrySet() )
        {
            String key = entry.getKey();
            EventQueryParams params = entry.getValue();

            aggregatedEventData = eventAnalyticsService.getAggregatedEventData( params );

            System.out.println("eventData: " + aggregatedEventData);

            assertEventGrid( aggregatedEventData, results.get( key ) );
        }
    }

    // -------------------------------------------------------------------------
    // Internal Logic
    // -------------------------------------------------------------------------

    private void parseEventData( ArrayList<String[]> lines )
    {
        String storedBy = "johndoe";

        for( String[] line : lines)
        {
            Event event = new Event( );
            event.setProgram( line[0] );
            event.setProgramStage( line[1] );

            DataValue dataValue = new DataValue();
            dataValue.setDataElement( line[2] );
            dataValue.setValue( line[5] );
            dataValue.setStoredBy( storedBy );

            event.setEventDate( line[3] );
            event.setOrgUnit( line[4] );

            event.setDataValues( Lists.newArrayList( dataValue ) );

            event.setCompletedDate( line[3] );
            event.setStatus( EventStatus.COMPLETED );
            event.setTrackedEntityInstance( maleA.getUid() );

            System.out.println("Event: " + event + "\nmale a: " + maleA);

            eventService.addEvent( event, null );
        }
    }

    private void assertEventGrid( Grid aggregatedEventData, Map<String, Double> keyValue )
    {
        assertNotNull( aggregatedEventData );
        for ( int i = 0; i < aggregatedEventData.getRows().size(); i++ )
        {
            int numberOfDimensions = aggregatedEventData.getRows().get( 0 ).size() - 1;

            StringBuilder key = new StringBuilder();
            for ( int j = 0; j < numberOfDimensions; j++ )
            {
                key.append( aggregatedEventData.getValue( i, j ).toString() );
                if ( j != numberOfDimensions - 1 )
                    key.append( "-" );
            }

            Double expected = keyValue.get( key.toString() );
            Double actual = (Double) aggregatedEventData.getValue( i, numberOfDimensions );

            assertNotNull( "Did not find '" + key + "' in provided results", expected );
            assertNotNull( aggregatedEventData.getRow( i ) );
            assertEquals( "Value for key: '" + key + "' not matching expected value: '" + expected + "'", expected,
                actual );
        }
    }
}
