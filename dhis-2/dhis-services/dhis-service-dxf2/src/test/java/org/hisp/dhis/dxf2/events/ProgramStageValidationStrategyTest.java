package org.hisp.dhis.dxf2.events;
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
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
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * @author David Katuscak
 */
//public class ProgramStageValidationStrategyTest extends DhisTest
public class ProgramStageValidationStrategyTest extends DhisSpringTest
{
    @Autowired
    private EventService eventService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    private TrackedEntityInstance trackedEntityInstanceMaleA;
    private OrganisationUnit organisationUnitA;
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
        userService = _userService;

        organisationUnitA = createOrganisationUnit( 'A' );
        manager.save( organisationUnitA );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );

        org.hisp.dhis.trackedentity.TrackedEntityInstance maleA = createTrackedEntityInstance( 'A', organisationUnitA );
        maleA.setTrackedEntityType( trackedEntityType );
        manager.save( maleA );

        trackedEntityInstanceMaleA = trackedEntityInstanceService.getTrackedEntityInstance( maleA );

        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.INTEGER );
        dataElementService.addDataElement( dataElementA );

        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setValueType( ValueType.TEXT );
        dataElementService.addDataElement( dataElementB );

        DataElement dataElementC = createDataElement( 'C' );
        dataElementC.setValueType( ValueType.INTEGER );
        dataElementService.addDataElement( dataElementC );

        programStageA = createProgramStage( 'A', 0 );
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.save( programStageA );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        manager.save( programA );

        ProgramStageDataElement programStageDataElementA = new ProgramStageDataElement();
        programStageDataElementA.setDataElement( dataElementA );
        programStageDataElementA.setProgramStage( programStageA );
        programStageDataElementA.setCompulsory( true );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementA );

        ProgramStageDataElement programStageDataElementB = new ProgramStageDataElement();
        programStageDataElementB.setDataElement( dataElementB );
        programStageDataElementB.setProgramStage( programStageA );
        programStageDataElementB.setCompulsory( true );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );

        ProgramStageDataElement programStageDataElementC = new ProgramStageDataElement();
        programStageDataElementC.setDataElement( dataElementC );
        programStageDataElementC.setProgramStage( programStageA );
        programStageDataElementC.setCompulsory( false );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementC );

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
        maleA.getProgramInstances().add( programInstance );

        programInstanceService.addProgramInstance( programInstance );
        manager.update( maleA );
        manager.update( programA );

        Period periodA = createPeriod( "201803" );
        manager.save( periodA );

        CategoryCombo categoryComboA = createCategoryCombo( 'A' );
        CategoryOptionCombo categoryOptionComboA = createCategoryOptionCombo( 'A' );
        categoryOptionComboA.setCategoryCombo( categoryComboA );
        manager.save( categoryComboA );
        manager.save( categoryOptionComboA );

        dataValueBMissing = new org.hisp.dhis.dxf2.events.event.DataValue(dataElementB.getUid(), "");
        dataValueCMissing = new org.hisp.dhis.dxf2.events.event.DataValue(dataElementC.getUid(), "");

        dataValueA = new org.hisp.dhis.dxf2.events.event.DataValue(dataElementA.getUid(), "42");
        dataValueB = new org.hisp.dhis.dxf2.events.event.DataValue(dataElementB.getUid(), "Ford Prefect");
        dataValueC = new org.hisp.dhis.dxf2.events.event.DataValue(dataElementC.getUid(), "84");

        createUserAndInjectSecurityContext( true );
    }

    /*
     *  ##############################################################################################################
     *  ##############################################################################################################
     *  ##############################################################################################################
     *  Following tests test creation/update of complete Event (Basically what /events endpoint does)
     *  ##############################################################################################################
     *  ##############################################################################################################
     *  ##############################################################################################################
     */


    /*
     *  #######################################################
     *  Tests with ValidationStrategy.NONE
     *  #######################################################
     */

    @Test
    public void missingCompulsoryDataElementWithValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementsWithValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }


    @Test
    public void missingCompulsoryDataElementAndCompletedEventWithValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndCompletedEventWithValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /*
     *  #######################################################
     *  Tests with ValidationStrategy.ON_UPDATE_AND_INSERT
     *  #######################################################
     */

    @Test
    public void missingCompulsoryDataElementWithValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementsWithValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }


    @Test
    public void missingCompulsoryDataElementAndCompletedEventWithValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndCompletedEventWithValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /*
     *  #######################################################
     *  Tests with ValidationStrategy.ON_COMPLETE
     *  #######################################################
     */

    @Test
    public void missingCompulsoryDataElementWithValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementsWithValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }


    @Test
    public void missingCompulsoryDataElementAndCompletedEventWithValidationOnCompleteShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueBMissing, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndCompletedEventWithValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.setStatus( EventStatus.COMPLETED );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));

        ImportSummary importSummary = eventService.addEvent( event, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /*
     *  #######################################################################################################################
     *  #######################################################################################################################
     *  #######################################################################################################################
     *  Following tests test update of 1 specific data element  (Basically what /events/{uid}/{dataElementUid} endpoint does)
     *  #######################################################################################################################
     *  #######################################################################################################################
     *  #######################################################################################################################
     */

    /*
     *  #######################################################
     *  Tests with ValidationStrategy.NONE
     *  #######################################################
     */

    @Test
    public void compulsoryDataElementWithEmptyValueAndValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementAndValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void compulsoryDataElementWithEmptyValueCompletedEventAndValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementWithCompletedEventAndValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementWithCompletedEventAndValidationNoneShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.NONE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }


    /*
     *  #######################################################
     *  Tests with ValidationStrategy.ON_UPDATE_AND_INSERT
     *  #######################################################
     */

    @Test
    public void compulsoryDataElementWithEmptyValueAndValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementAndValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void compulsoryDataElementWithEmptyValueCompletedEventAndValidationOnUpdateShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementWithCompletedEventAndValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementWithCompletedEventAndValidationOnUpdateShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    /*
     *  #######################################################
     *  Tests with ValidationStrategy.ON_COMPLETE
     *  #######################################################
     */

    @Test
    public void compulsoryDataElementWithEmptyValueAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void compulsoryDataElementWithEmptyValueCompletedEventAndValidationOnCompleteShouldFailTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueBMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.ERROR, importSummary.getStatus() );
    }

    @Test
    public void correctCompulsoryDataElementWithCompletedEventAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueB );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
    }

    @Test
    public void emptyNonCompulsoryDataElementWithCompletedEventAndValidationOnCompleteShouldPassTest()
    {
        programStageA.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        manager.update( programStageA );

        Event event = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        event.getDataValues().addAll( Arrays.asList( dataValueA, dataValueB, dataValueC ));
        event.setEvent( "abcdefghijk" );

        eventService.addEvent( event, null );

        Event updatedEvent = createEvent( programA.getUid(), programStageA.getUid(), organisationUnitA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance() );
        updatedEvent.getDataValues().add( dataValueCMissing );
        updatedEvent.setEvent( "abcdefghijk" );
        updatedEvent.setStatus( EventStatus.COMPLETED );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true, null );

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

