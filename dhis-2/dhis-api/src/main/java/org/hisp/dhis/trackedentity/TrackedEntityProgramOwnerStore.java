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
package org.hisp.dhis.trackedentity;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.GenericStore;

/**
 * @author Ameen Mohamed
 */
public interface TrackedEntityProgramOwnerStore extends GenericStore<TrackedEntityProgramOwner>
{
    String ID = TrackedEntityProgramOwnerStore.class.getName();

    /**
     * Get tracked entity program owner entity for the tei-program combination.
     *
     * @param teiId The tracked entity instance id.
     * @param programId the program id
     * @return matching tracked entity program owner entity
     */
    TrackedEntityProgramOwner getTrackedEntityProgramOwner( long teiId, long programId );

    /**
     * Get all Tracked entity program owner entities for the list of teis.
     *
     * @param teiIds The list of tracked entity instance ids.
     * @return matching tracked entity program owner entities.
     */
    List<TrackedEntityProgramOwner> getTrackedEntityProgramOwners( List<Long> teiIds );

    /**
     * Get all Tracked entity program owner entities for the list of teis and
     * program.
     *
     * @param teiIds The list of tracked entity instance ids.
     * @param programId The program id
     * @return matching tracked entity program owner entities.
     */
    List<TrackedEntityProgramOwner> getTrackedEntityProgramOwners( List<Long> teiIds, long programId );

    List<TrackedEntityProgramOwnerOrgUnit> getTrackedEntityProgramOwnerOrgUnits( Set<Long> teiIds );
}
