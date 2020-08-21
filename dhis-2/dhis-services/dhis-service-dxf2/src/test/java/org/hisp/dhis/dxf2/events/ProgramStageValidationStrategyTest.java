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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.hibernate.SessionFactory;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David Katuscak
 */
public class ProgramStageValidationStrategyTest extends DhisSpringTest
{
    @Autowired
    private EventService eventService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    @Autowired
    protected CurrentUserService currentUserService;

    private TrackedEntityInstance trackedEntityInstanceMaleA;

    private OrganisationUnit organisationUnitA;

    private org.hisp.dhis.dxf2.events.event.DataValue dataValueAMissing;

    private org.hisp.dhis.dxf2.events.event.DataValue dataValueBMissing;

    private org.hisp.dhis.dxf2.events.event.DataValue dataValueCMissing;

    private org.hisp.dhis.dxf2.events.event.DataValue dataValueA;

    private org.hisp.dhis.dxf2.events.event.DataValue dataValueB;

    private org.hisp.dhis.dxf2.events.event.DataValue dataValueC;

    private Program programA;

    private ProgramStage programStageA;

    @Override
    protected void setUpTest()
    {
        final int testYear = Calendar.getInstance().get(Calendar.YEAR) - 1;
        userService = _userService;

        createUserAndInjectSecurityContext( false, "F_TRACKED_ENTITY_DATAVALUE_ADD",
            "F_TRACKED_ENTITY_DATAVALUE_DELETE",
            "F_UNCOMPLETE_EVENT", "F_PROGRAMSTAGE_ADD", "F_PROGRAMSTAGE_DELETE", "F_PROGRAM_PUBLIC_ADD",
            "F_PROGRAM_PRIVATE_ADD",
            "F_PROGRAM_DELETE", "F_TRACKED_ENTITY_ADD", "F_TRACKED_ENTITY_UPDATE", "F_TRACKED_ENTITY_DELETE",
            "F_DATAELEMENT_PUBLIC_ADD",
            "F_DATAELEMENT_PRIVATE_ADD", "F_DATAELEMENT_DELETE", "F_CATEGORY_COMBO_PUBLIC_ADD",
            "F_CATEGORY_COMBO_PRIVATE_ADD",
            "F_CATEGORY_COMBO_DELETE" );

        User currentUser = currentUserService.getCurrentUser();
        UserAccess userAccess1 = new UserAccess( currentUser, "rwrw----" );
        UserAccess userAccess2 = new UserAccess( currentUser, "rwrw----" );
        UserAccess userAccess3 = new UserAccess( currentUser, "rwrw----" );

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitA.addUser( currentUser );
        organisationUnitA.getUserAccesses().add( userAccess1 );
        currentUser.getTeiSearchOrganisationUnits().add( organisationUnitA );
        manager.save( organisationUnitA, false );
        userService.updateUser( currentUser );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityType.getUserAccesses().add( userAccess1 );
        manager.save( trackedEntityType, false );

        org.hisp.dhis.trackedentity.TrackedEntityInstance maleA = createTrackedEntityInstance( organisationUnitA );
        maleA.setTrackedEntityType( trackedEntityType );
        maleA.getUserAccesses().add( userAccess1 );
        maleA.setUser( currentUser );
        manager.save( maleA, false );

        trackedEntityInstanceMaleA = trackedEntityInstanceService.getTrackedEntityInstance( maleA );

        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.INTEGER );
        dataElementA.getUserAccesses().add( userAccess1 );
        manager.save( dataElementA, false );

        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setValueType( ValueType.TEXT );
        dataElementB.getUserAccesses().add( userAccess2 );
        manager.save( dataElementB, false );

        DataElement dataElementC = createDataElement( 'C' );
        dataElementC.setValueType( ValueType.INTEGER );
        dataElementC.getUserAccesses().add( userAccess3 );
        manager.save( dataElementC, false );

