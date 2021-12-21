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
package org.hisp.dhis.tracker.validation;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.report.TrackerValidationHookTimerReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
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

    @Qualifier( "ruleEngineValidationHooks" )
    private final List<TrackerValidationHook> ruleEngineValidationHooks;

    @Override
    public TrackerValidationReport validate( TrackerBundle bundle )
    {
        return validate( bundle, validationHooks );
    }

    @Override
    public TrackerValidationReport validateRuleEngine( TrackerBundle bundle )
    {
        return validate( bundle, ruleEngineValidationHooks );
    }

    private TrackerValidationReport validate( TrackerBundle bundle, List<TrackerValidationHook> hooks )
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
        TrackerImportValidationContext context = new TrackerImportValidationContext( bundle );

        try
        {
            for ( TrackerValidationHook hook : hooks )
            {
                Timer hookTimer = Timer.startTimer();

                validationReport.add( hook.validate( context ) );

                validationReport.add( TrackerValidationHookTimerReport.builder()
                    .name( hook.getClass().getName() )
                    .totalTime( hookTimer.toString() ).build() );
            }
        }
        catch ( ValidationFailFastException e )
        {
            validationReport.add( e.getErrors() );
        }

        removeInvalidObjects( bundle, context.getRootReporter() );

        return validationReport;
    }

    private void removeInvalidObjects( TrackerBundle bundle, ValidationErrorReporter reporter )
    {
        bundle.setEvents( bundle.getEvents().stream().filter(
            e -> !reporter.isInvalid( e ) )
            .collect( Collectors.toList() ) );
        bundle.setEnrollments( bundle.getEnrollments().stream().filter(
            e -> !reporter.isInvalid( e ) )
            .collect( Collectors.toList() ) );
        bundle.setTrackedEntities( bundle.getTrackedEntities().stream().filter(
            e -> !reporter.isInvalid( e ) )
            .collect( Collectors.toList() ) );
        bundle.setRelationships( bundle.getRelationships().stream().filter(
            e -> !reporter.isInvalid( e ) )
            .collect( Collectors.toList() ) );
    }
}
