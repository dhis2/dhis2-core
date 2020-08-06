package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
 *
 */

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckExistenceValidationHook
    extends AbstractTrackerDtoValidationHook
{

    public PreCheckExistenceValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( teAttrService );
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter,
        TrackedEntity trackedEntity )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerBundle bundle = context.getBundle();
        TrackerImportStrategy importStrategy = bundle.getImportStrategy();

        TrackedEntityInstance existingTe = context
            .getTrackedEntityInstance( trackedEntity.getTrackedEntity() );

        if ( importStrategy.isCreateAndUpdate() )
        {
            if ( existingTe == null )
            {
                context.setStrategy( trackedEntity, TrackerImportStrategy.CREATE );
            }
            else
            {
                context.setStrategy( trackedEntity, TrackerImportStrategy.UPDATE );
            }
        }
        else if ( existingTe != null && importStrategy.isCreate() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1002 )
                .addArg( trackedEntity.getTrackedEntity() ) );
        }
        else if ( existingTe != null && existingTe.isDeleted() && importStrategy.isDelete() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1114 )
                .addArg( trackedEntity.getTrackedEntity() ) );
        }
        else if ( existingTe == null && importStrategy.isUpdateOrDelete() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1063 )
                .addArg( trackedEntity.getTrackedEntity() ) );
        }
        else
        {
            context.setStrategy( trackedEntity, importStrategy );
        }
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerBundle bundle = context.getBundle();
        TrackerImportStrategy importStrategy = bundle.getImportStrategy();

        ProgramInstance existingPi = context.getProgramInstance( enrollment.getEnrollment() );

        if ( importStrategy.isCreateAndUpdate() )
        {
            if ( existingPi == null )
            {
                context.setStrategy( enrollment, TrackerImportStrategy.CREATE );
            }
            else
            {
                context.setStrategy( enrollment, TrackerImportStrategy.UPDATE );
            }
        }
        else if ( existingPi != null && importStrategy.isCreate() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1080 )
                .addArg( enrollment.getEnrollment() ) );
        }
        else if ( existingPi != null && existingPi.isDeleted() && importStrategy.isDelete() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1113 )
                .addArg( enrollment.getEnrollment() ) );
        }
        else if ( existingPi == null && importStrategy.isUpdateOrDelete() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1081 )
                .addArg( enrollment.getEnrollment() ) );
        }
        else
        {
            context.setStrategy( enrollment, importStrategy );
        }
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerBundle bundle = context.getBundle();
        TrackerImportStrategy importStrategy = bundle.getImportStrategy();

        ProgramStageInstance existingPsi = context.getProgramStageInstance( event.getEvent() );

        if ( importStrategy.isCreateAndUpdate() )
        {
            if ( existingPsi == null )
            {
                context.setStrategy( event, TrackerImportStrategy.CREATE );
            }
            else
            {
                context.setStrategy( event, TrackerImportStrategy.UPDATE );
            }
        }
        else if ( existingPsi != null && importStrategy.isCreate() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1030 )
                .addArg( event.getEvent() ) );
        }
        else if ( existingPsi != null && existingPsi.isDeleted() && importStrategy.isDelete() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1082 )
                .addArg( event.getEvent() ) );
        }
        else if ( existingPsi == null && importStrategy.isUpdateOrDelete() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1032 )
                .addArg( event.getEvent() ) );
        }
        else
        {
            context.setStrategy( event, importStrategy );
        }
    }
}