        programStageA = createProgramStage( 'A', 0 );
        programStageA.setUid( CodeGenerator.generateUid() );
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        programStageA.getUserAccesses().add( userAccess1 );
        manager.save( programStageA, false );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        programA.getUserAccesses().add( userAccess1 );
        manager.save( programA, false );

        // Create a compulsory PSDE
        ProgramStageDataElement programStageDataElementA = new ProgramStageDataElement();
        programStageDataElementA.setDataElement( dataElementA );
        programStageDataElementA.setProgramStage( programStageA );
        programStageDataElementA.setCompulsory( true );
        programStageDataElementA.getUserAccesses().add( userAccess1 );
        manager.save( programStageDataElementA, false );

        // Create a compulsory PSDE
        ProgramStageDataElement programStageDataElementB = new ProgramStageDataElement();
        programStageDataElementB.setDataElement( dataElementB );
        programStageDataElementB.setProgramStage( programStageA );
        programStageDataElementB.setCompulsory( true );
        programStageDataElementB.getUserAccesses().add( userAccess1 );
        manager.save( programStageDataElementB, false );

        // Create a NON-compulsory PSDE
        ProgramStageDataElement programStageDataElementC = new ProgramStageDataElement();
        programStageDataElementC.setDataElement( dataElementC );
        programStageDataElementC.setProgramStage( programStageA );
        programStageDataElementC.setCompulsory( false );
        programStageDataElementC.getUserAccesses().add( userAccess1 );
        manager.save( programStageDataElementC, false );

        // Assign all 3 created PSDEs to created ProgramStage programStageA and to
        // created Program programA
        programStageA.getProgramStageDataElements().add( programStageDataElementA );
        programStageA.getProgramStageDataElements().add( programStageDataElementB );
        programStageA.getProgramStageDataElements().add( programStageDataElementC );
        programStageA.setProgram( programA );
        programA.getProgramStages().add( programStageA );

        manager.update( programStageA );
        manager.update( programA );

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setProgram( programA );
        programInstance.setIncidentDate( new Date() );
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setEntityInstance( maleA );
        programInstance.getUserAccesses().add( userAccess1 );
        maleA.getProgramInstances().add( programInstance );

        manager.save( programInstance, false );
        manager.update( maleA );

        Period periodA = createPeriod( testYear + "03" );
        periodA.getUserAccesses().add( userAccess1 );
        manager.save( periodA, false );

        CategoryCombo categoryComboA = createCategoryCombo( 'A' );
        categoryComboA.setUid( CodeGenerator.generateUid() );
        CategoryOptionCombo categoryOptionComboA = createCategoryOptionCombo( 'A' );
        categoryOptionComboA.setUid( CodeGenerator.generateUid() );
        categoryOptionComboA.setCategoryCombo( categoryComboA );
        categoryComboA.getUserAccesses().add( userAccess1 );
        categoryOptionComboA.getUserAccesses().add( userAccess1 );
        manager.save( categoryComboA, false );
        manager.save( categoryOptionComboA, false );

        dataValueAMissing = new org.hisp.dhis.dxf2.events.event.DataValue( dataElementA.getUid(), "" );
        dataValueBMissing = new org.hisp.dhis.dxf2.events.event.DataValue( dataElementB.getUid(), "" );
        dataValueCMissing = new org.hisp.dhis.dxf2.events.event.DataValue( dataElementC.getUid(), "" );

