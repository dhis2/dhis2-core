package org.hisp.dhis.tracker.validation.hooks;

import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerErrorCode;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.validation.ValidationHookErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.List;

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

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class AddUpdateTrackedEntityValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        boolean isCreate = bundle.getImportStrategy().isCreate();
        boolean isUpdate = bundle.getImportStrategy().isUpdate();
        if ( !isCreate && !isUpdate )
        {
            return null;
        }

        ValidationHookErrorReporter errorReporter = new ValidationHookErrorReporter( bundle,
            AddUpdateTrackedEntityValidationHook.class );

        TrackerPreheat preheat = bundle.getPreheat();
        User user = preheat.getUser();

        List<TrackedEntity> trackedEntities = bundle.getTrackedEntities();
        for ( TrackedEntity te : trackedEntities )
        {
            // Tracked entity existence checks... This is a very expensive check, maybe move out to preheat?
            boolean teExists = trackedEntityInstanceStore.existsIncludingDeleted( te.getTrackedEntity() );
            if ( isCreate && teExists )
            {
                errorReporter.raiseError( TrackerErrorCode.E1002, te.getTrackedEntity() );
                continue;
            }
            else if ( isUpdate && !teExists )
            {
                errorReporter.raiseError( TrackerErrorCode.E1063, te.getTrackedEntity() );
            }

            // Tracked entity type checks...
            TrackedEntityInstance tei = null;
            TrackedEntityType trackedEntityType = null;
            if ( isCreate )
            {
                if ( te.getTrackedEntityType() == null )
                {
                    errorReporter.raiseError( TrackerErrorCode.E1004, te.getTrackedEntity() );
                    continue;
                }

                trackedEntityType = preheat
                    .get( TrackerIdentifier.UID, TrackedEntityType.class, te.getTrackedEntityType() );
                if ( trackedEntityType == null )
                {
                    errorReporter.raiseError( TrackerErrorCode.E1005, te.getTrackedEntityType() );
                    continue;
                }
            }
            else if ( isUpdate )
            {
                tei = preheat.getTrackedEntity( TrackerIdentifier.UID, te.getTrackedEntity() );
                if ( tei == null )
                {
                    errorReporter.raiseError( TrackerErrorCode.E1060, te.getTrackedEntity() );
                    continue;
                }

                trackedEntityType = tei.getTrackedEntityType();
            }

            // Access check 1...
            if ( !userHasWriteAccess( errorReporter, user, trackedEntityType ) )
            {
                continue;
            }

            // Org unit checks...
            OrganisationUnit ou = null;
            if ( isCreate )
            {
                ou = getOrganisationUnit( errorReporter, preheat, te );
                if ( ou == null )
                {
                    continue;
                }
            }
            else if ( isUpdate )
            {
                ou = tei.getOrganisationUnit();
                if ( ou == null )
                {
                    errorReporter.raiseError( TrackerErrorCode.E1011, te.getOrgUnit() );
                    continue;
                }
            }

            // Access check 2...
            if ( !isInUserSearchHierarchy( errorReporter, user, ou ) )
            {
                continue;
            }

            if ( !validateAttributes( errorReporter, preheat, te, tei, ou ) )
            {
                continue;
            }

            // Geo. checks...
            FeatureType featureType = null;
            if ( isCreate )
            {
                featureType = te.getFeatureType();
            }
            else if ( isUpdate )
            {
                // Why do we check tei.getTrackedEntityType().getFeatureType() on update?
                featureType = trackedEntityType.getFeatureType();
            }

            validateGeo( errorReporter, te, featureType );
        }

        return errorReporter.getReportList();
    }
}
