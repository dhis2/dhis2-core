package org.hisp.dhis.trackedentity;

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

import java.util.List;

/**
 * @author Chau Thu Tran
 * 
 * @version $ TrackedEntityService.java Feb 15, 2014 7:23:48 PM $
 */
public interface TrackedEntityService
{
    String ID = TrackedEntityService.class.getName();

    /**
     * Adds an {@link TrackedEntity}
     * 
     * @param trackedEntity The to TrackedEntity
     *        add.
     * 
     * @return A generated unique id of the added
     *         {@link TrackedEntity}.
     */
    int addTrackedEntity( TrackedEntity trackedEntity );

    /**
     * Deletes a {@link TrackedEntity}.
     * 
     * @param trackedEntity the TrackedEntity to
     *        delete.
     */
    void deleteTrackedEntity( TrackedEntity trackedEntity );

    /**
     * Updates a {@link TrackedEntity}.
     * 
     * @param trackedEntity the TrackedEntity to
     *        update.
     */
    void updateTrackedEntity( TrackedEntity trackedEntity );

    /**
     * Returns a {@link TrackedEntity}.
     * 
     * @param id the id of the TrackedEntity to return.
     * 
     * @return the TrackedEntity with the given id
     */
    TrackedEntity getTrackedEntity( int id );

    /**
     * Returns a {@link TrackedEntity}.
     *
     * @param id the id of the TrackedEntity to return.
     *
     * @return the TrackedEntity with the given id
     */
    TrackedEntity getTrackedEntity( String uid );

    /**
     * Returns a {@link TrackedEntity} with a given name.
     * 
     * @param name the name of the TrackedEntity to return.
     * 
     * @return the TrackedEntity with the given name, or null if
     *         no match.
     */
    TrackedEntity getTrackedEntityByName( String name );

    /**
     * Returns all {@link TrackedEntity}
     * 
     * @return a List of all TrackedEntity, or an empty
     *         List if there are no TrackedEntitys.
     */
    List<TrackedEntity> getAllTrackedEntity();
    
    /**
     * Returns The number of TrackedEntities with the key searched
     * 
     * @param name Keyword for searching by name
     * 
     * @return A number
     * 
     */
    Integer getTrackedEntityCountByName( String name );

    /**
     * Returns {@link TrackedEntity} list with paging
     * 
     * @param name Keyword for searching by name
     * @param min
     * @param max
     * @return a List of all TrackedEntity, or an empty
     *         List if there are no TrackedEntity.
     */
    List<TrackedEntity> getTrackedEntityBetweenByName( String name,
        int min, int max );

    /**
     * Returns The number of all TrackedEntity available
     * 
     */
    Integer getTrackedEntityCount();

    /**
     * Returns {@link TrackedEntity} list with paging
     * 
     * @param min
     * @param max
     * @return a List of all TrackedEntity, or an empty
     *         List if there are no TrackedEntity.
     */
    List<TrackedEntity> getTrackedEntitysBetween( int min, int max );
}
