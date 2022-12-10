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
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E5000;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E5001;
import static org.hisp.dhis.tracker.validation.PersistablesFilter.filter;
import static org.hisp.dhis.tracker.validation.hooks.AssertTrackerValidationReport.assertHasError;
import static org.hisp.dhis.tracker.validation.hooks.AssertTrackerValidationReport.assertHasNoErrorWithCode;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

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
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.utils.Assertions;
import org.junit.jupiter.api.Test;

class PersistablesFilterTest
{
    @Test
    void testCreateAndUpdateValidEntitiesCanBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" )
                    .event( "Qck4PQ7TMun" )
            .trackedEntity( "QxGbKYwChDM" )
                .enrollment( "Ok4Fe5moc3N" )
                    .event( "Ox1qBWsnVwE" )
                    .event( "jNyGqnwryNi" )
            .relationship("Te3IC6TpnBB",
                trackedEntity("xK7H53f4Hc2"),
                trackedEntity("QxGbKYwChDM") )
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2", "QxGbKYwChDM" ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p", "Ok4Fe5moc3N" ),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun", "Ox1qBWsnVwE", "jNyGqnwryNi" ),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB"),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    // TODO(DHIS2-14213) also test relationship
    @Test
    void testCreateAndUpdateValidEntitiesReferencingParentsNotInPayload()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" ).isExisting().isNotInPayload()
                    .enrollment( "t1zaUjKgT3p")
                .enrollment( "Ok4Fe5moc3N").isExisting().isNotInPayload()
                    .event( "Ox1qBWsnVwE" )
                .build();
//                .relationship("Te3IC6TpnBB",
//                        trackedEntity("xK7H53f4Hc2"),
//                        trackedEntity("QxGbKYwChDM")
//                );

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p"),
                () -> assertContainsOnly( persistable, Event.class,  "Ox1qBWsnVwE"),
//                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB"),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    // TODO(DHIS2-14213) move to delete tests
    @Test
    void testDeleteValidEntitiesReferencingParentsNotInPayload()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2").isExisting().isNotInPayload()
                    .enrollment( "t1zaUjKgT3p")
                .enrollment( "Ok4Fe5moc3N").isExisting().isNotInPayload()
                    .event( "Ox1qBWsnVwE" )
                .build();
//                .relationship("Te3IC6TpnBB",
//                        trackedEntity("xK7H53f4Hc2"),
//                        trackedEntity("QxGbKYwChDM")
//                );

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p"),
                () -> assertContainsOnly( persistable, Event.class,  "Ox1qBWsnVwE"),
//                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB"),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testDeleteInvalidEntitiesReferencingParentsNotInPayload()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2").isExisting().isNotInPayload()
                    .enrollment( "t1zaUjKgT3p").isInvalid()
                .enrollment( "Ok4Fe5moc3N").isExisting().isNotInPayload() // TODO(DHIS2-14213) could there be a case where a parent of a parent has a reference? second if
                    .event( "Ox1qBWsnVwE" ).isInvalid()
                .build();
