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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface ProgramStageInstanceService
{
    String ID = ProgramStageInstanceService.class.getName();

    /**
     * Adds a {@link ProgramStageInstance}
     *
     * @param programStageInstance The ProgramStageInstance to add.
     * @return A generated unique id of the added {@link ProgramStageInstance}.
     */
    int addProgramStageInstance( ProgramStageInstance programStageInstance );

    /**
     * Deletes a {@link ProgramStageInstance}.
     *
     * @param programStageInstance the ProgramStageInstance to delete.
     */
    void deleteProgramStageInstance( ProgramStageInstance programStageInstance );

    /**
     * Updates a {@link ProgramStageInstance}.
     *
     * @param programStageInstance the ProgramStageInstance to update.
     */
    void updateProgramStageInstance( ProgramStageInstance programStageInstance );

    boolean programStageInstanceExists(String uid);

    /**
     * Returns a {@link ProgramStageInstance}.
     *
     * @param id the id of the ProgramStageInstance to return.
     * @return the ProgramStageInstance with the given id.
     */
    ProgramStageInstance getProgramStageInstance( int id );

    /**
     * Returns the {@link ProgramStageInstance} with the given UID.
     *
     * @param uid the UID.
     * @return the ProgramStageInstance with the given UID, or null if no
     *         match.
     */
    ProgramStageInstance getProgramStageInstance( String uid );

    /**
     * Retrieve an event on a ProgramInstance and a ProgramStage. For
     * repeatable stages, the system returns the last event.
     *
     * @param programInstance the ProgramInstance.
     * @param programStage the ProgramStage.
     * @return the ProgramStageInstance corresponding to the given
     *          programInstance and ProgramStage, or null if no match.
     */
    ProgramStageInstance getProgramStageInstance( ProgramInstance programInstance, ProgramStage programStage );

    /**
     * Retrieve an event list on a list of ProgramInstances with a certain status.
     *
     * @param programInstances the ProgramInstance list.
     * @param status the EventStatus.
     * @return a list of all ProgramStageInstances for the given ProgramInstances
     *          and EventStatus.
     */
    List<ProgramStageInstance> getProgramStageInstances( Collection<ProgramInstance> programInstances,
        EventStatus status );

    /**
     * Get all events by TrackedEntityInstance, optionally filtering by
     * completed.
     *
     * @param entityInstance the TrackedEntityInstance.
     * @param status the EventStatus.
     * @return a list of all ProgramStageInstance for the given
     *          TrackedEntityInstance and EventStatus.
     */
    List<ProgramStageInstance> getProgramStageInstances( TrackedEntityInstance entityInstance, EventStatus status );

    /**
     * Gets the number of ProgramStageInstances added since the given number of days.
     *
     * @param days number of days.
     * @return the number of ProgramStageInstances.
     */
    long getProgramStageInstanceCount( int days );

    /**
     * Retrieve scheduled list of entityInstances registered.
     *
     * @return A list of SchedulingProgramObject.
     */
    Collection<SchedulingProgramObject> getSendMessageEvents();

    /**
     * Complete an event. Besides, program template messages will be sent if it was
     * defined for sending upon completion.
     *
     * @param programStageInstance the ProgramStageInstance.
     * @param sendNotifications whether to send messages and notifications or not.
     * @param format the I18nFormat for the notification messages.
     */
    void completeProgramStageInstance( ProgramStageInstance programStageInstance, boolean sendNotifications, I18nFormat format );

    /**
     * Creates a ProgramStageInstance. Will create a ProgramInstance in case
     * the program is single event.
     *
     * @param entityInstance the TrackedEntityInstance.
     * @param program the Program.
     * @param executionDate the report date of the event.
     * @param organisationUnit the OrganisationUnit where the event took place.
     * @return ProgramStageInstance the ProgramStageInstance which was created.
     */
    ProgramStageInstance createProgramStageInstance( TrackedEntityInstance entityInstance, Program program,
        Date executionDate, OrganisationUnit organisationUnit );

    /**
     * Creates a program stage instance. 
     *
     * @param programInstance the ProgramInstance.
     * @param programStage the ProgramStage.
     * @param enrollmentDate the enrollment date.
     * @param incidentDate date of the incident.
     * @param organisationUnit the OrganisationUnit where the event took place.
     * @return ProgramStageInstance the ProgramStageInstance which was created.
     */
    ProgramStageInstance createProgramStageInstance( ProgramInstance programInstance, ProgramStage programStage,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit );
}
