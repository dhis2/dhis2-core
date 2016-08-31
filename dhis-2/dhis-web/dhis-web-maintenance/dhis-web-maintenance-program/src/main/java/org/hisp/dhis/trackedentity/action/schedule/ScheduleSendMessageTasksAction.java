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

import org.hisp.dhis.scheduling.ProgramSchedulingManager;
import org.hisp.dhis.scheduling.SendScheduledMessageTask;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.scheduling.ScheduledTaskStatus;
import org.hisp.dhis.user.CurrentUserService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * 
 * @version ScheduleSendMessageTasksAction.java 12:10:35 PM Sep 10, 2012 $
 */
public class ScheduleSendMessageTasksAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private ProgramSchedulingManager schedulingManager;

    public void setSchedulingManager( ProgramSchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private SendScheduledMessageTask sendMessageScheduled;

    public void setSendMessageScheduled( SendScheduledMessageTask sendMessageScheduled )
    {
        this.sendMessageScheduled = sendMessageScheduled;
    }

    private Notifier notifier;

    public void setNotifier( Notifier notifier )
    {
        this.notifier = notifier;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private boolean execute;

    public void setExecute( boolean execute )
    {
        this.execute = execute;
    }

    private String timeSendingMessage;

    public void setTimeSendingMessage( String timeSendingMessage )
    {
        this.timeSendingMessage = timeSendingMessage;
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

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        TaskId taskId = new TaskId( TaskCategory.SENDING_REMINDER_MESSAGE, currentUserService.getCurrentUser() );
        notifier.clear( taskId );
        sendMessageScheduled.setTaskId( taskId );

        systemSettingManager.saveSystemSetting( SettingKey.TIME_FOR_SENDING_MESSAGE, timeSendingMessage );

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
                Map<String, String> keyCronMap = new HashMap<>();
                String time = (String) systemSettingManager.getSystemSetting( SettingKey.TIME_FOR_SENDING_MESSAGE );

                // Schedule for sending messages
                String[] infor = time.split( ":" );
                String hour = infor[0].trim();
                String minute = infor[1].trim();

                if ( hour.trim().equals( "00" ) )
                {
                    hour = "0";
                }
                if ( minute.trim().equals( "00" ) )
                {
                    minute = "0";
                }
                
                String cron = "0 " + Integer.parseInt( minute ) + " " + Integer.parseInt( hour ) + " ? * *";

                keyCronMap.put( SettingKey.SEND_MESSAGE_SCHEDULED_TASKS.getName(), cron );
                keyCronMap.put( SettingKey.SCHEDULE_MESSAGE_TASKS.getName(), "0 0 0 * * ?" );

                schedulingManager.scheduleTasks( keyCronMap );
            }
        }

        status = schedulingManager.getTaskStatus();
        running = ScheduledTaskStatus.RUNNING.equals( status );

        return SUCCESS;
    }
}
