package org.hisp.dhis.program.notification;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.security.NoSecurityContextRunnable;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.DateUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Halvdan Hoem Grelland
 */
public class ProgramNotificationTask
    extends NoSecurityContextRunnable
{
    public static final String KEY_TASK = "scheduledProgramNotificationsTask";

    private static final Log log = LogFactory.getLog( ProgramNotificationTask.class );

    @Autowired
    private ProgramNotificationService programNotificationService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private MessageService messageService;

    @Autowired
    private Notifier notifier;

    private TaskId taskId;

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }

    // -------------------------------------------------------------------------
    // Runnable implementation
    // -------------------------------------------------------------------------

    @Override
    public void call()
    {
        final Clock clock = new Clock().startClock();

        notifier.notify( taskId, "Generating and sending scheduled program notifications" );
        
        try
        {
            runInternal();

            notifier.notify( taskId, NotificationLevel.INFO, "Generated and sent scheduled program notifications: " + clock.time(), true );
        }
        catch ( RuntimeException rte )
        {
            String title = (String) systemSettingManager.getSystemSetting( SettingKey.APPLICATION_TITLE );

            notifier.notify( taskId, NotificationLevel.ERROR, "Process failed: " + rte.getMessage(), true );

            messageService.sendSystemNotification(
                "Generating and sending scheduled program notifications failed",
                "The process failed, please check the server logs. Time of failure: " + new DateTime().toString() + ". " +
                "System:" + title + " " +
                "Message: " + rte.getMessage() + " " +
                "Cause: " + DebugUtils.getStackTrace( rte.getCause() )
            );

            throw rte;
        }

        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_SCHEDULED_PROGRAM_NOTIFICATIONS, new DateTime( clock.getStartTime() ) );
    }

    private void runInternal()
    {
        programNotificationService.sendScheduledNotificationsForDay( DateUtils.getDateForTomorrow( 0 ) );
    }
}
