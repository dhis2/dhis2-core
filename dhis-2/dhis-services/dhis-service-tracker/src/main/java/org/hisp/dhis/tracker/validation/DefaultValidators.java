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
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.validation.hooks.AssignedUserValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentAttributeValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentDateValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentGeoValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentInExistingValidator;
import org.hisp.dhis.tracker.validation.hooks.EnrollmentNoteValidator;
import org.hisp.dhis.tracker.validation.hooks.EventCategoryOptValidator;
import org.hisp.dhis.tracker.validation.hooks.EventDataValuesValidator;
import org.hisp.dhis.tracker.validation.hooks.EventDateValidator;
import org.hisp.dhis.tracker.validation.hooks.EventGeoValidator;
import org.hisp.dhis.tracker.validation.hooks.EventNoteValidator;
import org.hisp.dhis.tracker.validation.hooks.RelationshipsValidator;
import org.hisp.dhis.tracker.validation.hooks.TrackedEntityAttributeValidator;
import org.springframework.stereotype.Component;

/**
 * {@link Validators} used in
 * {@link TrackerValidationService#validate(TrackerBundle)}.
 */
@RequiredArgsConstructor
@Component( "org.hisp.dhis.tracker.validation.DefaultValidators" )
public class DefaultValidators implements Validators
{

    private final TrackedEntityAttributeValidator attributeValidator;

    private final EnrollmentNoteValidator enrollmentNoteValidator;

    private final EnrollmentInExistingValidator enrollmentInExistingValidator;

    private final EnrollmentGeoValidator enrollmentGeoValidator;

    private final EnrollmentDateValidator enrollmentDateValidator;

    private final EnrollmentAttributeValidator enrollmentAttributeValidator;

    private final EventCategoryOptValidator eventCategoryOptValidator;

    private final EventDateValidator eventDateValidator;

    private final EventGeoValidator eventGeoValidator;

    private final EventNoteValidator eventNoteValidator;

    private final EventDataValuesValidator eventDataValuesValidator;

    private final AssignedUserValidator assignedUserValidator;

    private final RelationshipsValidator relationshipsValidator;

    @Override
    public List<Validator<TrackedEntity>> getTrackedEntityValidators()
    {
        return List.of(
            attributeValidator );
    }

    @Override
    public List<Validator<Enrollment>> getEnrollmentValidators()
    {
        return List.of(
            enrollmentNoteValidator,
            enrollmentInExistingValidator,
            enrollmentGeoValidator,
            enrollmentDateValidator,
            enrollmentAttributeValidator );
    }

    @Override
    public List<Validator<Event>> getEventValidators()
    {
        return List.of(
            eventCategoryOptValidator,
            eventDateValidator,
            eventGeoValidator,
            eventNoteValidator,
            eventDataValuesValidator,
            assignedUserValidator );
    }

    @Override
    public List<Validator<Relationship>> getRelationshipValidators()
    {
        return List.of(
            relationshipsValidator );
    }
}
