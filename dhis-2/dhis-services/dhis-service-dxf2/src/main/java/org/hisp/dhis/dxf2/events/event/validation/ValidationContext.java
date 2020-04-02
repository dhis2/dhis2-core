package org.hisp.dhis.dxf2.events.event.validation;

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
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import lombok.Builder;
import lombok.Getter;
import org.hisp.dhis.trackedentity.TrackerAccessManager;

/**
 * @author Luciano Fiandesio
 */
@Getter
@Builder
public class ValidationContext
{
    private ImportOptions importOptions;

    /**
     * Holds a list of all programs in the system
     *
     * Map key is program uid
     */
    private Map<String, Program> programsMap;

    /**
     * Holds a Map of OrganisationUnit associated to an Event
     *
     * Map key is Event uid, value is {@see OrganisationUnit}
     */
    private Map<String, OrganisationUnit> organisationUnitMap;

    /**
     * Holds a Map of Tracked Entity Instances associated to an Event
     *
     * Map key is Event uid, value is {@see TrackedEntityInstance}
     */
    Map<String, TrackedEntityInstance> trackedEntityInstanceMap;

    /**
     * Map key is Event uid, value is {@see ProgramInstance}
     */
    private Map<String, ProgramInstance> programInstanceMap;

    private Map<String, ProgramStageInstance> programStageInstanceMap;

    private Map<String, CategoryOptionCombo> categoryOptionComboMap;

    private ProgramInstanceStore programInstanceStore;

    private TrackerAccessManager trackerAccessManager;

    /**
     * Checks within all the cached program for a ProgramStage having the
     * uid matching the specified uid.
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
