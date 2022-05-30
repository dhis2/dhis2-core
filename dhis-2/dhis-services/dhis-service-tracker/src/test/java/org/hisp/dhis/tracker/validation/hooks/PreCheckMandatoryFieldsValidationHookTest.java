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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1008;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class PreCheckMandatoryFieldsValidationHookTest
{

    private PreCheckMandatoryFieldsValidationHook validationHook;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        validationHook = new PreCheckMandatoryFieldsValidationHook();

        when( bundle.getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( bundle.getPreheat() ).thenReturn( preheat );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void verifyTrackedEntityValidationSuccess()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( CodeGenerator.generateUid() )
            .trackedEntityType( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .build();

        validationHook.validateTrackedEntity( reporter, bundle, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationFailsOnMissingOrgUnit()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( CodeGenerator.generateUid() )
            .trackedEntityType( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .orgUnit( MetadataIdentifier.EMPTY_UID )
            .build();

        validationHook.validateTrackedEntity( reporter, bundle, trackedEntity );

        assertMissingPropertyForTrackedEntity( reporter, trackedEntity.getUid(), "orgUnit" );
    }

    @Test
    void verifyTrackedEntityValidationFailsOnMissingTrackedEntityType()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( CodeGenerator.generateUid() )
            .trackedEntityType( MetadataIdentifier.EMPTY_UID )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .build();

        validationHook.validateTrackedEntity( reporter, bundle, trackedEntity );

        assertMissingPropertyForTrackedEntity( reporter, trackedEntity.getUid(), "trackedEntityType" );
    }

    @Test
    void verifyEnrollmentValidationSuccess()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .trackedEntity( CodeGenerator.generateUid() )
            .build();

        validationHook.validateEnrollment( reporter, bundle, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationFailsOnMissingTrackedEntity()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .trackedEntity( null )
            .build();

        validationHook.validateEnrollment( reporter, bundle, enrollment );

        assertMissingPropertyForEnrollment( reporter, enrollment.getUid(), "trackedEntity" );
    }

    @Test
    void verifyEnrollmentValidationFailsOnMissingProgram()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .program( MetadataIdentifier.EMPTY_UID )
            .trackedEntity( CodeGenerator.generateUid() )
            .build();

        validationHook.validateEnrollment( reporter, bundle, enrollment );

        assertMissingPropertyForEnrollment( reporter, enrollment.getUid(), "program" );
    }

    @Test
    void verifyEnrollmentValidationFailsOnMissingOrgUnit()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.EMPTY_UID )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .trackedEntity( CodeGenerator.generateUid() )
            .build();

        validationHook.validateEnrollment( reporter, bundle, enrollment );

        assertMissingPropertyForEnrollment( reporter, enrollment.getUid(), "orgUnit" );
    }

    @Test
    void verifyEventValidationSuccess()
    {
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .programStage( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .build();

        validationHook.validateEvent( reporter, bundle, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEventValidationFailsOnMissingProgram()
    {
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .programStage( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .program( MetadataIdentifier.EMPTY_UID )
            .build();

        validationHook.validateEvent( reporter, bundle, event );

        assertMissingPropertyForEvent( reporter, event.getUid(), "program" );
    }

    @Test
    void verifyEventValidationFailsOnMissingProgramStageReferenceToProgram()
    {
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .programStage( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .build();
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( event.getProgramStage().getIdentifier() );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStage ) ) )
            .thenReturn( programStage );

        validationHook.validateEvent( reporter, bundle, event );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        hasTrackerError( reporter, E1008, EVENT, event.getUid() );
    }

    @Test
    void verifyEventValidationFailsOnMissingProgramStage()
    {
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .programStage( MetadataIdentifier.EMPTY_UID )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .build();

        validationHook.validateEvent( reporter, bundle, event );

        assertMissingPropertyForEvent( reporter, event.getUid(), "programStage" );
    }

    @Test
    void verifyEventValidationFailsOnMissingOrgUnit()
    {
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .orgUnit( MetadataIdentifier.EMPTY_UID )
            .programStage( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .build();

        validationHook.validateEvent( reporter, bundle, event );

        assertMissingPropertyForEvent( reporter, event.getUid(), "orgUnit" );
    }

    @Test
    void verifyRelationshipValidationSuccess()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .from( RelationshipItem.builder()
                .trackedEntity( trackedEntity() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( trackedEntity() )
                .build() )
            .build();

        validationHook.validateRelationship( reporter, bundle, relationship );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyRelationshipValidationFailsOnMissingFrom()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .to( RelationshipItem.builder()
                .trackedEntity( trackedEntity() )
                .build() )
            .build();

        validationHook.validateRelationship( reporter, bundle, relationship );

        assertMissingPropertyForRelationship( reporter, relationship.getUid(), "from" );
    }

    @Test
    void verifyRelationshipValidationFailsOnMissingTo()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .from( RelationshipItem.builder()
                .trackedEntity( trackedEntity() )
                .build() )
            .build();

        validationHook.validateRelationship( reporter, bundle, relationship );

        assertMissingPropertyForRelationship( reporter, relationship.getUid(), "to" );
    }

    @Test
    void verifyRelationshipValidationFailsOnMissingRelationshipType()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.EMPTY_UID )
            .from( RelationshipItem.builder()
                .trackedEntity( trackedEntity() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( trackedEntity() )
                .build() )
            .build();

        validationHook.validateRelationship( reporter, bundle, relationship );

        assertMissingPropertyForRelationship( reporter, relationship.getUid(), "relationshipType" );
    }

    private void assertMissingPropertyForTrackedEntity( ValidationErrorReporter reporter, String uid, String property )
    {
        assertMissingProperty( reporter, TRACKED_ENTITY, "tracked entity", uid, property, TrackerErrorCode.E1121 );
    }

    private void assertMissingPropertyForEnrollment( ValidationErrorReporter reporter, String uid, String property )
    {
        assertMissingProperty( reporter, ENROLLMENT, "enrollment", uid, property, TrackerErrorCode.E1122 );
    }

    private void assertMissingPropertyForEvent( ValidationErrorReporter reporter, String uid, String property )
    {
        assertMissingProperty( reporter, EVENT, "event", uid, property, TrackerErrorCode.E1123 );
    }

    private void assertMissingPropertyForRelationship( ValidationErrorReporter reporter, String uid, String property )
    {
        assertMissingProperty( reporter, RELATIONSHIP, "relationship", uid, property, TrackerErrorCode.E1124 );
    }

    private void assertMissingProperty( ValidationErrorReporter reporter, TrackerType type, String entity, String uid,
        String property,
        TrackerErrorCode errorCode )
    {
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        hasTrackerError( reporter, errorCode, type, uid );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Missing required " + entity + " property: `" + property + "`." ) );
    }

    private String trackedEntity()
    {
        return CodeGenerator.generateUid();
    }
}