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
package org.hisp.dhis.tracker.imports.validation;

import static org.hisp.dhis.tracker.imports.validation.PersistablesFilter.filter;

import java.util.HashSet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.ListUtils;
import org.hisp.dhis.tracker.imports.ValidationMode;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultValidationService
    implements ValidationService
{

    @Qualifier( "org.hisp.dhis.tracker.imports.validation.validator.DefaultValidator" )
    private final Validator<TrackerBundle> validator;

    @Qualifier( "org.hisp.dhis.tracker.imports.validation.validator.RuleEngineValidator" )
    private final Validator<TrackerBundle> ruleEngineValidator;

    @Override
    public ValidationResult validate( TrackerBundle bundle )
    {
        return validate( bundle, validator );
    }

    @Override
    public ValidationResult validateRuleEngine( TrackerBundle bundle )
    {
        return validate( bundle, ruleEngineValidator );
    }

    private ValidationResult validate( TrackerBundle bundle, Validator<TrackerBundle> validator )
    {
        User user = bundle.getUser();
        if ( (user == null || user.isSuper()) && ValidationMode.SKIP == bundle.getValidationMode() )
        {
            log.warn( "Skipping validation for metadata import by user '" +
                bundle.getUsername() + "'. Not recommended." );
            return Result.empty();
        }

        Reporter reporter = new Reporter( bundle.getPreheat().getIdSchemes(),
            bundle.getValidationMode() == ValidationMode.FAIL_FAST );

        try
        {
            validator.validate( reporter, bundle, bundle );
        }
        catch ( FailFastException e )
        {
            // exit early when in FAIL_FAST validation mode
        }

        PersistablesFilter.Result persistables = filter( bundle, reporter.getInvalidDTOs(),
            bundle.getImportStrategy() );

        return new Result(
            persistables.getTrackedEntities(),
            persistables.getEnrollments(),
            persistables.getEvents(),
            persistables.getRelationships(),
            new HashSet<>( ListUtils.union( reporter.getErrors(), persistables.getErrors() ) ),
            new HashSet<>( reporter.getWarnings() ) );
    }
}