        dataValueA = new org.hisp.dhis.dxf2.events.event.DataValue( dataElementA.getUid(), "42" );
        dataValueB = new org.hisp.dhis.dxf2.events.event.DataValue( dataElementB.getUid(), "Ford Prefect" );
        dataValueC = new org.hisp.dhis.dxf2.events.event.DataValue( dataElementC.getUid(), "84" );
        // Flush all changes to disk
        manager.flush();
    }

    /*
     * #############################################################################
     * #################################
     * #############################################################################
     * #################################
     * #############################################################################
     * ################################# Following tests test creation/update of
     * complete Event (Basically what /events endpoint does)
     * #############################################################################
     * #################################
     * #############################################################################
     * #################################
     * #############################################################################
     * #################################
     */

    /*
     * ####################################################### Tests with
     * ValidationStrategy.ON_UPDATE_AND_INSERT
     * #######################################################
     */

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void missingCompulsoryDataElementWithValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ) );

        manager.flush();
        sessionFactory.getCurrentSession().clear();

        ImportSummary importSummary = eventService.addEvent( event, null, false );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementsWithValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );

        ImportSummary importSummary = eventService.addEvent( event, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void missingCompulsoryDataElementAndCompletedEventWithValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ) );

        ImportSummary importSummary = eventService.addEvent( event, null, false );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndCompletedEventWithValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );

        ImportSummary importSummary = eventService.addEvent( event, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /*
     * ####################################################### Tests with
     * ValidationStrategy.ON_COMPLETE
     * #######################################################
     */

    @Test
    public void missingCompulsoryDataElementWithValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ) );

        ImportSummary importSummary = eventService.addEvent( event, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementsWithValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );

        ImportSummary importSummary = eventService.addEvent( event, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void missingCompulsoryDataElementAndCompletedEventWithValidationOnCompleteShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ) );

        ImportSummary importSummary = eventService.addEvent( event, null, false );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndCompletedEventWithValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        manager.flush();

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );

        ImportSummary importSummary = eventService.addEvent( event, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /*
     * #############################################################################
     * ##########################################
     * #############################################################################
     * ##########################################
     * #############################################################################
     * ########################################## Following tests test update of 1
     * specific data element (Basically what /events/{uid}/{dataElementUid} endpoint
     * does)
     * #############################################################################
     * ##########################################
     * #############################################################################
     * ##########################################
     * #############################################################################
     * ##########################################
     */

    /*
     * ####################################################### Tests with
     * ValidationStrategy.ON_UPDATE_AND_INSERT
     * #######################################################
     */

    @Test
    public void compulsoryDataElementWithEmptyValueAndValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        // Single value update -> should pass -> because data values are fetched from DB
        // and merged
        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        // NOT a single value update -> should fail -> because data values are NOT
        // fetched from DB and so NOT merged
        updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        importSummary = eventService.updateEvent( updatedEvent, false, null, false );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementButOtherCompulsoryMissingInDBAndValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueAMissing, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementAndValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void compulsoryDataElementWithEmptyValueCompletedEventAndValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        manager.flush();

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        // Single value update -> should pass -> because data values are fetched from DB
        // and merged
        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        // NOT a single value update -> should fail -> because data values are NOT
        // fetched from DB and so NOT merged
        updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        importSummary = eventService.updateEvent( updatedEvent, false, null, false );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementWithCompletedEventAndValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementWithCompletedEventAndValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /*
     * ####################################################### Tests with
     * ValidationStrategy.ON_COMPLETE
     * #######################################################
     */

    @Test
    public void compulsoryDataElementWithEmptyValueAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void compulsoryDataElementWithEmptyValueCompletedEventAndValidationOnCompleteShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        // Single value update -> should pass -> because data values are fetched from DB
        // and merged
        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        // NOT a single value update -> should fail -> because data values are NOT
        // fetched from DB and so NOT merged
        updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        importSummary = eventService.updateEvent( updatedEvent, false, null, false );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementWithCompletedEventAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementWithCompletedEventAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ) );
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null, false );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null, false );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    private Event createEvent( String program, String programStage, String orgUnit, String person )
    {
        Event event = new Event();
        event.setProgram( program );
        event.setProgramStage( programStage );
        event.setOrgUnit( orgUnit );
        event.setTrackedEntityInstance( person );
        event.setEventDate( "2013-01-01" );

        return event;
    }
}
