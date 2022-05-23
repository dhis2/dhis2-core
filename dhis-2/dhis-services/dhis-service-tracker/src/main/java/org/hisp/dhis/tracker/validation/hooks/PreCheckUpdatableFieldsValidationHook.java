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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1126;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1127;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1128;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
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
@RequiredArgsConstructor
public class PreCheckUpdatableFieldsValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter,
        TrackerBundle bundle, TrackedEntity trackedEntity )
    {
        TrackedEntityInstance trackedEntityInstance = reporter.getBundle()
            .getTrackedEntityInstance( trackedEntity.getTrackedEntity() );

        reporter.addErrorIf(
            () -> !trackedEntity.getTrackedEntityType().isEqualTo( trackedEntityInstance.getTrackedEntityType() ),
            trackedEntity, E1126, "trackedEntityType" );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        ProgramInstance pi = reporter.getBundle().getProgramInstance( enrollment.getEnrollment() );
        Program program = pi.getProgram();
        TrackedEntityInstance trackedEntityInstance = pi.getEntityInstance();

        reporter.addErrorIf( () -> !enrollment.getProgram().isEqualTo( program ), enrollment, E1127,
            "program" );
        reporter.addErrorIf( () -> !trackedEntityInstance.getUid().equals( enrollment.getTrackedEntity() ), enrollment,
            E1127, "trackedEntity" );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        ProgramStageInstance programStageInstance = reporter.getBundle().getProgramStageInstance( event.getEvent() );
        ProgramStage programStage = programStageInstance.getProgramStage();
        ProgramInstance programInstance = programStageInstance.getProgramInstance();

        reporter.addErrorIf( () -> !event.getProgramStage().isEqualTo( programStage ), event, E1128,
            "programStage" );
        reporter.addErrorIf(
            () -> event.getEnrollment() != null && !event.getEnrollment().equals( programInstance.getUid() ),
            event, E1128, "enrollment" );
    }

    @Override
    public boolean needsToRun( TrackerImportStrategy strategy )
    {
        return strategy == TrackerImportStrategy.UPDATE;
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        // Nothing to do
    }

    @Override
    public boolean removeOnError()
    {
        return false;
    }

}
