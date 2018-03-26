package org.hisp.dhis.dxf2.events.security;

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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class EventSecurityTests
    extends DhisSpringTest
{
    @Autowired
    private EventService eventService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    private OrganisationUnit organisationUnitA;
    private DataElement dataElementA;
    private Program programA;
    private ProgramStage programStageA;

    @Override
    protected void setUpTest()
    {
        userService = _userService;

        organisationUnitA = createOrganisationUnit( 'A' );
        manager.save( organisationUnitA );

        dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.INTEGER );
        manager.save( dataElementA );

        programStageA = createProgramStage( 'A', 0 );
        manager.save( programStageA );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );
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

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setProgram( programA );
        programInstance.setIncidentDate( new Date() );
        programInstance.setEnrollmentDate( new Date() );

        programInstanceService.addProgramInstance( programInstance );
        manager.update( programA );
    }

    @Test
    public void testAddEventSuperuser()
    {
        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        programStageA.setPublicAccess( AccessStringHelper.DEFAULT );

        manager.update( programA );
        manager.update( programStageA );

        createAndInjectAdminUser();

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertTrue( importSummary.getConflicts().isEmpty() );
    }

    @Test
    public void testAddEventSimpleUser()
    {
        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        programStageA.setPublicAccess( AccessStringHelper.DEFAULT );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" );
        injectSecurityContext( user );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    /**
     * program = DATA READ/WRITE
     * programStage = DATA READ/WRITE
     * orgUnit = Accessible
     * status = SUCCESS
     */
    @Test
    public void testAddEventSimpleUserFullAccess1()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /**
     * program = DATA READ
     * programStage = DATA READ/WRITE
     * orgUnit = Accessible
     * status = ERROR
     */
    @Test
    public void testAddEventSimpleUserFullAccess2()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    /**
     * program = DATA READ/WRITE
     * programStage = DATA READ
     * orgUnit = Accessible
     * status = ERROR
     */
    @Test
    public void testAddEventSimpleUserFullAccess3()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    /**
     * program = DATA READ/WRITE
     * programStage = DATA READ/WRITE
     * orgUnit = Not Accessible
     * status = ERROR
     */
    @Test
    public void testAddEventSimpleUserFullAccess4()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" );

        injectSecurityContext( user );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    /**
     * program = DATA READ
     * programStage = DATA READ
     * orgUnit = Accessible
     * status = SUCCESS
     */
    @Test
    public void testAddEventSimpleUserFullAccess5()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );

        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        assertTrue( programStageInstanceService.programStageInstanceExists( event.getEvent() ) );

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getUid() );
        assertNotNull( programStageInstance );

        Event eventFromPsi = eventService.getEvent( programStageInstance );
        assertNotNull( eventFromPsi );

        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    /**
     * program = DATA WRITE
     * programStage = DATA WRITE
     * orgUnit = Accessible
     * status = SUCCESS
     */
    @Test
    public void testAddEventSimpleUserFullAccess6()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );

        programA.setPublicAccess( AccessStringHelper.DATA_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        assertTrue( programStageInstanceService.programStageInstanceExists( event.getEvent() ) );

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getUid() );
        assertNotNull( programStageInstance );

        Event eventFromPsi = eventService.getEvent( programStageInstance );
        assertNotNull( eventFromPsi );

        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    /**
     * program = DATA WRITE
     * programStage = DATA WRITE
     * orgUnit = Not Accessible
     * status = ERROR
     */
    @Test( expected = IllegalQueryException.class )
    public void testAddEventSimpleUserFullAccess7()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );

        programA.setPublicAccess( AccessStringHelper.DATA_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" );

        injectSecurityContext( user );

        assertTrue( programStageInstanceService.programStageInstanceExists( event.getEvent() ) );

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getUid() );
        assertNotNull( programStageInstance );

        Event eventFromPsi = eventService.getEvent( programStageInstance );
        assertNotNull( eventFromPsi );

        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    /**
     * program = DATA READ
     * programStage = DATA READ
     * orgUnit = Not Accessible
     * status = ERROR
     */
    @Test( expected = IllegalQueryException.class )
    public void testAddEventSimpleUserFullAccess8()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );

        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" );

        injectSecurityContext( user );

        assertTrue( programStageInstanceService.programStageInstanceExists( event.getEvent() ) );

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getUid() );
        assertNotNull( programStageInstance );

        Event eventFromPsi = eventService.getEvent( programStageInstance );
        assertNotNull( eventFromPsi );

        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    /**
     * program =
     * programStage = DATA READ
     * orgUnit = Accessible
     * status = ERROR
     */
    @Test( expected = IllegalQueryException.class )
    public void testAddEventSimpleUserFullAccess9()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );

        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        assertTrue( programStageInstanceService.programStageInstanceExists( event.getEvent() ) );

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getUid() );
        assertNotNull( programStageInstance );

        Event eventFromPsi = eventService.getEvent( programStageInstance );
        assertNotNull( eventFromPsi );

        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    /**
     * program = DATA READ
     * programStage =
     * orgUnit = Accessible
     * status = ERROR
     */
    @Test( expected = IllegalQueryException.class )
    public void testAddEventSimpleUserFullAccess10()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );

        manager.update( programA );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions() );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );

        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        programStageA.setPublicAccess( AccessStringHelper.DEFAULT );

        manager.update( programA );
        manager.update( programStageA );

        User user = createUser( "user1" )
            .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        injectSecurityContext( user );

        assertTrue( programStageInstanceService.programStageInstanceExists( event.getEvent() ) );

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( event.getUid() );
        assertNotNull( programStageInstance );

        Event eventFromPsi = eventService.getEvent( programStageInstance );
        assertNotNull( eventFromPsi );

        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    private Event createEvent( String program, String orgUnit )
    {
        Event event = new Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setEvent( event.getUid() );
        event.setProgram( program );
        event.setOrgUnit( orgUnit );
        event.setEventDate( "2013-01-01" );

        event.getDataValues().add( new DataValue( dataElementA.getUid(), "10" ) );

        return event;
    }
}
