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

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentAttributeValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentDateValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentGeoValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentInExistingValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentNoteValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentPreCheckDataRelationsValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentPreCheckExistenceValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentPreCheckMandatoryFieldsValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentPreCheckMetaValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentPreCheckSecurityOwnershipValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentPreCheckUidValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentPreCheckUpdatableFieldsValidator;
import org.hisp.dhis.tracker.validation.hooks.EventValidator;
import org.hisp.dhis.tracker.validation.hooks.RelationshipPreCheckDataRelationsValidator;
import org.hisp.dhis.tracker.validation.hooks.RelationshipPreCheckExistenceValidator;
import org.hisp.dhis.tracker.validation.hooks.RelationshipPreCheckMandatoryFieldsValidator;
import org.hisp.dhis.tracker.validation.hooks.RelationshipPreCheckMetaValidator;
import org.hisp.dhis.tracker.validation.hooks.RelationshipPreCheckUidValidator;
import org.hisp.dhis.tracker.validation.hooks.RelationshipsValidator;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityAttributeValidator;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityPreCheckExistenceValidator;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityPreCheckMandatoryFieldsValidator;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityPreCheckMetaValidator;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityPreCheckSecurityOwnershipValidator;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityPreCheckUidValidator;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityPreCheckUpdatableFieldsValidator;
import org.springframework.stereotype.Component;

/**
 * {@link Validators} used in {@link ValidationService#validate(TrackerBundle)}.
 */
@RequiredArgsConstructor
@Component( "org.hisp.dhis.tracker.validation.DefaultValidators" )
public class DefaultValidators implements Validators
{

    private final TrackedEntityPreCheckUidValidator trackedEntityPreCheckUidValidator;

    private final TrackedEntityPreCheckExistenceValidator trackedEntityPreCheckExistenceValidator;

    private final TrackedEntityPreCheckMandatoryFieldsValidator trackedEntityPreCheckMandatoryFieldsValidator;

    private final TrackedEntityPreCheckMetaValidator trackedEntityPreCheckMetaValidator;

    private final TrackedEntityPreCheckUpdatableFieldsValidator trackedEntityPreCheckUpdatableFieldsValidator;

    private final TrackedEntityPreCheckSecurityOwnershipValidator trackedEntityPreCheckSecurityOwnershipValidator;

    private final TrackedEntityAttributeValidator attributeValidator;

    private final EventValidator eventValidator;

    private final EnrollmentPreCheckUidValidator enrollmentPreCheckUidValidator;

    private final EnrollmentPreCheckExistenceValidator enrollmentPreCheckExistenceValidator;

    private final EnrollmentPreCheckMandatoryFieldsValidator enrollmentPreCheckMandatoryFieldsValidator;

    private final EnrollmentPreCheckMetaValidator enrollmentPreCheckMetaValidator;

    private final EnrollmentPreCheckUpdatableFieldsValidator enrollmentPreCheckUpdatableFieldsValidator;

    private final EnrollmentPreCheckDataRelationsValidator enrollmentPreCheckDataRelationsValidator;

    private final EnrollmentPreCheckSecurityOwnershipValidator enrollmentPreCheckSecurityOwnershipValidator;

    private final EnrollmentNoteValidator enrollmentNoteValidator;

    private final EnrollmentInExistingValidator enrollmentInExistingValidator;

    private final EnrollmentGeoValidator enrollmentGeoValidator;

    private final EnrollmentDateValidator enrollmentDateValidator;

    private final EnrollmentAttributeValidator enrollmentAttributeValidator;

    private final RelationshipPreCheckUidValidator relationshipPreCheckUidValidator;

    private final RelationshipPreCheckExistenceValidator relationshipPreCheckExistenceValidator;

    private final RelationshipPreCheckMandatoryFieldsValidator relationshipPreCheckMandatoryFieldsValidator;

    private final RelationshipPreCheckMetaValidator relationshipPreCheckMetaValidator;

    private final RelationshipPreCheckDataRelationsValidator relationshipPreCheckDataRelationsValidator;

    private final RelationshipsValidator relationshipsValidator;

    @Override
    public List<Validator<TrackedEntity>> getTrackedEntityValidators()
    {
        return List.of(
            trackedEntityPreCheckUidValidator,
            trackedEntityPreCheckExistenceValidator,
            trackedEntityPreCheckMandatoryFieldsValidator,
            trackedEntityPreCheckMetaValidator,
            trackedEntityPreCheckUpdatableFieldsValidator,
            trackedEntityPreCheckSecurityOwnershipValidator,
            attributeValidator );
    }

    @Override
    public List<Validator<Enrollment>> getEnrollmentValidators()
    {
        return List.of(
            enrollmentPreCheckUidValidator,
            enrollmentPreCheckExistenceValidator,
            enrollmentPreCheckMandatoryFieldsValidator,
            enrollmentPreCheckMetaValidator,
            enrollmentPreCheckUpdatableFieldsValidator,
            enrollmentPreCheckDataRelationsValidator,
            enrollmentPreCheckSecurityOwnershipValidator,
            enrollmentNoteValidator,
            enrollmentInExistingValidator,
            enrollmentGeoValidator,
            enrollmentDateValidator,
            enrollmentAttributeValidator );
    }

    @Override
    public Validator<TrackerBundle> getEventValidator()
    {
        return eventValidator;
    }

    @Override
    public List<Validator<Relationship>> getRelationshipValidators()
    {
        return List.of(
            relationshipPreCheckUidValidator,
            relationshipPreCheckExistenceValidator,
            relationshipPreCheckMandatoryFieldsValidator,
            relationshipPreCheckMetaValidator,
            relationshipPreCheckDataRelationsValidator,
            relationshipsValidator );
    }
}
