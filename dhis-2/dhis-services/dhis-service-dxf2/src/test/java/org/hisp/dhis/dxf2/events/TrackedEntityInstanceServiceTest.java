package org.hisp.dhis.dxf2.events;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternMethod;
import org.hisp.dhis.textpattern.TextPatternSegment;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackedEntityInstanceServiceTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private IdentifiableObjectManager manager;

    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleA;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance maleB;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleA;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance femaleB;
    private org.hisp.dhis.trackedentity.TrackedEntityInstance dateConflictsMaleA;

    private OrganisationUnit organisationUnitA;
    private OrganisationUnit organisationUnitB;

    private Program programA;

    private ProgramStage programStageA1;

    private ProgramStage programStageA2;

    private TrackedEntityAttribute uniqueIdAttribute;

    @Override
    protected void setUpTest() throws Exception
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitB = createOrganisationUnit( 'B' );

        organisationUnitB.setParent( organisationUnitA );

        uniqueIdAttribute = createTrackedEntityAttribute( 'A' );
        uniqueIdAttribute.setGenerated( true );
        //uniqueIdAttribute.setPattern( "RANDOM(#####)" );
        TextPattern textPattern = new TextPattern(
            Lists.newArrayList( new TextPatternSegment( TextPatternMethod.RANDOM, "RANDOM(#####)" ) ) );
        uniqueIdAttribute.setTextPattern( textPattern );

        trackedEntityAttributeService.addTrackedEntityAttribute( uniqueIdAttribute );

        TrackedEntityType trackedEntityType = createTrackedEntityType( 'A' );

        TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();
        trackedEntityTypeAttribute.setTrackedEntityAttribute( uniqueIdAttribute );
        trackedEntityTypeAttribute.setTrackedEntityType( trackedEntityType );

        trackedEntityType.setTrackedEntityTypeAttributes( Lists.newArrayList( trackedEntityTypeAttribute ) );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityType );


        maleA = createTrackedEntityInstance( organisationUnitA );
        maleB = createTrackedEntityInstance( organisationUnitB );
        femaleA = createTrackedEntityInstance( organisationUnitA );
        femaleB = createTrackedEntityInstance( organisationUnitB );
        dateConflictsMaleA = createTrackedEntityInstance( organisationUnitA );

        TrackedEntityAttributeValue uniqueId = createTrackedEntityAttributeValue( 'A', maleA, uniqueIdAttribute );
        uniqueId.setValue( "12345" );

        maleA.setTrackedEntityType( trackedEntityType );
        maleA.setTrackedEntityAttributeValues( Sets.newHashSet( uniqueId ) );
        maleB.setTrackedEntityType( trackedEntityType );
        femaleA.setTrackedEntityType( trackedEntityType );
        femaleB.setTrackedEntityType( trackedEntityType );
        dateConflictsMaleA.setTrackedEntityType( trackedEntityType );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );

        programStageA1 = createProgramStage( '1', programA );
        programStageA2 = createProgramStage( '2', programA );


        programA.setProgramStages( Stream.of( programStageA1, programStageA2 ).collect( Collectors.toCollection( HashSet::new ) ) );

        manager.save( organisationUnitA );
        manager.save( organisationUnitB );
        manager.save( maleA );
        manager.save( maleB );
        manager.save( femaleA );
        manager.save( femaleB );
        manager.save( dateConflictsMaleA );
        manager.save( programA );
        manager.save( programStageA1 );
        manager.save( programStageA2 );

        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( uniqueId );

        programInstanceService.enrollTrackedEntityInstance( maleA, programA, null, null, organisationUnitA );
        programInstanceService.enrollTrackedEntityInstance( femaleA, programA, DateTime.now().plusMonths( 1 ).toDate(), null, organisationUnitA );
        programInstanceService.enrollTrackedEntityInstance( dateConflictsMaleA, programA, DateTime.now().plusMonths( 1 ).toDate(), DateTime.now().plusMonths( 2 ).toDate(), organisationUnitA );
    }

    @Test
    public void getPersonByUid()
    {
        assertEquals( maleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() ).getTrackedEntityInstance() );
        assertEquals( femaleB.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( femaleB.getUid() ).getTrackedEntityInstance() );
        assertNotEquals( femaleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( femaleB.getUid() ).getTrackedEntityInstance() );
        assertNotEquals( maleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( maleB.getUid() ).getTrackedEntityInstance() );
    }

    @Test
    public void getPersonByPatient()
    {
        assertEquals( maleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( maleA ).getTrackedEntityInstance() );
        assertEquals( femaleB.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( femaleB ).getTrackedEntityInstance() );
        assertNotEquals( femaleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( femaleB ).getTrackedEntityInstance() );
        assertNotEquals( maleA.getUid(), trackedEntityInstanceService.getTrackedEntityInstance( maleB ).getTrackedEntityInstance() );
    }

    @Test
    @Ignore
    public void testUpdatePerson()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        // person.setName( "UPDATED_NAME" );

        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance, null, null, true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        // assertEquals( "UPDATED_NAME", personService.getTrackedEntityInstance( maleA.getUid() ).getName() );
    }

    @Test
    public void testUpdateTeiByCompletingExistingEnrollmentAndOpeningNewEnrollment()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        assertNotNull( trackedEntityInstance.getEnrollments() );
        assertEquals( 1, trackedEntityInstance.getEnrollments().size() );

        Enrollment enrollment1 = trackedEntityInstance.getEnrollments().get( 0 );
        enrollment1.setStatus( EnrollmentStatus.COMPLETED );
        enrollment1.setCompletedBy( "test" );
        enrollment1.setCompletedDate( new Date() );

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setTrackedEntityInstance( maleA.getUid() );
        enrollment2.setEnrollmentDate( new Date() );
        enrollment2.setOrgUnit( organisationUnitA.getUid() );
        enrollment2.setProgram( programA.getUid() );
        enrollment2.setStatus( EnrollmentStatus.ACTIVE );

        trackedEntityInstance.getEnrollments().add( enrollment2 );

        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance, null, null, true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getStatus() );

    }

    @Test
    public void testUpdateTeiAfterChangingTextPatternForGeneratedAttribute()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService
            .getTrackedEntityInstance( maleA.getUid() );
        assertNotNull( trackedEntityInstance.getEnrollments() );
        assertEquals( 1, trackedEntityInstance.getEnrollments().size() );

        Enrollment enrollment1 = trackedEntityInstance.getEnrollments().get( 0 );
        enrollment1.setStatus( EnrollmentStatus.COMPLETED );
        enrollment1.setCompletedBy( "test" );
        enrollment1.setCompletedDate( new Date() );

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setTrackedEntityInstance( maleA.getUid() );

        TextPattern textPattern = new TextPattern(
            Lists.newArrayList( new TextPatternSegment( TextPatternMethod.RANDOM, "RANDOM(#######)" ) ) );
        textPattern.setOwnerUid( "owneruid" );
        textPattern.setOwnerObject( Objects.CONSTANT );
        uniqueIdAttribute.setTextPattern( textPattern );
        trackedEntityAttributeService.updateTrackedEntityAttribute( uniqueIdAttribute );

        enrollment2.setEnrollmentDate( new Date() );
        enrollment2.setOrgUnit( organisationUnitA.getUid() );
        enrollment2.setProgram( programA.getUid() );
        enrollment2.setStatus( EnrollmentStatus.ACTIVE );

        trackedEntityInstance.getEnrollments().add( enrollment2 );

        ImportSummary importSummary = trackedEntityInstanceService
            .updateTrackedEntityInstance( trackedEntityInstance, null, null, true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getStatus() );

    }

    @Test
    public void testUpdateTeiByCompletingExistingEnrollmentAndAddNewEventsToSameEnrollment()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        assertNotNull( trackedEntityInstance.getEnrollments() );
        assertEquals( 1, trackedEntityInstance.getEnrollments().size() );

        Enrollment enrollment1 = trackedEntityInstance.getEnrollments().get( 0 );
        enrollment1.setStatus( EnrollmentStatus.COMPLETED );
        enrollment1.setCompletedBy( "test" );
        enrollment1.setCompletedDate( new Date() );

        Event event1 = new Event();
        event1.setEnrollment( enrollment1.getEnrollment() );
        event1.setEventDate( DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now() ) );

        event1.setOrgUnit( organisationUnitA.getUid() );

        event1.setProgram( programA.getUid() );
        event1.setProgramStage( programStageA1.getUid() );
        event1.setStatus( EventStatus.COMPLETED );
        event1.setTrackedEntityInstance( maleA.getUid() );

        Event event2 = new Event();
        event2.setEnrollment( enrollment1.getEnrollment() );
        event2.setEventDate( DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now() ) );

        event2.setOrgUnit( organisationUnitA.getUid() );

        event2.setProgram( programA.getUid() );
        event2.setProgramStage( programStageA2.getUid() );
        event2.setStatus( EventStatus.ACTIVE );
        event2.setTrackedEntityInstance( maleA.getUid() );

        enrollment1.setEvents( Arrays.asList( event1, event2 ) );

        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance, null, null, true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getImportSummaries().get( 0 ).getEvents().getStatus() );

    }

    @Test
    public void testSyncTeiFutureDatesForEnrollmentAndIncident()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( dateConflictsMaleA.getUid() );

        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance,
            null, new ImportOptions().setImportStrategy( ImportStrategy.SYNC ), true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( 2, importSummary.getEnrollments().getImportSummaries().get( 0 ).getConflicts().size() );
        assertEquals( trackedEntityInstance.getEnrollments().get( 0 ).getEnrollment(),
            importSummary.getEnrollments().getImportSummaries().get( 0 ).getReference() );

    }

    @Test
    public void testUpdateTeiByCompletingExistingEnrollmentAndUpdateExistingEventsInSameEnrollment()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        assertNotNull( trackedEntityInstance.getEnrollments() );
        assertEquals( 1, trackedEntityInstance.getEnrollments().size() );

        Enrollment enrollment1 = trackedEntityInstance.getEnrollments().get( 0 );

        Event event1 = new Event();
        event1.setEnrollment( enrollment1.getEnrollment() );
        event1.setEventDate( DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now() ) );

        event1.setOrgUnit( organisationUnitA.getUid() );

        event1.setProgram( programA.getUid() );
        event1.setProgramStage( programStageA1.getUid() );
        event1.setStatus( EventStatus.ACTIVE );
        event1.setTrackedEntityInstance( maleA.getUid() );

        enrollment1.setEvents( Arrays.asList( event1 ) );

        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance, null, null, true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getImportSummaries().get( 0 ).getEvents().getStatus() );

        trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        assertNotNull( trackedEntityInstance.getEnrollments() );
        assertEquals( 1, trackedEntityInstance.getEnrollments().size() );

        assertNotNull( trackedEntityInstance.getEnrollments().get( 0 ).getEvents() );
        assertEquals( 1, trackedEntityInstance.getEnrollments().get( 0 ).getEvents().size() );

        enrollment1 = trackedEntityInstance.getEnrollments().get( 0 );
        enrollment1.setStatus( EnrollmentStatus.COMPLETED );
        enrollment1.setCompletedBy( "test" );
        enrollment1.setCompletedDate( new Date() );

        event1 = enrollment1.getEvents().get( 0 );
        event1.setStatus( EventStatus.COMPLETED );
        event1.setCompletedBy( "test" );
        event1.setCompletedDate( DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now() ) );

        importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance, null, null, true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getImportSummaries().get( 0 ).getEvents().getStatus() );

    }

    @Test
    public void testUpdateTeiByDeletingExistingEventAndAddNewEventForSameProgramStage()
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        assertNotNull( trackedEntityInstance.getEnrollments() );
        assertEquals( 1, trackedEntityInstance.getEnrollments().size() );

        Enrollment enrollment1 = trackedEntityInstance.getEnrollments().get( 0 );

        Event event1 = new Event();
        event1.setEnrollment( enrollment1.getEnrollment() );
        event1.setEventDate( DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now() ) );
        event1.setOrgUnit( organisationUnitA.getUid() );
        event1.setProgram( programA.getUid() );
        event1.setProgramStage( programStageA1.getUid() );
        event1.setStatus( EventStatus.COMPLETED );
        event1.setTrackedEntityInstance( maleA.getUid() );

        Event event2 = new Event();
        event2.setEnrollment( enrollment1.getEnrollment() );
        event2.setOrgUnit( organisationUnitA.getUid() );
        event2.setProgram( programA.getUid() );
        event2.setProgramStage( programStageA2.getUid() );
        event2.setStatus( EventStatus.SCHEDULE );
        event2.setDueDate( DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now().plusDays( 10 ) ) );
        event2.setTrackedEntityInstance( maleA.getUid() );

        enrollment1.setEvents( Arrays.asList( event1, event2 ) );

        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance, null, null, true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getImportSummaries().get( 0 ).getEvents().getStatus() );

        trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() );
        assertNotNull( trackedEntityInstance.getEnrollments() );
        assertEquals( 1, trackedEntityInstance.getEnrollments().size() );

        assertNotNull( trackedEntityInstance.getEnrollments().get( 0 ).getEvents() );
        assertEquals( 2, trackedEntityInstance.getEnrollments().get( 0 ).getEvents().size() );

        enrollment1 = trackedEntityInstance.getEnrollments().get( 0 );

        event2 = enrollment1.getEvents().stream().filter( e -> e.getProgramStage().equals( programStageA2.getUid() ) ).findFirst().get();
        event2.setDeleted( true );

        Event event3 = new Event();
        event3.setEnrollment( enrollment1.getEnrollment() );
        event3.setOrgUnit( organisationUnitA.getUid() );
        event3.setProgram( programA.getUid() );
        event3.setProgramStage( programStageA2.getUid() );
        event3.setStatus( EventStatus.SCHEDULE );
        event3.setDueDate( DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now().plusDays( 11 ) ) );
        event3.setTrackedEntityInstance( maleA.getUid() );

        enrollment1.getEvents().add( event3 );

        ImportOptions importOptions = new ImportOptions();
        importOptions.setImportStrategy( ImportStrategy.SYNC );

        importSummary = trackedEntityInstanceService.updateTrackedEntityInstance( trackedEntityInstance, null, importOptions, true );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getStatus() );
        assertEquals( ImportStatus.SUCCESS, importSummary.getEnrollments().getImportSummaries().get( 0 ).getEvents().getStatus() );
    }


    @Test
    @Ignore
    public void testSavePerson()
    {
        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        // person.setName( "NAME" );
        trackedEntityInstance.setOrgUnit( organisationUnitA.getUid() );

        ImportSummary importSummary = trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstance, null );
        assertEquals( ImportStatus.SUCCESS, importSummary.getStatus() );

        // assertEquals( "NAME", personService.getTrackedEntityInstance( importSummary.getReference() ).getName() );
    }

    @Test
    public void testDeletePerson()
    {
        trackedEntityInstanceService.deleteTrackedEntityInstance( maleA.getUid() );

        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() ) );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( maleB.getUid() ) );
    }

    @Test
    public void testDeleteTrackedEntityInstances()
    {
        List<TrackedEntityInstance> teis = Lists.newArrayList( trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() ), trackedEntityInstanceService.getTrackedEntityInstance( maleB.getUid() ) );
        ImportOptions importOptions = new ImportOptions();
        importOptions.setImportStrategy( ImportStrategy.DELETE );
        trackedEntityInstanceService.deleteTrackedEntityInstances( teis, importOptions );

        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( maleA.getUid() ) );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( maleB.getUid() ) );
    }
}
