package org.hisp.dhis.program;

import java.util.Date;

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

import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;

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
     * @param forceDelete false if PSI should be soft deleted.
     */
    void deleteProgramStageInstance( ProgramStageInstance programStageInstance, boolean forceDelete );

    /**
     * Soft deletes a {@link ProgramStageInstance}.
     *
     * @param programStageInstance
     */
    void deleteProgramStageInstance( ProgramStageInstance programStageInstance );

    /**
     * Updates a {@link ProgramStageInstance}.
     *
     * @param programStageInstance the ProgramStageInstance to update.
     */
    void updateProgramStageInstance( ProgramStageInstance programStageInstance );

    /**
     * Checks whether a {@link ProgramStageInstance} with the given identifier
     * exists.
     * 
     * @param uid the identifier.
     */
    boolean programStageInstanceExists( String uid );

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
     * Gets the number of ProgramStageInstances added since the given number of days.
     *
     * @param days number of days.
     * @return the number of ProgramStageInstances.
     */
    long getProgramStageInstanceCount( int days );

    /**
     * Complete an event. Besides, program template messages will be sent if it was
     * defined for sending upon completion.
     *
     * @param programStageInstance the ProgramStageInstance.
     * @param skipNotifications whether to send prgram stage notifications or not.
     * @param format the I18nFormat for the notification messages.
     */
    void completeProgramStageInstance( ProgramStageInstance programStageInstance, boolean skipNotifications, I18nFormat format );

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
