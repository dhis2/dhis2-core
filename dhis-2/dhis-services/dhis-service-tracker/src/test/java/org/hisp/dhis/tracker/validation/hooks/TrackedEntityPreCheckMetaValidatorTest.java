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

import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1005;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1049;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
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
class TrackedEntityPreCheckMetaValidatorTest
{
    private static final String ORG_UNIT_UID = "OrgUnitUid";

    private static final String TRACKED_ENTITY_TYPE_UID = "TrackedEntityTypeUid";

    private static final String TRACKED_ENTITY_UID = "TrackedEntityUid";

    private TrackedEntityPreCheckMetaValidator validator;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private TrackerBundle bundle;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        validator = new TrackedEntityPreCheckMetaValidator();

        when( bundle.getPreheat() ).thenReturn( preheat );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void verifyTrackedEntityValidationSuccess()
    {
        TrackedEntity tei = validTei();
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) ) )
            .thenReturn( new OrganisationUnit() );
        when( preheat.getTrackedEntityType( MetadataIdentifier.ofUid( TRACKED_ENTITY_TYPE_UID ) ) )
            .thenReturn( new TrackedEntityType() );

        validator.validate( reporter, bundle, tei );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenOrgUnitIsNotPresentInDb()
    {
        TrackedEntity tei = validTei();
        when( preheat.getTrackedEntityType( MetadataIdentifier.ofUid( TRACKED_ENTITY_TYPE_UID ) ) )
            .thenReturn( new TrackedEntityType() );

        validator.validate( reporter, bundle, tei );

        hasTrackerError( reporter, E1049, TRACKED_ENTITY, tei.getUid() );
    }

    @Test
    void verifyTrackedEntityValidationFailsWhenTrackedEntityTypeIsNotPresentInDb()
    {
        TrackedEntity tei = validTei();
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) ) )
            .thenReturn( new OrganisationUnit() );

        validator.validate( reporter, bundle, tei );

        hasTrackerError( reporter, E1005, TRACKED_ENTITY, tei.getUid() );
    }

    private TrackedEntity validTei()
    {
        return TrackedEntity.builder()
            .trackedEntity( TRACKED_ENTITY_UID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) )
            .trackedEntityType( MetadataIdentifier.ofUid( TRACKED_ENTITY_TYPE_UID ) )
            .build();
    }
}