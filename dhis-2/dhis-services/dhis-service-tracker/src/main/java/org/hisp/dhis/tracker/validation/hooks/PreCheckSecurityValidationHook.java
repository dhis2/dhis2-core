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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.TrackerImportAccessManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
    @Autowired
    private TrackerImportAccessManager accessManager;

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter,
        TrackedEntity trackedEntity )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( trackedEntity );

        Objects.requireNonNull( context.getBundle().getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( trackedEntity, TRACKED_ENTITY_CANT_BE_NULL );
        Objects.requireNonNull( trackedEntity.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        if ( strategy.isUpdateOrDelete() )
        {
            TrackedEntityInstance trackedEntityInstance = context
                .getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
            accessManager.checkOrgUnitInCaptureScope( reporter, trackedEntityInstance.getOrganisationUnit() );
        }

        // TODO: Added comment to make sure the reason for this not so intuitive reason,
        // This should be better commented and documented somewhere
        // Ameen 10.09.2019, 12:32 fix: relax restriction on writing to trackedEntity in search scope 48a82e5f
        // Why should we use search?
        OrganisationUnit incomingOrgUnit = context.getOrganisationUnit( trackedEntity.getOrgUnit() );
        accessManager.checkOrgUnitInCaptureScope( reporter, incomingOrgUnit );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();
        TrackerImportStrategy strategy = validationContext.getStrategy( enrollment );

        Objects.requireNonNull( validationContext.getBundle().getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( enrollment, ENROLLMENT_CANT_BE_NULL );
        Objects.requireNonNull( enrollment.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        if ( strategy.isUpdateOrDelete() )
        {
            ProgramInstance pi = validationContext.getProgramInstance( enrollment.getEnrollment() );
            accessManager.checkOrgUnitInCaptureScope( reporter, pi.getOrganisationUnit() );
        }

        OrganisationUnit incomingOrgUnit = validationContext.getOrganisationUnit( enrollment.getOrgUnit() );
        accessManager.checkOrgUnitInCaptureScope( reporter, incomingOrgUnit );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();
        TrackerImportStrategy strategy = validationContext.getStrategy( event );

        Objects.requireNonNull( validationContext.getBundle().getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( event, EVENT_CANT_BE_NULL );
        Objects.requireNonNull( event.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        if ( strategy.isUpdateOrDelete() )
        {
            ProgramStageInstance psi = validationContext.getProgramStageInstance( event.getEvent() );
            accessManager.checkOrgUnitInCaptureScope( reporter, psi.getOrganisationUnit() );
        }

        // TODO: this check is also done in DefaultTrackerImportAccessManager,
        //  one case is laxer and this check will possibly overrule in that case when programStageInstance.isCreatableInSearchScope == TRUE
        //  Investigate....
        OrganisationUnit incomingOrgUnit = validationContext.getOrganisationUnit( event.getOrgUnit() );
        accessManager.checkOrgUnitInCaptureScope( reporter, incomingOrgUnit );
    }
}
