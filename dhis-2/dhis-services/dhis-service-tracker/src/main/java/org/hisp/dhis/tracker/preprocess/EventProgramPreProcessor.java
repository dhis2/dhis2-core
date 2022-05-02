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
package org.hisp.dhis.tracker.preprocess;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

/**
 * This preprocessor is responsible for setting the Program UID on an Event from
 * the ProgramStage if the Program is not present in the payload
 *
 * @author Enrico Colasante
 */
@Component
public class EventProgramPreProcessor
    implements BundlePreProcessor
{
    @Override
    public void process( TrackerBundle bundle )
    {
        List<Event> eventsToPreprocess = bundle.getEvents()
            .stream()
            .filter( e -> e.getProgram().isBlank() || e.getProgramStage().isBlank() )
            .collect( Collectors.toList() );

        for ( Event event : eventsToPreprocess )
        {
            // Extract program from program stage
            if ( !event.getProgramStage().isBlank() )
            {
                ProgramStage programStage = bundle.getPreheat().getProgramStage( event.getProgramStage() );
                if ( Objects.nonNull( programStage ) )
                {
                    // TODO remove if once metadata import is fixed
                    if ( programStage.getProgram() == null )
                    {
                        // Program stages should always have a program! Due to
                        // how metadata
                        // import is currently implemented
                        // it's possible that users run into the edge case that
                        // a program
                        // stage does not have an associated
                        // program. Tell the user it's an issue with the
                        // metadata and not
                        // the event itself. This should be
                        // fixed in the metadata import. For more see
                        // https://jira.dhis2.org/browse/DHIS2-12123
                        //
                        // PreCheckMandatoryFieldsValidationHook.validateEvent
                        // will create
                        // a validation error for this edge case
                        return;
                    }
                    TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
                    event.setProgram( idSchemes.toMetadataIdentifier( programStage.getProgram() ) );
                    bundle.getPreheat().put( programStage.getProgram() );
                }
            }
            // If it is a program event, extract program stage from program
            else if ( !event.getProgram().isBlank() )
            {
                Program program = bundle.getPreheat().getProgram( event.getProgram() );
                if ( Objects.nonNull( program ) && program.isWithoutRegistration() )
                {
                    Optional<ProgramStage> programStage = program.getProgramStages().stream().findFirst();
                    if ( programStage.isPresent() )
                    {
                        TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
                        event.setProgramStage( idSchemes.toMetadataIdentifier( programStage.get() ) );
                        bundle.getPreheat().put( programStage.get() );
                    }
                }
            }
        }
        setAttributeOptionCombo( bundle );
    }

    private void setAttributeOptionCombo( TrackerBundle bundle )
    {

        TrackerPreheat preheat = bundle.getPreheat();
        List<Event> events = bundle.getEvents().stream()
            .filter( e -> isBlank( e.getAttributeOptionCombo() )
                && !isBlank( e.getAttributeCategoryOptions() ) )
            .filter( e -> preheat.getProgram( e.getProgram() ) != null )
            .collect( Collectors.toList() );

        for ( Event e : events )
        {
            Program program = preheat.getProgram( e.getProgram() );
            e.setAttributeOptionCombo( preheat.getCategoryOptionComboIdentifier( program.getCategoryCombo(),
                e.getAttributeCategoryOptions() ) );
        }
    }
}