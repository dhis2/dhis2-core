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

import org.hisp.dhis.dxf2.deprecated.tracker.event.EventContext;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.user.User;

/**
 * @author Ameen Mohamed
 */
public interface TrackerOwnershipManager
{
    String OWNERSHIP_ACCESS_DENIED = "OWNERSHIP_ACCESS_DENIED";

    String PROGRAM_ACCESS_CLOSED = "PROGRAM_ACCESS_CLOSED";

    /**
     * @param entityInstance The tracked entity instance object
     * @param program The program object
     * @param orgUnit The org unit that has to become the owner
     * @param skipAccessValidation whether ownership access validation has to be
     *        skipped or not.
     */
    void transferOwnership( TrackedEntity entityInstance, Program program, OrganisationUnit orgUnit,
        boolean skipAccessValidation, boolean createIfNotExists );

    /**
     * @param entityInstance The tracked entity instance object
     * @param program The program object
     * @param organisationUnit The org unit that has to become the owner
     */
    void assignOwnership( TrackedEntity entityInstance, Program program, OrganisationUnit organisationUnit,
        boolean skipAccessValidation, boolean overwriteIfExists );

    /**
     * Check whether the user has access (as owner or has temporarily broken the
     * glass) for the tracked entity instance - program combination.
     *
     * @param user The user with which access has to be checked for.
     * @param entityInstance The tracked entity instance.
     * @param program The program.
     * @return true if the user has access, false otherwise.
     */
    boolean hasAccess( User user, TrackedEntity entityInstance, Program program );

    boolean hasAccess( User user, String entityInstance, OrganisationUnit organisationUnit, Program program );

    boolean hasAccessUsingContext( User user, String trackedEntityUid, String programUid,
        EventContext eventContext );

    /**
     * Grant temporary ownership for a user for a specific tei-program
     * combination
     *
     * @param entityInstance The tracked entity instance object
     * @param program The program object
     * @param user The user for which temporary access is granted.
     * @param reason The reason for requesting temporary ownership
     */
    void grantTemporaryOwnership( TrackedEntity entityInstance, Program program, User user, String reason );

    /**
     * Ownership check can be skipped if the user is super user or if the
     * program type is without registration.
     *
     * @param user the {@User}.
     * @param program the {@link Program}.
     *
     * @return true if ownership check can be skipped.
     */
    boolean canSkipOwnershipCheck( User user, Program program );

    /**
     * Ownership check can be skipped if the user is super user or if the
     * program type is without registration.
     *
     * @param user the {@User}.
     * @param programType the {@link ProgramType}.
     *
     * @return true if ownership check can be skipped.
     */
    boolean canSkipOwnershipCheck( User user, ProgramType programType );
}
