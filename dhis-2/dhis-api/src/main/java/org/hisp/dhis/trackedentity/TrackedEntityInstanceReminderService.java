package org.hisp.dhis.trackedentity;

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

import java.util.List;
import java.util.Set;

import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.user.User;

/**
 * @author Chau Thu Tran
 * 
 * @version $ TrackedEntityInstanceReminderService.java Aug 7, 2013 9:53:19 AM $
 */
public interface TrackedEntityInstanceReminderService
{
    /**
     * Returns a {@link TrackedEntityInstanceReminder}.
     * 
     * @param id the id of the Reminder to return.
     * 
     * @return the Reminder with the given id
     */
    TrackedEntityInstanceReminder getReminder( int id );

    /**
     * Returns a {@link TrackedEntityInstanceReminder} with a given name.
     * 
     * @param name the name of the Reminder to return.
     * 
     * @return the Reminder with the given name, or null if no match.
     */
    TrackedEntityInstanceReminder getReminderByName( String name );

    /**
     * Get message for sending to a instance from program-instance template
     * defined
     * 
     * @param instanceReminder Reminder object
     * @param programInstance ProgramInstance
     * @param format I18nFormat object
     * 
     * @return A message for an program instance.
     */
    String getMessageFromTemplate( TrackedEntityInstanceReminder instanceReminder, ProgramInstance programInstance, I18nFormat format );

    /**
     * Get message for sending to a instance from program-stage-instance template
     * defined
     * 
     * @param instanceReminder Reminder object
     * @param programStageInstance ProgramStageInstance
     * @param format I18nFormat object
     * 
     * @return A message for an program instance.
     */
    String getMessageFromTemplate( TrackedEntityInstanceReminder instanceReminder, ProgramStageInstance programStageInstance,
        I18nFormat format );

    /**
     * Retrieve the phone numbers for sending sms based on a template defined
     * 
     * @param instanceReminder TrackedEntityInstanceReminder
     * @param instance TrackedEntityInstanceReminder
     * 
     * @return The list of the phone numbers ( instance attribute phone numbers,
     *         orgunit phone numbers, phone numbers of DHIS users at the orgunit
     *         OR phone numbers of DHIS users in a user group.
     */
    Set<String> getPhoneNumbers( TrackedEntityInstanceReminder reminder, TrackedEntityInstance instance );

    /**
     * Retrieve DHIS users from a template which is defined to send messages to
     * DHIS users
     * 
     * @param instanceReminder Reminder
     * @param instance TrackedEntityInstanceReminder
     * 
     * @return The list of DHIS users
     */
    Set<User> getUsers( TrackedEntityInstanceReminder instanceReminder, TrackedEntityInstance instance );
    
    List<String> getAttributeUids( String message );
}
