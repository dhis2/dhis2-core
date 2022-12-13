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
import static org.hamcrest.Matchers.empty;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4015;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4016;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4017;
import static org.hisp.dhis.tracker.validation.validators.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class RelationshipPreCheckExistenceValidationHookTest
{
    private final static String NOT_PRESENT_RELATIONSHIP_UID = "NotPresentRelationshipId";

    private final static String RELATIONSHIP_UID = "RelationshipId";

    private final static String SOFT_DELETED_RELATIONSHIP_UID = "SoftDeletedRelationshipId";

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private RelationshipPreCheckExistenceValidationHook validationHook = new RelationshipPreCheckExistenceValidationHook();

    private ValidationErrorReporter reporter;

    @BeforeEach
    void setUp()
    {
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void verifyRelationshipValidationSuccessWhenIsCreate()
    {
        Relationship rel = Relationship.builder()
            .relationship( NOT_PRESENT_RELATIONSHIP_UID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( rel ) ).thenReturn( TrackerImportStrategy.CREATE );

        validationHook.validate( reporter, bundle, rel );

        assertFalse( reporter.hasErrors() );
        assertThat( reporter.getWarnings(), empty() );
    }

    @Test
    void verifyRelationshipValidationSuccessWithWarningWhenUpdate()
    {
        Relationship rel = getPayloadRelationship();

        when( bundle.getStrategy( rel ) ).thenReturn( TrackerImportStrategy.UPDATE );
        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getRelationship( RELATIONSHIP_UID ) ).thenReturn( getRelationship() );

        validationHook.validate( reporter, bundle, rel );

        assertFalse( reporter.hasErrors() );
        assertTrue( reporter.hasWarningReport( r -> E4015.equals( r.getWarningCode() ) &&
            TrackerType.RELATIONSHIP.equals( r.getTrackerType() ) &&
            rel.getUid().equals( r.getUid() ) ) );
    }

    @Test
    void verifyRelationshipValidationFailsWhenIsCreateAndRelationshipIsAlreadyPresent()
    {
        Relationship rel = getPayloadRelationship();

        when( bundle.getStrategy( rel ) ).thenReturn( TrackerImportStrategy.CREATE );
        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getRelationship( RELATIONSHIP_UID ) ).thenReturn( getRelationship() );

        validationHook.validate( reporter, bundle, rel );

        hasTrackerError( reporter, E4015, RELATIONSHIP, rel.getUid() );
    }

    @Test
    void verifyRelationshipValidationFailsWhenIsDeleteAndRelationshipIsNotPresent()
    {
        Relationship rel = Relationship.builder()
            .relationship( NOT_PRESENT_RELATIONSHIP_UID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getStrategy( rel ) ).thenReturn( TrackerImportStrategy.DELETE );

        validationHook.validate( reporter, bundle, rel );

        hasTrackerError( reporter, E4016, RELATIONSHIP, rel.getUid() );
    }

    @Test
    void verifyRelationshipValidationFailsWhenIsSoftDeleted()
    {
        Relationship rel = Relationship.builder()
            .relationship( SOFT_DELETED_RELATIONSHIP_UID )
            .build();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( preheat.getRelationship( SOFT_DELETED_RELATIONSHIP_UID ) )
            .thenReturn( softDeletedRelationship() );
        validationHook.validate( reporter, bundle, rel );

        hasTrackerError( reporter, E4017, RELATIONSHIP, rel.getUid() );
    }

    private Relationship getPayloadRelationship()
    {
        return Relationship.builder()
            .relationship( RELATIONSHIP_UID )
            .build();
    }

    private org.hisp.dhis.relationship.Relationship softDeletedRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = new org.hisp.dhis.relationship.Relationship();
        relationship.setUid( SOFT_DELETED_RELATIONSHIP_UID );
        relationship.setDeleted( true );
        return relationship;
    }

    private org.hisp.dhis.relationship.Relationship getRelationship()
    {
        org.hisp.dhis.relationship.Relationship relationship = new org.hisp.dhis.relationship.Relationship();
        relationship.setUid( RELATIONSHIP_UID );
        return relationship;
    }
}