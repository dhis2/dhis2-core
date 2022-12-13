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
package org.hisp.dhis.tracker.validation.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.validation.validators.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
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
class RelationshipPreCheckMandatoryFieldsValidationHookTest
{

    private RelationshipPreCheckMandatoryFieldsValidationHook validationHook;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        validationHook = new RelationshipPreCheckMandatoryFieldsValidationHook();

        when( bundle.getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( bundle.getPreheat() ).thenReturn( preheat );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
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

        validationHook.validate( reporter, bundle, relationship );

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

        validationHook.validate( reporter, bundle, relationship );

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

        validationHook.validate( reporter, bundle, relationship );

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

        validationHook.validate( reporter, bundle, relationship );

        assertMissingPropertyForRelationship( reporter, relationship.getUid(), "relationshipType" );
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
        assertThat( reporter.getErrors(), hasSize( 1 ) );
        hasTrackerError( reporter, errorCode, type, uid );
        assertThat( reporter.getErrors().get( 0 ).getErrorMessage(),
            is( "Missing required " + entity + " property: `" + property + "`." ) );
    }

    private String trackedEntity()
    {
        return CodeGenerator.generateUid();
    }
}