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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1014;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1022;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1041;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class DataRelationsValidator
    implements Validator<Enrollment>
{
    @Override
    public void validate( Reporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        Program program = bundle.getPreheat().getProgram( enrollment.getProgram() );
        OrganisationUnit organisationUnit = bundle.getPreheat()
            .getOrganisationUnit( enrollment.getOrgUnit() );

        reporter.addErrorIf( () -> !program.isRegistration(), enrollment, E1014, program );

        TrackerPreheat preheat = bundle.getPreheat();
        if ( programDoesNotHaveOrgUnit( program, organisationUnit, preheat.getProgramWithOrgUnitsMap() ) )
        {
            reporter.addError( enrollment, E1041, organisationUnit, program );
        }

        validateTrackedEntityTypeMatchesPrograms( reporter, bundle, program, enrollment );
    }

    private boolean programDoesNotHaveOrgUnit( Program program, OrganisationUnit orgUnit,
        Map<String, List<String>> programAndOrgUnitsMap )
    {
        return !programAndOrgUnitsMap.containsKey( program.getUid() )
            || !programAndOrgUnitsMap.get( program.getUid() ).contains( orgUnit.getUid() );
    }

    private void validateTrackedEntityTypeMatchesPrograms( Reporter reporter, TrackerBundle bundle,
        Program program,
        Enrollment enrollment )
    {

        if ( program.getTrackedEntityType() == null )
        {
            return;
        }

        if ( !trackedEntityTypesMatch( bundle, program, enrollment ) )
        {
            reporter.addError( enrollment, E1022, enrollment.getTrackedEntity(), program );
        }
    }

    private boolean trackedEntityTypesMatch( TrackerBundle bundle, Program program, Enrollment enrollment )
    {
        final TrackedEntity trackedEntity = bundle
            .getPreheat().getTrackedEntity( enrollment.getTrackedEntity() );
        if ( trackedEntity != null )
        {
            return program.getTrackedEntityType().getUid()
                .equals( trackedEntity.getTrackedEntityType().getUid() );
        }

        return bundle.findTrackedEntityByUid( enrollment.getTrackedEntity() )
            .map( te -> te.getTrackedEntityType().isEqualTo( program.getTrackedEntityType() ) )
            .orElse( false );
    }

}
