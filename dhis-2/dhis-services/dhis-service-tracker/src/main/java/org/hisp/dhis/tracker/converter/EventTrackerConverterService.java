package org.hisp.dhis.tracker.converter;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Transactional
public class EventTrackerConverterService
    implements TrackerConverterService<Event, ProgramStageInstance>
{
    private final IdentifiableObjectManager manager;

    public EventTrackerConverterService( IdentifiableObjectManager manager )
    {
        this.manager = manager;
    }

    @Override
    public Event to( ProgramStageInstance programStageInstance )
    {
        return null;
    }

    @Override
    public Event to( List<ProgramStageInstance> programStageInstances )
    {
        return null;
    }

    @Override
    public Event to( TrackerPreheat preheat, List<ProgramStageInstance> programStageInstances )
    {
        return null;
    }

    @Override
    public ProgramStageInstance from( Event event )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();

        // temporary solution, will need to be part of general preheat process
        programStageInstance.setOrganisationUnit( manager.get( OrganisationUnit.class, event.getOrgUnit() ) );
        programStageInstance.setProgramStage( manager.get( ProgramStage.class, event.getProgramStage() ) );

        return programStageInstance;
    }

    @Override
    public ProgramStageInstance from( List<Event> events )
    {
        return null;
    }

    @Override
    public ProgramStageInstance from( TrackerPreheat preheat, List<Event> events )
    {
        return null;
    }
}
