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
package org.hisp.dhis.dxf2.events.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class EventSecurityTest extends TransactionalIntegrationTest
{

    @Autowired
    private org.hisp.dhis.dxf2.events.event.EventService eventService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private EventService programStageInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    @Autowired
    private CategoryService _categoryService;

    private OrganisationUnit organisationUnitA;

    private DataElement dataElementA;

    private Program programA;

    private ProgramStage programStageA;

    @Override
    protected void setUpTest()
    {
        userService = _userService;
        categoryService = _categoryService;
        createAndInjectAdminUser();
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
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( programA );
        enrollment.setIncidentDate( new Date() );
        enrollment.setEnrollmentDate( new Date() );
        programInstanceService.addProgramInstance( enrollment );
        manager.update( programA );
        manager.flush();
    }

    @Test
    void testAddEventSuperuser()
    {
        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        programStageA.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( programA );
        manager.update( programStageA );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertFalse( importSummary.hasConflicts() );
    }

    @Test
    void testAddEventSimpleUser()
    {
        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        programStageA.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" );
        injectSecurityContext( user );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    /**
     * program = DATA READ/WRITE programStage = DATA READ/WRITE orgUnit =
     * Accessible status = SUCCESS
     */
    @Test
    void testAddEventSimpleUserFullAccess1()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.updateNoAcl( programA );
        manager.updateNoAcl( programStageA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        userService.addUser( user );
        injectSecurityContext( user );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        // make sure data is flushed, so event service can access it
        manager.flush();
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /**
     * program = DATA READ programStage = DATA READ/WRITE orgUnit = Accessible
     * status = ERROR
     */
    @Test
    void testAddEventSimpleUserFullAccess2()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    /**
     * program = DATA READ/WRITE programStage = DATA READ orgUnit = Accessible
     * status = ERROR
     */
    @Test
    void testAddEventSimpleUserFullAccess3()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        // make sure data is flushed, so event service can access it
        manager.flush();
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /**
     * program = DATA READ/WRITE programStage = DATA READ/WRITE orgUnit = Not
     * Accessible status = ERROR
     */
    @Test
    void testAddEventSimpleUserFullAccess4()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" );
        injectSecurityContext( user );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    /**
     * program = DATA READ programStage = DATA READ orgUnit = Accessible status
     * = SUCCESS
     */
    @Test
    void testAddEventSimpleUserFullAccess5()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );
        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        assertTrue( programStageInstanceService.eventExists( event.getEvent() ) );
        Event programStageInstance = programStageInstanceService.getEvent( event.getUid() );
        assertNotNull( programStageInstance );
        org.hisp.dhis.dxf2.events.event.Event eventFromPsi = eventService.getEvent( programStageInstance,
            EventParams.FALSE );
        assertNotNull( eventFromPsi );
        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    /**
     * program = DATA WRITE programStage = DATA WRITE orgUnit = Accessible
     * status = SUCCESS
     */
    @Test
    void testAddEventSimpleUserFullAccess6()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );
        programA.setPublicAccess( AccessStringHelper.DATA_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        assertTrue( programStageInstanceService.eventExists( event.getEvent() ) );
        Event programStageInstance = programStageInstanceService.getEvent( event.getUid() );
        assertNotNull( programStageInstance );
        org.hisp.dhis.dxf2.events.event.Event eventFromPsi = eventService.getEvent( programStageInstance,
            EventParams.FALSE );
        assertNotNull( eventFromPsi );
        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    /**
     * program = DATA WRITE programStage = DATA WRITE orgUnit = Not Accessible
     * status = ERROR
     */
    @Test
    void testAddEventSimpleUserFullAccess7()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );
        programA.setPublicAccess( AccessStringHelper.DATA_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" );
        injectSecurityContext( user );
        assertTrue( programStageInstanceService.eventExists( event.getEvent() ) );
        Event programStageInstance = programStageInstanceService.getEvent( event.getUid() );
        assertNotNull( programStageInstance );
        assertThrows( IllegalQueryException.class,
            () -> eventService.getEvent( programStageInstance, EventParams.FALSE ) );
    }

    /**
     * program = DATA READ programStage = DATA READ orgUnit = Not Accessible
     * status = ERROR
     */
    @Test
    void testAddEventSimpleUserFullAccess8()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );
        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" );
        injectSecurityContext( user );
        assertTrue( programStageInstanceService.eventExists( event.getEvent() ) );
        Event programStageInstance = programStageInstanceService.getEvent( event.getUid() );
        assertNotNull( programStageInstance );
        assertThrows( IllegalQueryException.class,
            () -> eventService.getEvent( programStageInstance, EventParams.FALSE ) );
    }

    /**
     * program = programStage = DATA READ orgUnit = Accessible status = ERROR
     */
    @Test
    void testAddEventSimpleUserFullAccess9()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );
        programA.setPublicAccess( AccessStringHelper.DEFAULT );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        assertTrue( programStageInstanceService.eventExists( event.getEvent() ) );
        Event programStageInstance = programStageInstanceService.getEvent( event.getUid() );
        assertNotNull( programStageInstance );
        assertThrows( IllegalQueryException.class,
            () -> eventService.getEvent( programStageInstance, EventParams.FALSE ) );
    }

    /**
     * program = DATA READ programStage = orgUnit = Accessible status = ERROR
     */
    @Test
    void testAddEventSimpleUserFullAccess10()
    {
        programA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        programStageA.setPublicAccess( AccessStringHelper.DATA_READ_WRITE );
        manager.update( programA );
        manager.update( programStageA );
        org.hisp.dhis.dxf2.events.event.Event event = createEvent( programA.getUid(), programStageA.getUid(),
            organisationUnitA.getUid() );
        ImportSummary importSummary = eventService.addEvent( event, ImportOptions.getDefaultImportOptions(), false );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( event.getEvent(), importSummary.getReference() );
        programA.setPublicAccess( AccessStringHelper.DATA_READ );
        programStageA.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( programA );
        manager.update( programStageA );
        User user = createUserWithAuth( "user1" ).setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        injectSecurityContext( user );
        assertTrue( programStageInstanceService.eventExists( event.getEvent() ) );
        Event programStageInstance = programStageInstanceService.getEvent( event.getUid() );
        assertNotNull( programStageInstance );
        org.hisp.dhis.dxf2.events.event.Event eventFromPsi = eventService.getEvent( programStageInstance,
            EventParams.FALSE );
        assertNotNull( eventFromPsi );
        assertEquals( event.getUid(), eventFromPsi.getEvent() );
    }

    private org.hisp.dhis.dxf2.events.event.Event createEvent( String program, String programStage, String orgUnit )
    {
        org.hisp.dhis.dxf2.events.event.Event event = new org.hisp.dhis.dxf2.events.event.Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setEvent( event.getUid() );
        event.setProgram( program );
        event.setProgramStage( programStage );
        event.setOrgUnit( orgUnit );
        event.setEventDate( "2013-01-01" );
        event.getDataValues().add( new DataValue( dataElementA.getUid(), "10" ) );
        return event;
    }
}
