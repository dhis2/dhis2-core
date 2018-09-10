package org.hisp.dhis.dxf2.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.UserService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
public class EventImportTest extends DhisSpringTest
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

        org.hisp.dhis.trackedentity.TrackedEntityInstance maleA = createTrackedEntityInstance( 'A', organisationUnitA );

        maleA.setTrackedEntityType( trackedEntityType );

        manager.save( maleA );

        trackedEntityInstanceMaleA = trackedEntityInstanceService.getTrackedEntityInstance( maleA );

        dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.INTEGER );
        manager.save( dataElementA );

        dataElementA2 = createDataElement( 'a' );
        dataElementA2.setValueType( ValueType.INTEGER );
        manager.save( dataElementA2 );

        dataElementB = createDataElement( 'B' );
        dataElementB.setValueType( ValueType.INTEGER );
        manager.save( dataElementB );

        programStageA = createProgramStage( 'A', 0 );
        manager.save( programStageA );

        programStageA2 = createProgramStage( 'a', 0 );
        programStageA2.setRepeatable( true );
        manager.save( programStageA2 );

        programStageB = createProgramStage( 'B', 0 );
        manager.save( programStageB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        manager.save( programA );

        programB = createProgram( 'B', new HashSet<>(), organisationUnitB );
        programB.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        manager.save( programB );

        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA );
        programStageDataElement.setProgramStage( programStageA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );

        ProgramStageDataElement programStageDataElementA2 = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA2 );
        programStageDataElement.setProgramStage( programStageA2 );
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

        createUserAndInjectSecurityContext( true );
    }

    @Test
    public void testAddEventOnProgramWithoutRegistration()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(), organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
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

        InputStream is = createEventJsonInputStream( programB.getUid(), programStageB.getUid(), organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
    public void testAddEventOnNonExistentProgram()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( "nonexistent", programStageB.getUid(), organisationUnitB.getUid(), null, dataElementB, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(), CoreMatchers.containsString( "does not point to a valid program" ) );

    }

    @Test
    public void testAddEventOnNonExistentProgramStage()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), "nonexistent", organisationUnitA.getUid(), null, dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(), CoreMatchers.containsString( "does not point to a valid programStage" ) );

    }

    @Test
    public void testAddEventOnProgramWithRegistration()
        throws IOException
    {
        Enrollment enrollment = createEnrollment( programA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
    public void testAddEventOnProgramWithRegistrationWithoutTei()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), null, dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(),
            CoreMatchers.containsString( "No Event.trackedEntityInstance was provided for registration based program" ) );
    }

    @Test
    public void testAddEventOnProgramWithRegistrationWithInvalidTei()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), "nonexistent", dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(),
            CoreMatchers.containsString( "Event.trackedEntityInstance does not point to a valid tracked entity instance" ) );

    }

    @Test
    public void testAddEventOnProgramWithRegistrationButWithoutEnrollment()
        throws IOException
    {
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
        assertThat( importSummaries.getImportSummaries().get( 0 ).getDescription(), CoreMatchers.containsString( "is not enrolled in program" ) );
    }

    @Test
    public void testAddEventOnRepeatableProgramStageWithRegistration()
        throws IOException
    {
        Enrollment enrollment = createEnrollment( programA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        ImportSummary importSummary = enrollmentService.addEnrollment( enrollment, null, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        InputStream is = createEventJsonInputStream( programA.getUid(), programStageA2.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(), dataElementA2, "10" );
        ImportSummaries importSummaries = eventService.addEventsJson( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    private InputStream createEventJsonInputStream( String program, String programStage, String orgUnit, String person, DataElement dataElement, String value )
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

        JSONArray dataValues = new JSONArray();
        dataValues.add( dataValue );
        eventJsonPayload.put( "dataValues", dataValues );

        InputStream is = new ByteArrayInputStream( eventJsonPayload.toString().getBytes() );

        return is;
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
}
