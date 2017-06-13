package org.hisp.dhis.program.notification;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;

import java.util.Date;

/**
 * @author Halvdan Hoem Grelland
 */
public interface ProgramNotificationService
{
    /**
     * Send all scheduled notifications for the given day.
     *
     * Queries for any upcoming ProgramStageInstances or ProgramInstances
     * which have a {@link ProgramNotificationTemplate} scheduled for the
     * given day, creates the messages and immediately dispatches them.
     *
     * Potentially a time consuming job, depending on the amount of
     * configured notifications, the amount of recipients, the message
     * types (SMS, email, dhis message) and the amount of events resolved
     * by the query.
     *
     * Due to the time consuming nature of the process this method
     * should be wrapped in an asynchronous task.
     *
     * @param day the Date representing the day relative to the
     *             scheduled notifications for which to send messages.
     */
    void sendScheduledNotificationsForDay( Date day );

    /**
     * Send completion notifications for the ProgramStageInstance.
     * If the ProgramStage is not configured with suitable
     * {@link ProgramNotificationTemplate templates}, nothing will happen.
     *
     * @param programStageInstance the ProgramStageInstance.
     */
    void sendCompletionNotifications( ProgramStageInstance programStageInstance );

    /**
     * Send completion notifications for the ProgramInstance.
     * If the Program is not configured with suitable
     * {@link ProgramNotificationTemplate templates}, nothing will happen.
     *
     * @param programInstance the ProgramInstance.
     */
    void sendCompletionNotifications( ProgramInstance programInstance );

    /**
     * Send enrollment notifications for the ProgramInstance.
     * If the Program is not configured with suitable
     * {@link ProgramNotificationTemplate templates}, nothing will happen.

     * @param programInstance the ProgramInstance.
     */
    void sendEnrollmentNotifications( ProgramInstance programInstance );
}
