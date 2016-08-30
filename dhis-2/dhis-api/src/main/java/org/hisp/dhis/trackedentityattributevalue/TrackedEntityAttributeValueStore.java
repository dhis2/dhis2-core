package org.hisp.dhis.trackedentityattributevalue;

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
import java.util.List;

import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface TrackedEntityAttributeValueStore
    extends GenericStore<TrackedEntityAttributeValue>
{
    String ID = TrackedEntityAttributeValueStore.class.getName();

    /**
     * Adds a {@link TrackedEntityAttribute}
     * 
     * @param attribute The to TrackedEntityAttribute add.
     * 
     */
    void saveVoid( TrackedEntityAttributeValue attributeValue );

    /**
     * Deletes all {@link TrackedEntityAttributeValue} of a instance
     * 
     * @param instance {@link TrackedEntityInstance}
     * 
     * @return The error code. If the code is 0, deleting success
     */
    int deleteByTrackedEntityInstance( TrackedEntityInstance instance );

    /**
     * Retrieve a {@link TrackedEntityAttributeValue} on a
     * {@link TrackedEntityInstance} and {@link TrackedEntityAttribute}
     * 
     * @param attribute {@link TrackedEntityAttribute}
     * 
     * @return TrackedEntityAttributeValue
     */
    TrackedEntityAttributeValue get( TrackedEntityInstance instance, TrackedEntityAttribute attribute );

    /**
     * Retrieve {@link TrackedEntityAttributeValue} of a
     * {@link TrackedEntityInstance}
     * 
     * @param instance TrackedEntityInstance
     * 
     * @return TrackedEntityAttributeValue list
     */
    List<TrackedEntityAttributeValue> get( TrackedEntityInstance instance );

    /**
     * Retrieve {@link TrackedEntityAttributeValue} of a
     * {@link TrackedEntityInstance}
     * 
     * @param instance TrackedEntityInstance
     * 
     * @return TrackedEntityAttributeValue list
     */
    List<TrackedEntityAttributeValue> get( TrackedEntityAttribute attribute );

    /**
     * Retrieve {@link TrackedEntityAttributeValue} of a instance list
     * 
     * @param instances TrackedEntityInstance list
     * 
     * @return TrackedEntityAttributeValue list
     */
    List<TrackedEntityAttributeValue> get( Collection<TrackedEntityInstance> instances );

    /**
     * Search TrackedEntityAttributeValue objects by a TrackedEntityAttribute
     * and a attribute value (performs partial search )
     * 
     * @param attribute TrackedEntityAttribute
     * @param searchText A string for searching by attribute values
     * 
     * @return TrackedEntityAttributeValue list
     */
    List<TrackedEntityAttributeValue> searchByValue( TrackedEntityAttribute attribute, String searchText );

    /**
     * Gets a list of {@link TrackedEntityAttributeValue} that matches the params
     * @param attribute {@link TrackedEntityAttribute} to get value for
     * @param value literal value to find within the specified {@link TrackedEntityAttribute}
     * @return list of {@link TrackedEntityAttributeValue}
     */
    List<TrackedEntityAttributeValue> get( TrackedEntityAttribute attribute, String value );
    
        
    /**
     * Retrieve attribute values of an instance by a program
     * 
     * @param instance TrackedEntityInstance
     * @param value An attribute value for searching
     * 
     * @return TrackedEntityAttributeValue list
     */
    List<TrackedEntityAttributeValue> get( TrackedEntityInstance instance, Program program );
}
