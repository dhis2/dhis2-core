package org.hisp.dhis.program;

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

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface ProgramInstanceService
{
    String ID = ProgramInstanceService.class.getName();

    /**
     * Adds an {@link ProgramInstance}
     *
     * @param programInstance The to ProgramInstance add.
     * @return A generated unique id of the added {@link ProgramInstance}.
     */
    int addProgramInstance( ProgramInstance programInstance );

    /**
     * Deletes a {@link ProgramInstance}.
     *
     * @param programInstance the ProgramInstance to delete.
     */
    void deleteProgramInstance( ProgramInstance programInstance );

    /**
     * Updates an {@link ProgramInstance}.
     *
     * @param programInstance the ProgramInstance to update.
     */
    void updateProgramInstance( ProgramInstance programInstance );

    /**
     * Returns a {@link ProgramInstance}.
     *
     * @param id the id of the ProgramInstance to return.
     * @return the ProgramInstance with the given id
     */
    ProgramInstance getProgramInstance( int id );

    /**
     * Returns the {@link ProgramInstance} with the given UID.
     *
     * @param uid the UID.
     * @return the ProgramInstance with the given UID, or null if no match.
     */
    ProgramInstance getProgramInstance( String uid );

    /**
     * Checks for the existence of a PI by UID
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean programInstanceExists( String uid );

    /**
     * Returns a ProgramInstanceQueryParams based on the given input.
     *
     * @param ou                    the set of organisation unit identifiers.
     * @param ouMode                the OrganisationUnitSelectionMode.
     * @param lastUpdated           the last updated for PI.
     * @param program               the Program identifier.
     * @param programStatus         the ProgramStatus in the given program.
     * @param programStartDate      the start date for enrollment in the given
     *                              Program.
     * @param programEndDate        the end date for enrollment in the given Program.
     * @param trackedEntity         the TrackedEntity uid.
     * @param trackedEntityInstance the TrackedEntityInstance uid.
     * @param followUp              indicates follow up status in the given Program.
     * @param page                  the page number.
     * @param pageSize              the page size.
     * @param totalPages            indicates whether to include the total number of pages.
     * @param skipPaging            whether to skip paging.
     * @return a ProgramInstanceQueryParams.
     */
    ProgramInstanceQueryParams getFromUrl( Set<String> ou, OrganisationUnitSelectionMode ouMode, Date lastUpdated, String program,
        ProgramStatus programStatus, Date programStartDate, Date programEndDate, String trackedEntity, String trackedEntityInstance,
        Boolean followUp, Integer page, Integer pageSize, boolean totalPages, boolean skipPaging );

    /**
     * Returns a list with program instance values based on the given
     * ProgramInstanceQueryParams.
     *
     * @param params the ProgramInstanceQueryParams.
     * @return List of PIs matching the params
     */
    List<ProgramInstance> getProgramInstances( ProgramInstanceQueryParams params );

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
     * Validates the given ProgramInstanceQueryParams. The params is
     * considered valid if no exception are thrown and the method returns
     * normally.
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
    List<ProgramInstance> getProgramInstances( Program program );

    /**
     * Retrieve program instances on program list
     *
     * @param programs Program list
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( Collection<Program> programs );

    /**
     * Retrieve program instances of whom registered in to a orgunit from
     * program list
     *
     * @param programs         Program list
     * @param organisationUnit Organisation Unit
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( Collection<Program> programs, OrganisationUnit organisationUnit );

    /**
     * Retrieve program instances of whom registered in to a orgunit from
     * program list with a certain status
     *
     * @param programs         Program list
     * @param organisationUnit Organisation Unit
     * @param status           Status of program-instance, include STATUS_ACTIVE,
     *                         STATUS_COMPLETED and STATUS_CANCELLED
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( Collection<Program> programs, OrganisationUnit organisationUnit, ProgramStatus status );

    /**
     * Retrieve program instances on a program by status
     *
     * @param program Program
     * @param status  Status of program-instance, include STATUS_ACTIVE,
     *                STATUS_COMPLETED and STATUS_CANCELLED
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( Program program, ProgramStatus status );

    /**
     * Retrieve program instances on a program list by status
     *
     * @param programs Program list
     * @param status   Status of program-instance, include STATUS_ACTIVE,
     *                 STATUS_COMPLETED and STATUS_CANCELLED
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( Collection<Program> programs, ProgramStatus status );

    /**
     * Retrieve program instances on a TrackedEntityInstance by a status
     *
     * @param entityInstance TrackedEntityInstance
     * @param status         Status of program-instance, include STATUS_ACTIVE,
     *                       STATUS_COMPLETED and STATUS_CANCELLED
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( TrackedEntityInstance entityInstance, ProgramStatus status );

    /**
     * Retrieve program instances on a TrackedEntityInstance by a program
     *
     * @param entityInstance TrackedEntityInstance
     * @param program        Program
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( TrackedEntityInstance entityInstance, Program program );

    /**
     * Retrieve program instances on a TrackedEntityInstance with a status by a program
     *
     * @param entityInstance TrackedEntityInstance
     * @param program        Program
     * @param status         Status of program-instance, include STATUS_ACTIVE,
     *                       STATUS_COMPLETED and STATUS_CANCELLED
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( TrackedEntityInstance entityInstance, Program program, ProgramStatus status );

    /**
     * Retrieve program instances with active status on an orgunit by a program
     * with result limited
     *
     * @param program          Program
     * @param organisationUnit Organisation Unit
     * @param min              First result
     * @param max              Maximum results
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( Program program, OrganisationUnit organisationUnit, Integer min, Integer max );

    /**
     * Retrieve program instances with active status on an organisation unit by
     * a program for a certain period with result limited
     *
     * @param program    Program
     * @param orgunitIds Organisation Units
     * @param startDate  The start date for retrieving on enrollment-date
     * @param endDate    The end date for retrieving on enrollment-date
     * @param min        First result
     * @param max        Maximum results
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstances( Program program, Collection<Integer> orgunitIds, Date startDate,
        Date endDate, Integer min, Integer max );

    /**
     * Get the number of program instances which are active status and
     * registered in a certain organisation unit by a program for a certain period
     *
     * @param program    Program
     * @param orgunitIds Organisation Units
     * @param startDate  The start date for retrieving on enrollment-date
     * @param endDate    The end date for retrieving on enrollment-date
     * @return ProgramInstance list
     */
    int countProgramInstances( Program program, Collection<Integer> orgunitIds, Date startDate, Date endDate );

    /**
     * Retrieve program instances with a certain status on a program and an
     * orgunit ids list for a period
     *
     * @param status     of program-instance, include STATUS_ACTIVE,
     *                   STATUS_COMPLETED and STATUS_CANCELLED
     * @param program    ProgramInstance
     * @param orgunitIds A list of orgunit ids
     * @param startDate  The start date for retrieving on enrollment-date
     * @param endDate    The end date for retrieving on enrollment-date
     * @return ProgramInstance list
     */
    List<ProgramInstance> getProgramInstancesByStatus( ProgramStatus status, Program program,
        Collection<Integer> orgunitIds, Date startDate, Date endDate );

    /**
     * Get the number of program instances of a program which have a certain
     * status and an orgunit ids list for a period
     *
     * @param status     of program-instance, include STATUS_ACTIVE,
     *                   STATUS_COMPLETED and STATUS_CANCELLED
     * @param program    ProgramInstance
     * @param orgunitIds A list of orgunit ids
     * @param startDate  The start date for retrieving on enrollment-date
     * @param endDate    The end date for retrieving on enrollment-date
     * @return A number
     */
    int countProgramInstancesByStatus( ProgramStatus status, Program program, Collection<Integer> orgunitIds, Date startDate,
        Date endDate );

    /**
     * Retrieve scheduled list of entityInstances registered
     *
     * @return A SchedulingProgramObject list
     */
    Collection<SchedulingProgramObject> getScheduledMessages();

    /**
     * Enroll a TrackedEntityInstance into a program. Must be run inside a transaction.
     *
     * @param trackedEntityInstance TrackedEntityInstance
     * @param program               Program
     * @param enrollmentDate        The date of enrollment
     * @param incidentDate          The date of incident
     * @param orgunit               Organisation Unit
     * @param uid                   UID to use for new instance
     * @return ProgramInstance
     */
    ProgramInstance enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Date enrollmentDate, Date incidentDate, OrganisationUnit orgunit, String uid );

    /**
     * Enroll a TrackedEntityInstance into a program. Must be run inside a transaction.
     *
     * @param trackedEntityInstance TrackedEntityInstance
     * @param program               Program
     * @param enrollmentDate        The date of enrollment
     * @param incidentDate          The date of incident
     * @param orgunit               Organisation Unit
     * @return ProgramInstance
     */
    ProgramInstance enrollTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, Program program, Date enrollmentDate, Date incidentDate,
        OrganisationUnit orgunit );

    /**
     * Check a program instance if it can be completed automatically. If there
     * is some event of this program-isntance uncompleted or this program has
     * any repeatable stage, then this program cannot be completed automatically
     *
     * @param programInstance ProgramInstance
     * @return True/False value
     */
    boolean canAutoCompleteProgramInstanceStatus( ProgramInstance programInstance );

    /**
     * Complete a program instance. Besides, program template messages will be
     * send if it was defined to send when to complete this program
     *
     * @param programInstance ProgramInstance
     */
    void completeProgramInstanceStatus( ProgramInstance programInstance );

    /**
     * Set status as skipped for overdue events; Remove scheduled events
     *
     * @param programInstance ProgramInstance
     */
    void cancelProgramInstanceStatus( ProgramInstance programInstance );
    
    /**
     * Incomplete a program instance. This is is possible only if there is
     * no other program instance with active status.
     *
     * @param programInstance ProgramInstance
     */
    void incompleteProgramInstanceStatus( ProgramInstance programInstance );
}
