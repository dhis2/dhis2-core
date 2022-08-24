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

import static java.util.Calendar.APRIL;
import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.JANUARY;
import static java.util.Calendar.MARCH;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsService;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.EventStore;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramOwnershipHistory;
import org.hisp.dhis.program.ProgramOwnershipHistoryService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Tests event and enrollment analytics services.
 *
 * @author Henning Haakonsen
 * @author Jim Grace (nearly complete rewrite)
 */
class EventAnalyticsServiceTest
    extends SingleSetupIntegrationTestBase
{
    @Autowired
    private EventAnalyticsService eventTarget;

    @Autowired
    private EnrollmentAnalyticsService enrollmentTarget;

    @Autowired
    private List<AnalyticsTableService> analyticsTableServices;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private EventStore eventStore;

    @Autowired
    private ProgramOwnershipHistoryService programOwnershipHistoryService;

    @Autowired
    private UserService _userService;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private OrganisationUnit ouD;

    private OrganisationUnit ouE;

    private OrganisationUnit ouF;

    private OrganisationUnit ouG;

    private OrganisationUnit ouH;

    private OrganisationUnit ouI;

    private OrganisationUnit ouJ;

    private OrganisationUnit ouK;

    private OrganisationUnit ouL;

    private OrganisationUnit ouM;

    private OrganisationUnit ouN;

    private List<OrganisationUnit> level3Ous;

    private Period peJan = createPeriod( "2017-01" );

    private Period peFeb = createPeriod( "2017-02" );

    private Period peMar = createPeriod( "2017-03" );

    private CategoryOption coA;

    private CategoryOption coB;

    private Category caA;

    private Category caB;

    private CategoryCombo ccA;

    private CategoryOptionCombo cocA;

    private CategoryOptionCombo cocB;

    private DataElement deU;

    private TrackedEntityAttribute atU;

    private Program programA;

    private Program programB;

    private ProgramStage psA;

    private User userA;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws IOException,
        InterruptedException
    {
        userService = _userService;

        // Organisation Units
        //
        // A -> B -> D,E,F,G
        // A -> C -> H,I,J,K,L,M,N
        //
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C', ouA );
        ouC.setOpeningDate( getDate( 2016, 4, 10 ) );
        ouD = createOrganisationUnit( 'D', ouB );
        ouD.setOpeningDate( getDate( 2016, 12, 10 ) );
        ouE = createOrganisationUnit( 'E', ouB );
        ouF = createOrganisationUnit( 'F', ouB );
        ouG = createOrganisationUnit( 'G', ouB );
        ouH = createOrganisationUnit( 'H', ouC );
        ouI = createOrganisationUnit( 'I', ouC );
        ouJ = createOrganisationUnit( 'J', ouC );
        ouK = createOrganisationUnit( 'K', ouC );
        ouL = createOrganisationUnit( 'L', ouC );
        ouM = createOrganisationUnit( 'M', ouC );
        ouN = createOrganisationUnit( 'N', ouC );

        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );
        idObjectManager.save( ouD );
        idObjectManager.save( ouE );
        idObjectManager.save( ouF );
        idObjectManager.save( ouG );
        idObjectManager.save( ouH );
        idObjectManager.save( ouI );
        idObjectManager.save( ouJ );
        idObjectManager.save( ouK );
        idObjectManager.save( ouL );
        idObjectManager.save( ouM );
        idObjectManager.save( ouN );

        level3Ous = organisationUnitService.getOrganisationUnitsAtLevel( 3 );

        // Organisation Unit Levels
        OrganisationUnitLevel ou1 = new OrganisationUnitLevel( 1, "Ou Level 1" );
        OrganisationUnitLevel ou2 = new OrganisationUnitLevel( 2, "Ou Level 2" );
        OrganisationUnitLevel ou3 = new OrganisationUnitLevel( 3, "Ou Level 3" );
        idObjectManager.save( ou1 );
        idObjectManager.save( ou2 );
        idObjectManager.save( ou3 );

        // Category Options
        coA = createCategoryOption( 'A' );
        coB = createCategoryOption( 'B' );
        coA.setUid( "cataOptionA" );
        coB.setUid( "cataOptionB" );
        categoryService.addCategoryOption( coA );
        categoryService.addCategoryOption( coB );

        // Categories
        caA = createCategory( 'A', coA );
        caB = createCategory( 'B', coB );
        caA.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        caB.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        caA.setUid( "categoryIdA" );
        caB.setUid( "categoryIdB" );
        categoryService.addCategory( caA );
        categoryService.addCategory( caB );

        // Category Combos
        ccA = createCategoryCombo( "CCa", "categComboA", caA, caB );
        categoryService.addCategoryCombo( ccA );

        // Category Option Combos
        cocA = createCategoryOptionCombo( "COCa", "catOptCombA", ccA, coA );
        cocB = createCategoryOptionCombo( "COCb", "catOptCombB", ccA, coB );
        categoryService.addCategoryOptionCombo( cocA );
        categoryService.addCategoryOptionCombo( cocB );
        ccA.getOptionCombos().add( cocA );
        ccA.getOptionCombos().add( cocB );
        categoryService.updateCategoryCombo( ccA );
        coA.getCategoryOptionCombos().add( cocA );
        coB.getCategoryOptionCombos().add( cocB );
        categoryService.updateCategoryOption( coA );
        categoryService.updateCategoryOption( coB );

        // Default Category Option Combo
        CategoryOptionCombo cocDefault = categoryService.getDefaultCategoryOptionCombo();

        // Periods
        periodService.addPeriod( peJan );
        periodService.addPeriod( peFeb );
        periodService.addPeriod( peMar );

        Date jan1 = new GregorianCalendar( 2017, JANUARY, 1 ).getTime();
        Date jan15 = new GregorianCalendar( 2017, JANUARY, 15 ).getTime();
        Date feb14 = new GregorianCalendar( 2017, FEBRUARY, 14 ).getTime();
        Date feb15 = new GregorianCalendar( 2017, FEBRUARY, 15 ).getTime();
        Date mar14 = new GregorianCalendar( 2017, MARCH, 14 ).getTime();
        Date mar15 = new GregorianCalendar( 2017, MARCH, 15 ).getTime();
        Date apr15 = new GregorianCalendar( 2017, APRIL, 15 ).getTime();

        // Data Elements
        deU = createDataElement( 'U', ORGANISATION_UNIT, NONE );
        deU.setUid( "deOrgUnitU" );
        dataElementService.addDataElement( deU );

        // Program Stages
        psA = createProgramStage( 'A', 0 );
        psA.setUid( "progrStageA" );
        psA.addDataElement( deU, 2 );
        idObjectManager.save( psA );

        ProgramStage psB = createProgramStage( 'B', 0 );
        psB.setUid( "progrStageB" );
        psB.addDataElement( deU, 2 );
        idObjectManager.save( psB );

        // Programs
        programA = createProgram( 'A' );
        programA.getProgramStages().add( psA );
        programA.getOrganisationUnits().addAll( level3Ous );
        programA.setUid( "programA123" );
        idObjectManager.save( programA );

        programB = createProgram( 'B' );
        programB.getProgramStages().add( psB );
        programB.getOrganisationUnits().addAll( level3Ous );
        programB.setUid( "programB123" );
        programB.setCategoryCombo( ccA );
        idObjectManager.save( programB );

        // Tracked Entity Attributes
        atU = createTrackedEntityAttribute( 'U', ORGANISATION_UNIT );
        atU.setUid( "teaAttribuU" );
        idObjectManager.save( atU );

        ProgramTrackedEntityAttribute pTea = createProgramTrackedEntityAttribute( programA, atU );
        programA.getProgramAttributes().add( pTea );
        idObjectManager.update( programA );

        // Tracked Entity Types
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        idObjectManager.save( trackedEntityType );

        // Tracked Entities (Registrations)
        org.hisp.dhis.trackedentity.TrackedEntityInstance teiA = createTrackedEntityInstance( ouD );
        teiA.setUid( "trackEntInA" );
        teiA.setTrackedEntityType( trackedEntityType );
        idObjectManager.save( teiA );

        // Tracked Entity Attribute Values
        TrackedEntityAttributeValue atv = createTrackedEntityAttributeValue( 'A', teiA, atU );
        atv.setValue( ouF.getUid() );
        attributeValueService.addTrackedEntityAttributeValue( atv );

        // Program Instances (Enrollments)
        ProgramInstance piA = programInstanceService.enrollTrackedEntityInstance( teiA, programA, jan1, jan1, ouE );
        piA.setEnrollmentDate( jan1 );
        piA.setIncidentDate( jan1 );
        programInstanceService.addProgramInstance( piA );

        ProgramInstance piB = programInstanceService.enrollTrackedEntityInstance( teiA, programB, jan1, jan1, ouE );
        piB.setEnrollmentDate( jan1 );
        piB.setIncidentDate( jan1 );
        programInstanceService.addProgramInstance( piB );

        // Change Enrollment Ownership through time
        addProgramOwnershipHistory( programA, teiA, ouF, jan15, feb14 );
        addProgramOwnershipHistory( programA, teiA, ouG, feb15, mar14 );
        addProgramOwnershipHistory( programA, teiA, ouH, mar15, apr15 );

        // Program Stage Instances (Events)
        ProgramStageInstance psiA = createProgramStageInstance( psA, piA, ouI );
        psiA.setDueDate( jan15 );
        psiA.setExecutionDate( jan15 );
        psiA.setUid( "progStagInA" );
        psiA.setEventDataValues( Set.of( new EventDataValue( deU.getUid(), ouL.getUid() ) ) );
        psiA.setAttributeOptionCombo( cocDefault );

        ProgramStageInstance psiB = createProgramStageInstance( psB, piB, ouI );
        psiB.setDueDate( jan15 );
        psiB.setExecutionDate( jan15 );
        psiB.setUid( "progStagInB" );
        psiB.setEventDataValues( Set.of( new EventDataValue( deU.getUid(), ouL.getUid() ) ) );
        psiB.setAttributeOptionCombo( cocDefault );

        ProgramStageInstance psiC = createProgramStageInstance( psA, piA, ouJ );
        psiC.setDueDate( feb15 );
        psiC.setExecutionDate( feb15 );
        psiC.setUid( "progStagInC" );
        psiC.setEventDataValues( Set.of( new EventDataValue( deU.getUid(), ouM.getUid() ) ) );
        psiC.setAttributeOptionCombo( cocDefault );

        ProgramStageInstance psiD = createProgramStageInstance( psA, piA, ouK );
        psiD.setDueDate( mar15 );
        psiD.setExecutionDate( mar15 );
        psiD.setUid( "progStagInD" );
        psiD.setEventDataValues( Set.of( new EventDataValue( deU.getUid(), ouN.getUid() ) ) );
        psiD.setAttributeOptionCombo( cocDefault );

        saveEvents( List.of( psiA, psiB, psiC, psiD ) );

        userA = createUserWithAuth( "A", "F_VIEW_EVENT_ANALYTICS" );
        userA.setCatDimensionConstraints( Sets.newHashSet( caA, caB ) );
        userService.addUser( userA );
        enableDataSharing( userA, programA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( userA, programB, AccessStringHelper.DATA_READ_WRITE );
        idObjectManager.update( userA );

        // Sleep for one second. This is needed because last updated time for
        // the data we just created is stored to milliseconds, hh:mm:ss.SSS.
        // The queries to build analytics tables tests data last updated times
        // to be in the past but compares only to the second. So for example a
        // recorded last updated time of 11:23:50.123 could appear to be in the
        // future compared with 11:23:50. To compensate for this, we wait a
        // second until the time is 11:23:51. Then 11:23:50.123 will appear to
        // be in the past.
        TimeUnit.SECONDS.sleep( 1 );

        // Generate resource tables and analytics tables
        analyticsTableGenerator.generateTables( AnalyticsTableUpdateParams.newBuilder().build(),
            NoopJobProgress.INSTANCE );
    }

    /**
     * Saves events. Since the store is called directly, wraps the call in a
     * transaction. Since transactional methods must be overridable, the method
     * is protected.
     */
    @Transactional
    protected void saveEvents( List<ProgramStageInstance> events )
    {
        eventStore.saveEvents( events );
    }

    /**
     * Adds a program ownership history entry.
     */
    private void addProgramOwnershipHistory( Program program, TrackedEntityInstance tei, OrganisationUnit ou,
        Date startDate, Date endDate )
    {
        ProgramOwnershipHistory poh = new ProgramOwnershipHistory( program, tei, ou, startDate, endDate, "admin" );

        programOwnershipHistoryService.addProgramOwnershipHistory( poh );
    }

    @Override
    public void tearDownTest()
    {
        for ( AnalyticsTableService service : analyticsTableServices )
        {
            service.dropTables();
        }
    }

    @BeforeEach
    public void beforeEach()
    {
        // Reset the security context for each test.
        clearSecurityContext();
    }

    // -------------------------------------------------------------------------
    // Test dimension restrictions
    // -------------------------------------------------------------------------

    @Test
    void testDimensionRestrictionSuccessfully()
    {
        // Given
        // The category options are readable by the user
        coA.getSharing().setOwner( userA );
        coB.getSharing().setOwner( userA );
        enableDataSharing( userA, coA, "rwrw----" );
        enableDataSharing( userA, coB, "rwrw----" );
        categoryService.updateCategoryOption( coA );
        categoryService.updateCategoryOption( coB );

        // The categories are readable by the user
        caA.getSharing().setOwner( userA );
        caB.getSharing().setOwner( userA );
        categoryService.updateCategory( caA );
        categoryService.updateCategory( caB );

        // The user is active
        injectSecurityContext( userA );

        // All events in 2017
        EventQueryParams events_2017_params = new EventQueryParams.Builder()
            .withOrganisationUnits( Lists.newArrayList( ouD ) ).withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 12, 31 ) ).withProgram( programB ).build();

        // Then
        assertDoesNotThrow( () -> eventTarget.getAggregatedEventData( events_2017_params ) );
    }

    @Test
    void testDimensionRestrictionWhenUserCannotReadCategoryOptions()
    {
        // Given
        // The category options are not readable by the user
        coA.getSharing().setOwner( "cannotRead" );
        coB.getSharing().setOwner( "cannotRead" );
        removeUserAccess( coA );
        removeUserAccess( coB );
        categoryService.updateCategoryOption( coA );
        categoryService.updateCategoryOption( coB );

        // The category is not readable by the user
        caA.getSharing().setOwner( "cannotRead" );
        caB.getSharing().setOwner( "cannotRead" );
        categoryService.updateCategory( caA );
        categoryService.updateCategory( caB );

        // The user is active
        injectSecurityContext( userA );

        // All events in 2017
        EventQueryParams events_2017_params = new EventQueryParams.Builder()
            .withOrganisationUnits( Lists.newArrayList( ouD ) ).withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 12, 31 ) ).withProgram( programB ).build();

        // Then
        Throwable exception = assertThrows( IllegalQueryException.class,
            () -> eventTarget.getAggregatedEventData( events_2017_params ) );
        assertThat( exception.getMessage(),
            containsString( "Current user is constrained by a dimension but has access to no dimension items" ) );
    }

    // -------------------------------------------------------------------------
    // Test getAggregatedEventData with OrgUnitFields
    // -------------------------------------------------------------------------

    @Test
    void testGetAggregatedEventDataWithRegistrationOrgUnit()
    {
        EventQueryParams params = getAggregatedQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "REGISTRATION" ) )
            .build();

        Grid grid = eventTarget.getAggregatedEventData( params );

        assertGridContains(
            // Headers
            List.of( "pe", "ou" ),
            // Grid
            List.of(
                List.of( "201701", "ouabcdefghD" ),
                List.of( "201702", "ouabcdefghD" ),
                List.of( "201703", "ouabcdefghD" ) ),
            grid );
    }

    @Test
    void testGetAggregatedEventDataWithEnrollmentOrgUnit()
    {
        EventQueryParams params = getAggregatedQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "ENROLLMENT" ) )
            .build();

        Grid grid = eventTarget.getAggregatedEventData( params );

        assertGridContains(
            // Headers
            List.of( "pe", "ou" ),
            // Grid
            List.of(
                List.of( "201701", "ouabcdefghE" ),
                List.of( "201702", "ouabcdefghE" ),
                List.of( "201703", "ouabcdefghE" ) ),
            grid );
    }

    @Test
    void testGetAggregatedEventDataWithOwnerAtStart()
    {
        EventQueryParams params = getAggregatedQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "OWNER_AT_START" ) )
            .build();

        Grid grid = eventTarget.getAggregatedEventData( params );

        assertGridContains(
            // Headers
            List.of( "pe", "ou" ),
            // Grid
            List.of(
                List.of( "201701", "ouabcdefghE" ),
                List.of( "201702", "ouabcdefghF" ),
                List.of( "201703", "ouabcdefghG" ) ),
            grid );
    }

    @Test
    void testGetAggregatedEventDataWithOwnerAtEnd()
    {
        EventQueryParams params = getAggregatedQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "OWNER_AT_END" ) )
            .build();

        Grid grid = eventTarget.getAggregatedEventData( params );

        assertGridContains(
            // Headers
            List.of( "pe", "ou" ),
            // Grid
            List.of(
                List.of( "201701", "ouabcdefghF" ),
                List.of( "201702", "ouabcdefghG" ),
                List.of( "201703", "ouabcdefghH" ) ),
            grid );
    }

    @Test
    void testGetAggregatedEventDataWithDefaultEventOrgUnit()
    {
        EventQueryParams params = getAggregatedQueryBuilderA()
            .build();

        Grid grid = eventTarget.getAggregatedEventData( params );

        assertGridContains(
            // Headers
            List.of( "pe", "ou" ),
            // Grid
            List.of(
                List.of( "201701", "ouabcdefghI" ),
                List.of( "201702", "ouabcdefghJ" ),
                List.of( "201703", "ouabcdefghK" ) ),
            grid );
    }

    @Test
    void testGetAggregatedEventDataWithDataElementOrgUnit()
    {
        EventQueryParams params = getAggregatedQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( deU.getUid() ) )
            .build();

        Grid grid = eventTarget.getAggregatedEventData( params );

        assertGridContains(
            // Headers
            List.of( "pe", "ou" ),
            // Grid
            List.of(
                List.of( "201701", "ouabcdefghL" ),
                List.of( "201702", "ouabcdefghM" ),
                List.of( "201703", "ouabcdefghN" ) ),
            grid );
    }

    // -------------------------------------------------------------------------
    // Test getEvents with OrgUnitFields
    // -------------------------------------------------------------------------

    @Test
    void testGetEventsWithRegistrationOrgUnit()
    {
        EventQueryParams params = getEventQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "REGISTRATION" ) )
            .build();

        Grid grid = eventTarget.getEvents( params );

        assertGridContains(
            // Headers
            List.of( "eventdate", "ou", "deOrgUnitU" ),
            // Grid
            List.of(
                List.of( "2017-01-15 00:00:00.0", "ouabcdefghD", "OrganisationUnitL" ),
                List.of( "2017-02-15 00:00:00.0", "ouabcdefghD", "OrganisationUnitM" ),
                List.of( "2017-03-15 00:00:00.0", "ouabcdefghD", "OrganisationUnitN" ) ),
            grid );
    }

    @Test
    void testGetEventsWithEnrollmentOrgUnit()
    {
        EventQueryParams params = getEventQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "ENROLLMENT" ) )
            .build();

        Grid grid = eventTarget.getEvents( params );

        assertGridContains(
            // Headers
            List.of( "eventdate", "ou", "deOrgUnitU" ),
            // Grid
            List.of(
                List.of( "2017-01-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitL" ),
                List.of( "2017-02-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitM" ),
                List.of( "2017-03-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitN" ) ),
            grid );
    }

    @Test
    void testGetEventsWithOwnerAtStart()
    {
        EventQueryParams params = getEventQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "OWNER_AT_START" ) )
            .build();

        Grid grid = eventTarget.getEvents( params );

        // Note that owner at start does not change with each event because
        // there is no monthly aggregation.
        assertGridContains(
            // Headers
            List.of( "eventdate", "ou", "deOrgUnitU" ),
            // Grid
            List.of(
                List.of( "2017-01-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitL" ),
                List.of( "2017-02-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitM" ),
                List.of( "2017-03-15 00:00:00.0", "ouabcdefghE", "OrganisationUnitN" ) ),
            grid );
    }

    @Test
    void testGetEventsWithOwnerAtEnd()
    {
        EventQueryParams params = getEventQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "OWNER_AT_END" ) )
            .build();

        Grid grid = eventTarget.getEvents( params );

        // Note that owner at end does not change with each event because
        // there is no monthly aggregation.
        assertGridContains(
            // Headers
            List.of( "eventdate", "ou", "deOrgUnitU" ),
            // Grid
            List.of(
                List.of( "2017-01-15 00:00:00.0", "ouabcdefghH", "OrganisationUnitL" ),
                List.of( "2017-02-15 00:00:00.0", "ouabcdefghH", "OrganisationUnitM" ),
                List.of( "2017-03-15 00:00:00.0", "ouabcdefghH", "OrganisationUnitN" ) ),
            grid );
    }

    @Test
    void testGetEventsWithDefaultEventOrgUnit()
    {
        EventQueryParams params = getEventQueryBuilderA()
            .build();

        Grid grid = eventTarget.getEvents( params );

        assertGridContains(
            // Headers
            List.of( "eventdate", "ou", "deOrgUnitU" ),
            // Grid
            List.of(
                List.of( "2017-01-15 00:00:00.0", "ouabcdefghI", "OrganisationUnitL" ),
                List.of( "2017-02-15 00:00:00.0", "ouabcdefghJ", "OrganisationUnitM" ),
                List.of( "2017-03-15 00:00:00.0", "ouabcdefghK", "OrganisationUnitN" ) ),
            grid );
    }

    @Test
    void testGetEventsWithDataElementOrgUnit()
    {
        EventQueryParams params = getEventQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( deU.getUid() ) )
            .build();

        Grid grid = eventTarget.getEvents( params );

        assertGridContains(
            // Headers
            List.of( "eventdate", "ou", "deOrgUnitU" ),
            // Grid
            List.of(
                List.of( "2017-01-15 00:00:00.0", "ouabcdefghL", "OrganisationUnitL" ),
                List.of( "2017-02-15 00:00:00.0", "ouabcdefghM", "OrganisationUnitM" ),
                List.of( "2017-03-15 00:00:00.0", "ouabcdefghN", "OrganisationUnitN" ) ),
            grid );
    }

    // -------------------------------------------------------------------------
    // Test getEnrollments with OrgUnitFields
    // -------------------------------------------------------------------------

    @Test
    void testGetEnrollmentsWithRegistrationOrgUnit()
    {
        EventQueryParams params = getEnrollmentQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "REGISTRATION" ) )
            .build();

        Grid grid = enrollmentTarget.getEnrollments( params );

        assertGridContains(
            // Headers
            List.of( "enrollmentdate", "ou", "tei", "teaAttribuU" ),
            // Grid
            List.of(
                List.of( "2017-01-01 00:00:00.0", "ouabcdefghD", "trackEntInA", "ouabcdefghF" ) ),
            grid );
    }

    @Test
    void testGetEnrollmentsWithEnrollmentOrgUnit()
    {
        EventQueryParams params = getEnrollmentQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "ENROLLMENT" ) )
            .build();

        Grid grid = enrollmentTarget.getEnrollments( params );

        assertGridContains(
            // Headers
            List.of( "enrollmentdate", "ou", "ouname", "teaAttribuU" ),
            // Grid
            List.of(
                List.of( "2017-01-01 00:00:00.0", "ouabcdefghE", "OrganisationUnitE", "ouabcdefghF" ) ),
            grid );
    }

    @Test
    void testGetEnrollmentsWithOwnerAtStart()
    {
        EventQueryParams params = getEnrollmentQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "OWNER_AT_START" ) )
            .build();

        Grid grid = enrollmentTarget.getEnrollments( params );

        assertGridContains(
            // Headers
            List.of( "enrollmentdate", "ou", "oucode", "teaAttribuU" ),
            // Grid
            List.of(
                List.of( "2017-01-01 00:00:00.0", "ouabcdefghE", "OrganisationUnitCodeE", "ouabcdefghF" ) ),
            grid );
    }

    @Test
    void testGetEnrollmentsWithOwnerAtEnd()
    {
        EventQueryParams params = getEnrollmentQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( "OWNER_AT_END" ) )
            .build();

        Grid grid = enrollmentTarget.getEnrollments( params );

        assertGridContains(
            // Headers
            List.of( "enrollmentdate", "ou", "tei", "teaAttribuU" ),
            // Grid
            List.of(
                List.of( "2017-01-01 00:00:00.0", "ouabcdefghH", "trackEntInA", "ouabcdefghF" ) ),
            grid );
    }

    @Test
    void testGetEnrollmentsWithDefaultEnrollmentOrgUnit()
    {
        EventQueryParams params = getEnrollmentQueryBuilderA()
            .build();

        Grid grid = enrollmentTarget.getEnrollments( params );

        assertGridContains(
            // Headers
            List.of( "enrollmentdate", "ou", "tei", "teaAttribuU" ),
            // Grid
            List.of(
                List.of( "2017-01-01 00:00:00.0", "ouabcdefghE", "trackEntInA", "ouabcdefghF" ) ),
            grid );
    }

    @Test
    void testGetEnrollmentsWithAttributeOrgUnit()
    {
        EventQueryParams params = getEnrollmentQueryBuilderA()
            .withOrgUnitField( new OrgUnitField( atU.getUid() ) )
            .build();

        Grid grid = enrollmentTarget.getEnrollments( params );

        assertGridContains(
            // Headers
            List.of( "enrollmentdate", "ou", "tei", "teaAttribuU" ),
            // Grid
            List.of(
                List.of( "2017-01-01 00:00:00.0", "ouabcdefghF", "trackEntInA", "ouabcdefghF" ) ),
            grid );
    }

    // -------------------------------------------------------------------------
    // Supportive test methods
    // -------------------------------------------------------------------------

    /**
     * Params builder A for getting aggregated grids.
     */
    private EventQueryParams.Builder getAggregatedQueryBuilderA()
    {
        return new EventQueryParams.Builder()
            .withProgram( programA )
            .withProgramStage( psA )
            .withOutputType( EventOutputType.EVENT )
            .withDisplayProperty( DisplayProperty.SHORTNAME )
            .withEndpointItem( RequestTypeAware.EndpointItem.EVENT )
            .withPeriods( List.of( peJan, peFeb, peMar ), "Monthly" )
            .withOrganisationUnits( level3Ous )
            .withCoordinateField( "pisgeometry" )
            .withFallbackCoordinateField( "ougeometry" );
    }

    /**
     * Params builder A for getting events.
     */
    private EventQueryParams.Builder getEventQueryBuilderA()
    {
        return getEnrollmentQueryBuilderA()
            .withProgramStage( psA )
            .addItem( new QueryItem( deU, null, deU.getValueType(), deU.getAggregationType(), null ) );
    }

    /**
     * Params builder A for getting enrollments.
     */
    private EventQueryParams.Builder getEnrollmentQueryBuilderA()
    {
        BaseDimensionalObject periodDimension = new BaseDimensionalObject(
            PERIOD_DIM_ID, DimensionType.PERIOD, getList( peJan, peFeb, peMar ) );

        BaseDimensionalObject orgUnitDimension = new BaseDimensionalObject(
            ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA ) );

        return new EventQueryParams.Builder()
            .withProgram( programA )
            .addItem( new QueryItem( atU, null, atU.getValueType(), atU.getAggregationType(), null ) )
            .withOutputType( EventOutputType.EVENT )
            .withDisplayProperty( DisplayProperty.SHORTNAME )
            .withEndpointItem( RequestTypeAware.EndpointItem.EVENT )
            .addDimension( orgUnitDimension )
            .addDimension( periodDimension )
            .withPeriods( List.of( peJan, peFeb, peMar ), "Monthly" )
            .withCoordinateField( "pisgeometry" )
            .withFallbackCoordinateField( "ougeometry" );
    }

    /**
     * Asserts that a grid:
     * <ol>
     * <li>contains a given number of rows</li>
     * <li>includes the given column headers</li>
     * <li>has rows with expected values in those columns</li>
     * </ol>
     * Note that the headers and the expected values do not have to include
     * every column in the grid. They need only include those columns needed for
     * the test.
     * <p>
     * The grid rows may be found in any order. The expected and actual rows are
     * converted to text strings and sorted.
     */
    private void assertGridContains( List<String> headers, List<List<Object>> rows, Grid grid )
    {
        // Assert grid contains the expected number of rows
        assertEquals( rows.size(), grid.getHeight(), "Expected " + rows.size() + " rows in grid " + grid );

        // Make a map from header name to grid column index
        Map<String, Integer> headerMap = range( 0, grid.getHeaders().size() ).boxed()
            .collect( Collectors.toMap( i -> grid.getHeaders().get( i ).getName(), identity() ) );

        // Assert grid contains all the expected headers (column names)
        assertTrue( headerMap.keySet().containsAll( headers ), "Expected headers " + headers + " in grid " + grid );

        // Make colA:row1value/colB:row1value, colA:row2value/colB:row2value...
        List<String> expected = rows.stream()
            .map( r -> flattenExpectedRow( headers, r ) )
            .sorted()
            .collect( toList() );

        // Make colA:row1value/colB:row1value, colA:row2value/colB:row2value...
        List<String> actual = grid.getRows().stream()
            .map( r -> flattenGridRow( headers, headerMap, r ) )
            .sorted()
            .collect( toList() );

        // Assert the expected rows are present with the expected values
        assertEquals( expected, actual );
    }

    /**
     * Concatenates the headers and values that are expected to be contained in
     * a grid row. Returns colA:value/colB:value/...
     */
    private String flattenExpectedRow( List<String> headers, List<Object> values )
    {
        return range( 0, headers.size() ).boxed()
            .map( i -> headers.get( i ) + ":" + values.get( i ) )
            .collect( joining( "/" ) );
    }

    /**
     * Concatenates the headers and values from a returned grid row. Returns
     * colA:value/colB:value/...
     */
    private String flattenGridRow( List<String> headers, Map<String, Integer> headerMap, List<Object> values )
    {
        return range( 0, headers.size() ).boxed()
            .map( i -> headers.get( i ) + ":" + values.get( headerMap.get( headers.get( i ) ) ) )
            .collect( joining( "/" ) );
    }
}
