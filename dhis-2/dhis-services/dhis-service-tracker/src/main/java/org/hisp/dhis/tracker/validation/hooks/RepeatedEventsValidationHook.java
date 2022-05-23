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
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
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
    public void validate( ValidationErrorReporter reporter, TrackerBundle bundle )
    {
        Map<Pair<MetadataIdentifier, String>, List<Event>> eventsByEnrollmentAndNotRepeatableProgramStage = bundle
            .getEvents()
            .stream()
            .filter( e -> !reporter.isInvalid( e ) )
            .filter( e -> !bundle.getStrategy( e ).isDelete() )
            .filter( e -> {
                ProgramStage programStage = bundle.getPreheat().getProgramStage( e.getProgramStage() );
                return programStage.getProgram().isRegistration() && !programStage.getRepeatable();
            } )
            .collect( Collectors.groupingBy( e -> Pair.of( e.getProgramStage(), e.getEnrollment() ) ) );

        for ( Map.Entry<Pair<MetadataIdentifier, String>, List<Event>> mapEntry : eventsByEnrollmentAndNotRepeatableProgramStage
            .entrySet() )
        {
            if ( mapEntry.getValue().size() > 1 )
            {
                for ( Event event : mapEntry.getValue() )
                {
                    reporter.addError( event, TrackerErrorCode.E1039, mapEntry.getKey().getLeft() );
                }
            }
        }

        bundle.getEvents()
            .forEach( e -> validateNotMultipleEvents( reporter, bundle, e ) );
    }

    private void validateNotMultipleEvents( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        ProgramInstance programInstance = bundle.getProgramInstance( event.getEnrollment() );
        ProgramStage programStage = bundle.getPreheat().getProgramStage( event.getProgramStage() );

        TrackerImportStrategy strategy = bundle.getStrategy( event );

        if ( strategy == TrackerImportStrategy.CREATE && programStage != null && programInstance != null
            && !programStage.getRepeatable()
            && reporter.getBundle().getPreheat().hasProgramStageWithEvents( event.getProgramStage(),
                event.getEnrollment() ) )
        {
            reporter.addError( event, TrackerErrorCode.E1039, event.getProgramStage() );
        }
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }
}
