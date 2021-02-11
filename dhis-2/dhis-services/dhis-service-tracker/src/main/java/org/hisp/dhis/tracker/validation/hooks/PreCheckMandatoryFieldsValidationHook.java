/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.*;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

/**
 * @author Enrico Colasante
 */
@Component
public class PreCheckMandatoryFieldsValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private static final String ORG_UNIT = "orgUnit";

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity trackedEntity )
    {
        addErrorIf( () -> StringUtils.isEmpty( trackedEntity.getTrackedEntityType() ), reporter, E1121,
            "trackedEntityType" );
        addErrorIf( () -> StringUtils.isEmpty( trackedEntity.getOrgUnit() ), reporter, E1121, ORG_UNIT );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        addErrorIf( () -> StringUtils.isEmpty( enrollment.getOrgUnit() ), reporter, E1122, ORG_UNIT );
        addErrorIf( () -> StringUtils.isEmpty( enrollment.getProgram() ), reporter, E1122, "program" );
        addErrorIf( () -> StringUtils.isEmpty( enrollment.getTrackedEntity() ), reporter, E1122, "trackedEntity" );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        addErrorIf( () -> StringUtils.isEmpty( event.getOrgUnit() ), reporter, E1123, ORG_UNIT );
        addErrorIf( () -> StringUtils.isEmpty( event.getProgramStage() ), reporter, E1123, "programStage" );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        addErrorIfNull( relationship.getFrom(), reporter, E1124, "from" );
        addErrorIfNull( relationship.getTo(), reporter, E1124, "to" );
        addErrorIf( () -> StringUtils.isEmpty( relationship.getRelationshipType() ), reporter, E1124,
            "relationshipType" );
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }
}
