package org.hisp.dhis.scheduling;

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

import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.datastatistics.DataStatisticsTask;
import org.hisp.dhis.fileresource.FileResourceCleanUpTask;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.scheduling.ScheduledTaskStatus;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    
    @Autowired
    private FileResourceCleanUpTask fileResourceCleanUpTask;
    
    @Autowired
    private DataStatisticsTask dataStatisticsTask;

    // TODO Avoid map, use bean identifier directly and get bean from context

    // -------------------------------------------------------------------------
    // SchedulingManager implementation
    // -------------------------------------------------------------------------

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        scheduleTasks();
        scheduleFixedTasks();
    }
    
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
    
    /**
     * Schedules fixed tasks, i.e. tasks which are required for various system
     * functions to work.
     */
    private void scheduleFixedTasks()
    {
        scheduler.scheduleTask( FileResourceCleanUpTask.KEY_TASK, fileResourceCleanUpTask, Scheduler.CRON_DAILY_2AM );
        scheduler.scheduleTask( DataStatisticsTask.KEY_TASK, dataStatisticsTask, Scheduler.CRON_DAILY_2AM );
    }
    
    @Override
    public void scheduleTasks( ListMap<String, String> cronKeyMap )
    {
        systemSettingManager.saveSystemSetting( SettingKey.SCHEDULED_TASKS, new ListMap<>( cronKeyMap ) );
        
        scheduleTasks();
    }
    
    @Override
    public void stopTasks()
    {
        systemSettingManager.deleteSystemSetting( SettingKey.METADATA_SYNC_CRON);
        systemSettingManager.saveSystemSetting( SettingKey.SCHEDULED_TASKS, null );
        
        scheduler.stopAllTasks();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ListMap<String, String> getCronKeyMap()
    {
        return (ListMap<String, String>) systemSettingManager.getSystemSetting( SettingKey.SCHEDULED_TASKS, new ListMap<String, String>() );
    }

    @Override
    public String getCronForTask( final String taskKey )
    {
        return getCronKeyMap().entrySet().stream()
            .filter( entry -> entry.getValue().contains( taskKey ) )
            .findAny()
            .map( Map.Entry::getKey )
            .orElse( null );
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
    public ScheduledTaskStatus getTaskStatus()
    {
        ListMap<String, String> cronKeyMap = getCronKeyMap();

        if ( cronKeyMap.size() == 0 )
        {
            return ScheduledTaskStatus.NOT_STARTED;
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

    @Override
    public void executeTask( String taskKey )
    {
        Runnable task = tasks.get( taskKey );

        if ( task != null && !isTaskInProgress( taskKey ) )
        {
            scheduler.executeTask( taskKey, task );
        }
    }

    @Override
    public boolean isTaskInProgress(String taskKey)
    {
        return ScheduledTaskStatus.RUNNING == scheduler.getCurrentTaskStatus( taskKey );
    }

}
