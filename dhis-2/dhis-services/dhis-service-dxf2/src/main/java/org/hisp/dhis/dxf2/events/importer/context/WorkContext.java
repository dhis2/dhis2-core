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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifierBasedOnIdScheme;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.importer.ServiceDelegator;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.User;

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
     * Holds a Map of all Programs in the system. See {@see ProgramSupplier} for
     * a detailed explanation of the data kept in this map
     *
     * Map: key -> Program ID (based on IdScheme) value -> Program
     */
    private final Map<String, Program> programsMap;

    /**
     * Holds a Map of all {@see OrganisationUnit} associated to the Events to
     * import. Each {@see OrganisationUnit} also contain the complete hierarchy
     * ( via .getParent() )
     *
     * Map: key -> Event UID value -> OrganisationUnit
     */
    private final Map<String, OrganisationUnit> organisationUnitMap;

    /**
     * Holds a Map of all {@see TrackedEntity} associated to the Events to
     * import.
     *
     * Map: key -> Event UID value -> Pair<TrackedEntity,
     * canBeUpdatedByCurrentUser boolean>
     */
    private final Map<String, Pair<TrackedEntity, Boolean>> trackedEntityInstanceMap;

    /**
     * Holds a Map of all {@see Enrollment} associated to the Events to import.
     *
     * Map: key -> Event UID value -> Enrollment
     */
    private final Map<String, Enrollment> programInstanceMap;

    /**
     * Holds a Map of all {@see Event} associated to the Events to import.
     *
     * Map: key -> Event UID value -> Event
     */
    private final Map<String, Event> programStageInstanceMap;

    /**
     * Holds a Map of all {@see Event} associated to the Events to import and
     * only contains already persisted values.
     *
     * Map: key -> Event UID value -> Event
     */
    private final Map<String, Event> persistedProgramStageInstanceMap;

    /**
     * Holds a Map of all {@see CategoryOptionCombo} associated to the Events to
     * import.
     *
     * Map: key -> Event UID value -> CategoryOptionCombo
     */
    private final Map<String, CategoryOptionCombo> categoryOptionComboMap;

    /**
     * Holds a Map of all {@see DataElement} associated to the Events to import.
     *
     * Map: key -> DataElement ID (based on IdScheme) value -> DataElement
     */
    private final Map<String, DataElement> dataElementMap;

    /**
     * Holds a Map of the EventDataValue for each event. Each entry value in the
     * Map, has a Set of EventDataValue, which have been already "prepared" for
     * update (insert/update). This means that the "incoming" Data Values have
     * been merged with the already existing Data Values (in case of an update).
     * This is the "reference" Map for Data Values during the Event import
     * process, meaning that the import components should only reference this
     * Map when dealing with Event Data Values (validation, etc)
     *
     */
    private final Map<String, Set<EventDataValue>> eventDataValueMap;

    private final Map<String, User> assignedUserMap;

    private final Map<String, Note> notesMap;

    /**
     * Holds a Map of Program ID (primary key) and List of Org Unit ID
     * associated to each program. Note that the List only contains the Org Unit
     * ID of org units that are specified in the payload.
     */
    private final Map<Long, List<Long>> programWithOrgUnitsMap;

    /**
     * Services / components
     */
    private final ServiceDelegator serviceDelegator;

    /**
     * Checks within all the cached program for a ProgramStage having the uid
     * matching the specified uid.
     *
     * @param programStageId the id according to the IdScheme of a program stage
     * @return a ProgramStage object or null
     */
    public ProgramStage getProgramStage( IdScheme idScheme, String programStageId )
    {
        for ( Program program : programsMap.values() )
        {
            Set<ProgramStage> programStages = program.getProgramStages();
            for ( ProgramStage programStage : programStages )
            {
                final String id = getIdentifierBasedOnIdScheme( programStage, idScheme );

                if ( id != null && id.equals( programStageId ) )
                {
                    programStage.setProgram( program );
                    return programStage;
                }
            }
        }
        return null;
    }

    public Optional<TrackedEntity> getTrackedEntityInstance( String event )
    {
        final Pair<TrackedEntity, Boolean> teiPair = this.trackedEntityInstanceMap.get( event );

        return (teiPair != null) ? Optional.of( teiPair.getKey() ) : Optional.empty();
    }

    public Optional<Event> getProgramStageInstance( String event )
    {
        return Optional.ofNullable( this.getProgramStageInstanceMap().get( event ) );
    }

    public Optional<Enrollment> getProgramInstance( String event )
    {
        return Optional.ofNullable( this.getProgramInstanceMap().get( event ) );
    }
}
