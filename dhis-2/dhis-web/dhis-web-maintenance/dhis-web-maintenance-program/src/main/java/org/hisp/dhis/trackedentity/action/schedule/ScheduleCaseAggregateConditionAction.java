package org.hisp.dhis.trackedentity.action.schedule;

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

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.scheduling.CaseAggregateConditionSchedulingManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.scheduling.ScheduledTaskStatus;
import org.hisp.dhis.system.scheduling.Scheduler;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * 
 * @version ScheduleCaseAggregateConditionAction.java 11:14:34 AM Oct 10, 2012 $
 */
public class ScheduleCaseAggregateConditionAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CaseAggregateConditionSchedulingManager schedulingManager;

    public void setSchedulingManager( CaseAggregateConditionSchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private boolean execute;

    public void setExecute( boolean execute )
    {
        this.execute = execute;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private ScheduledTaskStatus status;

    public ScheduledTaskStatus getStatus()
    {
        return status;
    }

    private boolean running;

    public boolean isRunning()
    {
        return running;
    }

    private String taskStrategy;

    public void setTaskStrategy( String taskStrategy )
    {
        this.taskStrategy = taskStrategy;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------
 
    @Override
    public String execute()
        throws Exception
    {
        if ( execute )
        {
            schedulingManager.executeTasks();
        }
        else
        {
            if ( ScheduledTaskStatus.RUNNING.equals( schedulingManager.getTaskStatus() ) )
            {
                schedulingManager.stopTasks();
            }
            else
            {
                systemSettingManager.saveSystemSetting( SettingKey.SCHEDULE_AGGREGATE_QUERY_BUILDER_TASK_STRATEGY,
                    taskStrategy );

                Map<String, String> keyCronMap = new HashMap<>();

                keyCronMap.put( CaseAggregateConditionSchedulingManager.TASK_AGGREGATE_QUERY_BUILDER,
                    Scheduler.CRON_DAILY_11PM );

                schedulingManager.scheduleTasks( keyCronMap );
            }
        }

        status = schedulingManager.getTaskStatus();

        running = ScheduledTaskStatus.RUNNING.equals( status );

        return SUCCESS;
    }
}
