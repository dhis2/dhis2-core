/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newWarningReport;

import java.util.List;

import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.programrule.RuleActionValidator;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * @author Enrico Colasante
 */
@Component
public class EnrollmentRuleValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private List<RuleActionValidator> validators;

    public EnrollmentRuleValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( Enrollment.class, TrackerImportStrategy.CREATE_AND_UPDATE, teAttrService );
    }

    @Autowired( required = false )
    public void setValidators( List<RuleActionValidator> validators )
    {
        this.validators = validators;
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        validators
            .stream()
            .filter( v -> !v.isWarning() )
            .flatMap( v -> {
                List<String> errors = v.validateEnrollments( context.getBundle() ).get( enrollment.getEnrollment() );
                return errors != null ? errors.stream() : Lists.newArrayList().stream();
            } )
            .forEach( e -> reporter.addError( newReport( TrackerErrorCode.E1200 ).addArg( e ) ) );

        validators
            .stream()
            .filter( v -> v.isWarning() )
            .flatMap( v -> {
                List<String> warnings = v.validateEnrollments( context.getBundle() ).get( enrollment.getEnrollment() );
                return warnings != null ? warnings.stream() : Lists.newArrayList().stream();
            } )
            .forEach( e -> reporter.addWarning( newWarningReport( TrackerErrorCode.E1200 ).addArg( e ) ) );
    }
}