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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * This validator checks if in the payload there is more than one valid event
 * per enrollment based on a ProgramStage that is not repeatable.
 *
 * @author Enrico Colasante
 */
@Component
public class RepeatedEventsValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public ValidationErrorReporter validate( TrackerImportValidationContext context )
    {
        TrackerBundle bundle = context.getBundle();

        ValidationErrorReporter rootReporter = context.getRootReporter();

        Map<Pair<String, String>, List<Event>> eventsByEnrollmentAndNotRepeatableProgramStage = bundle.getEvents()
            .stream()
            .filter( e -> !rootReporter.isInvalid( e ) )
            .filter( e -> !context.getStrategy( e ).isDelete() )
            .filter( e -> {
                ProgramStage programStage = context.getProgramStage( e.getProgramStage() );
                return programStage.getProgram().isRegistration() && !programStage.getRepeatable();
            } )
            .collect( Collectors.groupingBy( e -> Pair.of( e.getProgramStage(), e.getEnrollment() ) ) );

        for ( Map.Entry<Pair<String, String>, List<Event>> mapEntry : eventsByEnrollmentAndNotRepeatableProgramStage
            .entrySet() )
        {
            if ( mapEntry.getValue().size() > 1 )
            {
                for ( Event event : mapEntry.getValue() )
                {
                    final ValidationErrorReporter reporter = new ValidationErrorReporter( context, event );
                    addError( reporter, TrackerErrorCode.E1039, mapEntry.getKey().getLeft() );
                    context.getRootReporter().merge( reporter );
                }
            }
        }

        bundle.getEvents()
            .forEach( e -> validateNotMultipleEvents( context, e ) );

        return rootReporter;
    }

    private void validateNotMultipleEvents( TrackerImportValidationContext context, Event event )
    {
        ProgramInstance programInstance = context.getProgramInstance( event.getEnrollment() );
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );

        TrackerImportStrategy strategy = context.getStrategy( event );

        if ( strategy == TrackerImportStrategy.CREATE && programStage != null && programInstance != null
            && !programStage.getRepeatable()
            && context.programStageHasEvents( programStage.getUid(), programInstance.getUid() ) )
        {
            final ValidationErrorReporter reporter = new ValidationErrorReporter( context, event );
            addError( reporter, TrackerErrorCode.E1039, event.getProgramStage() );
            context.getRootReporter().merge( reporter );
        }
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }
}
