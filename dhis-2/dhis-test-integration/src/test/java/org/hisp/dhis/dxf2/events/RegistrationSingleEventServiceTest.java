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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.HashSet;

import org.hamcrest.CoreMatchers;
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
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class RegistrationSingleEventServiceTest extends TransactionalIntegrationTest
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
    private UserService _userService;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleB;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleA;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB;

    private TrackedEntityInstance trackedEntityInstanceMaleA;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private DataElement dataElementA;

    private Program programA;

    private ProgramStage programStageA;

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
        maleA = createTrackedEntityInstance( organisationUnitA );
        maleB = createTrackedEntityInstance( organisationUnitB );
        femaleA = createTrackedEntityInstance( organisationUnitA );
        femaleB = createTrackedEntityInstance( organisationUnitB );
        maleA.setTrackedEntityType( trackedEntityType );
        maleB.setTrackedEntityType( trackedEntityType );
        femaleA.setTrackedEntityType( trackedEntityType );
        femaleB.setTrackedEntityType( trackedEntityType );
        manager.save( maleA );
        manager.save( maleB );
        manager.save( femaleA );
        manager.save( femaleB );
        trackedEntityInstanceMaleA = trackedEntityInstanceService.getTrackedEntityInstance( maleA );
        dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.INTEGER );
        manager.save( dataElementA );
        programStageA = createProgramStage( 'A', 0 );
        manager.save( programStageA );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        manager.save( programA );
        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA );
        programStageDataElement.setProgramStage( programStageA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        programStageA.getProgramStageDataElements().add( programStageDataElement );
        programStageA.setProgram( programA );
        programA.getProgramStages().add( programStageA );
        manager.update( programStageA );
        manager.update( programA );
        createUserAndInjectSecurityContext( true );
    }

    @Test
    void testSaveWithoutEnrollmentShouldFail()
    {
        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = eventService.addEvent( event, null, false );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
        assertThat( importSummary.getDescription(), CoreMatchers.containsString( "is not enrolled in program" ) );
    }

    @Test
    void testSaveWithEnrollmentShouldNotFail()
    {
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        importSummary = eventService.addEvent( event, null, false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    @Disabled( "luciano -> re-enable after delete has been implemented" )
    void testDeleteEventShouldReturnReference()
    {
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        importSummary = eventService.addEvent( event, null, false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        importSummary = eventService.deleteEvent( tei.getEnrollments().get( 0 ).getEvents().get( 0 ).getEvent() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertNotNull( importSummary.getReference() );
        assertEquals( tei.getEnrollments().get( 0 ).getEvents().get( 0 ).getEvent(), importSummary.getReference() );
    }

    @Test
    void testDeleteEnrollmentShouldReturnReference()
    {
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        importSummary = enrollmentService.deleteEnrollment( tei.getEnrollments().get( 0 ).getEnrollment() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertNotNull( importSummary.getReference() );
        assertEquals( tei.getEnrollments().get( 0 ).getEnrollment(), importSummary.getReference() );
    }

    @Test
    @Disabled
    void testSavingMultipleEventsShouldOnlyUpdate()
    {
        Enrollment enrollment = createEnrollment( programA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        importSummary = eventService.addEvent( event, null, false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        EventSearchParams params = new EventSearchParams();
        params.setProgram( programA );
        params.setOrgUnit( organisationUnitA );
        params.setOrgUnitSelectionMode( OrganisationUnitSelectionMode.SELECTED );
        assertEquals( 1, eventService.getEvents( params ).getEvents().size() );
        event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        importSummary = eventService.addEvent( event, null, false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( 1, eventService.getEvents( params ).getEvents().size() );
        event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        importSummary = eventService.addEvent( event, null, false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( 1, eventService.getEvents( params ).getEvents().size() );
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

    private Event createEvent( String program, String programStage, String orgUnit, String person )
    {
        Event event = new Event();
        event.setProgram( program );
        event.setProgramStage( programStage );
        event.setOrgUnit( orgUnit );
        event.setTrackedEntityInstance( person );
        event.setEventDate( "2013-01-01" );
        event.getDataValues().add( new DataValue( dataElementA.getUid(), "10" ) );
        return event;
    }
}
