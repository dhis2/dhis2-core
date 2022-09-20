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
package org.hisp.dhis.dxf2.events.aggregates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hisp.dhis.matchers.DateTimeFormatMatcher.hasDateTimeFormat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.TrackerTest;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.ProgramOwner;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
class TrackedEntityInstanceAggregateTest extends TrackerTest
{
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private TrackedEntityProgramOwnerService programOwnerService;

    @Autowired
    private CurrentUserService currentUserService;

    private final static String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private User superUser;

    private User nonSuperUser;

    @BeforeEach
    void setUp()
    {
        doInTransaction( () -> {
            superUser = preCreateInjectAdminUser();
            injectSecurityContext( superUser );

            nonSuperUser = createUserWithAuth( "testUser2" );
            nonSuperUser.addOrganisationUnit( organisationUnitA );
            nonSuperUser.getTeiSearchOrganisationUnits().add( organisationUnitA );
            nonSuperUser.getTeiSearchOrganisationUnits().add( organisationUnitB );
            userService.updateUser( nonSuperUser );

            dbmsManager.clearSession();
        } );

    }

    @Test
    void testFetchTrackedEntityInstances()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 0 ) );
        // Check further for explicit uid in param
        queryParams.getTrackedEntityInstanceUids().addAll( trackedEntityInstances.stream().limit( 2 )
            .map( TrackedEntityInstance::getTrackedEntityInstance ).collect( Collectors.toSet() ) );
        final List<TrackedEntityInstance> limitedTTrackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( limitedTTrackedEntityInstances, hasSize( 2 ) );
    }

    @Test
    void testFetchTrackedEntityInstancesWithExplicitUid()
    {
        final String[] teiUid = new String[2];
        doInTransaction( () -> {
            org.hisp.dhis.trackedentity.TrackedEntityInstance t1 = this.persistTrackedEntityInstance();
            org.hisp.dhis.trackedentity.TrackedEntityInstance t2 = this.persistTrackedEntityInstance();
            teiUid[0] = t1.getUid();
            teiUid[1] = t2.getUid();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.getTrackedEntityInstanceUids().add( teiUid[0] );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 0 ).getTrackedEntityInstance(), is( teiUid[0] ) );
        // Query 2 tei uid explicitly
        queryParams.getTrackedEntityInstanceUids().add( teiUid[1] );
        final List<TrackedEntityInstance> multiTrackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( multiTrackedEntityInstances, hasSize( 2 ) );
        Set<String> teis = multiTrackedEntityInstances.stream().map( t -> t.getTrackedEntityInstance() )
            .collect( Collectors.toSet() );
        assertTrue( teis.contains( teiUid[0] ) );
        assertTrue( teis.contains( teiUid[1] ) );
    }

    @Test
    void testFetchTrackedEntityInstancesWithSingleQuoteInAttributeSearchInput()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.addFilter( new QueryItem( createTrackedEntityAttribute( 'A' ), QueryOperator.EQ, "M'M",
            ValueType.TEXT, AggregationType.NONE, null ) );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 0 ) );
    }

    @Test
    void testFetchTrackedEntityInstancesWithLastUpdatedParameter()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setLastUpdatedStartDate( Date.from( Instant.now().minus( 1, ChronoUnit.DAYS ) ) );
        queryParams.setLastUpdatedEndDate( new Date() );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 0 ) );
        // Update last updated start date to today
        queryParams.setLastUpdatedStartDate( Date.from( Instant.now().plus( 1, ChronoUnit.DAYS ) ) );
        final List<TrackedEntityInstance> limitedTTrackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( limitedTTrackedEntityInstances, hasSize( 0 ) );
    }

    @Test
    @Disabled( "12098 This test is not working" )
    void testFetchTrackedEntityInstancesWithEventFilters()
    {
        injectSecurityContext( superUser );
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();

            hibernateService.flushSession();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setUser( superUser );
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setProgram( programA );
        queryParams.setEventStatus( EventStatus.COMPLETED );
        queryParams.setEventStartDate( Date.from( Instant.now().minus( 10, ChronoUnit.DAYS ) ) );
        queryParams.setEventEndDate( Date.from( Instant.now().plus( 10, ChronoUnit.DAYS ) ) );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        // Update status to active
        queryParams.setEventStatus( EventStatus.ACTIVE );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( limitedTrackedEntityInstances, hasSize( 0 ) );
        // Update status to overdue
        queryParams.setEventStatus( EventStatus.OVERDUE );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances2 = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( limitedTrackedEntityInstances2, hasSize( 0 ) );
        // Update status to schedule
        queryParams.setEventStatus( EventStatus.SCHEDULE );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances3 = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( limitedTrackedEntityInstances3, hasSize( 0 ) );
        // Update status to schedule
        queryParams.setEventStatus( EventStatus.SKIPPED );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances4 = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( limitedTrackedEntityInstances4, hasSize( 0 ) );
        // Update status to visited
        queryParams.setEventStatus( EventStatus.VISITED );
        final List<TrackedEntityInstance> limitedTrackedEntityInstances5 = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( limitedTrackedEntityInstances5, hasSize( 0 ) );
    }

    @Test
    void testIncludeDeletedIsPropagetedFromTeiToEnrollmentsAndEvents()
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeDeleted( true );

        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.TRUE;

        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
        } );

        List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertFalse( trackedEntityInstances.get( 0 ).getEnrollments().get( 0 ).isDeleted() );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
        assertFalse( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents().stream()
            .anyMatch( Event::isDeleted ) );

        this.deleteOneEnrollment( trackedEntityInstances.get( 0 ) );
        this.deleteOneEvent( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ) );

        trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );

        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertTrue( trackedEntityInstances.get( 0 ).getEnrollments().get( 0 ).isDeleted() );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
        assertTrue( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents().stream()
            .anyMatch( Event::isDeleted ) );

        queryParams.setIncludeDeleted( false );
        trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents(), hasSize( 4 ) );
        assertFalse( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents().stream()
            .anyMatch( Event::isDeleted ) );
    }

    @Test
    void testFetchTrackedEntityInstancesAndEnrollments()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeEnrollments( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments(), hasSize( 1 ) );
    }

    @Test
    void testFetchTrackedEntityInstancesWithoutEnrollments()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments(), hasSize( 0 ) );
    }

    @Test
    void testFetchTrackedEntityInstancesWithEvents()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeEnrollments( true )
            .withIncludeEvents( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
    }

    @Test
    void testFetchTrackedEntityInstancesWithoutEvents()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeEnrollments( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments().get( 0 ).getEvents(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments().get( 0 ).getEvents(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments().get( 0 ).getEvents(), hasSize( 0 ) );
    }

    @Test
    void testTrackedEntityInstanceMapping()
    {
        final Date currentTime = new Date();
        doInTransaction( this::persistTrackedEntityInstanceWithEnrollmentAndEvents );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );
        assertThat( trackedEntityInstance.getTrackedEntityType(), is( trackedEntityTypeA.getUid() ) );
        assertTrue( CodeGenerator.isValidUid( trackedEntityInstance.getTrackedEntityInstance() ) );
        assertThat( trackedEntityInstance.getOrgUnit(), is( organisationUnitA.getUid() ) );
        assertThat( trackedEntityInstance.isInactive(), is( false ) );
        assertThat( trackedEntityInstance.isDeleted(), is( false ) );
        assertThat( trackedEntityInstance.getFeatureType(), is( FeatureType.NONE ) );
        // Dates
        checkDate( currentTime, trackedEntityInstance.getCreated(), 50L );
        checkDate( currentTime, trackedEntityInstance.getCreatedAtClient(), 50L );
        checkDate( currentTime, trackedEntityInstance.getLastUpdatedAtClient(), 50L );
        checkDate( currentTime, trackedEntityInstance.getLastUpdated(), 300L );
        // get stored by is always null
        assertThat( trackedEntityInstance.getStoredBy(), is( nullValue() ) );
    }

    @Test
    void testEventMapping()
    {
        final Date currentTime = new Date();
        doInTransaction( this::persistTrackedEntityInstanceWithEnrollmentAndEvents );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeEnrollments( true )
            .withIncludeEvents( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        TrackedEntityInstance tei = trackedEntityInstances.get( 0 );
        Enrollment enrollment = tei.getEnrollments().get( 0 );
        Event event = enrollment.getEvents().get( 0 );
        assertNotNull( event );
        // The id is not serialized to JSON
        assertThat( event.getId(), is( notNullValue() ) );
        assertThat( event.getUid(), is( nullValue() ) );
        assertTrue( CodeGenerator.isValidUid( event.getEvent() ) );
        assertThat( event.getStatus(), is( EventStatus.COMPLETED ) );
        assertThat( event.getProgram(), is( programA.getUid() ) );
        assertThat( event.getProgramStage(), is( programStageA1.getUid() ) );
        assertThat( event.getEnrollment(), is( enrollment.getEnrollment() ) );
        assertThat( event.getEnrollmentStatus(), is( enrollment.getStatus() ) );
        assertThat( event.getOrgUnit(), is( organisationUnitA.getUid() ) );
        assertThat( event.getOrgUnitName(), is( organisationUnitA.getName() ) );
        assertThat( event.getTrackedEntityInstance(), is( tei.getTrackedEntityInstance() ) );
        assertThat( event.getAttributeOptionCombo(), is( DEF_COC_UID ) );
        assertThat( event.isDeleted(), is( false ) );
        assertThat( event.getStoredBy(), is( "admin_test" ) );
        assertThat( event.getFollowup(), is( nullValue() ) );
        // Dates
        checkDate( currentTime, event.getCreated(), 500L );
        checkDate( currentTime, event.getLastUpdated(), 500L );
        assertThat( event.getEventDate(), is( notNullValue() ) );
        checkDate( currentTime, event.getDueDate(), 500L );
        checkDate( currentTime, event.getCreatedAtClient(), 500L );
        checkDate( currentTime, event.getLastUpdatedAtClient(), 500L );
        checkDate( currentTime, event.getCompletedDate(), 500L );
        assertThat( event.getCompletedBy(), is( "[Unknown]" ) );
    }

    @Test
    void testEnrollmentMapping()
    {
        final Date currentTime = new Date();
        doInTransaction( this::persistTrackedEntityInstanceWithEnrollmentAndEvents );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeEnrollments( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        Enrollment enrollment = trackedEntityInstances.get( 0 ).getEnrollments().get( 0 );
        assertThat( "Tracked Entity Type does not match", enrollment.getTrackedEntityType(),
            is( trackedEntityTypeA.getUid() ) );
        assertThat( "Tracked Entity Instance UID does not match", enrollment.getTrackedEntityInstance(),
            is( trackedEntityInstances.get( 0 ).getTrackedEntityInstance() ) );
        assertThat( "Org Unit UID does not match", enrollment.getOrgUnit(), is( organisationUnitA.getUid() ) );
        assertThat( "Org Unit Name does not match", enrollment.getOrgUnitName(), is( organisationUnitA.getName() ) );
        assertTrue( CodeGenerator.isValidUid( enrollment.getEnrollment() ) );
        assertThat( enrollment.getProgram(), is( programA.getUid() ) );
        assertThat( enrollment.getStatus(), is( EnrollmentStatus.COMPLETED ) );
        assertThat( enrollment.isDeleted(), is( false ) );
        assertThat( enrollment.getStoredBy(), is( "admin_test" ) );
        assertThat( enrollment.getFollowup(), is( nullValue() ) );
        // Dates
        checkDate( currentTime, enrollment.getCreated(), 200L );
        checkDate( currentTime, enrollment.getCreatedAtClient(), 200L );
        checkDate( currentTime, enrollment.getLastUpdatedAtClient(), 200L );
        checkDate( currentTime, enrollment.getLastUpdated(), 300L );
        checkDate( currentTime, enrollment.getEnrollmentDate(), 300L );
        checkDate( currentTime, enrollment.getIncidentDate(), 300L );
        checkDate( currentTime, enrollment.getCompletedDate(), 200L );
        assertThat( enrollment.getCompletedBy(), is( "hello-world" ) );
        // The Enrollment ID is not serialized to JSON
        assertThat( enrollment.getId(), is( notNullValue() ) );
    }

    @Test
    void testEnrollmentFollowup()
    {
        Map<String, Object> enrollmentValues = new HashMap<>();
        enrollmentValues.put( "followup", Boolean.TRUE );
        doInTransaction( () -> this.persistTrackedEntityInstanceWithEnrollmentAndEvents( enrollmentValues ) );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeEnrollments( true )
            .withIncludeEvents( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        TrackedEntityInstance tei = trackedEntityInstances.get( 0 );
        Enrollment enrollment = tei.getEnrollments().get( 0 );
        Event event = enrollment.getEvents().get( 0 );
        assertThat( enrollment.getFollowup(), is( true ) );
        assertThat( event.getFollowup(), is( true ) );
    }

    @Test
    void testTrackedEntityInstanceRelationshipsTei2Tei()
    {
        final String[] teiUid = new String[2];
        doInTransaction( () -> {
            injectSecurityContext( superUser );
            org.hisp.dhis.trackedentity.TrackedEntityInstance t1 = this.persistTrackedEntityInstance();
            org.hisp.dhis.trackedentity.TrackedEntityInstance t2 = this.persistTrackedEntityInstance();
            this.persistRelationship( t1, t2 );
            teiUid[0] = t1.getUid();
            teiUid[1] = t2.getUid();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeRelationships( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances.get( 0 ).getRelationships(), hasSize( 1 ) );
        final Relationship relationship = trackedEntityInstances.get( 0 ).getRelationships().get( 0 );
        assertThat( relationship.getFrom().getTrackedEntityInstance().getTrackedEntityInstance(), is( teiUid[0] ) );
        assertThat( relationship.getTo().getTrackedEntityInstance().getTrackedEntityInstance(), is( teiUid[1] ) );
    }

    @Test
    void testTrackedEntityInstanceRelationshipsTei2Enrollment()
    {
        User currentUser = currentUserService.getCurrentUser();
        final String[] relationshipItemsUid = new String[2];
        doInTransaction( () -> {
            org.hisp.dhis.trackedentity.TrackedEntityInstance t1 = this.persistTrackedEntityInstance();
            org.hisp.dhis.trackedentity.TrackedEntityInstance t2 = this.persistTrackedEntityInstanceWithEnrollment();
            ProgramInstance pi = t2.getProgramInstances().iterator().next();
            this.persistRelationship( t1, pi );
            relationshipItemsUid[0] = t1.getUid();
            relationshipItemsUid[1] = pi.getUid();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeRelationships( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        // Fetch the TEI which is the vertex of the relationship TEI <-->
        // ENROLLMENT
        Optional<TrackedEntityInstance> trackedEntityInstance = trackedEntityInstances.stream()
            .filter( t -> t.getTrackedEntityInstance().equals( relationshipItemsUid[0] ) ).findFirst();
        if ( trackedEntityInstance.isPresent() )
        {
            assertThat( trackedEntityInstance.get().getRelationships(), hasSize( 1 ) );
            final Relationship relationship = trackedEntityInstance.get().getRelationships().get( 0 );
            assertThat( relationship.getFrom().getTrackedEntityInstance().getTrackedEntityInstance(),
                is( relationshipItemsUid[0] ) );
            assertThat( relationship.getTo().getEnrollment().getEnrollment(), is( relationshipItemsUid[1] ) );
        }
        else
        {
            fail();
        }
    }

    @Test
    void testTrackedEntityInstanceRelationshipsTei2Event()
    {
        final String[] relationshipItemsUid = new String[2];
        doInTransaction( () -> {
            org.hisp.dhis.trackedentity.TrackedEntityInstance t1 = this.persistTrackedEntityInstance();
            org.hisp.dhis.trackedentity.TrackedEntityInstance t2 = this
                .persistTrackedEntityInstanceWithEnrollmentAndEvents();
            sessionFactory.getCurrentSession().flush();
            sessionFactory.getCurrentSession().clear();
            t2 = manager.getByUid( org.hisp.dhis.trackedentity.TrackedEntityInstance.class,
                Collections.singletonList( t2.getUid() ) ).get( 0 );
            ProgramInstance pi = t2.getProgramInstances().iterator().next();
            final ProgramStageInstance psi = pi.getProgramStageInstances().iterator().next();
            this.persistRelationship( t1, psi );
            relationshipItemsUid[0] = t1.getUid();
            relationshipItemsUid[1] = psi.getUid();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeEnrollments( true )
            .withIncludeEnrollments( true ).withIncludeRelationships( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        // Fetch the TEI which is the vertex of the relationship TEI <-->
        // ENROLLMENT
        Optional<TrackedEntityInstance> trackedEntityInstance = trackedEntityInstances.stream()
            .filter( t -> t.getTrackedEntityInstance().equals( relationshipItemsUid[0] ) ).findFirst();
        if ( trackedEntityInstance.isPresent() )
        {
            assertThat( trackedEntityInstance.get().getRelationships(), hasSize( 1 ) );
            final Relationship relationship = trackedEntityInstance.get().getRelationships().get( 0 );
            assertThat( relationship.getFrom().getTrackedEntityInstance().getTrackedEntityInstance(),
                is( relationshipItemsUid[0] ) );
            assertThat( relationship.getTo().getEvent().getEvent(), is( relationshipItemsUid[1] ) );
        }
        else
        {
            fail();
        }
    }

    @Test
    void testTrackedEntityInstanceProgramOwners()
    {
        doInTransaction( () -> {
            final org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = persistTrackedEntityInstance();
            programOwnerService.createOrUpdateTrackedEntityProgramOwner( trackedEntityInstance, programA,
                organisationUnitA );
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE.withIncludeProgramOwners( true );
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances.get( 0 ).getProgramOwners(), hasSize( 1 ) );
        ProgramOwner programOwner = trackedEntityInstances.get( 0 ).getProgramOwners().get( 0 );
        assertThat( programOwner.getProgram(), is( programA.getUid() ) );
        assertThat( programOwner.getOwnerOrgUnit(), is( organisationUnitA.getUid() ) );
        assertThat( programOwner.getTrackedEntityInstance(),
            is( trackedEntityInstances.get( 0 ).getTrackedEntityInstance() ) );
    }

    private void checkDate( Date currentTime, String date, long milliseconds )
    {
        final long interval = currentTime.getTime() - DateUtils.parseDate( date ).getTime();
        assertThat( date, hasDateTimeFormat( DATE_TIME_FORMAT ) );
        assertTrue( Math.abs( interval ) < milliseconds,
            "Timestamp is higher than expected interval. Expecting: " + milliseconds + " got: " + interval );
    }

    private void checkDate( Date currentTime, Date date, long milliseconds )
    {
        final long interval = currentTime.getTime() - date.getTime();
        assertTrue( Math.abs( interval ) < milliseconds,
            "Timestamp is higher than expected interval. Expecting: " + milliseconds + " got: " + interval );
    }
}
