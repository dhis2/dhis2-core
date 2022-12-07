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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.validation.PersistablesFilter.filter;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.utils.Assertions;
import org.junit.jupiter.api.Test;

class PersistablesFilterTest
{
    // TODO refactor
    // TODO implement DELETE with CASCADE authority
    // TODO refactor using my sketch
    // TODO run all tests with everything wired up

    @Test
    void testCreateAndUpdateValidEntitiesCanBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" )
                    .event( "Qck4PQ7TMun" )
            .trackedEntity( "QxGbKYwChDM" )
                .enrollment( "Ok4Fe5moc3N" )
                    .event( "Ox1qBWsnVwE" )
                    .event( "jNyGqnwryNi" )
            .relationship("Te3IC6TpnBB",
                trackedEntity("xK7H53f4Hc2"),
                trackedEntity("QxGbKYwChDM")
            );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2", "QxGbKYwChDM" ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p", "Ok4Fe5moc3N" ),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun", "Ox1qBWsnVwE", "jNyGqnwryNi" ),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB")
        );
    }

    @Test
    void testCreateAndUpdateValidEntitiesCanBePersistedIfTheyExist()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" ).isExisting()
                .enrollment( "t1zaUjKgT3p" ).isExisting()
                    .event( "Qck4PQ7TMun" ).isExisting();

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p" ),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun" )
        );
    }

    @Test
    void testCreateAndUpdateInvalidTeiCannotBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" ).isInvalid();

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertIsEmpty( persistable.get( TrackedEntity.class ) );
    }

    @Test
    void testCreateAndUpdateValidEnrollmentOfInvalidTeiCanBeUpdatedIfTeiExists()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" ).isInvalid().isExisting()
                .enrollment( "t1zaUjKgT3p" );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertIsEmpty( persistable.get( TrackedEntity.class ) ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p" )
        );
    }

    @Test
    void testCreateAndUpdateValidEnrollmentOfInvalidTeiCannotBeUpdatedIfTeiDoesNotExist()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" ).isInvalid()
                .enrollment( "t1zaUjKgT3p" );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertIsEmpty( persistable.get( TrackedEntity.class ) ),
                () -> assertIsEmpty( persistable.get( Enrollment.class ) )
        );
    }

    @Test
    void testCreateAndUpdateInvalidEnrollmentOfValidTeiCannotBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" ).isInvalid();

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Enrollment.class ) )
        );
    }

    @Test
    void testCreateAndUpdateValidEventOfInvalidEnrollmentCanBeUpdatedIfEnrollmentExists()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" ).isInvalid().isExisting()
                    .event( "Qck4PQ7TMun" );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Enrollment.class ) ),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun" )
        );
    }

    @Test
    void testCreateAndUpdateValidEventOfInvalidEnrollmentCannotBeUpdatedIfEnrollmentDoesNotExist()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" ).isInvalid()
                    .event( "Qck4PQ7TMun" );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Enrollment.class ) ),
                () -> assertIsEmpty( persistable.get( Event.class ) )
        );
    }

    @Test
    void testCreateAndUpdateInvalidEventOfValidEnrollmentCannotBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" )
                    .event( "Qck4PQ7TMun" ).isInvalid();

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p" ),
                () -> assertIsEmpty( persistable.get( Event.class ) )
        );
    }

    @Test
    void testCreateAndUpdateValidEventWithoutEnrollment()
    {
        // @formatter:off
        // an event in an event program does not have an enrollment set (even though technically its enrolled in a default program)
        Setup setup = new Setup()
            .eventWithoutRegistration( "Qck4PQ7TMun" ).isInvalid()
            .eventWithoutRegistration( "Ok4Fe5moc3N" ).isExisting()
            .eventWithoutRegistration( "nVjkL7qHYvL" ).isExisting().isInvalid()
            .eventWithoutRegistration( "MeC1UpOX4Wu" );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertIsEmpty( persistable.get( TrackedEntity.class ) ),
                        () -> assertIsEmpty( persistable.get( Enrollment.class ) ),
                                () -> assertContainsOnly( persistable, Event.class, "Ok4Fe5moc3N", "MeC1UpOX4Wu" )
        );
    }

    @Test
    void testCreateAndUpdateInvalidRelationshipCannotBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                .trackedEntity( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    trackedEntity("QxGbKYwChDM")
                ).isInvalid();

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2", "QxGbKYwChDM" ),
                () -> assertIsEmpty( persistable.get( Relationship.class ) )
        );
    }

    @Test
    void testCreateAndUpdateValidRelationshipWithInvalidButExistingFrom()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" ).isInvalid().isExisting()
                .trackedEntity( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    trackedEntity("QxGbKYwChDM")
                );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class,  "QxGbKYwChDM" ),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB")
        );
    }

    @Test
    void testCreateAndUpdateValidRelationshipWithInvalidButExistingTo()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                .trackedEntity( "QxGbKYwChDM" ).isInvalid().isExisting()
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    trackedEntity("QxGbKYwChDM")
                );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class,  "xK7H53f4Hc2" ),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB")
        );
    }

    @Test
    void testCreateAndUpdateValidRelationshipWithInvalidFromCannotBeCreated()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "QxGbKYwChDM" ).isInvalid()
                .relationship("Te3IC6TpnBB",
                    enrollment("QxGbKYwChDM"),
                    trackedEntity("xK7H53f4Hc2")
                );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class,  "xK7H53f4Hc2" ),
            () -> assertIsEmpty( persistable.get( Relationship.class ) )
        );
    }

    @Test
    void testCreateAndUpdateValidRelationshipWithInvalidToCannotBeCreated()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                .eventWithoutRegistration( "QxGbKYwChDM" ).isInvalid()
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    event("QxGbKYwChDM")
                );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class,  "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Relationship.class ) )
        );
    }

    @Test
    void testDeleteValidEntitiesCanBeDeleted()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" )
                        .event( "Qck4PQ7TMun" )
                .trackedEntity( "QxGbKYwChDM" )
                    .enrollment( "Ok4Fe5moc3N" )
                        .event( "Ox1qBWsnVwE" )
                        .event( "jNyGqnwryNi" )
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    trackedEntity("QxGbKYwChDM")
                );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2", "QxGbKYwChDM" ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p", "Ok4Fe5moc3N" ),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun", "Ox1qBWsnVwE", "jNyGqnwryNi" ),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB")
        );
    }

    @Test
    void testDeleteInvalidTrackedEntityAndItsChildrenCannotBeDeleted()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" ).isInvalid()
                    .enrollment( "t1zaUjKgT3p" )
                        .event( "Qck4PQ7TMun" )
                        .event( "Ox1qBWsnVwE" );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p"),
                () ->assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun", "Ox1qBWsnVwE")
        );
    }

    @Test
    void testDeleteValidTrackedEntityOfInvalidEnrollmentCannotBeDeleted()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" ).isInvalid()
                        .event( "Qck4PQ7TMun" );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun")
        );
    }

    @Test
    void testDeleteValidTrackedEntityAndEnrollmentWithInvalidEventCannotBeDeleted()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" )
                        .event( "Qck4PQ7TMun" ).isInvalid()
                        .event( "Ox1qBWsnVwE" )
                .eventWithoutRegistration("G9cH8AVvguf");

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertContainsOnly( persistable, Event.class, "Ox1qBWsnVwE", "G9cH8AVvguf")
        );
    }

    @Test
    void testDeleteInvalidRelationshipPreventsDeletionOfTrackedEntityAndEvent()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                .eventWithoutRegistration( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                        trackedEntity("xK7H53f4Hc2"),
                        event("QxGbKYwChDM")
                ).isInvalid();

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertIsEmpty(persistable.get(Event.class)),
                () -> assertIsEmpty(persistable.get(Relationship.class))
        );
    }

    @Test
    void testDeleteInvalidRelationshipPreventsDeletionOfEnrollmentAndEvent()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" )
                        .event( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                        event("QxGbKYwChDM"),
                        enrollment("t1zaUjKgT3p")
                ).isInvalid();

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertIsEmpty(persistable.get(Event.class)),
                () -> assertIsEmpty(persistable.get(Relationship.class))
        );
    }

    @Test
    void testDeleteValidRelationshipWithInvalidFrom()
    {
        // @formatter:off
        Setup setup = new Setup()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" ).isInvalid()
                .eventWithoutRegistration( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                        enrollment("t1zaUjKgT3p"),
                        event("QxGbKYwChDM")
                );

        PersistablesFilter.Result persistable = filter( setup.entities(), setup.invalidEntities(),
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertContainsOnly( persistable, Event.class, "QxGbKYwChDM"),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB")
        );
    }

    /**
     * Setup builds the arguments for calling
     * {@link PersistablesFilter#filter(TrackerBundle, EnumMap, TrackerImportStrategy)}
     * Adding an entity with methods like {@link #trackedEntity(String)} always assumes the
     * entity is valid and does not yet exist.
     * <p>
     * Call {@link #isInvalid()} or {@link #isExisting()} to mark the current
     * entity as invalid or existing. You need to make sure to add entities in
     * the right order (hierarchy) otherwise you'll get NPE. This means that you
     * cannot add an {@link #enrollment(String)} without first adding a
     * {@link #trackedEntity(String)}.
     * </p>
     */
    private static class Setup
    {

        private final TrackerBundle bundle;

        private final EnumMap<TrackerType, Set<String>> invalidEntities = PersistablesFilterTest.invalidEntities();

        private final TrackerPreheat preheat;

        /**
         * Keeps track of the current entity that can be set to
         * {@link #isInvalid()} or {@link #isExisting()}.
         */
        private TrackerDto current;

        /**
         * Keeps track of the current trackedEntity to which enrollments and
         * events will be added on calls to {@link #enrollment(String)} or
         * {@link #event(String)}.
         */
        private TrackedEntity currentTrackedEntity;

        private Enrollment currentEnrollment;

        Setup()
        {
            this.preheat = mock( TrackerPreheat.class );
            this.bundle = TrackerBundle.builder().preheat(this.preheat).build();
        }

        Setup trackedEntity( String uid )
        {
            TrackedEntity trackedEntity = TrackedEntity.builder().trackedEntity(uid).build();
            bundle.getTrackedEntities().add(trackedEntity);
            // set cursors
            current = trackedEntity;
            currentTrackedEntity = trackedEntity;
            return this;
        }

        Setup enrollment( String uid )
        {
            Enrollment enrollment = enrollment( uid, currentTrackedEntity );
            bundle.getEnrollments().add(enrollment);
            // set cursors
            current = enrollment;
            currentEnrollment = enrollment;
            return this;
        }

        private static Enrollment enrollment( String uid, TrackedEntity parent )
        {
            // set child/parent links
            Enrollment enrollment = Enrollment.builder().enrollment( uid ).trackedEntity( parent.getUid() ).build();
            parent.getEnrollments().add( enrollment );
            return enrollment;
        }

        Setup event( String uid )
        {
            Event event = event( uid, currentEnrollment );
            bundle.getEvents().add(event);
            // set cursors
            current = event;
            return this;
        }

        Setup eventWithoutRegistration( String uid )
        {
            Event event = event( uid, null );
            bundle.getEvents().add(event);
            // set cursors
            current = event;
            // unset enrollment cursor. it might be confusing otherwise when calling .enrollment().eventWithoutRegistration().event()
            currentEnrollment = null;
            return this;
        }

        private static Event event( String uid, Enrollment parent )
        {
            Event event;
            Event.EventBuilder eventBuilder = Event.builder().event( uid );

            if ( parent != null )
            {
                // set child/parent links only if the event has a parent. Events in an event program have no enrollment.
                // They do have a "fake" enrollment (a default program) but it's not set on the event DTO.
                event = eventBuilder.enrollment( parent.getUid() ).build();
                parent.getEvents().add( event );
            }
            else
            {
                event = eventBuilder.build();
            }

            return event;
        }

        Setup relationship(String uid, RelationshipItem from, RelationshipItem to) {
            Relationship relationship = Relationship.builder()
                    .relationship(uid)
                    .from(from)
                    .to(to)
                    .build();
            bundle.getRelationships().add(relationship);
            // set cursors
            current = relationship;
            return this;
        }

        /**
         * Marks {@link #current} {@link TrackerDto} as invalid.
         *
         * @return this setup
         */
        Setup isInvalid()
        {
            this.invalidEntities.get( current.getTrackerType() ).add( current.getUid() );
            return this;
        }

        /**
         * Marks {@link #current} {@link TrackerDto} as existing in
         * {@link #preheat}.
         *
         * @return this setup
         */
        Setup isExisting()
        {
            when( this.preheat.exists( current.getTrackerType(), current.getUid() ) )
                .thenReturn( true);
            when( this.preheat.exists( current ) ).thenReturn( true);
            return this;
        }

        TrackerBundle entities()
        {
            return bundle;
        }

        EnumMap<TrackerType, Set<String>> invalidEntities()
        {
            return invalidEntities;
        }
    }

    private static RelationshipItem trackedEntity(String uid) {
        return RelationshipItem.builder().trackedEntity(uid).build();
    }

    private static RelationshipItem enrollment(String uid) {
        return RelationshipItem.builder().enrollment(uid).build();
    }

    private static RelationshipItem event(String uid) {
        return RelationshipItem.builder().event(uid).build();
    }

    private static EnumMap<TrackerType, Set<String>> invalidEntities()
    {
        return new EnumMap<>( Map.of(
            TRACKED_ENTITY, new HashSet<>(),
            ENROLLMENT, new HashSet<>(),
            EVENT, new HashSet<>(),
            RELATIONSHIP, new HashSet<>() ) );
    }

    private static <T extends TrackerDto> void assertContainsOnly(PersistablesFilter.Result persistable,
                                                                  Class<T> type, String... uid )
    {
        Assertions.assertContainsOnly( List.of( uid ), persistableUids( persistable, type ) );
    }

    private static <T extends TrackerDto> List<String> persistableUids( PersistablesFilter.Result persistable,
        Class<T> type )
    {
        return persistable.get( type ).stream().map( TrackerDto::getUid ).collect( Collectors.toList() );
    }

}