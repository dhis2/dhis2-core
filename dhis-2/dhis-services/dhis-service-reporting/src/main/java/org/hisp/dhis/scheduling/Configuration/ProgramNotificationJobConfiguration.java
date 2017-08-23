package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.program.notification.ProgramNotificationService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class ProgramNotificationJobConfiguration extends JobConfiguration
{
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
    public void run()
    {
        SecurityContextHolder.clearContext(); // No security context

        final Clock clock = new Clock().startClock();

        notifier.notify( taskId, "Generating and sending scheduled program notifications" );

        try
        {
            runInternal();

            notifier.notify( taskId, NotificationLevel.INFO, "Generated and sent scheduled program notifications: " + clock.time(), true );
        }
        catch ( RuntimeException ex )
        {
            notifier.notify( taskId, NotificationLevel.ERROR, "Process failed: " + ex.getMessage(), true );

            messageService.sendSystemErrorNotification( "Generating and sending scheduled program notifications failed", ex );

            throw ex;
        }

        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_SCHEDULED_PROGRAM_NOTIFICATIONS, new Date( clock.getStartTime() ) );
    }

    private void runInternal()
    {
        // Today at 00:00:00
        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.HOUR, 0 );

        programNotificationService.sendScheduledNotificationsForDay( calendar.getTime() );
    }
}
