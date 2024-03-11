package org.hisp.dhis.trackedentity;

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

import java.util.List;

/**
 * @author Chau Thu Tran
 * 
 * @version $ TrackedEntityService.java Feb 15, 2014 7:23:48 PM $
 */
public interface TrackedEntityTypeService
{
    String ID = TrackedEntityTypeService.class.getName();

    /**
     * Adds an {@link TrackedEntityType}
     * 
     * @param trackedEntityType The to TrackedEntityType
     *        add.
     * 
     * @return A generated unique id of the added
     *         {@link TrackedEntityType}.
     */
    int addTrackedEntityType( TrackedEntityType trackedEntityType );

    /**
     * Deletes a {@link TrackedEntityType}.
     * 
     * @param trackedEntityType the TrackedEntityType to
     *        delete.
     */
    void deleteTrackedEntityType( TrackedEntityType trackedEntityType );

    /**
     * Updates a {@link TrackedEntityType}.
     * 
     * @param trackedEntityType the TrackedEntityType to
     *        update.
     */
    void updateTrackedEntityType( TrackedEntityType trackedEntityType );

    /**
     * Returns a {@link TrackedEntityType}.
     * 
     * @param id the id of the TrackedEntityType to return.
     * 
     * @return the TrackedEntityType with the given id
     */
    TrackedEntityType getTrackedEntityType( int id );

    /**
     * Returns a {@link TrackedEntityType}.
     *
     * @param uid the identifier of the TrackedEntityType to return.
     *
     * @return the TrackedEntityType with the given id
     */
    TrackedEntityType getTrackedEntityType( String uid );

    /**
     * Returns a {@link TrackedEntityType} with a given name.
     * 
     * @param name the name of the TrackedEntityType to return.
     * 
     * @return the TrackedEntityType with the given name, or null if
     *         no match.
     */
    TrackedEntityType getTrackedEntityByName( String name );

    /**
     * Returns all {@link TrackedEntityType}
     * 
     * @return a List of all TrackedEntityType, or an empty
     *         List if there are no TrackedEntitys.
     */
    List<TrackedEntityType> getAllTrackedEntityType();
}
