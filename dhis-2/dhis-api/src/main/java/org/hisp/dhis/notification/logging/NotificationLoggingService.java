package org.hisp.dhis.notification.logging;

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

import java.util.List;

/**
 * Created by zubair@dhis2.org on 10.01.18.
 */
public interface NotificationLoggingService
{
    /***
     *
     * @param uid of the log entry
     * @return log entry if exists otherwise null.
     */
    ExternalNotificationLogEntry get( String uid );

    /**
     *
     * @param templateUid is the uid for the notification template which this log entry is associated to.
     * @return log entry if exists otherwise null.
     */
    ExternalNotificationLogEntry getByTemplateUid( String templateUid );

    /**
     *
     * @param id of the log entry
     * @return log entry if exists otherwise null.
     */
    ExternalNotificationLogEntry get( int id );

    /**
     *
     * @param key unique identifier for the log entry.
     * @return log entry if exists otherwise null.
     */

    ExternalNotificationLogEntry getByKey( String key );

    /**
     * Get all log entries.
     *
     * @return A list containing all notification log entries.
     */
    List<ExternalNotificationLogEntry> getAllLogEntries();

    /**
     *
     * @param templateUid Uid of the template which needs to be sent.
     * @return true in case there is no log entry for this template uid or template is eligible for sending more then once. Otherwise false.
     */
    boolean isValidForSending( String templateUid );

    /**
     *
     * @param entry to be saved.
     */
    void save( ExternalNotificationLogEntry entry );

    /**
     *
     * @param entry to be updated.
     */
    void update( ExternalNotificationLogEntry entry );
}
