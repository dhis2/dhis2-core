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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1008;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1121;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1122;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1123;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1124;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.ProgramStage;
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
        reporter.addErrorIf( () -> trackedEntity.getTrackedEntityType().isBlank(), trackedEntity, E1121,
            "trackedEntityType" );
        reporter.addErrorIf( () -> trackedEntity.getOrgUnit().isBlank(), trackedEntity, E1121, ORG_UNIT );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        reporter.addErrorIf( () -> enrollment.getOrgUnit().isBlank(), enrollment, E1122, ORG_UNIT );
        reporter.addErrorIf( () -> enrollment.getProgram().isBlank(), enrollment, E1122, "program" );
        reporter.addErrorIf( () -> StringUtils.isEmpty( enrollment.getTrackedEntity() ), enrollment, E1122,
            "trackedEntity" );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        reporter.addErrorIf( () -> event.getOrgUnit().isBlank(), event, E1123, ORG_UNIT );
        reporter.addErrorIf( () -> event.getProgramStage().isBlank(), event, E1123, "programStage" );

        // TODO remove if once metadata import is fixed
        ProgramStage programStage = reporter.getBundle().getPreheat().getProgramStage( event.getProgramStage() );
        if ( programStage != null )
        {
            // Program stages should always have a program! Due to how metadata
            // import is currently implemented
            // it's possible that users run into the edge case that a program
            // stage does not have an associated
            // program. Tell the user it's an issue with the metadata and not
            // the event itself. This should be
            // fixed in the metadata import. For more see
            // https://jira.dhis2.org/browse/DHIS2-12123
            reporter.addErrorIfNull( programStage.getProgram(), event, E1008, event.getProgramStage() );
            // return since program is not a required field according to our API
            // and the issue is with the missing reference in
            // the DB entry of the program stage and not the payload itself
            return;
        }

        reporter.addErrorIf( event.getProgram()::isBlank, event, E1123, "program" );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        reporter.addErrorIfNull( relationship.getFrom(), relationship, E1124, "from" );
        reporter.addErrorIfNull( relationship.getTo(), relationship, E1124, "to" );
        reporter.addErrorIf( () -> relationship.getRelationshipType().isBlank(), relationship, E1124,
            "relationshipType" );
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }
}
