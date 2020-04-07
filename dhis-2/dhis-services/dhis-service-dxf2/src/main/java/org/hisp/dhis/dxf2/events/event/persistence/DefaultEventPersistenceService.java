package org.hisp.dhis.dxf2.events.event.persistence;

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

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.JdbcEventStore;
import org.hisp.dhis.dxf2.events.event.mapper.ProgramStageInstanceMapper;
import org.hisp.dhis.dxf2.events.event.validation.ValidationContext;
import org.hisp.dhis.dxf2.events.eventdatavalue.EventDataValueService;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.springframework.stereotype.Service;

/**
 * @author Luciano Fiandesio
 */
@Service
public class DefaultEventPersistenceService
    implements
    EventPersistenceService
{
    private final JdbcEventStore jdbcEventStore;

    private final EventDataValueService eventDataValueService;

    private final ProgramStageInstanceStore programStageInstanceStore;

    public DefaultEventPersistenceService( JdbcEventStore jdbcEventStore, EventDataValueService eventDataValueService,  ProgramStageInstanceStore programStageInstanceStore )
    {
        // TODO add null check
        this.jdbcEventStore = jdbcEventStore;
        this.eventDataValueService = eventDataValueService;
        this.programStageInstanceStore = programStageInstanceStore;
    }

    @Override
    public List<ProgramStageInstance> save( ValidationContext context, List<Event> events )
    {
        List<ProgramStageInstance> programStageInstances = convertToProgramStageInstances(
            new ProgramStageInstanceMapper( context ), events );

        if ( !events.isEmpty() )
        {
            jdbcEventStore.saveEvents( programStageInstances );

            // TODO save notes too

            // TODO this for loop slows down the process 5X
            for ( ProgramStageInstance programStageInstance : programStageInstances )
            {
                ImportSummary importSummary = new ImportSummary( programStageInstance.getUid() );
                eventDataValueService.processDataValues(
                    programStageInstanceStore.getByUid( programStageInstance.getUid() ),
                    getEvent( programStageInstance.getUid(), events), false, context.getImportOptions(),
                    importSummary, context.getDataElementMap() );
            }


        }
        return null; // TODO

    }

    private Event getEvent( String uid, List<Event> events )
    {
        return events.stream().filter( event -> event.getUid().equals( uid ) ).findFirst().get();
    }

    private List<ProgramStageInstance> convertToProgramStageInstances( ProgramStageInstanceMapper mapper,
        List<Event> events )
    {
        return events.stream().map( mapper::convert ).collect( Collectors.toList() );
    }
}