//                .relationship("Te3IC6TpnBB",
//                        trackedEntity("xK7H53f4Hc2"),
//                        trackedEntity("QxGbKYwChDM")
//                );

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertIsEmpty(persistable.get(Event.class)),
                () -> assertIsEmpty(persistable.get(Relationship.class)),
                () -> assertNoErrorWithCode( persistable, E5001)
        );
    }

    @Test
    void testCreateAndUpdateValidEntitiesCanBePersistedIfTheyExist()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" ).isExisting()
                .enrollment( "t1zaUjKgT3p" ).isExisting()
                    .event( "Qck4PQ7TMun" ).isExisting()
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p" ),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun" ),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateInvalidTeiCannotBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" ).isInvalid()
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertIsEmpty( persistable.get( TrackedEntity.class ) ),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateValidEnrollmentOfInvalidTeiCanBeUpdatedIfTeiExists()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" ).isInvalid().isExisting()
                .enrollment( "t1zaUjKgT3p" )
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertIsEmpty( persistable.get( TrackedEntity.class ) ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p" ),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateValidEnrollmentOfInvalidTeiCannotBeUpdatedIfTeiDoesNotExist()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" ).isInvalid()
                .enrollment( "t1zaUjKgT3p" )
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertIsEmpty( persistable.get( TrackedEntity.class ) ),
                () -> assertIsEmpty( persistable.get( Enrollment.class ) ),
                () -> assertError(persistable, E5000, ENROLLMENT, "t1zaUjKgT3p", "because \"trackedEntity\" `xK7H53f4Hc2`")
        );
    }

    @Test
    void testCreateAndUpdateInvalidEnrollmentOfValidTeiCannotBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" ).isInvalid()
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Enrollment.class ) ),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateValidEventOfInvalidEnrollmentCanBeUpdatedIfEnrollmentExists()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" ).isInvalid().isExisting()
                    .event( "Qck4PQ7TMun" )
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Enrollment.class ) ),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun" ),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateValidEventOfInvalidEnrollmentCannotBeCreatedIfEnrollmentDoesNotExist()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" ).isInvalid()
                    .event( "Qck4PQ7TMun" )
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Enrollment.class ) ),
                () -> assertIsEmpty( persistable.get( Event.class ) ),
                () -> assertError(persistable, E5000, EVENT, "Qck4PQ7TMun", "because \"enrollment\" `t1zaUjKgT3p`")
        );
    }

    @Test
    void testCreateAndUpdateInvalidEventOfValidEnrollmentCannotBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
            .trackedEntity( "xK7H53f4Hc2" )
                .enrollment( "t1zaUjKgT3p" )
                    .event( "Qck4PQ7TMun" ).isInvalid()
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2" ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p" ),
                () -> assertIsEmpty( persistable.get( Event.class ) ),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateValidEventWithoutEnrollment()
    {
        // @formatter:off
        // an event in an event program does not have an enrollment set (even though technically its enrolled in a default program)
        Setup setup = new Setup.Builder()
            .eventWithoutRegistration( "Qck4PQ7TMun" ).isInvalid()
            .eventWithoutRegistration( "Ok4Fe5moc3N" ).isExisting()
            .eventWithoutRegistration( "nVjkL7qHYvL" ).isExisting().isInvalid()
            .eventWithoutRegistration( "MeC1UpOX4Wu" )
            .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, Event.class, "Ok4Fe5moc3N", "MeC1UpOX4Wu" ),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateInvalidRelationshipCannotBePersisted()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                .trackedEntity( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    trackedEntity("QxGbKYwChDM")).isInvalid()
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2", "QxGbKYwChDM" ),
                () -> assertIsEmpty( persistable.get( Relationship.class ) ),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateValidRelationshipWithInvalidButExistingFrom()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" ).isInvalid().isExisting()
                .trackedEntity( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    trackedEntity("QxGbKYwChDM") )
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class,  "QxGbKYwChDM" ),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB"),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateValidRelationshipWithInvalidButExistingTo()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                .trackedEntity( "QxGbKYwChDM" ).isInvalid().isExisting()
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    trackedEntity("QxGbKYwChDM") )
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class,  "xK7H53f4Hc2" ),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB"),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testCreateAndUpdateValidRelationshipWithInvalidFromCannotBeCreated()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "QxGbKYwChDM" ).isInvalid()
                .relationship("Te3IC6TpnBB",
                    enrollment("QxGbKYwChDM"),
                    trackedEntity("xK7H53f4Hc2") )
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class,  "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Relationship.class ) ),
                () -> assertError(persistable, E5000, RELATIONSHIP, "Te3IC6TpnBB", "because \"enrollment\" `QxGbKYwChDM`")
        );
    }

    @Test
    void testCreateAndUpdateValidRelationshipWithInvalidToCannotBeCreated()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                .eventWithoutRegistration( "QxGbKYwChDM" ).isInvalid()
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    event("QxGbKYwChDM") )
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.CREATE_AND_UPDATE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class,  "xK7H53f4Hc2" ),
                () -> assertIsEmpty( persistable.get( Relationship.class ) ),
                () -> assertError(persistable, E5000, RELATIONSHIP, "Te3IC6TpnBB", "because \"event\" `QxGbKYwChDM`")
        );
    }

    @Test
    void testDeleteValidEntitiesCanBeDeleted()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" )
                        .event( "Qck4PQ7TMun" )
                .trackedEntity( "QxGbKYwChDM" )
                    .enrollment( "Ok4Fe5moc3N" )
                        .event( "Ox1qBWsnVwE" )
                        .event( "jNyGqnwryNi" )
                .relationship("Te3IC6TpnBB",
                    trackedEntity("xK7H53f4Hc2"),
                    trackedEntity("QxGbKYwChDM") )
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertContainsOnly( persistable, TrackedEntity.class, "xK7H53f4Hc2", "QxGbKYwChDM" ),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p", "Ok4Fe5moc3N" ),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun", "Ox1qBWsnVwE", "jNyGqnwryNi" ),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB"),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testDeleteInvalidTrackedEntityCannotBeDeletedButItsChildrenCan()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" ).isInvalid()
                    .enrollment( "t1zaUjKgT3p" )
                        .event( "Qck4PQ7TMun" )
                        .event( "Ox1qBWsnVwE" )
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertContainsOnly( persistable, Enrollment.class, "t1zaUjKgT3p"),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun", "Ox1qBWsnVwE"),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testDeleteValidTrackedEntityOfInvalidEnrollmentCannotBeDeleted()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" ).isInvalid()
                        .event( "Qck4PQ7TMun" )
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertContainsOnly( persistable, Event.class, "Qck4PQ7TMun"),
                () -> assertError(persistable, E5001, TRACKED_ENTITY, "xK7H53f4Hc2", "because \"enrollment\" `t1zaUjKgT3p`")
        );
    }

    @Test
    void testDeleteOnlyReportErrorsIfItAddsNewInformation()
    {
        // If entities are found to be invalid during the validation an error for the entity will already be in the
        // validation report. Only add errors if it would not be clear why an entity cannot be persisted.

        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" ).isInvalid()
                    .enrollment( "t1zaUjKgT3p" ).isInvalid()
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertIsEmpty( persistable.getErrors())
        );
    }

    @Test
    void testDeleteValidTrackedEntityAndEnrollmentWithInvalidEventCannotBeDeleted()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" )
                        .event( "Qck4PQ7TMun" ).isInvalid()
                        .event( "Ox1qBWsnVwE" )
                .eventWithoutRegistration("G9cH8AVvguf")
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertContainsOnly( persistable, Event.class, "Ox1qBWsnVwE", "G9cH8AVvguf"),
                () -> assertError(persistable, E5001, TRACKED_ENTITY, "xK7H53f4Hc2", "because \"enrollment\" `t1zaUjKgT3p`"),
                () -> assertError(persistable, E5001, ENROLLMENT, "t1zaUjKgT3p", "because \"event\" `Qck4PQ7TMun`")
        );
    }

    @Test
    void testDeleteInvalidRelationshipPreventsDeletionOfTrackedEntityAndEvent()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                .eventWithoutRegistration( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                        trackedEntity("xK7H53f4Hc2"),
                        event("QxGbKYwChDM") ).isInvalid()
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertIsEmpty(persistable.get(Event.class)),
                () -> assertIsEmpty(persistable.get(Relationship.class)),
                () -> assertError(persistable, E5001, TRACKED_ENTITY, "xK7H53f4Hc2", "because \"relationship\" `Te3IC6TpnBB`"),
                () -> assertError(persistable, E5001, EVENT, "QxGbKYwChDM", "because \"relationship\" `Te3IC6TpnBB`")
        );
    }

    @Test
    void testDeleteInvalidRelationshipPreventsDeletionOfEnrollmentAndEvent()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" )
                        .event( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                        event("QxGbKYwChDM"),
                        enrollment("t1zaUjKgT3p") ).isInvalid()
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertIsEmpty(persistable.get(Event.class)),
                () -> assertIsEmpty(persistable.get(Relationship.class)),
                () -> assertError(persistable, E5001, ENROLLMENT, "t1zaUjKgT3p", "because \"relationship\" `Te3IC6TpnBB`"),
                () -> assertError(persistable, E5001, EVENT, "QxGbKYwChDM", "because \"relationship\" `Te3IC6TpnBB`")
        );
    }

    @Test
    void testDeleteValidRelationshipWithInvalidFrom()
    {
        // @formatter:off
        Setup setup = new Setup.Builder()
                .trackedEntity( "xK7H53f4Hc2" )
                    .enrollment( "t1zaUjKgT3p" ).isInvalid()
                .eventWithoutRegistration( "QxGbKYwChDM" )
                .relationship("Te3IC6TpnBB",
                        enrollment("t1zaUjKgT3p"),
                        event("QxGbKYwChDM") )
                .build();

        PersistablesFilter.Result persistable = filter( setup.bundle, setup.invalidEntities,
                TrackerImportStrategy.DELETE );

        assertAll(
                () -> assertIsEmpty(persistable.get(TrackedEntity.class)),
                () -> assertIsEmpty(persistable.get(Enrollment.class)),
                () -> assertContainsOnly( persistable, Event.class, "QxGbKYwChDM"),
                () -> assertContainsOnly( persistable, Relationship.class, "Te3IC6TpnBB")
        );
    }

    @RequiredArgsConstructor
    private static class Entity<T extends TrackerDto> {
        private boolean valid = true;

        private boolean isInPayload = true;

        private boolean isInDB = false;

        private final T entity;
    }

    @RequiredArgsConstructor
    private static class Setup {
        private final TrackerBundle bundle;

        private final EnumMap<TrackerType, Set<String>> invalidEntities;

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
        private static class Builder
        {
            private final List<Entity<TrackedEntity>> trackedEntities = new ArrayList<>();

            private final List<Entity<Enrollment>> enrollments = new ArrayList<>();

            private final List<Entity<Event>> events = new ArrayList<>();
            private final List<Entity<Relationship>> relationships = new ArrayList<>();

            /**
             * Keeps track of the current entity that can be set to
             * {@link #isInvalid()} or {@link #isExisting()}.
             */
            private Entity<? extends TrackerDto> current;

            /**
             * Keeps track of the current trackedEntity to which enrollments and
             * events will be added on calls to {@link #enrollment(String)} or
             * {@link #event(String)}.
             */
            private Entity<TrackedEntity> currentTrackedEntity;

            /**
             * Keeps track of the current enrollment to which events will be added on calls to {@link #event(String)}.
             */
            private Entity<Enrollment> currentEnrollment;

            Builder trackedEntity(String uid) {
                Entity<TrackedEntity> entity = new Entity<>(TrackedEntity.builder().trackedEntity(uid).build());
                this.trackedEntities.add(entity);
                current = entity;
                currentTrackedEntity = entity;
                return this;
            }

            /**
             * Add an enrollment to the payload with the {@link #currentTrackedEntity} as its parent which is also in the payload.
             * <p>
             * <b>Requires call</b> to {@link #trackedEntity(String)} first.
             * </p>
             *
             * @param uid uid of enrollment
             * @return setup
             */
            Builder enrollment( String uid )
            {
                Entity<Enrollment> entity = enrollment( uid, currentTrackedEntity );
                this.enrollments.add(entity);
                current = entity;
                currentEnrollment = entity;
                return this;
            }

            private static Entity<Enrollment> enrollment( String uid, Entity<TrackedEntity> parent )
            {
                // set child/parent links
                Enrollment enrollment = Enrollment.builder().enrollment( uid ).trackedEntity( parent.entity.getUid() ).build();
                parent.entity.getEnrollments().add( enrollment );
                return new Entity<>(enrollment);
            }

            Builder event( String uid )
            {
                Entity<Event> entity = event( uid, currentEnrollment );
                this.events.add(entity);
                current = entity;
                return this;
            }

            Builder eventWithoutRegistration( String uid )
            {
                Entity<Event> entity = event( uid, currentEnrollment );
                this.events.add(entity);
                current = entity;
                // unset enrollment cursor. Otherwise, it might be confusing when calling .enrollment().eventWithoutRegistration().event()
                currentEnrollment = null;
                return this;
            }

            private static Entity<Event> event( String uid, Entity<Enrollment> parent )
            {
                Event event;
                Event.EventBuilder eventBuilder = Event.builder().event( uid );

                if ( parent != null )
                {
                    // set child/parent links only if the event has a parent. Events in an event program have no enrollment.
                    // They do have a "fake" enrollment (a default program) but it's not set on the event DTO.
                    event = eventBuilder.enrollment( parent.entity.getUid() ).build();
                    parent.entity.getEvents().add( event );
                }
                else
                {
                    event = eventBuilder.build();
                }

                return new Entity<>(event);
            }

            Builder relationship(String uid, RelationshipItem from, RelationshipItem to) {
                Relationship relationship = Relationship.builder()
                        .relationship(uid)
                        .from(from)
                        .to(to)
                        .build();
                Entity<Relationship> entity = new Entity<>(relationship);
                this.relationships.add(entity);
                // set cursors
                current = entity;
                return this;
            }

            // TODO(DHIS2-14213) return something? Maybe I need a setup builder and on build I return the setup with the args and mocks and so on
            private Setup build() {
                TrackerPreheat preheat = mock( TrackerPreheat.class );
                TrackerBundle bundle = TrackerBundle.builder().preheat(preheat).build();
                EnumMap<TrackerType, Set<String>> invalidEntities = PersistablesFilterTest.invalidEntities();

                bundle.setTrackedEntities( toEntitiesInPayload(trackedEntities));
                bundle.setEnrollments( toEntitiesInPayload(enrollments));
                bundle.setEvents( toEntitiesInPayload(events));
                bundle.setRelationships( toEntitiesInPayload(relationships));

                invalidEntities.get(TRACKED_ENTITY).addAll(invalid(trackedEntities));
                invalidEntities.get(ENROLLMENT).addAll(invalid(enrollments));
                invalidEntities.get(EVENT).addAll(invalid(events));
                invalidEntities.get(RELATIONSHIP).addAll(invalid(relationships));

                inDB(preheat, trackedEntities);
                inDB(preheat, enrollments);
                inDB(preheat, events);
                inDB(preheat, relationships);

                return new Setup(bundle, invalidEntities);
            }

            private <T extends TrackerDto> List<T> toEntitiesInPayload(List<Entity<T>> entities) {
                return entities.stream()
                            .filter(e -> e.isInPayload)
                            .map(e -> e.entity)
                            .collect(Collectors.toList());
            }

            private <T extends TrackerDto> Set<String> invalid(List<Entity<T>> entities) {
                return entities.stream()
                        .filter(e -> !e.valid)
                        .map(e -> e.entity.getUid())
                        .collect(Collectors.toSet());
            }

            private <T extends TrackerDto> void inDB(TrackerPreheat preheat, List<Entity<T>> entities) {
                // TODO(DHIS2-14213) mocking of preheat.exists( dto) does not work if the dto is not of the same instance
                // makes sense but is annoying; so I have to write preheat.exists( parent.getTrackerType(), parent.getUid())
                // for now
                entities.stream()
                    .filter(e -> e.isInDB)
                    .map(e -> e.entity)
                    .forEach(e -> {
                        when( preheat.exists( e.getTrackerType(), e.getUid() ) ).thenReturn( true);
                        when( preheat.exists( e) ).thenReturn( true);
                    });
            }

            /**
             * Marks {@link #current} {@link TrackerDto} as invalid in {@link #invalid}.
             *
             * @return this setup
             */
            Builder isInvalid()
            {
                this.current.valid = false;
                return this;
            }

            /**
             * Marks {@link #current} {@link TrackerDto} as existing in
             * {@link #bundle}'s {@link TrackerPreheat}.
             *
             * @return this setup
             */
            Builder isExisting()
            {
                this.current.isInDB = true;
                return this;
            }

            /**
             * Exlude {@link #current} {@link TrackerDto} from the {@link #bundle}.
             *
             * @return this setup
             */
            Builder isNotInPayload()
            {
                this.current.isInPayload = false;
                return this;
            }
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

    private static void assertError(PersistablesFilter.Result result, TrackerErrorCode code, TrackerType type, String uid, String messageContains ) {
        assertHasError(result.getErrors(), code, type, uid, messageContains);
    }

    private static void assertNoErrorWithCode(PersistablesFilter.Result result, TrackerErrorCode code) {
        assertHasNoErrorWithCode(result.getErrors(), code);
    }
}