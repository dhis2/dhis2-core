package org.hisp.dhis.tracker.preheat;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.descriptors.OrganisationUnitSchemaDescriptor;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentStore;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

/**
 * @author Luciano Fiandesio
 */
@Ignore // FIXME this test has to be rewritten from scratch
public class DefaultTrackerPreheatServiceTest
{
    @Mock
    private SchemaService schemaService;

    @Mock
    private QueryService queryService;

    @Mock
    private IdentifiableObjectManager manager;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PeriodStore periodStore;

    @Mock
    private TrackedEntityInstanceStore trackedEntityInstanceStore;

    @Mock
    private ProgramInstanceStore programInstanceStore;

    @Mock
    private ProgramStageInstanceStore programStageInstanceStore;

    @Mock
    private RelationshipStore relationshipStore;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Mock
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Mock
    private TrackedEntityCommentStore trackedEntityCommentStore;

    @InjectMocks
    private DefaultTrackerPreheatService subject;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void verifyTrackedEntitiesPreheated()
    {
        // Given
        final List<TrackedEntity> trackedEntities = rnd.randomObjects( TrackedEntity.class, 3 );
        final List<TrackedEntityInstance> preheatTeis = rnd.randomObjects( TrackedEntityInstance.class, 3, "uid" );

        IntStream.range( 0, 3 ).forEach( i -> preheatTeis.get( i ).setUid( trackedEntities.get( i ).getUid() ) );

        when( manager.get( User.class, getUser().getUid() ) ).thenReturn( getUser() );
        when( trackedEntityInstanceStore.getByUid( anyList(), any( User.class ) ) ).thenReturn( preheatTeis );

        final TrackerImportParams preheatParams = TrackerImportParams.builder()
            .user( getUser() )
            .trackedEntities( trackedEntities )
            .build();

        final TrackerPreheat preheat = subject.preheat( preheatParams );

        // Then
        final Map<String, TrackedEntityInstance> teis = preheat.getTrackedEntities().get( TrackerIdScheme.UID );

        assertThat( teis.keySet(), hasSize( 3 ) );
        assertThat( teis.keySet(), containsInAnyOrder( trackedEntities.get( 0 ).getUid(),
            trackedEntities.get( 1 ).getUid(), trackedEntities.get( 2 ).getUid() ) );
    }

    @Test
    public void verifyEnrollmentsPreheated()
    {
        // Given
        final List<Enrollment> enrollments = rnd.randomObjects( Enrollment.class, 3 );
        final List<ProgramInstance> preheatPi = rnd.randomObjects( ProgramInstance.class, 3, "uid" );

        IntStream.range( 0, 3 ).forEach( i -> preheatPi.get( i ).setUid( enrollments.get( i ).getUid() ) );

        when( manager.get( User.class, getUser().getUid() ) ).thenReturn( getUser() );
        when( programInstanceStore.getByUid( anyList(), any( User.class ) ) ).thenReturn( preheatPi );

        final TrackerImportParams preheatParams = TrackerImportParams.builder()
            .user( getUser() )
            .enrollments( enrollments )
            .build();

        final TrackerPreheat preheat = subject.preheat( preheatParams );

        // Then
        final Map<String, ProgramInstance> pis = preheat.getEnrollments().get( TrackerIdScheme.UID );

        assertThat( pis.keySet(), hasSize( 3 ) );
        assertThat( pis.keySet(), containsInAnyOrder( enrollments.get( 0 ).getUid(),
            enrollments.get( 1 ).getUid(), enrollments.get( 2 ).getUid() ) );
    }

    @Test
    public void verifyEventsPreheated()
    {
        // Given
        final List<Event> events = rnd.randomObjects( Event.class, 3 );
        final List<ProgramStageInstance> preheatPsi = rnd.randomObjects( ProgramStageInstance.class, 3, "uid" );

        IntStream.range( 0, 3 ).forEach( i -> preheatPsi.get( i ).setUid( events.get( i ).getUid() ) );

        when( manager.get( User.class, getUser().getUid() ) ).thenReturn( getUser() );
        when( programStageInstanceStore.getByUid( anyList(), any( User.class ) ) ).thenReturn( preheatPsi );

        // When
        final TrackerImportParams preheatParams = TrackerImportParams.builder()
            .user( getUser() )
            .events( events )
            .build();

        final TrackerPreheat preheat = subject.preheat( preheatParams );

        // Then
        final Map<String, ProgramStageInstance> psis = preheat.getEvents().get( TrackerIdScheme.UID );

        assertThat( psis.keySet(), hasSize( 3 ) );
        assertThat( psis.keySet(), containsInAnyOrder( events.get( 0 ).getUid(),
            events.get( 1 ).getUid(), events.get( 2 ).getUid() ) );
    }

