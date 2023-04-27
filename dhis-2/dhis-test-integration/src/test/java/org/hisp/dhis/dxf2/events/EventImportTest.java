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
package org.hisp.dhis.dxf2.events;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exparity.hamcrest.date.DateMatchers;
import org.hamcrest.CoreMatchers;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
class EventImportTest extends TransactionalIntegrationTest
{
    private static final String DUE_DATE = "2021-02-28T13:05:00";

    private static final String EVENT_DATE = "2021-02-25T12:15:00";

    @Autowired
    private EventService eventService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private UserService _userService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private TrackedEntityInstance trackedEntityInstanceMaleA;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private DataElement dataElementA;

    private DataElement dataElementA2;

    private DataElement dataElementB;

    private Program programA;

    private Program programB;

    private ProgramStage programStageA;

    private ProgramStage programStageA2;

    private ProgramStage programStageB;

    private ProgramInstance pi;

    private org.hisp.dhis.dxf2.events.event.Event event;

    private User superUser;

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat( DateUtils.ISO8601_NO_TZ_PATTERN );

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
        superUser = preCreateInjectAdminUser();
        injectSecurityContext( superUser );

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );
        manager.save( organisationUnitA );
        manager.save( organisationUnitB );
        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );
        org.hisp.dhis.trackedentity.TrackedEntityInstance maleA = createTrackedEntityInstance( organisationUnitA );
        maleA.setTrackedEntityType( trackedEntityType );
        manager.save( maleA );
        trackedEntityInstanceMaleA = trackedEntityInstanceService.getTrackedEntityInstance( maleA );
        CategoryOption categoryOption1 = new CategoryOption( "male" );
        categoryOption1.setAutoFields();
        CategoryOption categoryOption2 = new CategoryOption( "female" );
        categoryOption2.setAutoFields();
        manager.save( Lists.newArrayList( categoryOption1, categoryOption2 ) );
        Category cat1 = new Category( "cat1", DataDimensionType.DISAGGREGATION );
        cat1.setShortName( cat1.getName() );
        cat1.setCategoryOptions( Lists.newArrayList( categoryOption1, categoryOption2 ) );
        manager.save( Lists.newArrayList( cat1 ) );
        CategoryCombo categoryCombo = manager.getByName( CategoryCombo.class, "default" );
        categoryCombo.setCategories( Lists.newArrayList( cat1 ) );
        dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.INTEGER );
        dataElementA.setCategoryCombo( categoryCombo );
        manager.save( dataElementA );
        dataElementA2 = createDataElement( 'a' );
        dataElementA2.setValueType( ValueType.INTEGER );
        dataElementA2.setCategoryCombo( categoryCombo );
        manager.save( dataElementA2 );
        dataElementB = createDataElement( 'B' );
        dataElementB.setValueType( ValueType.INTEGER );
        dataElementB.setCategoryCombo( categoryCombo );
        manager.save( dataElementB );
        programStageA = createProgramStage( 'A', 0 );
        programStageA.setFeatureType( FeatureType.POINT );
        manager.save( programStageA );
        programStageA2 = createProgramStage( 'a', 0 );
        programStageA2.setFeatureType( FeatureType.POINT );
        programStageA2.setRepeatable( true );
        manager.save( programStageA2 );
        programStageB = createProgramStage( 'B', 0 );
        programStageB.setFeatureType( FeatureType.POINT );
        manager.save( programStageB );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        programA.setCategoryCombo( categoryCombo );
        manager.save( programA );
        programB = createProgram( 'B', new HashSet<>(), organisationUnitB );
        programB.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        programB.setCategoryCombo( categoryCombo );
        manager.save( programB );
        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA );
        programStageDataElement.setProgramStage( programStageA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        ProgramStageDataElement programStageDataElementA2 = new ProgramStageDataElement();
        programStageDataElementA2.setDataElement( dataElementA2 );
        programStageDataElementA2.setProgramStage( programStageA2 );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementA2 );
        ProgramStageDataElement programStageDataElementB = new ProgramStageDataElement();
        programStageDataElementB.setDataElement( dataElementB );
        programStageDataElementB.setProgramStage( programStageB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );
        programStageA.getProgramStageDataElements().add( programStageDataElement );
        programStageA2.getProgramStageDataElements().add( programStageDataElementA2 );
        programStageA.setProgram( programA );
        programStageA2.setProgram( programA );
        programA.getProgramStages().add( programStageA );
        programA.getProgramStages().add( programStageA2 );
        programStageB.getProgramStageDataElements().add( programStageDataElementB );
        programStageB.setProgram( programB );
        programB.getProgramStages().add( programStageB );
        manager.update( programStageA );
        manager.update( programStageA2 );
        manager.update( programA );
        manager.update( programStageB );
        manager.update( programB );
        pi = new ProgramInstance();
        pi.setEnrollmentDate( new Date() );
        pi.setIncidentDate( new Date() );
        pi.setProgram( programB );
        pi.setStatus( ProgramStatus.ACTIVE );
        pi.setStoredBy( "test" );
        pi.setName( "EventImportTestPI" );
        pi.setUid( CodeGenerator.generateUid() );
        manager.save( pi );
        event = createEvent( "eventUid001" );
        createUserAndInjectSecurityContext( true );
    }

    @Test
    void testAddEventOnProgramWithoutRegistration()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(),
            organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
    void testAddEventWithDueDateForProgramWithoutRegistration()
    {
        String eventUid = CodeGenerator.generateUid();

        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        org.hisp.dhis.dxf2.events.event.Event event = createScheduledTrackerEvent( eventUid, programA, programStageA,
            EventStatus.SCHEDULE,
            organisationUnitA );

        ImportSummary summary = eventService.addEvent( event, null, false );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        DataValue dataValue = new DataValue();
        dataValue.setValue( "10" );
        dataValue.setDataElement( dataElementA.getUid() );
        event.setDataValues( Set.of( dataValue ) );
        event.setStatus( EventStatus.COMPLETED );

        summary = eventService.updateEvent( event, true, null, false );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        Event psi = programStageInstanceService.getProgramStageInstance( eventUid );

        assertEquals( DUE_DATE, DateUtils.getLongDateString( psi.getDueDate() ) );
    }

    /**
     * TODO: LUCIANO: this test has been ignored because the Importer should not
     * import an event linked to a Program with 2 or more Program Instances
     */
    @Test
    @Disabled
    void testAddEventOnProgramWithoutRegistrationAndExistingProgramInstance()
        throws IOException
    {
        ProgramInstance pi = new ProgramInstance();
        pi.setEnrollmentDate( new Date() );
        pi.setIncidentDate( new Date() );
        pi.setProgram( programB );
        pi.setStatus( ProgramStatus.ACTIVE );
        pi.setStoredBy( "test" );
        programInstanceService.addProgramInstance( pi );
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(),
            organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
    void testAddEventOnNonExistentProgram()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( "null", programStageB.getUid(), organisationUnitB.getUid(), null,
            dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(),
            CoreMatchers.containsString( "does not point to a valid program" ) );
    }

    @Test
    void testAddEventOnNonExistentProgramStage()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), "null", organisationUnitA.getUid(), null,
            dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(),
            CoreMatchers.containsString( "does not point to a valid programStage" ) );
    }

    @Test
    void testAddEventOnProgramWithRegistration()
        throws IOException,
        ParseException
    {
        String lastUpdateDateBefore = trackedEntityInstanceService
            .getTrackedEntityInstance( trackedEntityInstanceMaleA.getTrackedEntityInstance() ).getLastUpdated();
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
        cleanSession();

        // We use JDBC to get the timestamp, since it's stored using JDBC not
        // hibernate.
        String lastUpdateDateNew = DateUtils.getIso8601NoTz( this.jdbcTemplate.queryForObject(
            "SELECT lastupdated FROM trackedentityinstance WHERE uid IN ('"
                + trackedEntityInstanceMaleA.getTrackedEntityInstance() + "')",
            Timestamp.class ) );

        assertTrue( simpleDateFormat
            .parse( lastUpdateDateNew )
            .getTime() > simpleDateFormat
                .parse( lastUpdateDateBefore )
                .getTime() );
    }

    @Test
    void testAddEventOnProgramWithRegistrationWithoutTei()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid(), null, dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(), CoreMatchers
            .containsString( "Event.trackedEntityInstance does not point to a valid tracked entity instance: null" ) );
    }

    @Test
    void testAddEventOnProgramWithRegistrationWithInvalidTei()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid(), "null", dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(), CoreMatchers
            .containsString( "Event.trackedEntityInstance does not point to a valid tracked entity instance: null" ) );
    }

    @Test
    void testAddEventOnProgramWithRegistrationButWithoutEnrollment()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(),
            CoreMatchers.containsString( "is not enrolled in program" ) );
    }

    @Test
    void testAddEventOnRepeatableProgramStageWithRegistration()
        throws IOException
    {
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA2.getUid(),
            organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA2, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
    void testAddOneValidAndOneInvalidEvent()
        throws IOException
    {
        org.hisp.dhis.dxf2.events.event.Event validEvent = createEvent( "eventUid004" );
        org.hisp.dhis.dxf2.events.event.Event invalidEvent = createEvent( "eventUid005" );
        invalidEvent.setOrgUnit( "INVALID" );
        InputStream is = createEventsJsonInputStream( Lists.newArrayList( validEvent, invalidEvent ), dataElementA,
            "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertEquals( 1, importSummaries.getImported() );
        assertEquals( 1, importSummaries.getIgnored() );
        assertEquals( 0, importSummaries.getDeleted() );
        assertEquals( 0, importSummaries.getUpdated() );
    }

    @Test
    void testAddValidEnrollmentWithOneValidAndOneInvalidEvent()
    {
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        org.hisp.dhis.dxf2.events.event.Event validEvent = createEvent( "eventUid004" );
        validEvent.setOrgUnit( organisationUnitA.getUid() );
        org.hisp.dhis.dxf2.events.event.Event invalidEvent = createEvent( "eventUid005" );
        invalidEvent.setOrgUnit( "INVALID" );
        enrollment.setEvents( Lists.newArrayList( validEvent, invalidEvent ) );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( 1, importSummary.getImportCount().getImported() );
        assertEquals( 0, importSummary.getImportCount().getIgnored() );
        assertEquals( 0, importSummary.getImportCount().getDeleted() );
        assertEquals( 0, importSummary.getImportCount().getUpdated() );
        ImportSummaries eventImportSummaries = importSummary.getEvents();
        assertEquals( ImportStatus.ERROR, eventImportSummaries.getStatus() );
        assertEquals( 1, eventImportSummaries.getImported() );
        assertEquals( 1, eventImportSummaries.getIgnored() );
        assertEquals( 0, eventImportSummaries.getDeleted() );
        assertEquals( 0, eventImportSummaries.getUpdated() );
    }

    @Test
    void testEventDeletion()
    {
        programInstanceService.addProgramInstance( pi );
        ImportOptions importOptions = new ImportOptions();
        ImportSummary importSummary = eventService.addEvent( event, importOptions, false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        Event psi = programStageInstanceService.getProgramStageInstance( event.getUid() );
        assertNotNull( psi );
        importSummary = eventService.deleteEvent( event.getUid() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        psi = programStageInstanceService.getProgramStageInstance( event.getUid() );
        assertNull( psi );
        boolean existsDeleted = programStageInstanceService
            .programStageInstanceExistsIncludingDeleted( event.getUid() );
        assertTrue( existsDeleted );
    }

    @Test
    void testAddAlreadyDeletedEvent()
    {
        programInstanceService.addProgramInstance( pi );
        ImportOptions importOptions = new ImportOptions();
        eventService.addEvent( event, importOptions, false );
        eventService.deleteEvent( event.getUid() );
        manager.flush();
        importOptions.setImportStrategy( ImportStrategy.CREATE );
        event.setDeleted( true );
        ImportSummary importSummary = eventService.addEvent( event, importOptions, false );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
        assertEquals( 1, importSummary.getImportCount().getIgnored() );
        assertTrue( importSummary.getDescription().contains( "already exists or was deleted earlier" ) );
    }

    @Test
    void testAddAlreadyDeletedEventInBulk()
    {
        programInstanceService.addProgramInstance( pi );
        ImportOptions importOptions = new ImportOptions();
        eventService.addEvent( event, importOptions, false );
        eventService.deleteEvent( event.getUid() );
        manager.flush();
        org.hisp.dhis.dxf2.events.event.Event event2 = createEvent( "eventUid002" );
        org.hisp.dhis.dxf2.events.event.Event event3 = createEvent( "eventUid003" );
        importOptions.setImportStrategy( ImportStrategy.CREATE );
        event.setDeleted( true );
        List<org.hisp.dhis.dxf2.events.event.Event> events = new ArrayList<>();
        events.add( event );
        events.add( event2 );
        events.add( event3 );
        ImportSummaries importSummaries = eventService.addEvents( events, importOptions, true );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertEquals( 1, importSummaries.getIgnored() );
        assertEquals( 2, importSummaries.getImported() );
        assertTrue( importSummaries.getImportSummaries().stream()
            .anyMatch( is -> is.getDescription().contains( "already exists or was deleted earlier" ) ) );
        manager.flush();
        List<String> uids = new ArrayList<>();
        uids.add( "eventUid001" );
        uids.add( "eventUid002" );
        uids.add( "eventUid003" );
        List<String> fetchedUids = programStageInstanceService.getProgramStageInstanceUidsIncludingDeleted( uids );
        assertTrue( Sets.difference( new HashSet<>( uids ), new HashSet<>( fetchedUids ) ).isEmpty() );
    }

    @Test
    void testGeometry()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(),
            organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    //
    // UPDATE EVENT TESTS
    //
    @Test
    void testVerifyEventCanBeUpdatedUsingProgramOnly2()
        throws IOException
    {
        // CREATE A NEW EVENT
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(),
            organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        String uid = importSummaries.getImportSummaries().get( 0 ).getReference();
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
        // FETCH NEWLY CREATED EVENT
        programStageInstanceService.getProgramStageInstance( uid );
        // UPDATE EVENT - Program is not specified
        org.hisp.dhis.dxf2.events.event.Event event = new org.hisp.dhis.dxf2.events.event.Event();
        event.setEvent( uid );
        event.setStatus( EventStatus.COMPLETED );
        final ImportSummary summary = eventService.updateEvent( event, false, ImportOptions.getDefaultImportOptions(),
            false );
        assertThat( summary.getStatus(), is( ImportStatus.ERROR ) );
        assertThat( summary.getDescription(), is( "Event.program does not point to a valid program: null" ) );
        assertThat( summary.getReference(), is( uid ) );
    }

    @Test
    void testVerifyEventCanBeUpdatedUsingProgramOnly()
        throws IOException
    {
        // CREATE A NEW EVENT
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(),
            organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        String uid = importSummaries.getImportSummaries().get( 0 ).getReference();
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
        // FETCH NEWLY CREATED EVENT
        Event psi = programStageInstanceService.getProgramStageInstance( uid );
        // UPDATE EVENT (no actual changes, except for empty data value)
        // USE ONLY PROGRAM
        org.hisp.dhis.dxf2.events.event.Event event = new org.hisp.dhis.dxf2.events.event.Event();
        event.setEvent( uid );
        event.setProgram( programB.getUid() );
        event.setStatus( EventStatus.COMPLETED );
        assertEquals( ImportStatus.SUCCESS,
            eventService.updateEvent( event, false, ImportOptions.getDefaultImportOptions(), false ).getStatus() );

        // cleanSession();
        dbmsManager.clearSession();
        Event psi2 = programStageInstanceService.getProgramStageInstance( uid );

        assertThat( psi.getLastUpdated(), DateMatchers.before( psi2.getLastUpdated() ) );
        assertThat( psi.getCreated(), is( psi2.getCreated() ) );
        assertThat( psi.getProgramInstance().getUid(), is( psi2.getProgramInstance().getUid() ) );
        assertThat( psi.getProgramStage().getUid(), is( psi2.getProgramStage().getUid() ) );
        assertThat( psi.getOrganisationUnit().getUid(), is( psi2.getOrganisationUnit().getUid() ) );
        assertThat( psi.getAttributeOptionCombo().getUid(), is( psi2.getAttributeOptionCombo().getUid() ) );
        assertThat( psi.getStatus().getValue(), is( psi2.getStatus().getValue() ) );
        assertThat( psi.getExecutionDate(), is( psi2.getExecutionDate() ) );
        assertThat( psi.getCompletedDate(), is( psi2.getCompletedDate() ) );
        assertThat( psi.getCompletedBy(), is( psi2.getCompletedBy() ) );
        assertThat( psi.isDeleted(), is( psi2.isDeleted() ) );
        assertThat( psi.getEventDataValues().size(), is( 1 ) );
        assertThat( psi2.getEventDataValues().size(), is( 0 ) );
    }

    @Test
    void testVerifyEventUncompleteSetsCompletedDateToNull()
        throws IOException
    {
        // CREATE A NEW EVENT
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(),
            organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        String uid = importSummaries.getImportSummaries().get( 0 ).getReference();
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
        // FETCH NEWLY CREATED EVENT
        Event psi = programStageInstanceService.getProgramStageInstance( uid );
        // UPDATE EVENT (no actual changes, except for empty data value and
        // status
        // change)
        org.hisp.dhis.dxf2.events.event.Event event = new org.hisp.dhis.dxf2.events.event.Event();
        event.setEvent( uid );
        event.setProgram( programB.getUid() );
        event.setStatus( EventStatus.ACTIVE );
        assertEquals( ImportStatus.SUCCESS,
            eventService.updateEvent( event, false, ImportOptions.getDefaultImportOptions(), false ).getStatus() );
        dbmsManager.clearSession();

        Event psi2 = programStageInstanceService.getProgramStageInstance( uid );
        assertThat( psi.getLastUpdated(), DateMatchers.before( psi2.getLastUpdated() ) );
        assertThat( psi.getCreated(), is( psi2.getCreated() ) );
        assertThat( psi.getProgramInstance().getUid(), is( psi2.getProgramInstance().getUid() ) );
        assertThat( psi.getProgramStage().getUid(), is( psi2.getProgramStage().getUid() ) );
        assertThat( psi.getOrganisationUnit().getUid(), is( psi2.getOrganisationUnit().getUid() ) );
        assertThat( psi.getAttributeOptionCombo().getUid(), is( psi2.getAttributeOptionCombo().getUid() ) );
        assertThat( psi2.getStatus(), is( EventStatus.ACTIVE ) );
        assertThat( psi.getExecutionDate(), is( psi2.getExecutionDate() ) );
        assertThat( psi2.getCompletedDate(), is( nullValue() ) );
        assertThat( psi.getCompletedBy(), is( psi2.getCompletedBy() ) );
        assertThat( psi.isDeleted(), is( psi2.isDeleted() ) );
        assertThat( psi.getEventDataValues().size(), is( 1 ) );
        assertThat( psi2.getEventDataValues().size(), is( 0 ) );
    }

    private void cleanSession()
    {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }

    private InputStream createEventsJsonInputStream( List<org.hisp.dhis.dxf2.events.event.Event> events,
        DataElement dataElement, String value )
    {
        JsonArray jsonArrayEvents = new JsonArray();
        events.stream().forEach( e -> jsonArrayEvents.add( createEventJsonObject( e, dataElement, value ) ) );
        JsonObject jsonEvents = new JsonObject();
        jsonEvents.add( "events", jsonArrayEvents );

        return new ByteArrayInputStream( jsonEvents.toString().getBytes() );
    }

    private InputStream createEventJsonInputStream( String program, String programStage, String orgUnit, String person,
        DataElement dataElement, String value )
    {
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( null );
        event.setProgram( program );
        event.setProgramStage( programStage );
        event.setOrgUnit( orgUnit );
        event.setTrackedEntityInstance( person );
        return new ByteArrayInputStream( createEventJsonObject( event, dataElement, value ).toString().getBytes() );
    }

    private JsonObject createEventJsonObject( org.hisp.dhis.dxf2.events.event.Event event, DataElement dataElement,
        String value )
    {
        JsonObject eventJsonPayload = new JsonObject();
        eventJsonPayload.addProperty( "program", event.getProgram() );
        eventJsonPayload.addProperty( "programStage", event.getProgramStage() );
        eventJsonPayload.addProperty( "orgUnit", event.getOrgUnit() );
        eventJsonPayload.addProperty( "status", "COMPLETED" );
        eventJsonPayload.addProperty( "eventDate", "2018-08-20" );
        eventJsonPayload.addProperty( "completedDate", "2018-08-27" );
        eventJsonPayload.addProperty( "trackedEntityInstance", event.getTrackedEntityInstance() );
        JsonObject dataValue = new JsonObject();
        dataValue.addProperty( "dataElement", dataElement.getUid() );
        dataValue.addProperty( "value", value );
        // JsonObject geometry = new JsonObject();
        // geometry.put( "type", "Point" );
        // JsonArray coordinates = new JsonArray();
        // coordinates.add( "1.33343" );
        // coordinates.add( "-21.9954" );
        // geometry.put( "coordinates", coordinates );
        // eventJsonPayload.put( "geometry", geometry );
        JsonArray dataValues = new JsonArray();
        dataValues.add( dataValue );
        eventJsonPayload.add( "dataValues", dataValues );
        return eventJsonPayload;
    }

    private Enrollment createEnrollment( String program, String person )
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setOrgUnit( organisationUnitA.getUid() );
        enrollment.setProgram( program );
        enrollment.setTrackedEntityInstance( person );
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );
        return enrollment;
    }

    private org.hisp.dhis.dxf2.events.event.Event createEvent( String uid )
    {
        org.hisp.dhis.dxf2.events.event.Event event = new org.hisp.dhis.dxf2.events.event.Event();
        event.setUid( uid );
        event.setEvent( uid );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgram( programB.getUid() );
        event.setProgramStage( programStageB.getUid() );
        event.setTrackedEntityInstance( trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setOrgUnit( organisationUnitB.getUid() );
        event.setEnrollment( pi.getUid() );
        event.setEventDate( EVENT_DATE );
        event.setDeleted( false );
        return event;
    }

    private org.hisp.dhis.dxf2.events.event.Event createScheduledTrackerEvent( String uid, Program program,
        ProgramStage ps, EventStatus eventStatus,
        OrganisationUnit organisationUnit )
    {
        org.hisp.dhis.dxf2.events.event.Event event = new org.hisp.dhis.dxf2.events.event.Event();
        event.setUid( uid );
        event.setEvent( uid );
        event.setStatus( eventStatus );
        event.setProgram( program.getUid() );
        event.setProgramStage( ps.getUid() );
        event.setTrackedEntityInstance( trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setOrgUnit( organisationUnit.getUid() );
        event.setEnrollment( pi.getUid() );
        event.setDueDate( DUE_DATE );
        event.setDeleted( false );
        return event;
    }
}
