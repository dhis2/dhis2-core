package org.hisp.dhis.trackedentitydatavalue;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Abyot Asalefew Gizaw
 * @version $Id$
 */
public interface TrackedEntityDataValueService
{
    String ID = TrackedEntityDataValueService.class.getName();

    /**
     * Adds an {@link TrackedEntityDataValue}
     * 
     * @param dataValue The to TrackedEntityDataValue add.
     * 
     * @return A generated unique id of the added {@link TrackedEntityDataValue}.
     */
    void saveTrackedEntityDataValue( TrackedEntityDataValue dataValue );

    /**
     * Updates an {@link TrackedEntityDataValue}.
     * 
     * @param dataValue the TrackedEntityDataValue to update.
     */
    void updateTrackedEntityDataValue( TrackedEntityDataValue dataValue );

    /**
     * Deletes a {@link TrackedEntityDataValue}.
     * 
     * @param dataValue the TrackedEntityDataValue to delete.
     */
    void deleteTrackedEntityDataValue( TrackedEntityDataValue dataValue );

    /**
     * Deletes all {@link TrackedEntityDataValue} of {@link ProgramStageInstance}
     * 
     * @param programStageInstance The {@link ProgramStageInstance}.
     * 
     * @return Error code. If this code is 0, deleting succeed.
     */
    void deleteTrackedEntityDataValue( ProgramStageInstance programStageInstance );

    /**
     * Retrieve data values of a event
     * 
     * @param programStageInstance ProgramStageInstance
     * 
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> getTrackedEntityDataValues( ProgramStageInstance programStageInstance );

    /**
     * Retrieve data values of a event with data elements specified
     * 
     * @param programStageInstance ProgramStageInstance
     * @param dataElement DataElement List
     * 
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> getTrackedEntityDataValues( ProgramStageInstance programStageInstance,
        Collection<DataElement> dataElement );

    /**
     * Retrieve data values of many events
     * 
     * @param programStageInstance ProgramStageInstance
     * 
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> getTrackedEntityDataValues( Collection<ProgramStageInstance> programStageInstances );

    /**
     * Retrieve data values of a data element
     * 
     * @param dataElement DataElement
     * 
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> getTrackedEntityDataValues( DataElement dataElement );

    /**
     * Retrieve data values of a instance on data elements specified from
     * a certain period
     * 
     * @param instance TrackedEntityInstance
     * @param dataElements DataElement List
     * @param after Optional date the instance should be on or after.
     * @param before Optional date the instance should be on or before.
     * 
     * @return TrackedEntityDataValue list
     */
    List<TrackedEntityDataValue> getTrackedEntityDataValues( TrackedEntityInstance instance, Collection<DataElement> dataElements,
        Date after, Date before );

    /**
     * Retrieve a data value on an event and a data element
     * 
     * @param programStageInstance ProgramStageInstance
     * @param dataElement DataElement
     * 
     * @return TrackedEntityDataValue
     */
    TrackedEntityDataValue getTrackedEntityDataValue( ProgramStageInstance programStageInstance, DataElement dataElement );
}
