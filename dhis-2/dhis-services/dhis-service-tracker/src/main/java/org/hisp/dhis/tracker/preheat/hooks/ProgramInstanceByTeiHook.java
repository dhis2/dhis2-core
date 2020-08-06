package org.hisp.dhis.tracker.preheat.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatHook;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.springframework.stereotype.Component;

/**
 * This pre-heater hook is responsible for creating an associative Map of Event UID -> {@see ProgramInstance}
 * The map
 *
 * @author Luciano Fiandesio
 */
@Component
public class ProgramInstanceByTeiHook implements TrackerPreheatHook
{
    private final ProgramInstanceStore programInstanceStore;

    private static final String KEY_SEPARATOR = "-";

    public ProgramInstanceByTeiHook( ProgramInstanceStore programInstanceStore )
    {
        this.programInstanceStore = programInstanceStore;
    }

    @Override
    public void preheat( TrackerPreheatParams params, TrackerPreheat preheat )
    {
        final Map<TrackerIdScheme, Map<String, ProgramInstance>> enrollmentsMap = preheat.getEnrollments();
        final Map<String, ProgramInstance> enrollments = enrollmentsMap.getOrDefault( TrackerIdScheme.UID, new HashMap<>() );

        // List of Events that have no 'enrollment' field or 'enrollment' points to an
        // invalid PI
        List<Event> eventWithoutPI = getEventsWithoutProgramInstance( params,
            enrollments.values().stream().map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() ) );

        if ( isNotEmpty( eventWithoutPI ) )
        {
            // Assign the map of event uid -> List Program Instance to the Preheat context
            preheat.setProgramInstances( getProgramInstancesByProgramAndTei(
                preheat,
                eventWithoutPI ) );
        }
    }

    private List<Event> getEventsWithoutProgramInstance( TrackerPreheatParams params, List<String> enrollmentsUid )
    {
        return params.getEvents().stream().filter( e -> !enrollmentsUid.contains( e.getEnrollment() ) )
            .collect( Collectors.toList() );
    }

    /**
     * Fetches Program Instances by Event Program and Event Tei.
     * The resulting Map has the Event UID as key and a List of Program Instances as value.
     *
     */
    private Map<String, List<ProgramInstance>> getProgramInstancesByProgramAndTei( TrackerPreheat preheat,
        List<Event> events )
    {
        Map<String, List<ProgramInstance>> result = new HashMap<>();

        // Build a look-up map
        final Map<String, Event> idToEventMap = events.stream()
            // filter out events without program or tei
            .filter( e -> StringUtils.isNotEmpty( e.getProgram() ) && StringUtils.isNotEmpty( e.getTrackedEntity() ) )
            .collect( Collectors.toMap( e -> e.getProgram() + KEY_SEPARATOR + e.getTrackedEntity(), e -> e ) );

        // @formatter:off
        final List<ProgramInstance> resultList = programInstanceStore.getByProgramAndTrackedEntityInstance(
            events.stream().map( e -> Pair.of(
                getProgram( preheat, e.getProgram() ),
                getTrackedEntityInstance( preheat, e.getTrackedEntity() ) ) )
                .collect( Collectors.toList() ), ProgramStatus.ACTIVE );
        // @formatter:on

        for ( ProgramInstance pi : resultList )
        {
            final Event event = idToEventMap.get( makeKey( pi ) );
            if ( event != null )
            {
                final List<ProgramInstance> programInstances = result.getOrDefault( event.getUid(), new ArrayList<>() );
                programInstances.add( pi );
                result.put( event.getUid(), programInstances );
            }
        }

        return result;
    }

    private Program getProgram( TrackerPreheat preheat, String uid )
    {
        return preheat.get( TrackerIdScheme.UID, Program.class, uid );
    }

    private TrackedEntityInstance getTrackedEntityInstance( TrackerPreheat preheat, String uid )
    {
        return preheat.getTrackedEntity( TrackerIdScheme.UID, uid );
    }

    private String makeKey( ProgramInstance programInstance )
    {
        return programInstance.getProgram().getUid() + KEY_SEPARATOR + programInstance.getEntityInstance().getUid();
    }
}