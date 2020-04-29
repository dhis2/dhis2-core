package org.hisp.dhis.dxf2.events.event.context;

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

import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.event.validation.ServiceDelegator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;

import lombok.Builder;
import lombok.Getter;

/**
 * This class acts as a cache for data required during the Event import process.
 * 
 * @author Luciano Fiandesio
 */
@Getter
@Builder
public class WorkContext
{
    private final ImportOptions importOptions;

    /**
     * Holds a Map of all Programs in the system. 
     * Each Program in the Map also contains: 
     * 
     * - all connected Program Stages 
     * - all connected Org Units 
     * - the connected CategoryCombo
     *
     * Map:
     *  key     -> Program UID
     *  value   -> Program
     */
    private final Map<String, Program> programsMap;

    /**
     * Holds a Map of all {@see OrganisationUnit} associated to the Events to import.
     * Each {@see OrganisationUnit} also contain the complete hierarchy ( via .getParent() )
     *
     * Map:
     *  key     -> Event UID
     *  value   -> OrganisationUnit
     */
    private final Map<String, OrganisationUnit> organisationUnitMap;

    /**
     * Holds a Map of all {@see TrackedEntityInstance} associated to the Events to import.
     * 
     * Map:
     *  key     -> Event UID
     *  value   -> TrackedEntityInstance
     */
    private final Map<String, TrackedEntityInstance> trackedEntityInstanceMap;

    /**
     * Holds a Map of all {@see ProgramInstance} associated to the Events to import.
     *
     * Map:
     *  key     -> Event UID
     *  value   -> ProgramInstance
     */
    private final Map<String, ProgramInstance> programInstanceMap;

    /**
     * Holds a Map of all {@see ProgramStageInstance} associated to the Events to import.
     *
     * Map:
     *  key     -> ProgramStageInstance UID
     *  value   -> ProgramStageInstance
     */
    private final Map<String, ProgramStageInstance> programStageInstanceMap;

    /**
     * Holds a Map of all {@see CategoryOptionCombo} associated to the Events to import.
     *
     * Map:
     *  key     -> Event UID
     *  value   -> CategoryOptionCombo
     */
    private final Map<String, CategoryOptionCombo> categoryOptionComboMap;

    /**
     * Holds a Map of all {@see DataElement} associated to the Events to import.
     *
     * Map:
     *  key     -> Event UID
     *  value   -> DataElement
     */
    private final Map<String, DataElement> dataElementMap;

    private final Map<String, User> assignedUserMap;

    private final Map<String, Note> notesMap;

    /**
     * Services / components
     */
    private final ServiceDelegator serviceDelegator;

    /**
     * Checks within all the cached program for a ProgramStage having the uid
     * matching the specified uid.
     *
     * @param uid the uid of a program stage
     * @return a ProgramStage object or null
     */
    public ProgramStage getProgramStage( String uid )
    {
        for ( Program program : programsMap.values() )
        {
            Set<ProgramStage> programStages = program.getProgramStages();
            for ( ProgramStage programStage : programStages )
            {
                if ( programStage.getUid().equals( uid ) )
                {
                    programStage.setProgram( program );
                    return programStage;
                }
            }
        }
        return null;
    }
}
