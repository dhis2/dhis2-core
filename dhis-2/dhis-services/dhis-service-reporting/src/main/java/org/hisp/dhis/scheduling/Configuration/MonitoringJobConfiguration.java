package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.validation.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class MonitoringJobConfiguration extends JobConfiguration
{
    @Autowired
    private ValidationService validationService;

    @Autowired
    private Notifier notifier;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SystemSettingManager systemSettingManager;

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
        final Date startTime = new Date();

        notifier.clear( taskId ).notify( taskId, "Monitoring data" );

        try
        {
            validationService.startScheduledValidationAnalysis();

            notifier.notify( taskId, INFO, "Monitoring process done", true );
        }
        catch ( RuntimeException ex )
        {
            notifier.notify( taskId, ERROR, "Process failed: " + ex.getMessage(), true );

            messageService.sendSystemErrorNotification( "Monitoring process failed", ex );

            throw ex;
        }

        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_MONITORING, startTime );
    }
}
