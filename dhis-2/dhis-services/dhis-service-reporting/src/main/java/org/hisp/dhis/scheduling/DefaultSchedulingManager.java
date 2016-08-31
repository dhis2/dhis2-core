package org.hisp.dhis.scheduling;

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

import static org.hisp.dhis.system.scheduling.Scheduler.STATUS_NOT_STARTED;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.setting.Setting;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.scheduling.Scheduler;

/**
 * Cron refers to the cron expression used for scheduling. Key refers to the key
 * identifying the scheduled tasks.
 * 
 * @author Lars Helge Overland
 */
public class DefaultSchedulingManager
    implements SchedulingManager
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SystemSettingManager systemSettingManager;
    
    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private Scheduler scheduler;

    public void setScheduler( Scheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    private Map<String, Runnable> tasks = new HashMap<>();

    public void setTasks( Map<String, Runnable> tasks )
    {
        this.tasks = tasks;
    }
    
    // TODO Avoid map, use bean identifier directly and get bean from context

    // -------------------------------------------------------------------------
    // SchedulingManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void scheduleTasks()
    {        
        ListMap<String, String> cronKeyMap = getCronKeyMap();
        
        for ( String cron : cronKeyMap.keySet() )
        {
            ScheduledTasks scheduledTasks = getScheduledTasksForCron( cron, cronKeyMap );
            
            if ( !scheduledTasks.isEmpty() )
            {
                scheduler.scheduleTask( cron, scheduledTasks, cron );
            }
        }
    }
    
    @Override
    public void scheduleTasks( ListMap<String, String> cronKeyMap )
    {
        systemSettingManager.saveSystemSetting( Setting.SCHEDULED_TASKS, new ListMap<>( cronKeyMap ) );
        
        scheduleTasks();
    }
    
    @Override
    public void stopTasks()
    {
        systemSettingManager.saveSystemSetting( Setting.SCHEDULED_TASKS, null );
        
        scheduler.stopAllTasks();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ListMap<String, String> getCronKeyMap()
    {
        return (ListMap<String, String>) systemSettingManager.getSystemSetting( Setting.SCHEDULED_TASKS, new ListMap<String, String>() );
    }
    
    @Override
    public Set<String> getScheduledKeys()
    {
        ListMap<String, String> cronKeyMap = getCronKeyMap();
        
        Set<String> keys = new HashSet<>();
        
        for ( String cron : cronKeyMap.keySet() )
        {
            keys.addAll( cronKeyMap.get( cron ) );
        }
        
        return keys;
    }
    
    @Override
    public String getTaskStatus()
    {
        ListMap<String, String> cronKeyMap = getCronKeyMap();

        if ( cronKeyMap.size() == 0 )
        {
            return STATUS_NOT_STARTED;
        }
        
        String firstTask = cronKeyMap.keySet().iterator().next();
        
        return scheduler.getTaskStatus( firstTask );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a ScheduledTasks object for the given cron expression. The
     * ScheduledTasks object contains a list of tasks.
     */
    private ScheduledTasks getScheduledTasksForCron( String cron, ListMap<String, String> cronKeyMap )
    {
        ScheduledTasks scheduledTasks = new ScheduledTasks();
        
        for ( String key : cronKeyMap.get( cron ) )
        {
            scheduledTasks.addTask( tasks.get( key ) );
        }
        
        return scheduledTasks;
    }
}
