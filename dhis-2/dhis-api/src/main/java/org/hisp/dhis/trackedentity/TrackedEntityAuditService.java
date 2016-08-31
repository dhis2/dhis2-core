package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.Date;
import java.util.List;

/**
 * @author Chau Thu Tran
 * 
 * @version TrackedEntityAuditService.java 9:01:49 AM Sep 26, 2012 $
 */
public interface TrackedEntityAuditService
{
    String ID = TrackedEntityAuditService.class.getName();

    /**
     * Adds an {@link TrackedEntityAudit}
     * 
     * @param trackedEntityAudit The to TrackedEntityAudit add.
     * 
     * @return A generated unique id of the added {@link TrackedEntityAudit}.
     */
    int saveTrackedEntityAudit( TrackedEntityAudit trackedEntityAudit );

    /**
     * Deletes a {@link TrackedEntityAudit}.
     * 
     * @param trackedEntityAudit the TrackedEntityAudit to delete.
     */
    void deleteTrackedEntityAudit( TrackedEntityAudit trackedEntityAudit );

    /**
     * Returns a {@link TrackedEntityAudit}.
     * 
     * @param id the id of the TrackedEntityAudit to return.
     * 
     * @return the TrackedEntityAudit with the given id
     */
    TrackedEntityAudit getTrackedEntityAudit( int id );

    /**
     * Get all instance audits of a instance
     * 
     * @param instance TrackedEntityInstance
     * 
     * @return List of TrackedEntityAudit
     */
    List<TrackedEntityAudit> getTrackedEntityAudits( TrackedEntityInstance instance );

    /**
     * Get instance audit of a instance
     * 
     * @param instanceId The id of instance
     * @param visitor The user who accessed to see a certain information of the
     *        instance
     * @param date The data this user visited
     * @param accessedModule The module this user accessed
     * 
     * @return TrackedEntityAudit
     */
    TrackedEntityAudit getTrackedEntityAudit( Integer instanceId, String visitor, Date date, String accessedModule );

}
