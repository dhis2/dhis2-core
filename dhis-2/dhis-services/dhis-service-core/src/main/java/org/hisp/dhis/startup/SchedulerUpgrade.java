package org.hisp.dhis.startup;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Sets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.scheduling.DefaultJobConfigurationService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.scheduling.JobType.*;

/**
 * Handles porting from the old scheduler to the new.
 *
 * @author Henning Håkonsen
 */
public class SchedulerUpgrade
    extends AbstractStartupRoutine
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

    private boolean addDefaultJob( String name, List<JobConfiguration> jobConfigurations )
    {
        return jobConfigurations.stream().noneMatch( jobConfiguration -> jobConfiguration.getName().equals( name ) );
    }

    private void addAndScheduleJob( JobConfiguration jobConfiguration )
    {
        jobConfigurationService.addJobConfiguration( jobConfiguration );
        schedulingManager.scheduleJob( jobConfiguration );
    }

    /**
     * Method which ports the jobs in the system from the old scheduler to the new.
     * Collects all old jobs and adds them. Also adds default jobs.
     */
    @Override
    public void execute()
        throws Exception
    {
        List<JobConfiguration> jobConfigurations = jobConfigurationService.getAllJobConfigurations();

        String CRON_DAILY_2AM = "0 0 2 * * ?";
        String CRON_DAILY_7AM = "0 0 7 * * ?";

        String DEFAULT_FILE_RESOURCE_CLEANUP = "File resource clean up";
        String DEFAULT_DATA_STATISTICS = "Data statistics";
        String DEFAULT_VALIDATION_RESULTS_NOTIFICATION = "Validation result notification";
        String DEFAULT_CREDENTIALS_EXPIRY_ALERT = "Credentials expiry alert";
        String DEFAULT_DATA_SET_NOTIFICATION = "Dataset notification";

        log.info( "Setting up default jobs." );
        if ( addDefaultJob( DEFAULT_FILE_RESOURCE_CLEANUP, jobConfigurations ) )
        {
            JobConfiguration fileResourceCleanUp = new JobConfiguration( DEFAULT_FILE_RESOURCE_CLEANUP,
                FILE_RESOURCE_CLEANUP, CRON_DAILY_2AM, null,
                false, true );
            fileResourceCleanUp.setConfigurable( false );
            addAndScheduleJob( fileResourceCleanUp );
        }

        if ( addDefaultJob( DEFAULT_DATA_STATISTICS, jobConfigurations ) )
        {
            JobConfiguration dataStatistics = new JobConfiguration( DEFAULT_DATA_STATISTICS, DATA_STATISTICS,
                CRON_DAILY_2AM,
                null,
                false, true );
            dataStatistics
                .setLastExecuted( (Date) systemSettingManager.getSystemSetting( "lastSuccessfulDataStatistics" ) );
            dataStatistics.setConfigurable( false );
            addAndScheduleJob( dataStatistics );
        }

        if ( addDefaultJob( DEFAULT_VALIDATION_RESULTS_NOTIFICATION, jobConfigurations ) )
        {
            JobConfiguration validationResultNotification = new JobConfiguration(
                DEFAULT_VALIDATION_RESULTS_NOTIFICATION,
                VALIDATION_RESULTS_NOTIFICATION, CRON_DAILY_7AM, null,
                false, true );
            validationResultNotification.setConfigurable( false );
            addAndScheduleJob( validationResultNotification );
        }

        if ( addDefaultJob( DEFAULT_CREDENTIALS_EXPIRY_ALERT, jobConfigurations ) )
        {
            JobConfiguration credentialsExpiryAlert = new JobConfiguration( DEFAULT_CREDENTIALS_EXPIRY_ALERT,
                CREDENTIALS_EXPIRY_ALERT, CRON_DAILY_2AM, null,
                false, true );
            credentialsExpiryAlert.setConfigurable( false );
            addAndScheduleJob( credentialsExpiryAlert );
        }

        if ( addDefaultJob( DEFAULT_DATA_SET_NOTIFICATION, jobConfigurations ) )
        {
            JobConfiguration dataSetNotification = new JobConfiguration( DEFAULT_DATA_SET_NOTIFICATION,
                DATA_SET_NOTIFICATION, CRON_DAILY_2AM, null,
                false, true );
            dataSetNotification.setConfigurable( false );
            addAndScheduleJob( dataSetNotification );
        }

        @SuppressWarnings("unchecked")
        ListMap<String, String> scheduledSystemSettings = (ListMap<String, String>) systemSettingManager
            .getSystemSetting( "keySchedTasks" );
        
        if ( scheduledSystemSettings != null && scheduledSystemSettings.containsKey( "ported" ) )
        {
            log.info( "Scheduler ported" );
            return;
        }

        if ( scheduledSystemSettings != null )
        {
            log.info( "Porting old jobs" );
            JobConfiguration resourceTable = new JobConfiguration( "Resource table", RESOURCE_TABLE, null, null, false,
                true );
            resourceTable.setLastExecuted(
                (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulResourceTablesUpdate" ) );

            JobConfiguration analytics = new JobConfiguration( "Analytics", ANALYTICS_TABLE, null,
                new AnalyticsJobParameters( null, Sets.newHashSet(), false ), false, true );
            analytics.setLastExecuted(
                (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulAnalyticsTablesUpdate" ) );

            JobConfiguration monitoring = new JobConfiguration( "Monitoring", MONITORING, null, null, false, true );
            monitoring.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulMonitoring" ) );

            JobConfiguration dataSync = new JobConfiguration( "Data synchronization", DATA_SYNC, null, null, false,
                true );
            dataSync.setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulDataSynch" ) );

            JobConfiguration metadataSync = new JobConfiguration( "Metadata sync", META_DATA_SYNC, null, null, false,
                true );
            metadataSync
                .setLastExecuted( (Date) systemSettingManager.getSystemSetting( "keyLastMetaDataSyncSuccess" ) );

            JobConfiguration sendScheduledMessage = new JobConfiguration( "Send scheduled messages",
                SEND_SCHEDULED_MESSAGE, null, null,
                false, true );

            JobConfiguration scheduledProgramNotifications = new JobConfiguration( "Scheduled program notifications",
                PROGRAM_NOTIFICATIONS, null, null,
                false, true );
            scheduledProgramNotifications.setLastExecuted(
                (Date) systemSettingManager.getSystemSetting( "keyLastSuccessfulScheduledProgramNotifications" ) );

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

        ListMap<String, String> emptySystemSetting = new ListMap<>();
        emptySystemSetting.putValue( "ported", "" );

        log.info( "Porting to new scheduler finished. Setting system settings key 'keySchedTasks' to 'ported'." );
        systemSettingManager.saveSystemSetting( "keySchedTasks", emptySystemSetting );
    }
}
