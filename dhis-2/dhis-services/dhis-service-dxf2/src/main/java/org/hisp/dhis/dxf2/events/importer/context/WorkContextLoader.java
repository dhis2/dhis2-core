package org.hisp.dhis.dxf2.events.importer.context;

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

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class WorkContextLoader
{
    private final ProgramSupplier programSupplier;

    private final OrganisationUnitSupplier organisationUnitSupplier;

    private final TrackedEntityInstanceSupplier trackedEntityInstanceSupplier;

    private final ProgramInstanceSupplier programInstanceSupplier;

    private final ProgramStageInstanceSupplier programStageInstanceSupplier;

    private final CategoryOptionComboSupplier categoryOptionComboSupplier;

    private final DataElementSupplier dataElementSupplier;

    private final NoteSupplier noteSupplier;

    private final AssignedUserSupplier assignedUserSupplier;

    private final ServiceDelegatorSupplier serviceDelegatorSupplier;

    public WorkContextLoader( ProgramSupplier programSupplier, OrganisationUnitSupplier organisationUnitSupplier,
        TrackedEntityInstanceSupplier trackedEntityInstanceSupplier, ProgramInstanceSupplier programInstanceSupplier,
        ProgramStageInstanceSupplier programStageInstanceSupplier,
        CategoryOptionComboSupplier categoryOptionComboSupplier, DataElementSupplier dataElementSupplier,
        NoteSupplier noteSupplier, AssignedUserSupplier assignedUserSupplier,
        ServiceDelegatorSupplier serviceDelegatorSupplier )
    {
        this.programSupplier = programSupplier;
        this.organisationUnitSupplier = organisationUnitSupplier;
        this.trackedEntityInstanceSupplier = trackedEntityInstanceSupplier;
        this.programInstanceSupplier = programInstanceSupplier;
        this.programStageInstanceSupplier = programStageInstanceSupplier;
        this.categoryOptionComboSupplier = categoryOptionComboSupplier;
        this.dataElementSupplier = dataElementSupplier;
        this.noteSupplier = noteSupplier;
        this.assignedUserSupplier = assignedUserSupplier;
        this.serviceDelegatorSupplier = serviceDelegatorSupplier;
    }

    public WorkContext load( ImportOptions importOptions, List<Event> events )
    {
        return WorkContext.builder()
        // @formatter:off
            .importOptions( importOptions )
            .programsMap( programSupplier.get( importOptions, events ) )
            .organisationUnitMap( organisationUnitSupplier.get( importOptions, events ) )
            .trackedEntityInstanceMap( trackedEntityInstanceSupplier.get( importOptions, events ) )
            .programInstanceMap( programInstanceSupplier.get( importOptions, events ) )
            .programStageInstanceMap( programStageInstanceSupplier.get( importOptions, events ) )
            .categoryOptionComboMap( categoryOptionComboSupplier.get( importOptions, events ) )
            .dataElementMap( dataElementSupplier.get( importOptions, events ) )
            .notesMap( noteSupplier.get( importOptions, events ) )
            .assignedUserMap( assignedUserSupplier.get( importOptions, events ) )
            .serviceDelegator( serviceDelegatorSupplier.get() )
            .build();
        // @formatter:on
    }
}
