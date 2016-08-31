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
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface ProgramStageInstanceService
{
    String ID = ProgramStageInstanceService.class.getName();

    /**
     * Adds an {@link TrackedEntityAttribute}
     *
     * @param programStageInstance The to TrackedEntityAttribute add.
     * @return A generated unique id of the added {@link TrackedEntityAttribute}
     *         .
     */
    int addProgramStageInstance( ProgramStageInstance programStageInstance );

    /**
     * Deletes a {@link TrackedEntityAttribute}.
     *
     * @param programStageInstance the TrackedEntityAttribute to delete.
     */
    void deleteProgramStageInstance( ProgramStageInstance programStageInstance );

    /**
     * Updates an {@link TrackedEntityAttribute}.
     *
     * @param programStageInstance the TrackedEntityAttribute to update.
     */
    void updateProgramStageInstance( ProgramStageInstance programStageInstance );

    boolean programStageInstanceExists(String uid);

    /**
     * Returns a {@link TrackedEntityAttribute}.
     *
     * @param id the id of the TrackedEntityAttribute to return.
     * @return the TrackedEntityAttribute with the given id
     */
    ProgramStageInstance getProgramStageInstance( int id );

    /**
     * Returns the {@link TrackedEntityAttribute} with the given UID.
     *
     * @param uid the UID.
     * @return the TrackedEntityAttribute with the given UID, or null if no
     *         match.
     */
    ProgramStageInstance getProgramStageInstance( String uid );

    /**
     * Retrieve an event on a program instance and a program stage. For
     * repeatable stage, the system returns the last event
     *
     * @param programInstance ProgramInstance
     * @param programStage ProgramStage
     * @return ProgramStageInstance
     */
    ProgramStageInstance getProgramStageInstance( ProgramInstance programInstance, ProgramStage programStage );

    /**
     * Retrieve an event list on program instance list with a certain status
     *
     * @param programInstances ProgramInstance list
     * @param status EventStatus
     * @return ProgramStageInstance list
     */
    List<ProgramStageInstance> getProgramStageInstances( Collection<ProgramInstance> programInstances,
        EventStatus status );

    /**
     * Get all events by TrackedEntityInstance, optionally filtering by
     * completed.
     *
     * @param entityInstance TrackedEntityInstance
     * @param status EventStatus
     * @return ProgramStageInstance list
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
     * Retrieve scheduled list of entityInstances registered
     *
     * @return A SchedulingProgramObject list
     */
    Collection<SchedulingProgramObject> getSendMessageEvents();

    /**
     * Complete an event. Besides, program template messages will be send if it
     * was defined to send when to complete this program
     *
     * @param programStageInstance ProgramStageInstance
     * @param sendNotifications indicates whether to send messages and notifications.
     * @param format I18nFormat
     */
    void completeProgramStageInstance( ProgramStageInstance programStageInstance, boolean sendNotifications, I18nFormat format );

    /**
     * Creates a program stage instance. Will create a program instance in case
     * the program is single event.
     *
     * @param entityInstance the tracked entity instance.
     * @param program the program.
     * @param executionDate the report date of the event.
     * @param organisationUnit the organisation unit where the event took place.
     * @return ProgramStageInstance a ProgramStageInstance object.
     */
    ProgramStageInstance createProgramStageInstance( TrackedEntityInstance entityInstance, Program program,
        Date executionDate, OrganisationUnit organisationUnit );

    /**
     * Creates a program stage instance. 
     *
     * @param programInstance the program instance.
     * @param programStage the program stage.
     * @param enrollmentDate the enrollment date.
     * @param incidentDate the date of incident.
     * @param organisationUnit the organisation unit where the event took place.
     * @return ProgramStageInstance a ProgramStageInstance object.
     */
    ProgramStageInstance createProgramStageInstance( ProgramInstance programInstance, ProgramStage programStage,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit );
}
