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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1068;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1069;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1070;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.trackedEntityExists;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class MetaValidator
    implements Validator<Enrollment>
{
    @Override
    public void validate( Reporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        OrganisationUnit organisationUnit = bundle.getPreheat().getOrganisationUnit( enrollment.getOrgUnit() );
        reporter.addErrorIfNull( organisationUnit, enrollment, E1070, enrollment.getOrgUnit() );

        Program program = bundle.getPreheat().getProgram( enrollment.getProgram() );
        reporter.addErrorIfNull( program, enrollment, E1069, enrollment.getProgram() );

        reporter.addErrorIf( () -> !trackedEntityExists( bundle, enrollment.getTrackedEntity() ),
            enrollment,
            E1068, enrollment.getTrackedEntity() );
    }

}
