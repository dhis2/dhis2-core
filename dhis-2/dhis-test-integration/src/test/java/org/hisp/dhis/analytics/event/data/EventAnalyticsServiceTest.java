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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.AnalyticsTestUtils;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.system.util.CsvUtils;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Tests aggregation of data in event analytics tables.
 *
 * @author Henning Haakonsen
 */
class EventAnalyticsServiceTest extends NonTransactionalIntegrationTest
{

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private Program programA;

    @Autowired
    private List<AnalyticsTableService> analyticsTableServices;

    @Autowired
    private EventAnalyticsService eventAnalyticsService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private UserService _userService;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        // Common stubbed data
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
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
    }

    @Override
    public void tearDownTest()
    {
        for ( AnalyticsTableService service : analyticsTableServices )
        {
            service.dropTables();
        }
    }

    /**
     * <p>
     * To create a new test that needs the analytics table populated:
     * <p>
     * <ul>
     * <li>Make new EventQueryParam.</li>
     * <li>Add to 'eventQueryParams' map.</li>
     * <li>Add HashMap<String, Double> with expected output to results map.</li>
     * </ul>
     */
    @Test
    void testGridAggregation()
        throws IOException
    {
        // Given
        // Stubbed events data
        stubAnalyticsEventsData();
        // Events data from CSV file
        List<String[]> eventDataLines = CsvUtils.readCsvAsListFromClasspath( "analytics/csv/eventData.csv", true );
        parseEventData( eventDataLines );
        // The generated analytics tables
        analyticsTableGenerator.generateTables( AnalyticsTableUpdateParams.newBuilder().build(),
            NoopJobProgress.INSTANCE );
        // The user
        createAndInjectAdminUser();
        // All events in program A - 2017
        EventQueryParams events_2017_params = new EventQueryParams.Builder()
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 12, 31 ) ).withProgram( programA ).build();
        // The results
        Map<String, Object> events_2017_keyValue = new HashMap<>();
        events_2017_keyValue.put( "ouabcdefghA", 6.0 );
        // When
        Grid aggregatedDataValueGrid = eventAnalyticsService.getAggregatedEventData( events_2017_params );
        // Then
        AnalyticsTestUtils.assertResultGrid( events_2017_keyValue, aggregatedDataValueGrid );
    }

    @Test
    void testDimensionRestrictionSuccessfully()
    {
        // Given
        // A program
        Program aProgram = createProgram( 'B', null, null, Sets.newHashSet( ouA, ouB ), null );
        aProgram.setUid( "aProgram123" );
        idObjectManager.save( aProgram );
        // The category options
        CategoryOption coA = createCategoryOption( 'A' );
        CategoryOption coB = createCategoryOption( 'B' );
        categoryService.addCategoryOption( coA );
        categoryService.addCategoryOption( coB );
        // The categories
        Category caA = createCategory( 'A', coA );
        Category caB = createCategory( 'B', coB );
        categoryService.addCategory( caA );
        categoryService.addCategory( caB );
        // The constraints
        Set<Category> catDimensionConstraints = Sets.newHashSet( caA, caB );
        // The user
        User user = createUserWithAuth( "A", "F_VIEW_EVENT_ANALYTICS" );
        user.setCatDimensionConstraints( catDimensionConstraints );
        userService.addUser( user );
        enableDataSharing( user, aProgram, AccessStringHelper.DATA_READ_WRITE );
        idObjectManager.update( user );
        injectSecurityContext( user );
        // All events in program B - 2017
        EventQueryParams events_2017_params = new EventQueryParams.Builder()
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 12, 31 ) ).withProgram( aProgram ).build();
        // When
        Grid aggregatedDataValueGrid = eventAnalyticsService.getAggregatedEventData( events_2017_params );
        // Then
        boolean noCategoryRestrictionExceptionIsThrown = true;
        assertThat( aggregatedDataValueGrid, is( notNullValue() ) );
        assert (noCategoryRestrictionExceptionIsThrown);
    }

    @Test
    void testDimensionRestrictionWhenUserCannotReadCategoryOptions()
    {
        // Given
        // A program
        Program aProgram = createProgram( 'B', null, null, Sets.newHashSet( ouA, ouB ), null );
        aProgram.setUid( "aProgram123" );
        idObjectManager.save( aProgram );
        // The category options
        CategoryOption coA = createCategoryOption( 'A' );
        CategoryOption coB = createCategoryOption( 'B' );
        coA.getSharing().setOwner( "cannotRead" );
        coB.getSharing().setOwner( "cannotRead" );
        categoryService.addCategoryOption( coA );
        categoryService.addCategoryOption( coB );
        // The categories
        Category caA = createCategory( 'A', coA );
        Category caB = createCategory( 'B', coB );
        categoryService.addCategory( caA );
        categoryService.addCategory( caB );
        removeUserAccess( coA );
        removeUserAccess( coB );
        categoryService.updateCategoryOption( coA );
        categoryService.updateCategoryOption( coB );
        // The constraints
        Set<Category> catDimensionConstraints = Sets.newHashSet( caA, caB );
        // The user
        User user = createUserWithAuth( "A", "F_VIEW_EVENT_ANALYTICS" );
        user.setCatDimensionConstraints( catDimensionConstraints );
        userService.addUser( user );
        enableDataSharing( user, aProgram, AccessStringHelper.DATA_READ_WRITE );
        idObjectManager.update( user );
        injectSecurityContext( user );
        // All events in program B - 2017
        EventQueryParams events_2017_params = new EventQueryParams.Builder()
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 12, 31 ) ).withProgram( aProgram ).build();
        // Then
        Throwable exception = assertThrows( IllegalQueryException.class,
            () -> eventAnalyticsService.getAggregatedEventData( events_2017_params ) );
        assertThat( exception.getMessage(),
            containsString( "Current user is constrained by a dimension but has access to no dimension items" ) );
    }

    // -------------------------------------------------------------------------
    // Internal Logic
    // -------------------------------------------------------------------------
    private void parseEventData( List<String[]> lines )
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
            event.setDataValues( Sets.newHashSet( dataValue ) );
            event.setCompletedDate( line[3] );
            event.setTrackedEntityInstance( line[5] );
            event.setStatus( EventStatus.COMPLETED );
        }
    }

    private void stubAnalyticsEventsData()
    {
        Period peJan = createPeriod( "2017-01" );
        Period peFeb = createPeriod( "2017-02" );
        Period peMar = createPeriod( "2017-03" );
        Period peApril = createPeriod( "2017-04" );
        periodService.addPeriod( peJan );
        periodService.addPeriod( peFeb );
        periodService.addPeriod( peMar );
        periodService.addPeriod( peApril );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        DataElement deC = createDataElement( 'C' );
        DataElement deD = createDataElement( 'D' );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        programA = createProgram( 'A', null, null, Sets.newHashSet( ouA, ouB ), null );
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
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        idObjectManager.save( trackedEntityType );
        org.hisp.dhis.trackedentity.TrackedEntityInstance maleA = createTrackedEntityInstance( ouA );
        maleA.setUid( "person1234A" );
        org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB = createTrackedEntityInstance( ouB );
        femaleB.setUid( "person1234B" );
        maleA.setTrackedEntityType( trackedEntityType );
        femaleB.setTrackedEntityType( trackedEntityType );
        idObjectManager.save( maleA );
        idObjectManager.save( femaleB );
        programInstanceService.enrollTrackedEntityInstance( maleA, programA, null, null, ouA );
        programInstanceService.enrollTrackedEntityInstance( femaleB, programA, null, null, ouA );
    }
}
