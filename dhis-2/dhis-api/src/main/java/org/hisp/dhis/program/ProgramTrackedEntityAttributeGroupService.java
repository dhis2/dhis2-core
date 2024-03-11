package org.hisp.dhis.program;

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
 * @author Viet Nguyen
 */
public interface ProgramTrackedEntityAttributeGroupService
{
    /**
     * Adds an {@link ProgramTrackedEntityAttributeGroup}
     * 
     * @param programTrackedEntityAttributeGroup The to ProgramTrackedEntityAttributeGroup
     *         add.
     * 
     * @return A generated unique id of the added {@link ProgramTrackedEntityAttributeGroup}.
     */
    int addProgramTrackedEntityAttributeGroup( ProgramTrackedEntityAttributeGroup programTrackedEntityAttributeGroup );

    /**
     * Deletes a {@link ProgramTrackedEntityAttributeGroup}.
     * 
     * @param programTrackedEntityAttributeGroup the ProgramTrackedEntityAttributeGroup to
     *        delete.
     */
    void deleteProgramTrackedEntityAttributeGroup( ProgramTrackedEntityAttributeGroup programTrackedEntityAttributeGroup );

    /**
     * Updates a {@link ProgramTrackedEntityAttributeGroup}.
     * 
     * @param programTrackedEntityAttributeGroup the ProgramTrackedEntityAttributeGroup to
     *        update.
     */
    void updateProgramTrackedEntityAttributeGroup( ProgramTrackedEntityAttributeGroup programTrackedEntityAttributeGroup );

    /**
     * Returns a {@link ProgramTrackedEntityAttributeGroup}.
     * 
     * @param id the id of the ProgramTrackedEntityAttributeGroup to return.
     * 
     * @return the ProgramTrackedEntityAttributeGroup with the given id
     */
    ProgramTrackedEntityAttributeGroup getProgramTrackedEntityAttributeGroup( int id );

    /**
     * Returns a {@link ProgramTrackedEntityAttributeGroup}.
     * 
     * @param uid the id of the ProgramTrackedEntityAttributeGroup to return.
     * 
     * @return the ProgramTrackedEntityAttributeGroup with the given id
     */
    ProgramTrackedEntityAttributeGroup getProgramTrackedEntityAttributeGroup( String uid );

    /**
     * Returns all {@link ProgramTrackedEntityAttributeGroup}
     * 
     * @return a List of all ProgramTrackedEntityAttributeGroup, or an empty
     *         List if there are no ProgramTrackedEntityAttributeGroups.
     */
    List<ProgramTrackedEntityAttributeGroup> getAllProgramTrackedEntityAttributeGroups();
}
