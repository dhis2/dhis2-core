package org.hisp.dhis.system.notification;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.collection.TaskLocalList;
import org.hisp.dhis.system.collection.TaskLocalMap;

/**
 * @author Lars Helge Overland
 */
public class InMemoryNotifier
    implements Notifier
{
    private static final Log log = LogFactory.getLog( InMemoryNotifier.class );
    
    private static final int MAX_SIZE = 75;
    
    private TaskLocalList<Notification> notifications;
    
    private TaskLocalMap<TaskCategory, Object> taskSummaries;
    
    @PostConstruct
    public void init()
    {
        notifications = new TaskLocalList<>();
        taskSummaries = new TaskLocalMap<>();
    }

    // -------------------------------------------------------------------------
    // Notifier implementation
    // -------------------------------------------------------------------------

    @Override
    public Notifier notify( TaskId id, String message )
    {
        return notify( id, NotificationLevel.INFO, message, false );
    }
    
    @Override
    public Notifier notify( TaskId id, NotificationLevel level, String message, boolean completed )
    {
        if ( id != null )
        {
            Notification notification = new Notification( level, id.getCategory(), new Date(), message, completed );
        
            notifications.get( id ).add( 0, notification );
            
            if ( notifications.get( id ).size() > MAX_SIZE )
            {
                notifications.get( id ).remove( MAX_SIZE );
            }
            
            log.info( notification );
        }
        
        return this;
    }

    @Override
    public List<Notification> getNotifications( TaskId id, String lastUid )
    {
        List<Notification> list = new ArrayList<>();
        
        if ( id != null )
        {
            for ( Notification notification : notifications.get( id ) )
            {
                if ( lastUid != null && lastUid.equals( notification.getUid() ) )
                {
                    break;
                }
                
                list.add( notification );
            }
        }
        
        return list;
    }
    
    @Override
    public Notifier clear( TaskId id )
    {
        if ( id != null )
        {
            notifications.clear( id );
            taskSummaries.get( id ).remove( id.getCategory() );
        }
        
        return this;
    }
    
    @Override
    public Notifier addTaskSummary( TaskId id, Object taskSummary )
    {
        if ( id != null )
        {
            taskSummaries.get( id ).put( id.getCategory(), taskSummary );
        }
        
        return this;
    }

    @Override
    public Object getTaskSummary( TaskId id )
    {
        if ( id != null )
        {
            return taskSummaries.get( id ).get( id.getCategory() );
        }
        
        return null;
    }
}