    @Test
    public void verifyOrgUnitsPreheated()
    {
        // Given
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass( Query.class );
        final List<OrganisationUnit> organisationUnits = rnd.randomObjects( OrganisationUnit.class, 3 );
        final List<TrackedEntity> trackedEntitiesIithOu = rnd.randomObjects( TrackedEntity.class, 3 );

        IntStream.range( 0, 3 )
            .forEach( i -> trackedEntitiesIithOu.get( i ).setOrgUnit( organisationUnits.get( i ).getUid() ) );

        when( manager.get( User.class, getUser().getUid() ) ).thenReturn( getUser() );
        when( schemaService.getDynamicSchema( OrganisationUnit.class ) )
            .thenReturn( new OrganisationUnitSchemaDescriptor().getSchema() );

        doReturn( organisationUnits ).when( queryService ).query( queryCaptor.capture() );

        // When
        final TrackerImportParams preheatParams = TrackerImportParams.builder()
            .user( getUser() )
            .trackedEntities( trackedEntitiesIithOu )
            .build();

        final TrackerPreheat preheat = subject.preheat( preheatParams );

        // Then
        assertThat( getGeneric( preheat, OrganisationUnit.class, organisationUnits.get( 0 ).getUid() ),
            is( notNullValue() ) );
        assertThat( getGeneric( preheat, OrganisationUnit.class, organisationUnits.get( 1 ).getUid() ),
            is( notNullValue() ) );
        assertThat( getGeneric( preheat, OrganisationUnit.class, organisationUnits.get( 2 ).getUid() ),
            is( notNullValue() ) );

        final Query query = queryCaptor.getValue();
        assertThat( query.getSchema().getKlass().getName(), is( OrganisationUnit.class.getName() ) );
        assertThat( query.getDefaults(), is( Defaults.INCLUDE ) );
        assertThat( query.getUser().getUid(), is( "user1234" ) );
    }

    @Test
    public void verifyEnrollmentsNotesPreheated()
    {
        // Given
        final List<Enrollment> enrollments = rnd.randomObjects( Enrollment.class, 3 );
        final List<TrackedEntityComment> notes = rnd.randomObjects( TrackedEntityComment.class, 3 );
        final List<ProgramInstance> preheatPi = rnd.randomObjects( ProgramInstance.class, 3, "uid" );

        IntStream.range( 0, 3 ).forEach( i -> {
            enrollments.get( i ).setNotes( Collections.singletonList(
                new Note( CodeGenerator.generateUid(), "", "", RandomStringUtils.randomAlphabetic( 3 ) ) ) );
            preheatPi.get( i ).setUid( enrollments.get( i ).getUid() );
            notes.get( 0 ).setUid( enrollments.get( i ).getNotes().get( 0 ).getNote() );
        } );

        when( manager.get( User.class, getUser().getUid() ) ).thenReturn( getUser() );
        when( programInstanceStore.getByUid( anyList(), any( User.class ) ) ).thenReturn( preheatPi );
        when( trackedEntityCommentStore.getByUid( anyList(), any( User.class ) ) ).thenReturn( notes );

        final TrackerImportParams preheatParams = TrackerImportParams.builder()
            .user( getUser() )
            .enrollments( enrollments )
            .build();

        final TrackerPreheat preheat = subject.preheat( preheatParams );

        // Then
        assertTrue( preheat.getNote( notes.get( 0 ).getUid() ).isPresent());
        assertTrue( preheat.getNote( notes.get( 1 ).getUid() ).isPresent());
        assertTrue( preheat.getNote( notes.get( 2 ).getUid() ).isPresent());
    }

    @Test
    public void verifyEventsNotesPreheated()
    {
        // Given
        final List<Event> events = rnd.randomObjects( Event.class, 3 );
        final List<ProgramStageInstance> preheatPsi = rnd.randomObjects( ProgramStageInstance.class, 3, "uid" );
        final List<TrackedEntityComment> notes = rnd.randomObjects( TrackedEntityComment.class, 3 );

        IntStream.range( 0, 3 ).forEach( i -> {
            events.get( i ).setNotes( Collections.singletonList(
                    new Note( CodeGenerator.generateUid(), "", "", RandomStringUtils.randomAlphabetic( 3 ) ) ) );
            preheatPsi.get( i ).setUid( events.get( i ).getUid() );
            notes.get( 0 ).setUid( events.get( i ).getNotes().get( 0 ).getNote() );
        } );

        when( manager.get( User.class, getUser().getUid() ) ).thenReturn( getUser() );
        when( programStageInstanceStore.getByUid( anyList(), any( User.class ) ) ).thenReturn( preheatPsi );
        when( trackedEntityCommentStore.getByUid( anyList(), any( User.class ) ) ).thenReturn( notes );

        final TrackerImportParams preheatParams = TrackerImportParams.builder()
                .user( getUser() )
                .events( events )
                .build();

        final TrackerPreheat preheat = subject.preheat( preheatParams );

        // Then
        assertTrue( preheat.getNote( notes.get( 0 ).getUid() ).isPresent());
        assertTrue( preheat.getNote( notes.get( 1 ).getUid() ).isPresent());
        assertTrue( preheat.getNote( notes.get( 2 ).getUid() ).isPresent());

    }

    private <T extends IdentifiableObject> T getGeneric( TrackerPreheat preheat, Class<T> klazz, String uid )
    {
        return preheat.get( klazz, uid );
    }

    private User getUser()
    {
        User user = new User();
        user.setUid( "user1234" );
        return user;
    }

}