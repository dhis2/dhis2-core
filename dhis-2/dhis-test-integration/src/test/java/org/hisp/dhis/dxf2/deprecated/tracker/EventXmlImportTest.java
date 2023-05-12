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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventService;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Events;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Enrico Colasante
 */
class EventXmlImportTest extends TransactionalIntegrationTest
{

    @Autowired
    private EventService eventService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    @Autowired
    private EnrollmentService enrollmentService;

    private OrganisationUnit organisationUnitA;

    private Program programA;

    private ProgramStage programStageA;

    private DataElement dataElementA;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitA.setUid( "A" );
        manager.save( organisationUnitA );
        dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.INTEGER );
        dataElementA.setUid( "A" );
        manager.save( dataElementA );
        programStageA = createProgramStage( 'A', 0 );
        programStageA.setFeatureType( FeatureType.POINT );
        programStageA.setUid( "A" );
        manager.save( programStageA );
        ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
        programStageDataElement.setDataElement( dataElementA );
        programStageDataElement.setProgramStage( programStageA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        programA.setUid( "A" );
        manager.save( programA );
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( programA );
        enrollment.setAutoFields();
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );
        enrollment.setStatus( ProgramStatus.ACTIVE );
        enrollmentService.addEnrollment( enrollment );
        programStageA.getProgramStageDataElements().add( programStageDataElement );
        programStageA.setProgram( programA );
        programA.getProgramStages().add( programStageA );
        manager.update( programStageA );
        manager.update( programA );
        createUserAndInjectSecurityContext( new HashSet<>( Arrays.asList( organisationUnitA ) ), true );
    }

    @Test
    void testGeometry()
        throws IOException
    {
        InputStream is = createEventXmlInputStream();
        ImportSummaries importSummaries = eventService.addEventsXml( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
        Events events = eventService.getEvents( new EventSearchParams().setProgram( programA )
            .setOrgUnitSelectionMode( OrganisationUnitSelectionMode.ACCESSIBLE ) );
        assertEquals( 1, events.getEvents().size() );
        assertTrue( events.getEvents().stream().allMatch( e -> e.getGeometry().getGeometryType().equals( "Point" ) ) );
    }

    @Test
    void testNoAccessEvent()
        throws IOException
    {
        InputStream is = createEventXmlInputStream();
        ImportSummaries importSummaries = eventService.addEventsXml( is, null );
        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
        // Get by admin
        Events events = eventService.getEvents( new EventSearchParams().setProgram( programA )
            .setOrgUnitSelectionMode( OrganisationUnitSelectionMode.ACCESSIBLE ) );
        assertEquals( 1, events.getEvents().size() );
        // Get by user without access
        User user = createUserWithAuth( "A" );
        user.addOrganisationUnit( organisationUnitA );
        userService.addUser( user );
        injectSecurityContext( user );
        events = eventService.getEvents( new EventSearchParams().setProgram( programA )
            .setOrgUnitSelectionMode( OrganisationUnitSelectionMode.ACCESSIBLE ) );
        assertEquals( 0, events.getEvents().size() );
    }

    private InputStream createEventXmlInputStream()
        throws IOException
    {
        return new ClassPathResource( "dxf2/events/events.xml" ).getInputStream();
    }
}
