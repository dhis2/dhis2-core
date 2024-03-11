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

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface ProgramStageInstanceStore
    extends IdentifiableObjectStore<ProgramStageInstance>
{
    String ID = ProgramStageInstanceStore.class.getName();

    /**
     * Retrieve an event on a program instance and a program stage. For
     * repeatable stage, the system returns the last event
     *
     * @param programInstance ProgramInstance
     * @param programStage    ProgramStage
     * @return ProgramStageInstance
     */
    ProgramStageInstance get( ProgramInstance programInstance, ProgramStage programStage );

    /**
     * Retrieve an event list on program instance list with a certain status
     *
     * @param programInstances ProgramInstance list
     * @param status           EventStatus
     * @return ProgramStageInstance list
     */
    List<ProgramStageInstance> get( Collection<ProgramInstance> programInstances, EventStatus status );

    /**
     * Get all events by TrackedEntityInstance, optionally filtering by completed.
     *
     * @param entityInstance TrackedEntityInstance
     * @param status         EventStatus
     * @return ProgramStageInstance list
     */
    List<ProgramStageInstance> get( TrackedEntityInstance entityInstance, EventStatus status );

    /**
     * Get the number of ProgramStageInstances updates since the given Date.
     *
     * @param time the time.
     * @return the number of ProgramStageInstances.
     */
    long getProgramStageInstanceCountLastUpdatedAfter( Date time );

    /**
     * Checks for the existence of a PSI by UID. The deleted PSIs are not taken into account.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean exists( String uid );

    /**
     * Checks for the existence of a PSI by UID. It takes into account also the deleted PSIs.
     *
     * @param uid PSI UID to check for
     * @return true/false depending on result
     */
    boolean existsIncludingDeleted( String uid );

    /**
     * Get all ProgramStageInstances which have notifications with the given ProgramNotificationTemplate scheduled on the given date.
     *
     * @param template         the template.
     * @param notificationDate the Date for which the notification is scheduled.
     * @return a list of ProgramStageInstance.
     */
    List<ProgramStageInstance> getWithScheduledNotifications( ProgramNotificationTemplate template, Date notificationDate );

    /**
     * Set lastSynchronized timestamp to provided timestamp for provided PSIs
     *
     * @param programStageInstanceUIDs UIDs of ProgramStageInstances where the lastSynchronized flag should be updated
     * @param lastSynchronized         The date of last successful sync
     */
    void updateProgramStageInstancesSyncTimestamp( List<String> programStageInstanceUIDs, Date lastSynchronized );
}
