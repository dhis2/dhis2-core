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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.validation.PersistablesFilter.filter;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.ListUtils;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
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

    @Qualifier( "org.hisp.dhis.tracker.validation.DefaultValidators" )
    private final Validators validators;

    @Qualifier( "org.hisp.dhis.tracker.validation.RuleEngineValidators" )
    private final Validators ruleEngineValidators;

    @Override
    public ValidationResult validate( TrackerBundle bundle )
    {
        return validate( bundle, validators );
    }

    @Override
    public ValidationResult validateRuleEngine( TrackerBundle bundle )
    {
        return validate( bundle, ruleEngineValidators );
    }

    private ValidationResult validate( TrackerBundle bundle, Validators validators )
    {
        User user = bundle.getUser();

        if ( (user == null || user.isSuper()) && ValidationMode.SKIP == bundle.getValidationMode() )
        {
            log.warn( "Skipping validation for metadata import by user '" +
                bundle.getUsername() + "'. Not recommended." );
            return Result.empty();
        }

        // Note that the bundle gets cloned internally, so the original bundle
        // is always available
        Reporter reporter = new Reporter( bundle.getPreheat().getIdSchemes(),
            bundle.getValidationMode() == ValidationMode.FAIL_FAST );

        try
        {
            validateTrackedEntities( bundle, validators.getTrackedEntityValidators(), reporter );
            validateEnrollments( bundle, validators.getEnrollmentValidators(), reporter );
            validateEvents( bundle, validators.getEventValidators(), reporter );
            validateRelationships( bundle, validators.getRelationshipValidators(), reporter );
            validateBundle( bundle, validators.getBundleValidators(), reporter );
        }
        catch ( FailFastException e )
        {
            // exit early when in FAIL_FAST validation mode
        }

        PersistablesFilter.Result persistables = filter( bundle, reporter.getInvalidDTOs(),
            bundle.getImportStrategy() );

        bundle.setTrackedEntities( persistables.getTrackedEntities() );
        bundle.setEnrollments( persistables.getEnrollments() );
        bundle.setEvents( persistables.getEvents() );
        bundle.setRelationships( persistables.getRelationships() );

        List<Error> errors = ListUtils.union( reporter.getErrors(), persistables.getErrors() );
        return Result.withValidations( Set.copyOf( errors ), Set.copyOf( reporter.getWarnings() ) );
    }

    private void validateTrackedEntities( TrackerBundle bundle, List<Validator<TrackedEntity>> validators,
        Reporter reporter )
    {
        for ( TrackedEntity tei : bundle.getTrackedEntities() )
        {
            for ( Validator<TrackedEntity> validator : validators )
            {
                if ( validator.needsToRun( bundle.getStrategy( tei ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    validator.validate( reporter, bundle, tei );

                    reporter.addTiming( new Timing(
                        validator.getClass().getName(),
                        hookTimer.toString() ) );

                    if ( validator.skipOnError() && didNotPassValidation( reporter, tei.getUid() ) )
                    {
                        break; // skip subsequent validation for this invalid entity
                    }
                }
            }
        }
    }

    private void validateEnrollments( TrackerBundle bundle, List<Validator<Enrollment>> validators,
        Reporter reporter )
    {
        for ( Enrollment enrollment : bundle.getEnrollments() )
        {
            for ( Validator<Enrollment> validator : validators )
            {
                if ( validator.needsToRun( bundle.getStrategy( enrollment ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    validator.validate( reporter, bundle, enrollment );

                    reporter.addTiming( new Timing(
                        validator.getClass().getName(),
                        hookTimer.toString() ) );

                    if ( validator.skipOnError() && didNotPassValidation( reporter, enrollment.getUid() ) )
                    {
                        break; // skip subsequent validation for this invalid entity
                    }
                }
            }
        }
    }

    private void validateEvents( TrackerBundle bundle, List<Validator<Event>> validators,
        Reporter reporter )
    {
        for ( Event event : bundle.getEvents() )
        {
            for ( Validator<Event> validator : validators )
            {
                if ( validator.needsToRun( bundle.getStrategy( event ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    validator.validate( reporter, bundle, event );

                    reporter.addTiming( new Timing(
                        validator.getClass().getName(),
                        hookTimer.toString() ) );

                    if ( validator.skipOnError() && didNotPassValidation( reporter, event.getUid() ) )
                    {
                        break; // skip subsequent validation for this invalid entity
                    }
                }
            }
        }
    }

    private void validateRelationships( TrackerBundle bundle, List<Validator<Relationship>> validators,
        Reporter reporter )
    {
        for ( Relationship relationship : bundle.getRelationships() )
        {
            for ( Validator<Relationship> validator : validators )
            {
                if ( validator.needsToRun( bundle.getStrategy( relationship ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    validator.validate( reporter, bundle, relationship );

                    reporter.addTiming( new Timing(
                        validator.getClass().getName(),
                        hookTimer.toString() ) );

                    if ( validator.skipOnError() && didNotPassValidation( reporter, relationship.getUid() ) )
                    {
                        break; // skip subsequent validation for this invalid entity
                    }
                }
            }
        }
    }

    private static void validateBundle( TrackerBundle bundle, List<Validator<TrackerBundle>> validators,
        Reporter reporter )
    {
        for ( Validator<TrackerBundle> hook : validators )
        {
            Timer hookTimer = Timer.startTimer();

            hook.validate( reporter, bundle, bundle );

            reporter.addTiming( new Timing(
                hook.getClass().getName(),
                hookTimer.toString() ) );
        }
    }

    private boolean didNotPassValidation( Reporter reporter, String uid )
    {
        return reporter.getErrors().stream().anyMatch( r -> r.getUid().equals( uid ) );
    }
}
