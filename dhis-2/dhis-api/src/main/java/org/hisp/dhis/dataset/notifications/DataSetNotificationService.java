package org.hisp.dhis.dataset.notifications;

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

import org.hisp.dhis.dataset.CompleteDataSetRegistration;

import java.util.Date;

/**
 * Created by zubair on 04.07.17.
 */
public interface DataSetNotificationService
{
    /**
     * Send all scheduled dataset notifications for the given day.
     * These notifications could be reminders for upcoming datasets submissions
     * or it could be reminders for datasets where submissions are overdue.
     * @param day the Date representing the day relative to the
     *             scheduled notifications for which to send messages.
     */
    void sendScheduledDataSetNotificationsForDay( Date day );

    /**
     * Send completion notifications when a DataSet is completed.
     * If the DataSet is not configured with suitable
     * {@link DataSetNotificationTemplate templates}, nothing will happen.
     *
     * @param completeDataSetRegistration the CompleteDataSetRegistration.
     */
    void sendCompleteDataSetNotifications( CompleteDataSetRegistration completeDataSetRegistration );
}
