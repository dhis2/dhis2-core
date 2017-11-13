package org.hisp.dhis.scheduling;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.scheduling.JobType.*;
import static org.hisp.dhis.scheduling.JobType.CREDENTIALS_EXPIRY_ALERT;

/**
 * Handles porting from the old scheduler to the new.
 *
 * @author Henning Håkonsen
 */
public class SchedulerUpgrade
{
    private static final Log log = LogFactory.getLog( DefaultJobConfigurationService.class );

    private JobConfigurationService jobConfigurationService;

    public void setJobConfigurationService( JobConfigurationService jobConfigurationService )
    {
        this.jobConfigurationService = jobConfigurationService;
    }

    private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    @Autowired
    private SystemSettingManager systemSettingManager;

    /**
     * Method which ports the jobs in the system from the old scheduler to the new.
     * Collects all old jobs and adds them. Also adds default jobs.
     */
    void handleServerUpgrade()
    {
        ListMap<String, String> scheduledSystemSettings = (ListMap<String, String>) systemSettingManager.getSystemSetting( "keySchedTasks" );

        if ( scheduledSystemSettings != null && scheduledSystemSettings.containsKey( "ported" ) )
        {
            log.info( "Scheduler ported" );
            return;
        }

        if (scheduledSystemSettings != null) {
            log.info( "Porting old jobs" );
            JobConfiguration resourceTable = new JobConfiguration("Resource table", RESOURCE_TABLE, null, null, true, false );
            resourceTable.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulResourceTablesUpdate" ) );

            JobConfiguration analytics = new JobConfiguration("Analytics", ANALYTICS_TABLE, null, new AnalyticsJobParameters(null, Sets
                .newHashSet(), false), true, false );
            analytics.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulAnalyticsTablesUpdate" ) );

            JobConfiguration monitoring = new JobConfiguration("Monitoring", MONITORING, null, null, true, false );
            monitoring.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulMonitoring" ) );

            JobConfiguration dataSync = new JobConfiguration("Data synchronization", DATA_SYNC, null, null, true, false );
            dataSync.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulDataSynch" ) );

            JobConfiguration metadataSync = new JobConfiguration("Metadata sync", META_DATA_SYNC, null, null, true, false );
            metadataSync.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastMetaDataSyncSuccess" ) );

            JobConfiguration sendScheduledMessage = new JobConfiguration("Send scheduled messages", SEND_SCHEDULED_MESSAGE, null, null, true, false );

            JobConfiguration scheduledProgramNotifications = new JobConfiguration("Scheduled program notifications", PROGRAM_NOTIFICATIONS, null, null, true, false );
            scheduledProgramNotifications.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulScheduledProgramNotifications" ) );

            HashMap<String, JobConfiguration> standardJobs = new HashMap<String, JobConfiguration>()
            {{
                put( "resourceTable", resourceTable );
                put( "analytics", analytics );
                put( "monitoring", monitoring );
                put( "dataSynch", dataSync );
                put( "metadataSync", metadataSync );
                put( "sendScheduledMessage", sendScheduledMessage );
                put( "scheduledProgramNotifications", scheduledProgramNotifications );
            }};

            scheduledSystemSettings.forEach( ( cron, jobType ) -> jobType.forEach( type -> {
                for ( Map.Entry<String, JobConfiguration> entry : standardJobs.entrySet() )
                {
                    if ( type.startsWith( entry.getKey() ) )
                    {
                        JobConfiguration jobConfiguration = entry.getValue();

                        if ( jobConfiguration != null )
                        {
                            jobConfiguration.setCronExpression( cron );
                            jobConfiguration.setNextExecutionTime( null );
                            jobConfigurationService.addJobConfiguration( jobConfiguration );

                            schedulingManager.scheduleJob( jobConfiguration );
                        }
                        break;
                    }

                    log.error( "Could not map job type '" + jobType + "' with cron '" + cron + "'" );
                }
            } ) );
        }

        String CRON_DAILY_2AM = "0 0 2 * * ?";
        String CRON_DAILY_7AM = "0 0 7 * * ?";

        log.info( "Setting up default jobs." );
        JobConfiguration fileResourceCleanUp = new JobConfiguration("File resource clean up", FILE_RESOURCE_CLEANUP, CRON_DAILY_2AM, null, true, false );

        JobConfiguration dataStatistics = new JobConfiguration("Data statistics", DATA_STATISTICS, CRON_DAILY_2AM, null, true, false );
        dataStatistics.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "lastSuccessfulDataStatistics" ) );

        JobConfiguration validationResultNotification = new JobConfiguration("Validation result notification", VALIDATION_RESULTS_NOTIFICATION, CRON_DAILY_7AM, null, true, false );
        JobConfiguration credentialsExpiryAlert = new JobConfiguration("Credentials expiry alert", CREDENTIALS_EXPIRY_ALERT, CRON_DAILY_2AM, null, true, false );
        // Dataset notification HH

        List<JobConfiguration> defaultJobs = Lists
            .newArrayList( fileResourceCleanUp, dataStatistics, validationResultNotification, credentialsExpiryAlert );
        jobConfigurationService.addJobConfigurations( defaultJobs );
        schedulingManager.scheduleJobs( defaultJobs );

        ListMap<String, String> emptySystemSetting = new ListMap<>();
        emptySystemSetting.putValue("ported", "");

        log.info( "Porting to new scheduler finished. Setting system settings key 'keySchedTasks' to 'ported'.");
        systemSettingManager.saveSystemSetting( "keySchedTasks", emptySystemSetting);
    }
}
