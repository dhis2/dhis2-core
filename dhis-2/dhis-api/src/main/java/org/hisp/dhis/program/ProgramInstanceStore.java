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

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Abyot Asalefew
 */
public interface ProgramInstanceStore
    extends IdentifiableObjectStore<Enrollment>
{
    String ID = ProgramInstanceStore.class.getName();

    /**
     * Count all program instances by PI query params.
     *
     * @param params ProgramInstanceQueryParams to use
     * @return Count of matching PIs
     */
    int countProgramInstances( ProgramInstanceQueryParams params );

    /**
     * Get all program instances by PI query params.
     *
     * @param params ProgramInstanceQueryParams to use
     * @return PIs matching params
     */
    List<Enrollment> getProgramInstances( ProgramInstanceQueryParams params );

    /**
     * Retrieve program instances on a program
     *
     * @param program Program
     * @return ProgramInstance list
     */
    List<Enrollment> get( Program program );

    /**
     * Retrieve program instances on a program by status
     *
     * @param program Program
     * @param status Status of program-instance, include STATUS_ACTIVE,
     *        STATUS_COMPLETED and STATUS_CANCELLED
     * @return ProgramInstance list
     */
    List<Enrollment> get( Program program, ProgramStatus status );

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
    List<Enrollment> get( TrackedEntityInstance entityInstance, Program program, ProgramStatus status );

    /**
     * Checks for the existence of a PI by UID, Deleted PIs are not taken into
     * account.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean exists( String uid );

    /**
     * Checks for the existence of a PI by UID. Takes into account also the
     * deleted PIs.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean existsIncludingDeleted( String uid );

    /**
     * Returns UIDs of existing ProgramInstances (including deleted) from the
     * provided UIDs
     *
     * @param uids PI UIDs to check
     * @return List containing UIDs of existing PIs (including deleted)
     */
    List<String> getUidsIncludingDeleted( List<String> uids );

    /**
     * Fetches ProgramInstances matching the given list of UIDs
     *
     * @param uids a List of UID
     * @return a List containing the ProgramInstances matching the given
     *         parameters list
     */
    List<Enrollment> getIncludingDeleted( List<String> uids );

    /**
     * Get all ProgramInstances which have notifications with the given
     * ProgramNotificationTemplate scheduled on the given date.
     *
     * @param template the template.
     * @param notificationDate the Date for which the notification is scheduled.
     * @return a list of ProgramInstance.
     */
    List<Enrollment> getWithScheduledNotifications( ProgramNotificationTemplate template, Date notificationDate );

    /**
     * Return all program instance linked to programs.
     *
     * @param programs Programs to fetch by
     * @return List of all PIs that that are linked to programs
     */
    List<Enrollment> getByPrograms( List<Program> programs );

    /**
     * Return all program instance by type.
     * <p>
     * Warning: this is meant to be used for WITHOUT_REGISTRATION programs only,
     * be careful if you need it for other uses.
     *
     * @param type ProgramType to fetch by
     * @return List of all PIs that matches the wanted type
     */
    List<Enrollment> getByType( ProgramType type );

    /**
     * Hard deletes a {@link Enrollment}.
     *
     * @param enrollment the ProgramInstance to delete.
     */
    void hardDelete( Enrollment enrollment );

    /**
     * Executes a query to fetch all {@see ProgramInstance} matching the
     * Program/TrackedEntityInstance list.
     *
     * Resulting SQL query:
     *
     * <pre>
     * {@code
     *  select programinstanceid, programid, trackedentityinstanceid
     *      from programinstance
     *      where (programid = 726 and trackedentityinstanceid = 19 and status = 'ACTIVE')
     *         or (programid = 726 and trackedentityinstanceid = 18 and status = 'ACTIVE')
     *         or (programid = 726 and trackedentityinstanceid = 17 and status = 'ACTIVE')
     * }
     * </pre>
     *
     * @param programTeiPair a List of Pair, where the left side is a
     *        {@see Program} and the right side is a
     *        {@see TrackedEntityInstance}
     * @param programStatus filter on the status of all the Program
     * @return a List of {@see ProgramInstance}
     */
    List<Enrollment> getByProgramAndTrackedEntityInstance(
        List<Pair<Program, TrackedEntityInstance>> programTeiPair, ProgramStatus programStatus );
}
