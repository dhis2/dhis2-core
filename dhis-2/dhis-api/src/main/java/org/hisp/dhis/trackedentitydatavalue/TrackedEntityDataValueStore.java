package org.hisp.dhis.trackedentitydatavalue;

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

import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Abyot Asalefew Gizaw
 */
public interface TrackedEntityDataValueStore
    extends GenericStore<TrackedEntityDataValue>
{
    String ID = TrackedEntityDataValueStore.class.getName();

    /**
     * Adds an {@link TrackedEntityDataValue}
     *
     * @param dataValue The to TrackedEntityDataValue add.
     */
    void saveVoid( TrackedEntityDataValue dataValue );

    /**
     * Deletes a {@link TrackedEntityDataValue}.
     *
     * @param programStageInstance ProgramStageInstance.
     */
    int delete( ProgramStageInstance programStageInstance );

    /**
     * Retrieve data values of a event
     *
     * @param programStageInstance ProgramStageInstance
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> get( ProgramStageInstance programStageInstance );

    /**
     * Retrieve data values of an event that are supposed to be synchronized in ProgramDataSynchronizationJob
     * (so with skipsynchronization=false in programstagedataelement table)
     *
     * @param programStageInstance ProgramStageInstance
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> getTrackedEntityDataValuesForSynchronization( ProgramStageInstance programStageInstance );

    /**
     * Retrieve data values of a event with data elements specified
     *
     * @param programStageInstance ProgramStageInstance
     * @param dataElements         DataElement List
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> get( ProgramStageInstance programStageInstance,
        Collection<DataElement> dataElements );

    /**
     * Retrieve data values of many events
     *
     * @param programStageInstances ProgramStageInstance
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> get( Collection<ProgramStageInstance> programStageInstances );

    /**
     * Retrieve data values on a data element
     *
     * @param dataElement {@link DataElement}
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> get( DataElement dataElement );

    /**
     * Retrieve data values of a {@link TrackedEntityInstance} on a
     * {@link DataElement} list.
     *
     * @param instance     TrackedEntityInstance
     * @param dataElements The data element list
     * @param after        Optional date the instance should be on or after.
     * @param before       Optional date the instance should be on or before.
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> get( TrackedEntityInstance instance, Collection<DataElement> dataElements,
        Date after, Date before );

    /**
     * Retrieve a data value on an event and a data element
     *
     * @param programStageInstance ProgramStageInstance
     * @param dataElement          DataElement
     * @return TrackedEntityDataValue
     */
    TrackedEntityDataValue get( ProgramStageInstance programStageInstance, DataElement dataElement );
}
