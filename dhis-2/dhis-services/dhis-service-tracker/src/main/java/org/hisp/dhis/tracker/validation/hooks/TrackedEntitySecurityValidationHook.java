package org.hisp.dhis.tracker.validation.hooks;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.security.Authorities;
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
import java.util.Set;
import java.util.stream.Collectors;

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
public class TrackedEntitySecurityValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 1;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationHookErrorReporter errorReporter = new ValidationHookErrorReporter( bundle,
            TrackedEntitySecurityValidationHook.class );

        User actingUser = bundle.getPreheat().getUser();

        List<TrackedEntity> trackedEntities = bundle.getTrackedEntities();
        for ( TrackedEntity te : trackedEntities )
        {
            TrackedEntityType entityType = getTrackedEntityType( bundle, te );
            if ( entityType != null && !aclService.canDataWrite( actingUser, entityType ) )
            {
                errorReporter.raiseError( TrackerErrorCode.E1001, entityType.getUid() );
            }

            OrganisationUnit ou = getOrganisationUnit( bundle, te );
            if ( ou != null && !organisationUnitService.isInUserSearchHierarchyCached( actingUser, ou ) )
            {
                errorReporter.raiseError( TrackerErrorCode.E1000, ou.getUid() );
            }

            if ( bundle.getImportStrategy().isDelete() )
            {
                TrackedEntityInstance tei = bundle.getPreheat()
                    .getTrackedEntity( TrackerIdentifier.UID, te.getTrackedEntity() );

                checkCanCascadeDeleteProgramInstances( errorReporter, actingUser, tei );
            }
        }

        return errorReporter.getReportList();
    }

    private void checkCanCascadeDeleteProgramInstances( ValidationHookErrorReporter errorReporter,
        User user, TrackedEntityInstance tei )
    {
        Set<ProgramInstance> programInstances = tei.getProgramInstances().stream()
            .filter( pi -> !pi.isDeleted() )
            .collect( Collectors.toSet() );

        if ( !programInstances.isEmpty() && !user.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
        {
            errorReporter
                .raiseError( TrackerErrorCode.NONE, tei.getUid(), Authorities.F_TEI_CASCADE_DELETE.getAuthority() );
        }
    }

}
