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
 */

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.TrackerImportAccessManager;
import org.springframework.stereotype.Component;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ENROLLMENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.EVENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.USER_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckSecurityValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private final TrackerImportAccessManager accessManager;

    public PreCheckSecurityValidationHook( TrackedEntityAttributeService teAttrService,
        TrackerImportAccessManager accessManager )
    {
        super( teAttrService );

        checkNotNull( accessManager );

        this.accessManager = accessManager;
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter,
        TrackedEntity trackedEntity )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( trackedEntity );

        checkNotNull( context.getBundle().getUser(), USER_CANT_BE_NULL );
        checkNotNull( trackedEntity, TRACKED_ENTITY_CANT_BE_NULL );
        checkNotNull( trackedEntity.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        if ( strategy.isUpdateOrDelete() )
        {
            TrackedEntityInstance trackedEntityInstance = context
                .getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
            accessManager.checkOrgUnitInCaptureScope( reporter, trackedEntityInstance.getOrganisationUnit() );
        }

        accessManager.checkOrgUnitInCaptureScope( reporter, context.getOrganisationUnit( trackedEntity.getOrgUnit() ) );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();
        TrackerImportStrategy strategy = validationContext.getStrategy( enrollment );

        checkNotNull( validationContext.getBundle().getUser(), USER_CANT_BE_NULL );
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );
        checkNotNull( enrollment.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        if ( strategy.isUpdateOrDelete() )
        {
            ProgramInstance pi = validationContext.getProgramInstance( enrollment.getEnrollment() );
            accessManager.checkOrgUnitInCaptureScope( reporter, pi.getOrganisationUnit() );
        }

        accessManager
            .checkOrgUnitInCaptureScope( reporter, validationContext.getOrganisationUnit( enrollment.getOrgUnit() ) );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();
        TrackerImportStrategy strategy = validationContext.getStrategy( event );

        checkNotNull( validationContext.getBundle().getUser(), USER_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );
        checkNotNull( event.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        if ( strategy.isUpdateOrDelete() )
        {
            ProgramStageInstance psi = validationContext.getProgramStageInstance( event.getEvent() );
            accessManager.checkOrgUnitInCaptureScope( reporter, psi.getOrganisationUnit() );
        }

        accessManager
            .checkOrgUnitInCaptureScope( reporter, validationContext.getOrganisationUnit( event.getOrgUnit() ) );
    }
}
