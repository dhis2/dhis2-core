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
package org.hisp.dhis.program;

import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;

/**
 * @author Abyot Asalefew
 */
public interface ProgramInstanceService
{
    /**
     * Adds an {@link Enrollment}
     *
     * @param enrollment The to ProgramInstance add.
     * @return A generated unique id of the added {@link Enrollment}.
     */
    long addProgramInstance( Enrollment enrollment );

    /**
     * Adds an {@link Enrollment}
     *
     * @param enrollment The to ProgramInstance add.
     * @param user the current user.
     * @return A generated unique id of the added {@link Enrollment}.
     */
    long addProgramInstance( Enrollment enrollment, User user );

    /**
     * Soft deletes a {@link Enrollment}.
     *
     * @param enrollment the ProgramInstance to delete.
     */
    void deleteProgramInstance( Enrollment enrollment );

    /**
     * Hard deletes a {@link Enrollment}.
     *
     * @param enrollment the ProgramInstance to delete.
     */
    void hardDeleteProgramInstance( Enrollment enrollment );

    /**
     * Updates an {@link Enrollment}.
     *
     * @param enrollment the ProgramInstance to update.
     */
    void updateProgramInstance( Enrollment enrollment );

    /**
     * Updates an {@link Enrollment}.
     *
     * @param enrollment the ProgramInstance to update.
     * @param user the current user.
     */
    void updateProgramInstance( Enrollment enrollment, User user );

    /**
     * Returns a {@link Enrollment}.
     *
     * @param id the id of the ProgramInstance to return.
     * @return the ProgramInstance with the given id
     */
    Enrollment getProgramInstance( long id );

    /**
     * Returns the {@link Enrollment} with the given UID.
     *
     * @param uid the UID.
     * @return the ProgramInstance with the given UID, or null if no match.
     */
    Enrollment getProgramInstance( String uid );

    /**
     * Returns a list of existing ProgramInstances from the provided UIDs
     *
     * @param uids PSI UIDs to check
     * @return ProgramInstance list
     */
    List<Enrollment> getProgramInstances( @Nonnull List<String> uids );

    /**
     * Checks for the existence of a PI by UID. Deleted values are not taken
     * into account.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean programInstanceExists( String uid );

    /**
     * Checks for the existence of a PI by UID. Takes into account also the
     * deleted values.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean programInstanceExistsIncludingDeleted( String uid );

    /**
     * Returns UIDs of existing ProgramInstances (including deleted) from the
     * provided UIDs
     *
     * @param uids PSI UIDs to check
     * @return Set containing UIDs of existing PSIs (including deleted)
     */
    List<String> getProgramInstancesUidsIncludingDeleted( List<String> uids );

    /**
     * Returns a list with program instance values based on the given
     * ProgramInstanceQueryParams.
     *
     * @param params the ProgramInstanceQueryParams.
     * @return List of PIs matching the params
     */
    List<Enrollment> getProgramInstances( ProgramInstanceQueryParams params );

    /**
     * Returns the number of program instance matches based on the given
     * ProgramInstanceQueryParams.
     *
     * @param params the ProgramInstanceQueryParams.
     * @return Number of PIs matching the params
     */
    int countProgramInstances( ProgramInstanceQueryParams params );

    /**
     * Decides whether current user is authorized to perform the given query.
     * IllegalQueryException is thrown if not.
     *
     * @param params the ProgramInstanceQueryParams.
     */
    void decideAccess( ProgramInstanceQueryParams params );

    /**
     * Validates the given ProgramInstanceQueryParams. The params is considered
     * valid if no exception are thrown and the method returns normally.
     *
     * @param params the ProgramInstanceQueryParams.
     * @throws IllegalQueryException if the given params is invalid.
     */
    void validate( ProgramInstanceQueryParams params )
        throws IllegalQueryException;

    /**
     * Retrieve program instances on a program
     *
     * @param program Program
     * @return ProgramInstance list
     */
    List<Enrollment> getProgramInstances( Program program );

    /**
     * Retrieve program instances on a program by status
     *
     * @param program Program
     * @param status Status of program-instance, include STATUS_ACTIVE,
     *        STATUS_COMPLETED and STATUS_CANCELLED
     * @return ProgramInstance list
     */
    List<Enrollment> getProgramInstances( Program program, ProgramStatus status );

    /**
     * Retrieve program instances on a TrackedEntityInstance with a status by a
     * program
     *
     * @param entityInstance TrackedEntityInstance
     * @param program Program
     * @param status Status of program-instance, include STATUS_ACTIVE,
     *        STATUS_COMPLETED and STATUS_CANCELLED
     * @return ProgramInstance list
     */
    List<Enrollment> getProgramInstances( TrackedEntityInstance entityInstance, Program program,
        ProgramStatus status );

    /**
     * Enroll a TrackedEntityInstance into a program. Must be run inside a
     * transaction.
     *
     * @param trackedEntityInstance TrackedEntityInstance
     * @param program Program
     * @param enrollmentDate The date of enrollment
     * @param incidentDate The date of incident
     * @param orgunit Organisation Unit
     * @param uid UID to use for new instance
     * @return ProgramInstance
     */
    Enrollment enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Date enrollmentDate, Date incidentDate, OrganisationUnit orgunit, String uid );

    /**
     * Enroll a TrackedEntityInstance into a program. Must be run inside a
     * transaction.
     *
     * @param trackedEntityInstance TrackedEntityInstance
     * @param program Program
     * @param enrollmentDate The date of enrollment
     * @param incidentDate The date of incident
     * @param orgunit Organisation Unit
     * @return ProgramInstance
     */
    Enrollment enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Date enrollmentDate, Date incidentDate,
        OrganisationUnit orgunit );

    /**
     * Complete a program instance. Besides, program template messages will be
     * send if it was defined to send when to complete this program
     *
     * @param enrollment ProgramInstance
     */
    void completeProgramInstanceStatus( Enrollment enrollment );

    /**
     * Set status as skipped for overdue events; Remove scheduled events
     *
     * @param enrollment ProgramInstance
     */
    void cancelProgramInstanceStatus( Enrollment enrollment );

    /**
     * Incomplete a program instance. This is is possible only if there is no
     * other program instance with active status.
     *
     * @param enrollment ProgramInstance
     */
    void incompleteProgramInstanceStatus( Enrollment enrollment );

    /**
     * Prepare a ProgramInstance for storing
     *
     * @param trackedEntityInstance TrackedEntityInstance
     * @param program Program
     * @param programStatus ProgramStatus
     * @param enrollmentDate The date of enrollment
     * @param incidentDate The date of incident
     * @param orgUnit Organisation Unit
     * @param uid UID to use for new instance
     * @return ProgramInstance
     */
    @Nonnull
    Enrollment prepareProgramInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        ProgramStatus programStatus, Date enrollmentDate, Date incidentDate, OrganisationUnit orgUnit, String uid );
}
