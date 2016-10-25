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
import org.hisp.dhis.system.scheduling.ScheduledTaskStatus;

import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public interface SchedulingManager
{
    String TASK_RESOURCE_TABLE = "resourceTableTask";
    String TASK_RESOURCE_TABLE_15_MINS = "resourceTable15MinTask";
    String TASK_DATAMART_LAST_YEAR = "dataMartLastYearTask";
    String TASK_ANALYTICS_ALL = "analyticsAllTask";
    String TASK_ANALYTICS_LAST_3_YEARS = "analyticsLast3YearsTask";
    String TASK_MONITORING_LAST_DAY = "monitoringLastDayTask";
    String TASK_DATA_SYNCH = "dataSynchTask";
    String TASK_META_DATA_SYNC = "metaDataSyncTask";
    String TASK_SEND_SCHEDULED_SMS_NOW = "sendScheduledMessageTaskNow";
    String TASK_SCHEDULED_PROGRAM_NOTIFICATIONS = "scheduledProgramNotificationsTask";
    
    /**
     * Schedules all tasks.
     */
    void scheduleTasks();
    
    /**
     * Execute the Task.
     */
    void executeTask(String taskKey);

    /**
     * Schedules the given tasks. The task map will replace the currently scheduled
     * tasks.
     * 
     * @param cronKeyMap a mapping of cron expressions and list of task keys. A
     * task key refers to a bean identifier in the application context. The bean
     * must implement Runnable in order to be scheduled for execution.
     */
    void scheduleTasks( ListMap<String, String> cronKeyMap );
    
    /**
     * Stops all tasks.
     */
    void stopTasks();

    /**
     * Resolve the cron expression mapped for the given task key, or null if none.
     *
     * @param taskKey the key of the task, not null.
     * @return the cron for the task or null.
     */
    String getCronForTask( final String taskKey );

    /**
     * Gets a mapping of cron expressions and list of task keys for all scheduled
     * tasks.
     */
    ListMap<String, String> getCronKeyMap();

    /**
     * Gets all keys currently scheduled for any task.
     */
    Set<String> getScheduledKeys();
    
    /**
     * Gets the task status.
     */
    ScheduledTaskStatus getTaskStatus();

    /**
     *
     * Returns the status of the currently executing task.
     */
    boolean isTaskInProgress(String taskKey);
}
