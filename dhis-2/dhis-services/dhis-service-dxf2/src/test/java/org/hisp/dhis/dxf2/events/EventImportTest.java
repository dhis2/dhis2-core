package org.hisp.dhis.dxf2.events;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hisp.dhis.DhisSpringTest;
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
import org.hisp.dhis.dxf2.events.event.Event;
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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.UserService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
public class EventImportTest
    extends DhisSpringTest
{
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

    private Event event;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;

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
        cat1.setCategoryOptions( Lists.newArrayList( categoryOption1, categoryOption2 ) );
        manager.save( Lists.newArrayList( cat1  ) );

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

        // Flush all data to disk
        manager.flush();
    }

    @Test
    public void testAddEventOnProgramWithoutRegistration()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(),
            organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    /**
     * TODO:  LUCIANO: this test has been ignored because the Importer should not import an event linked to a Program
     * with 2 or more Program Instances
     */
    @Test
    @Ignore
    public void testAddEventOnProgramWithoutRegistrationAndExistingProgramInstance()
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
    public void testAddEventOnNonExistentProgram()
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
    public void testAddEventOnNonExistentProgramStage()
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
    public void testAddEventOnProgramWithRegistration()
        throws IOException
    {
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
    public void testAddEventOnProgramWithRegistrationWithoutTei()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid(), null, dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(),
            CoreMatchers.containsString(
                "Event.trackedEntityInstance does not point to a valid tracked entity instance: null" ) );
    }

    @Test
    public void testAddEventOnProgramWithRegistrationWithInvalidTei()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid(), "null", dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(),
            CoreMatchers.containsString(
                "Event.trackedEntityInstance does not point to a valid tracked entity instance: null" ) );
    }

    @Test
    public void testAddEventOnProgramWithRegistrationButWithoutEnrollment()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(),
            CoreMatchers.containsString( "is not enrolled in program" ) );
    }

    @Test
    public void testAddEventOnRepeatableProgramStageWithRegistration()
        throws IOException
    {
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA2.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA2, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
    public void testEventDeletion()
    {
        programInstanceService.addProgramInstance( pi );

        ImportOptions importOptions = new ImportOptions();

        ImportSummary importSummary = eventService.addEvent( event, importOptions, false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( event.getUid() );
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
    public void testAddAlreadyDeletedEvent()
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
    public void testAddAlreadyDeletedEventInBulk()
    {
        programInstanceService.addProgramInstance( pi );

        ImportOptions importOptions = new ImportOptions();

        eventService.addEvent( event, importOptions, false );
        eventService.deleteEvent( event.getUid() );

        manager.flush();

        Event event2 = createEvent( "eventUid002" );
        Event event3 = createEvent( "eventUid003" );

        importOptions.setImportStrategy( ImportStrategy.CREATE );
        event.setDeleted( true );

        List<Event> events = new ArrayList<>();
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
    public void testGeometry()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(),
            organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @SuppressWarnings( "unchecked" )
    private InputStream createEventJsonInputStream( String program, String programStage, String orgUnit, String person,
        DataElement dataElement, String value )
    {
        JSONObject eventJsonPayload = new JSONObject();
        eventJsonPayload.put( "program", program );
        eventJsonPayload.put( "programStage", programStage );
        eventJsonPayload.put( "orgUnit", orgUnit );
        eventJsonPayload.put( "status", "COMPLETED" );
        eventJsonPayload.put( "eventDate", "2018-08-20" );
        eventJsonPayload.put( "completedDate", "2018-08-27" );
        eventJsonPayload.put( "trackedEntityInstance", person );

        JSONObject dataValue = new JSONObject();
        dataValue.put( "dataElement", dataElement.getUid() );
        dataValue.put( "value", value );

//        JSONObject geometry = new JSONObject();
//        geometry.put( "type", "Point" );
//        JSONArray coordinates = new JSONArray();
//        coordinates.add( "1.33343" );
//        coordinates.add( "-21.9954" );
//        geometry.put( "coordinates", coordinates );
//        eventJsonPayload.put( "geometry", geometry );

        JSONArray dataValues = new JSONArray();
        dataValues.add( dataValue );
        eventJsonPayload.put( "dataValues", dataValues );

        return new ByteArrayInputStream( eventJsonPayload.toString().getBytes() );
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

    private Event createEvent( String uid )
    {
        Event event = new Event();
        event.setUid( uid );
        event.setEvent( uid );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgram( programB.getUid() );
        event.setProgramStage( programStageB.getUid() );
        event.setTrackedEntityInstance( trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setOrgUnit( organisationUnitB.getUid() );
        event.setEnrollment( pi.getUid() );
        event.setEventDate( "2019-10-24" );
        event.setDeleted( false );

        return event;
    }
}
