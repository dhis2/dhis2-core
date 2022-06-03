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
package org.hisp.dhis.dxf2;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public abstract class TrackerTest extends IntegrationTestBase
{

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EventService eventService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    protected UserService userService;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
    protected DbmsManager dbmsManager;

    @Autowired
    private RelationshipService relationshipService;

    protected TrackedEntityType trackedEntityTypeA;

    protected OrganisationUnit organisationUnitA;

    protected OrganisationUnit organisationUnitB;

    protected Program programA;

    protected ProgramStage programStageA1;

    protected CategoryCombo categoryComboA;

    protected RelationshipType relationshipType;

    /**
     * Default COC created in DefaultCategoryService
     */
    protected final static String DEF_COC_UID = "HllvX50cXC0";

    @Override
    protected void setUpTest()
        throws Exception
    {
        // Tracker graph creation
        trackedEntityTypeA = createTrackedEntityType( 'A' );
        trackedEntityTypeA.setUid( CodeGenerator.generateUid() );
        trackedEntityTypeA.setName( "TrackedEntityTypeA" + trackedEntityTypeA.getUid() );
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitA.setUid( CodeGenerator.generateUid() );
        organisationUnitA.setCode( RandomStringUtils.randomAlphanumeric( 10 ) );
        organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitB.setUid( CodeGenerator.generateUid() );
        organisationUnitB.setCode( RandomStringUtils.randomAlphanumeric( 10 ) );
        categoryComboA = manager.getByName( CategoryCombo.class, "default" );
        categoryComboA.setUid( CodeGenerator.generateUid() );
        manager.update( categoryComboA );
        ProgramStage programStageA2;
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        programA.setCategoryCombo( categoryComboA );
        programA.setUid( CodeGenerator.generateUid() );
        programA.setCode( RandomStringUtils.randomAlphanumeric( 10 ) );
        CategoryOptionCombo defaultCategoryOptionCombo = createCategoryOptionCombo( 'A' );
        defaultCategoryOptionCombo.setCategoryCombo( categoryComboA );
        defaultCategoryOptionCombo.setUid( DEF_COC_UID );
        defaultCategoryOptionCombo.setName( "default1" );
        relationshipType = new RelationshipType();
        relationshipType.setFromToName( RandomStringUtils.randomAlphanumeric( 5 ) );
        relationshipType.setToFromName( RandomStringUtils.randomAlphanumeric( 5 ) );
        relationshipType.setName( RandomStringUtils.randomAlphanumeric( 10 ) );
        // Tracker graph persistence
        doInTransaction( () -> {
            trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeA );
            manager.save( organisationUnitA );
            manager.save( organisationUnitB );
            manager.save( categoryComboA );
            manager.save( programA );
            manager.save( relationshipType );
        } );
        programStageA1 = createProgramStage( programA, true );
        programStageA2 = createProgramStage( programA, true );
        programA.setProgramStages(
            Stream.of( programStageA1, programStageA2 ).collect( Collectors.toCollection( HashSet::new ) ) );
        manager.update( programA );
        super.userService = this.userService;
    }

    public TrackedEntityInstance persistTrackedEntityInstance()
    {
        TrackedEntityInstance entityInstance = createTrackedEntityInstance( organisationUnitA );
        entityInstance.setTrackedEntityType( trackedEntityTypeA );
        trackedEntityInstanceService.addTrackedEntityInstance( entityInstance );
        return entityInstance;
    }

    public TrackedEntityInstance persistTrackedEntityInstance( Map<String, Object> teiValues )
    {
        TrackedEntityInstance entityInstance = createTrackedEntityInstance( organisationUnitA );
        entityInstance.setTrackedEntityType( trackedEntityTypeA );
        if ( teiValues != null && !teiValues.isEmpty() )
        {
            for ( String method : teiValues.keySet() )
            {
                try
                {
                    BeanUtils.setProperty( entityInstance, method, teiValues.get( method ) );
                }
                catch ( IllegalAccessException | InvocationTargetException e )
                {
                    fail( e.getMessage() );
                }
            }
        }
        trackedEntityInstanceService.addTrackedEntityInstance( entityInstance );
        return entityInstance;
    }

    private Relationship _persistRelationship( RelationshipItem from, RelationshipItem to )
    {
        Relationship relationship = new Relationship();
        relationship.setFrom( from );
        relationship.setTo( to );
        relationship.setRelationshipType( relationshipType );
        relationship.setKey( RelationshipUtils.generateRelationshipKey( relationship ) );
        relationship.setInvertedKey( RelationshipUtils.generateRelationshipInvertedKey( relationship ) );
        relationshipService.addRelationship( relationship );
        return relationship;
    }

    public Relationship persistRelationship( TrackedEntityInstance t1, TrackedEntityInstance t2 )
    {
        RelationshipItem from = new RelationshipItem();
        from.setTrackedEntityInstance( t1 );
        RelationshipItem to = new RelationshipItem();
        to.setTrackedEntityInstance( t2 );
        return _persistRelationship( from, to );
    }

    public Relationship persistRelationship( TrackedEntityInstance tei, ProgramInstance pi )
    {
        RelationshipItem from = new RelationshipItem();
        from.setTrackedEntityInstance( tei );
        RelationshipItem to = new RelationshipItem();
        to.setProgramInstance( pi );
        return _persistRelationship( from, to );
    }

    public Relationship persistRelationship( TrackedEntityInstance tei, ProgramStageInstance psi )
    {
        RelationshipItem from = new RelationshipItem();
        from.setTrackedEntityInstance( tei );
        RelationshipItem to = new RelationshipItem();
        to.setProgramStageInstance( psi );
        return _persistRelationship( from, to );
    }

    public TrackedEntityInstance persistTrackedEntityInstanceWithEnrollment()
    {
        return _persistTrackedEntityInstanceWithEnrollmentAndEvents( 0, new HashMap<>() );
    }

    public TrackedEntityInstance persistTrackedEntityInstanceWithEnrollmentAndEvents()
    {
        return _persistTrackedEntityInstanceWithEnrollmentAndEvents( 5, new HashMap<>() );
    }

    public TrackedEntityInstance persistTrackedEntityInstanceWithEnrollmentAndEvents(
        Map<String, Object> enrollmentValues )
    {
        return _persistTrackedEntityInstanceWithEnrollmentAndEvents( 5, enrollmentValues );
    }

    public Enrollment deleteOneEnrollment(
        org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance trackedEntityInstance )
    {
        List<Enrollment> enrollments = trackedEntityInstance.getEnrollments();
        assertThat( enrollments, is( not( empty() ) ) );

        Enrollment enrollment = enrollments.get( 0 );
        ImportSummary importSummary = enrollmentService.deleteEnrollment( enrollment.getEnrollment() );
        assertEquals( 0, importSummary.getConflictCount() );
        return enrollment;

    }

    public Event deleteOneEvent( Enrollment enrollment )
    {
        List<Event> events = enrollment.getEvents();
        assertThat( events, is( not( empty() ) ) );

        Event event = events.get( 0 );
        ImportSummary importSummary = eventService.deleteEvent( event.getEvent() );
        assertEquals( 0, importSummary.getConflictCount() );
        return event;
    }

    private TrackedEntityInstance _persistTrackedEntityInstanceWithEnrollmentAndEvents( int eventSize,
        Map<String, Object> enrollmentValues )
    {
        TrackedEntityInstance entityInstance = persistTrackedEntityInstance();
        final ImportSummary importSummary = enrollmentService.addEnrollment(
            createEnrollmentWithEvents( this.programA, entityInstance, eventSize, enrollmentValues ),
            ImportOptions.getDefaultImportOptions() );
        assertEquals( 0, importSummary.getConflictCount() );
        assertThat( importSummary.getEvents().getImported(), is( eventSize ) );
        return entityInstance;
    }

    private Enrollment createEnrollmentWithEvents( Program program, TrackedEntityInstance trackedEntityInstance,
        int events )
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        enrollment.setOrgUnit( organisationUnitA.getUid() );
        enrollment.setProgram( program.getUid() );
        enrollment.setTrackedEntityInstance( trackedEntityInstance.getUid() );
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setStatus( EnrollmentStatus.COMPLETED );
        enrollment.setIncidentDate( new Date() );
        enrollment.setCompletedDate( new Date() );
        enrollment.setCompletedBy( "hello-world" );
        if ( events > 0 )
        {
            List<Event> eventList = new ArrayList<>();
            String now = DateUtils.getIso8601NoTz( new Date() );
            for ( int i = 0; i < events; i++ )
            {
                Event event1 = new Event();
                event1.setEnrollment( enrollment.getEnrollment() );
                event1.setEventDate(
                    DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH ).format( LocalDateTime.now() ) );
                event1.setProgram( programA.getUid() );
                event1.setProgramStage( programStageA1.getUid() );
                event1.setStatus( EventStatus.COMPLETED );
                event1.setTrackedEntityInstance( trackedEntityInstance.getUid() );
                event1.setOrgUnit( organisationUnitA.getUid() );
                event1.setAttributeOptionCombo( DEF_COC_UID );
                event1.setCreatedAtClient( now );
                event1.setLastUpdatedAtClient( now );
                event1.setCompletedDate( now );
                event1.setCompletedBy( "[Unknown]" );
                eventList.add( event1 );
            }
            enrollment.setEvents( eventList );
        }
        return enrollment;
    }

    private Enrollment createEnrollmentWithEvents( Program program, TrackedEntityInstance trackedEntityInstance,
        int events, Map<String, Object> enrollmentValues )
    {
        Enrollment enrollment = createEnrollmentWithEvents( program, trackedEntityInstance, events );
        if ( enrollmentValues != null && !enrollmentValues.isEmpty() )
        {
            for ( String method : enrollmentValues.keySet() )
            {
                try
                {
                    BeanUtils.setProperty( enrollment, method, enrollmentValues.get( method ) );
                }
                catch ( IllegalAccessException | InvocationTargetException e )
                {
                    fail( e.getMessage() );
                }
            }
        }
        return enrollment;
    }

    protected ProgramStage createProgramStage( Program program, boolean publicAccess )
    {
        ProgramStage programStage = createProgramStage( '1', program );
        programStage.setUid( CodeGenerator.generateUid() );
        programStage.setRepeatable( true );
        if ( publicAccess )
        {
            programStage.setPublicAccess( AccessStringHelper.FULL );
        }
        doInTransaction( () -> manager.save( programStage ) );
        return programStage;
    }

    protected void doInTransaction( Runnable operation )
    {
        final int defaultPropagationBehaviour = txTemplate.getPropagationBehavior();
        txTemplate.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRES_NEW );
        txTemplate.execute( status -> {
            operation.run();
            return null;
        } );
        // restore original propagation behaviour
        txTemplate.setPropagationBehavior( defaultPropagationBehaviour );
    }

    protected void makeUserSuper( User user )
    {
        UserRole group = new UserRole();
        group.setName( "Super" );
        group.setUid( "uid4" );
        group
            .setAuthorities( new HashSet<>( Arrays.asList( "z1", UserRole.AUTHORITY_ALL ) ) );
        user.setUserRoles( Sets.newHashSet( group ) );
    }
}
