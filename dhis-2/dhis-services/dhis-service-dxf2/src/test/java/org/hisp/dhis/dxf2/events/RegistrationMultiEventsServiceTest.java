package org.hisp.dhis.dxf2.events;

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

import org.hamcrest.CoreMatchers;
import org.hibernate.SessionFactory;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class RegistrationMultiEventsServiceTest
    extends DhisSpringTest
{
    @Autowired
    private EventService eventService;

    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private UserService _userService;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleB;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB;

    private TrackedEntityInstance trackedEntityInstanceMaleA;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private Program programA;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private ProgramStage programStageA;

    private ProgramStage programStageB;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );
        identifiableObjectManager.save( organisationUnitA );
        identifiableObjectManager.save( organisationUnitB );

        TrackedEntity trackedEntity = createTrackedEntity( 'A' );
        trackedEntityService.addTrackedEntity( trackedEntity );

        maleA = createTrackedEntityInstance( 'A', organisationUnitA );
        maleB = createTrackedEntityInstance( 'B', organisationUnitB );
        femaleA = createTrackedEntityInstance( 'C', organisationUnitA );
        femaleB = createTrackedEntityInstance( 'D', organisationUnitB );

        maleA.setTrackedEntity( trackedEntity );
        maleB.setTrackedEntity( trackedEntity );
        femaleA.setTrackedEntity( trackedEntity );
        femaleB.setTrackedEntity( trackedEntity );

        identifiableObjectManager.save( maleA );
        identifiableObjectManager.save( maleB );
        identifiableObjectManager.save( femaleA );
        identifiableObjectManager.save( femaleB );

        trackedEntityInstanceMaleA = trackedEntityInstanceService.getTrackedEntityInstance( maleA );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementA.setValueType( ValueType.INTEGER );
        dataElementB.setValueType( ValueType.INTEGER );

        identifiableObjectManager.save( dataElementA );
        identifiableObjectManager.save( dataElementB );

        programStageA = createProgramStage( 'A', 0 );
        programStageB = createProgramStage( 'B', 0 );
        programStageB.setRepeatable( true );

        identifiableObjectManager.save( programStageA );
        identifiableObjectManager.save( programStageB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        identifiableObjectManager.save( programA );

        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA );
        programStageDataElement.setProgramStage( programStageA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );

        programStageA.getProgramStageDataElements().add( programStageDataElement );
        programStageA.setProgram( programA );

        programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementB );
        programStageDataElement.setProgramStage( programStageB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );

        programStageB.getProgramStageDataElements().add( programStageDataElement );
        programStageB.setProgram( programA );
        programStageB.setMinDaysFromStart( 2 );

        programA.getProgramStages().add( programStageA );
        programA.getProgramStages().add( programStageB );

        identifiableObjectManager.update( programStageA );
        identifiableObjectManager.update( programStageB );
        identifiableObjectManager.update( programA );

        createUserAndInjectSecurityContext( true );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testSaveWithoutProgramStageShouldFail()
    {
        Event event = createEvent( programA.getUid(), null, organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
        assertThat( importSummary.getDescription(),
            CoreMatchers.containsString( "Event.programStage does not point to a valid programStage" ) );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testSaveWithoutEnrollmentShouldFail()
    {
        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
        assertThat( importSummary.getDescription(), CoreMatchers.containsString( "is not enrolled in program" ) );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testSaveRepeatableStageWithoutEventIdShouldCreateNewEvent()
    {
        Enrollment enrollment = createEnrollment( programA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA.getUid() );
        importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        event = createEvent( programA.getUid(), programStageB.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementB.getUid() );
        importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        sessionFactory.getCurrentSession().flush();

        EventSearchParams params = new EventSearchParams();
        params.setProgram( programA );
        params.setOrgUnit( organisationUnitA );
        params.setOrgUnitSelectionMode( OrganisationUnitSelectionMode.SELECTED );

        assertEquals( 2, eventService.getEvents( params ).getEvents().size() );

        event = createEvent( programA.getUid(), programStageB.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementB.getUid() );

        importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        assertEquals( 3, eventService.getEvents( params ).getEvents().size() );
    }

    @Test
    @Category( IntegrationTest.class )
    public void testSaveRepeatableStageWithEventIdShouldNotCreateAdditionalEvents()
    {
        Enrollment enrollment = createEnrollment( programA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA.getUid() );
        importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        event = createEvent( programA.getUid(), programStageB.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementB.getUid() );
        importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        sessionFactory.getCurrentSession().flush();

        EventSearchParams params = new EventSearchParams();
        params.setProgram( programA );
        params.setOrgUnit( organisationUnitA );
        params.setOrgUnitSelectionMode( OrganisationUnitSelectionMode.SELECTED );

        assertEquals( 2, eventService.getEvents( params ).getEvents().size() );

        event = createEvent( programA.getUid(), programStageB.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementB.getUid() );
        event.setEvent( importSummary.getReference() );
        importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        assertEquals( 2, eventService.getEvents( params ).getEvents().size() );

        event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA.getUid() );
        importSummary = eventService.addEvent( event, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        assertEquals( 2, eventService.getEvents( params ).getEvents().size() );
    }

    // -------------------------------------------------------------------------
    // Supportive tests
    // -------------------------------------------------------------------------

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

    private Event createEvent( String program, String programStage, String orgUnit, String person, String dataElement )
    {
        Event event = new Event();
        event.setProgram( program );
        event.setProgramStage( programStage );
        event.setOrgUnit( orgUnit );
        event.setTrackedEntityInstance( person );
        event.setEventDate( "2013-01-01" );

        event.getDataValues().add( new DataValue( dataElement, "10" ) );

        return event;
    }
}
