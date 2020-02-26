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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityRequiredValuesValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 0;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( bundle.getImportStrategy().isDelete() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        for ( TrackedEntity trackedEntity : bundle.getTrackedEntities() )
        {
            reporter.increment( trackedEntity );

            validateTrackedEntityType( reporter, bundle, trackedEntity );
            validateOrganisationUnit( reporter, bundle, trackedEntity );
        }

        return reporter.getReportList();
    }

    protected void validateTrackedEntityType( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        Objects.requireNonNull( trackedEntity, Constants.TRACKED_ENTITY_CAN_T_BE_NULL );

        if ( bundle.getImportStrategy().isCreate() )
        {
            if ( trackedEntity.getTrackedEntityType() == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1004 ) );
                return;
            }

            TrackedEntityType entityType = PreheatHelper
                .getTrackedEntityType( bundle, trackedEntity.getTrackedEntityType() );
            if ( entityType == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1005 )
                    .addArg( trackedEntity.getTrackedEntityType() ) );
            }
        }
    }

    protected void validateOrganisationUnit( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        Objects.requireNonNull( trackedEntity, Constants.TRACKED_ENTITY_CAN_T_BE_NULL );

        if ( bundle.getImportStrategy().isCreate() )
        {
            if ( StringUtils.isEmpty( trackedEntity.getOrgUnit() ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1010 ) );
                return;
            }

            OrganisationUnit organisationUnit = PreheatHelper
                .getOrganisationUnit( bundle, trackedEntity.getOrgUnit() );

            if ( organisationUnit == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1011 )
                    .addArg( trackedEntity.getOrgUnit() ) );
            }
        }
        else if ( bundle.getImportStrategy().isUpdate() )
        {
            TrackedEntityInstance tei = PreheatHelper
                .getTrackedEntityInstance( bundle, trackedEntity.getTrackedEntity() );
            if ( tei.getOrganisationUnit() == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1011 )
                    .addArg( tei ) );
            }
        }
    }
}
