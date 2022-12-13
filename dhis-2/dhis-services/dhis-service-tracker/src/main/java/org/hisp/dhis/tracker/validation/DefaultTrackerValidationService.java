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

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.Timing;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrackerValidationService
    implements TrackerValidationService
{

    @Qualifier( "validationHooks" )
    private final List<TrackerValidationHook> validationHooks;

    @Qualifier( "org.hisp.dhis.tracker.validation.DefaultValidators" )
    private final Validators validators;

    @Qualifier( "org.hisp.dhis.tracker.validation.RuleEngineValidators" )
    private final Validators ruleEngineValidators;

    @Override
    public TrackerValidationReport validate( TrackerBundle bundle )
    {
        return validate( bundle, validationHooks, validators );
    }

    @Override
    public TrackerValidationReport validateRuleEngine( TrackerBundle bundle )
    {
        return validate( bundle, Collections.emptyList(), ruleEngineValidators );
    }

    private TrackerValidationReport validate( TrackerBundle bundle, List<TrackerValidationHook> hooks,
        Validators validators )
    {
        TrackerValidationReport validationReport = new TrackerValidationReport();

        User user = bundle.getUser();

        if ( (user == null || user.isSuper()) && ValidationMode.SKIP == bundle.getValidationMode() )
        {
            log.warn( "Skipping validation for metadata import by user '" +
                bundle.getUsername() + "'. Not recommended." );
            return validationReport;
        }

        // Note that the bundle gets cloned internally, so the original bundle
        // is always available
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle.getPreheat().getIdSchemes(),
            bundle.getValidationMode() == ValidationMode.FAIL_FAST );

        try
        {
            validateTrackedEntities( bundle, hooks, validators, reporter );
            validateEnrollments( bundle, hooks, validators, reporter );
            validateEvents( bundle, hooks, validators, reporter );
            validateRelationships( bundle, hooks, validators, reporter );
            validateBundle( bundle, hooks, reporter );
        }
        catch ( ValidationFailFastException e )
        {
            // exit early when in FAIL_FAST validation mode
        }
        validationReport
            .addErrors( reporter.getErrors() )
            .addWarnings( reporter.getWarnings() );

        PersistablesFilter.Result persistables = filter( bundle, reporter.getInvalidDTOs(),
            bundle.getImportStrategy() );

        bundle.setTrackedEntities( persistables.getTrackedEntities() );
        bundle.setEnrollments( persistables.getEnrollments() );
        bundle.setEvents( persistables.getEvents() );
        bundle.setRelationships( persistables.getRelationships() );

        validationReport.addErrors( persistables.getErrors() );

        return validationReport;
    }

    private void validateTrackedEntities( TrackerBundle bundle, List<TrackerValidationHook> preCheckHooks,
        Validators validators, ValidationErrorReporter reporter )
    {
        for ( TrackedEntity tei : bundle.getTrackedEntities() )
        {
            boolean failed = false;
            for ( TrackerValidationHook hook : preCheckHooks )
            {
                if ( hook.needsToRun( bundle.getStrategy( tei ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    hook.validateTrackedEntity( reporter, bundle, tei );

                    reporter.addTiming( new Timing(
                        hook.getClass().getName(),
                        hookTimer.toString() ) );

                    if ( hook.skipOnError() && didNotPassValidation( reporter, tei.getUid() ) )
                    {
                        failed = true;
                        break; // skip subsequent validation for this invalid entity
                    }
                }
            }

            if ( failed )
            {
                continue; // skip specific validations for this invalid entity
            }

            for ( Validator<TrackedEntity> validator : validators.getTrackedEntityValidators() )
            {
                if ( validator.needsToRun( bundle.getStrategy( tei ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    validator.validate( reporter, bundle, tei );

                    reporter.addTiming( new Timing(
                        validator.getClass().getName(),
                        hookTimer.toString() ) );
                }
            }
        }
    }

    private void validateEnrollments( TrackerBundle bundle, List<TrackerValidationHook> hooks,
        Validators validators, ValidationErrorReporter reporter )
    {
        for ( Enrollment enrollment : bundle.getEnrollments() )
        {
            boolean failed = false;
            for ( TrackerValidationHook hook : hooks )
            {
                if ( hook.needsToRun( bundle.getStrategy( enrollment ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    hook.validateEnrollment( reporter, bundle, enrollment );

                    reporter.addTiming( new Timing(
                        hook.getClass().getName(),
                        hookTimer.toString() ) );

                    if ( hook.skipOnError() && didNotPassValidation( reporter, enrollment.getUid() ) )
                    {
                        failed = true;
                        break; // skip subsequent validation for this invalid entity
                    }
                }
            }

            if ( failed )
            {
                continue; // skip specific validations for this invalid entity
            }

            for ( Validator<Enrollment> validator : validators.getEnrollmentValidators() )
            {
                if ( validator.needsToRun( bundle.getStrategy( enrollment ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    validator.validate( reporter, bundle, enrollment );

                    reporter.addTiming( new Timing(
                        validator.getClass().getName(),
                        hookTimer.toString() ) );
                }
            }
        }
    }

    private void validateEvents( TrackerBundle bundle, List<TrackerValidationHook> hooks,
        Validators validators, ValidationErrorReporter reporter )
    {
        for ( Event event : bundle.getEvents() )
        {
            boolean failed = false;
            for ( TrackerValidationHook hook : hooks )
            {
                if ( hook.needsToRun( bundle.getStrategy( event ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    hook.validateEvent( reporter, bundle, event );

                    reporter.addTiming( new Timing(
                        hook.getClass().getName(),
                        hookTimer.toString() ) );

                    if ( hook.skipOnError() && didNotPassValidation( reporter, event.getUid() ) )
                    {
                        failed = true;
                        break; // skip subsequent validation for this invalid entity
                    }
                }
            }

            if ( failed )
            {
                continue; // skip specific validations for this invalid entity
            }

            for ( Validator<Event> validator : validators.getEventValidators() )
            {
                if ( validator.needsToRun( bundle.getStrategy( event ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    validator.validate( reporter, bundle, event );

                    reporter.addTiming( new Timing(
                        validator.getClass().getName(),
                        hookTimer.toString() ) );
                }
            }
        }
    }

    private void validateRelationships( TrackerBundle bundle, List<TrackerValidationHook> hooks,
        Validators validators, ValidationErrorReporter reporter )
    {
        for ( Relationship relationship : bundle.getRelationships() )
        {
            boolean failed = false;
            for ( TrackerValidationHook hook : hooks )
            {
                if ( hook.needsToRun( bundle.getStrategy( relationship ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    hook.validateRelationship( reporter, bundle, relationship );

                    reporter.addTiming( new Timing(
                        hook.getClass().getName(),
                        hookTimer.toString() ) );

                    if ( hook.skipOnError() && didNotPassValidation( reporter, relationship.getUid() ) )
                    {
                        failed = true;
                        break; // skip subsequent validation for this invalid entity
                    }
                }
            }

            if ( failed )
            {
                continue; // skip specific validations for this invalid entity
            }

            for ( Validator<Relationship> validator : validators.getRelationshipValidators() )
            {
                if ( validator.needsToRun( bundle.getStrategy( relationship ) ) )
                {
                    Timer hookTimer = Timer.startTimer();

                    validator.validate( reporter, bundle, relationship );

                    reporter.addTiming( new Timing(
                        validator.getClass().getName(),
                        hookTimer.toString() ) );
                }
            }
        }
    }

    private static void validateBundle( TrackerBundle bundle, List<TrackerValidationHook> hooks,
        ValidationErrorReporter reporter )
    {
        for ( TrackerValidationHook hook : hooks )
        {
            Timer hookTimer = Timer.startTimer();

            hook.validate( reporter, bundle );

            reporter.addTiming( new Timing(
                hook.getClass().getName(),
                hookTimer.toString() ) );
        }
    }

    private boolean didNotPassValidation( ValidationErrorReporter reporter, String uid )
    {
        return reporter.getErrors().stream().anyMatch( r -> r.getUid().equals( uid ) );
    }
}
