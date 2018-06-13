package org.hisp.dhis.trackedentity;

import org.hisp.dhis.organisationunit.OrganisationUnit;

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

import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.User;

/**
 * @author Ameen Mohamed
 */
public interface TrackerOwnershipAccessManager
{
    String ID = TrackerOwnershipAccessManager.class.getName();

    public static final String OWNERSHIP_ACCESS_DENIED = "OWNERSHIP_ACCESS_DENIED";

    /**
     * Check whether the user is part of the owner org unit for the tracked
     * entity instance - program combination.
     * 
     * @param user The user with which access has to be checked for.
     * @param entityInstance The tracked entity instance.
     * @param program The program.
     * @return true if the user has access, false otherwise.
     */
    boolean isOwner( User user, TrackedEntityInstance entityInstance, Program program );

    /**
     * Check whether the user is part of the owner org unit for the tracked
     * entity instance - program combination.
     * 
     * @param user The user with which access has to be checked for.
     * @param teiId the tracked entity instance id
     * @param programId the program id
     * @return true if the user has access, false otherwise.
     */
    boolean isOwner( User user, int teiId, int programId );

    /**
     * Check whether the user is part of the owner org unit for the tracked
     * entity instance - program combination.
     * 
     * @param user The user with which access has to be checked for.
     * @param teiUid the tracked entity instance uid
     * @param programUid the program uid
     * @return true if the user has access, false otherwise.
     */
    boolean isOwner( User user, String teiUid, String programUid );

    /**
     * @param teiUid the tracked entity instance uid
     * @param programUid the prorgram uid
     * @param orgUnitUid the org unit uid
     * @param skipAccessValidation whether ownership access validation has to be
     *        skipped or not.
     */
    void transferOwnership( String teiUid, String programUid, String orgUnitUid, boolean skipAccessValidation,
        boolean createIfNotExists );

    /**
     * @param teiId The tracked entity instance id
     * @param programId The program id
     * @param orgUnitId the organisation unit id
     * @param skipAccessValidation whether ownership access validation has to be
     *        skipped or not.
     */
    void transferOwnership( int teiId, int programId, int orgUnitId, boolean skipAccessValidation,
        boolean createIfNotExists );

    /**
     * @param entityInstance The tracked entity instance object
     * @param progrprogramamId The program object
     * @param organisationUnit The org unit that has to become the owner
     * @param skipAccessValidation whether ownership access validation has to be
     *        skipped or not.
     */
    void transferOwnership( TrackedEntityInstance entityInstance, Program program, OrganisationUnit orgUnit,
        boolean skipAccessValidation, boolean createIfNotExists );

    /**
     * @param teiUid the tracked entity instance uid
     * @param programUid the prorgram uid
     * @param orgUnitUid the org unit uid
     * @param skipAccessValidation whether ownership access validation has to be
     *        skipped or not.
     */
    void assignOwnership( String teiUid, String programUid, String orgUnitUid, boolean skipAccessValidation,
        boolean overwriteIfExists );

    /**
     * @param teiId The tracked entity instance id
     * @param programId The program id
     * @param orgUnitId the organisation unit id
     * @param skipAccessValidation whether ownership access validation has to be
     *        skipped or not.
     */
    void assignOwnership( int teiId, int programId, int orgUnitId, boolean skipAccessValidation,
        boolean overwriteIfExists );

    /**
     * @param entityInstance The tracked entity instance object
     * @param progrprogramamId The program object
     * @param organisationUnit The org unit that has to become the owner
     * @param skipAccessValidation
     * @param overwriteIfExists
     */
    void assignOwnership( TrackedEntityInstance entityInstance, Program program, OrganisationUnit organisationUnit,
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
    boolean hasAccess( User user, TrackedEntityInstance entityInstance, Program program );

    /**
     * Check whether the user has access (as owner or has temporarily broken the
     * glass) for the tracked entity instance - program combination.
     * 
     * @param user The user with which access has to be checked for.
     * @param teiUid the tracked entity instance uid
     * @param programUid the program uid
     * @return true if the user has access, false otherwise.
     */
    boolean hasAccess( User user, String teiUid, String programUid );

    /**
     * Check whether the user has access (as owner or has temporarily broken the
     * glass) for the tracked entity instance - program combination.
     * 
     * @param user The user with which access has to be checked for.
     * @param teiId the tracked entity instance Id
     * @param programId the program Id
     * @return true if the user has access, false otherwise.
     */
    boolean hasAccess( User user, int teiId, int programId );

    /**
     * Grant temporary ownership for a user for a specific tei-program
     * combination
     * 
     * @param teiId The tracked entity instnace id
     * @param programId The program id
     * @param user The user to which temporary access has to be granted
     * @param reason The reason for requesting temporary ownership
     */
    void grantTemporaryOwnership( int teiId, int programId, User user, String reason  );

    /**
     * Grant temporary ownership for a user for a specific tei-program
     * combination
     * 
     * @param teiUid The tracked entity instance uid
     * @param programUid The program Uid
     * @param user The user object
     * @param reason The reason for requesting temporary ownership
     */
    void grantTemporaryOwnership( String teiUid, String programUid, User user, String reason  );

    /**
     * Grant temporary ownership for a user for a specific tei-program
     * combination
     * 
     * @param entityInstance The tracked entity instance object
     * @param program The program object
     * @param user The user for which temporary access is granted.
     * @param reason The reason for requesting temporary ownership
     */
    void grantTemporaryOwnership( TrackedEntityInstance entityInstance, Program program, User user, String reason );

    /**
     * Check if the user has temporary access for a specific tei-program
     * combination
     * 
     * @param teiUid The tracked entity instance Uid
     * @param programUid The program Uid
     * @param user The user object against which the check has to be performed
     * @return true if the user has temporary access, false otherwise
     */
    boolean hasTemporaryAccess( String teiUid, String programUid, User user );

    /**
     * Check if the user has temporary access for a specific tei-program
     * combination
     * 
     * @param teiId The tracked entity instance Id
     * @param programId The program Id
     * @param user The user object against which the check has to be performed
     * @return true if the user has temporary access, false otherwise
     */
    boolean hasTemporaryAccess( int teiId, int programId, User user );

    /**
     * Check if the user has temporary access for a specific tei-program
     * combination
     * 
     * @param entityInstance The tracked entity instance object
     * @param program The program object
     * @param user The user object against which the check has to be performed
     * @return true if the user has temporary access, false otherwise
     */
    boolean hasTemporaryAccess( TrackedEntityInstance entityInstance, Program program, User user );

}
