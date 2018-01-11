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

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by zubair@dhis2.org on 10.01.18.
 */
public class DefaultNotificationLoggingService implements NotificationLoggingService
{
    @Autowired
    private NotificationLoggingStore notificationLoggingStore;

    @Override
    public ExternalNotificationLogEntry getNotificationLogEntry(String uid )
    {
        return notificationLoggingStore.getByUid( uid );
    }

    @Override
    public ExternalNotificationLogEntry getNotificationLogEntry( int id )
    {
        return notificationLoggingStore.get( id );
    }

    @Override
    public ExternalNotificationLogEntry getNotificationByKey( String key )
    {
        return null;
    }

    @Override
    public List<ExternalNotificationLogEntry> getNotificationByTriggerEvent( NotificationTriggerEvent event )
    {
        return null;
    }

    @Override
    public List<ExternalNotificationLogEntry> getAllNotificationLogEntries()
    {
        return notificationLoggingStore.getAll();
    }
}
