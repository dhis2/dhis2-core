package org.hisp.dhis.eventdatavalue;
/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;

import java.util.Map;
import java.util.Set;

/**
 * @author David Katuscak
 */
public interface EventDataValueService
{
    /**
     * Persist changes to provided dataValues
     *
     * @param newDataValues EventDataValues to add
     * @param updatedDataValues EventDataValues to update
     * @param removedDataValues EventDataValues to remove
     * @param dataElementsCache DataElements cache map with DataElements required for creating audit logs for changed EventDataValues
     * @param programStageInstance programStageInstance to which the EventDataValues belongs to
     * @param singleValue specifies whether the update is a single value update
     */
    void persistDataValues( Set<EventDataValue> newDataValues, Set<EventDataValue> updatedDataValues,Set<EventDataValue> removedDataValues,
        Map<String, DataElement> dataElementsCache, ProgramStageInstance programStageInstance, boolean singleValue );

    /**
     * Adds an {@link EventDataValue}
     *
     * @param programStageInstance the ProgramStageInstance that EventDataValue should be added to
     * @param eventDataValue the EventDataValue to add
     */
    void saveEventDataValue( ProgramStageInstance programStageInstance, EventDataValue eventDataValue );

    /**
     * Adds an {@link EventDataValue}s
     *
     * @param programStageInstance the ProgramStageInstance that EventDataValues should be added to
     * @param eventDataValues the Collection of EventDataValues to add
     */
    void saveEventDataValues( ProgramStageInstance programStageInstance, Set<EventDataValue> eventDataValues );

    /**
     * Updates an {@link EventDataValue}
     *
     * @param programStageInstance the ProgramStageInstance that EventDataValue belongs to
     * @param eventDataValue the EventDataValue to update
     */
    void updateEventDataValue( ProgramStageInstance programStageInstance, EventDataValue eventDataValue );

    /**
     * Updates an {@link EventDataValue}s
     *
     * @param programStageInstance the ProgramStageInstance that EventDataValues belongs to
     * @param eventDataValues the Collection of EventDataValues to update
     */
    void updateEventDataValues( ProgramStageInstance programStageInstance, Set<EventDataValue> eventDataValues );

    /**
     * Deletes a {@link EventDataValue} from {@link ProgramStageInstance}
     * @param programStageInstance the ProgramStageInstance to delete EventDataValue from
     * @param eventDataValue the EventDataValue to delete
     */
    void deleteEventDataValue( ProgramStageInstance programStageInstance, EventDataValue eventDataValue );

    /**
     * Deletes a {@link EventDataValue}s from {@link ProgramStageInstance}
     *
     * @param programStageInstance the ProgramStageInstance to delete EventDataValues from
     * @param eventDataValues the Collection of EventDataValues to delete
     */
    void deleteEventDataValues( ProgramStageInstance programStageInstance, Set<EventDataValue> eventDataValues );
}
