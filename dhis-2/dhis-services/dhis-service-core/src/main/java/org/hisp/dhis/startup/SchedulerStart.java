package org.hisp.dhis.startup;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.scheduling.JobStatus.FAILED;
import static org.hisp.dhis.scheduling.JobStatus.SCHEDULED;
import static org.hisp.dhis.scheduling.JobType.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;

import lombok.extern.slf4j.Slf4j;

/**
 * Reschedule old jobs and execute jobs which were scheduled when the server was
 * not running.
 *
 * @author Henning Håkonsen
 */
@Slf4j
public class SchedulerStart extends AbstractStartupRoutine
{
    private final String CRON_HOURLY = "0 0 * ? * *";
    private final String CRON_DAILY_2AM = "0 0 2 ? * *";
    private final String CRON_DAILY_7AM = "0 0 7 ? * *";
    private final String LEADER_JOB_CRON_FORMAT = "0 0/%s * * * *";
    private final String DEFAULT_FILE_RESOURCE_CLEANUP_UID = "pd6O228pqr0";
    private final String DEFAULT_FILE_RESOURCE_CLEANUP = "File resource clean up";
    private final String DEFAULT_DATA_STATISTICS_UID = "BFa3jDsbtdO";
    private final String DEFAULT_DATA_STATISTICS = "Data statistics";
    private final String DEFAULT_VALIDATION_RESULTS_NOTIFICATION_UID = "Js3vHn2AVuG";
    private final String DEFAULT_VALIDATION_RESULTS_NOTIFICATION = "Validation result notification";
    private final String DEFAULT_CREDENTIALS_EXPIRY_ALERT_UID = "sHMedQF7VYa";
    private final String DEFAULT_CREDENTIALS_EXPIRY_ALERT = "Credentials expiry alert";
    private final String DEFAULT_DATA_SET_NOTIFICATION_UID = "YvAwAmrqAtN";
    private final String DEFAULT_DATA_SET_NOTIFICATION = "Dataset notification";
    private final String DEFAULT_REMOVE_EXPIRED_RESERVED_VALUES_UID = "uwWCT2BMmlq";
    private final String DEFAULT_REMOVE_EXPIRED_RESERVED_VALUES = "Remove expired reserved values";
    private final String DEFAULT_LEADER_ELECTION_UID = "MoUd5BTQ3lY";
    private final String DEFAULT_LEADER_ELECTION = "Leader election in cluster";

    private final SystemSettingManager systemSettingManager;

    private final String redisEnabled;

    private final String leaderElectionTime;

    private final JobConfigurationService jobConfigurationService;

    private final SchedulingManager schedulingManager;

    private final MessageService messageService;

    public SchedulerStart( SystemSettingManager systemSettingManager, String redisEnabled, String leaderElectionTime,
        JobConfigurationService jobConfigurationService, SchedulingManager schedulingManager,
        MessageService messageService )
    {
        checkNotNull( systemSettingManager );
        checkNotNull( jobConfigurationService );
        checkNotNull( schedulingManager );
        checkNotNull( messageService );
        checkNotNull( leaderElectionTime );
        checkNotNull( redisEnabled );

        this.systemSettingManager = systemSettingManager;
        this.redisEnabled = redisEnabled;
        this.leaderElectionTime = leaderElectionTime;
        this.jobConfigurationService = jobConfigurationService;
        this.schedulingManager = schedulingManager;
        this.messageService = messageService;
    }

    @Override
    public void execute()
        throws Exception
    {
        Date now = new Date();
        List<String> unexecutedJobs = new ArrayList<>();

        List<JobConfiguration> jobConfigurations = jobConfigurationService.getAllJobConfigurations();
        addDefaultJobs( jobConfigurations );

        jobConfigurations.forEach( (jobConfig -> {
            if ( jobConfig.isEnabled() )
            {
                Date oldExecutionTime = jobConfig.getNextExecutionTime();

                jobConfig.setNextExecutionTime( null );
                jobConfig.setJobStatus( SCHEDULED );
                jobConfigurationService.updateJobConfiguration( jobConfig );

                if ( jobConfig.getLastExecutedStatus() == FAILED
                    || (oldExecutionTime != null && oldExecutionTime.compareTo( now ) < 0) )
                {
                    unexecutedJobs.add( "\nJob [" + jobConfig.getUid() + ", " + jobConfig.getName()
                        + "] has status failed or was scheduled in server downtime. Actual execution time was supposed to be: "
                        + oldExecutionTime );
                }

                schedulingManager.scheduleJob( jobConfig );
            }
        }) );

        if ( unexecutedJobs.size() > 0 )
        {
            StringBuilder jobs = new StringBuilder();

            for ( String unexecutedJob : unexecutedJobs )
            {
                jobs.append( unexecutedJob ).append( "\n" );
            }

            messageService.sendSystemErrorNotification( "Scheduler startup",
                new Exception( "Scheduler started with one or more unexecuted jobs:\n" + jobs ) );
        }
    }

    private void addDefaultJobs( List<JobConfiguration> jobConfigurations )
    {
        log.info( "Setting up default jobs." );
        if ( verifyNoJobExist( DEFAULT_FILE_RESOURCE_CLEANUP, jobConfigurations ) )
        {
            JobConfiguration fileResourceCleanUp = new JobConfiguration( DEFAULT_FILE_RESOURCE_CLEANUP,
                FILE_RESOURCE_CLEANUP, CRON_DAILY_2AM, null );
            fileResourceCleanUp.setUid( DEFAULT_FILE_RESOURCE_CLEANUP_UID );
            fileResourceCleanUp.setLeaderOnlyJob( true );
            addAndScheduleJob( fileResourceCleanUp );
        }

        if ( verifyNoJobExist( DEFAULT_DATA_STATISTICS, jobConfigurations ) )
        {
            JobConfiguration dataStatistics = new JobConfiguration( DEFAULT_DATA_STATISTICS, DATA_STATISTICS,
                CRON_DAILY_2AM, null );
            portJob( systemSettingManager, dataStatistics, SettingKey.LAST_SUCCESSFUL_DATA_STATISTICS );
            dataStatistics.setLeaderOnlyJob( true );
            dataStatistics.setUid( DEFAULT_DATA_STATISTICS_UID );
            addAndScheduleJob( dataStatistics );
        }

        if ( verifyNoJobExist( DEFAULT_VALIDATION_RESULTS_NOTIFICATION, jobConfigurations ) )
        {
            JobConfiguration validationResultNotification = new JobConfiguration( DEFAULT_VALIDATION_RESULTS_NOTIFICATION,
                VALIDATION_RESULTS_NOTIFICATION, CRON_DAILY_7AM, null );
            validationResultNotification.setLeaderOnlyJob( true );
            validationResultNotification.setUid( DEFAULT_VALIDATION_RESULTS_NOTIFICATION_UID );
            addAndScheduleJob( validationResultNotification );
        }

        if ( verifyNoJobExist( DEFAULT_CREDENTIALS_EXPIRY_ALERT, jobConfigurations ) )
        {
            JobConfiguration credentialsExpiryAlert = new JobConfiguration( DEFAULT_CREDENTIALS_EXPIRY_ALERT,
                CREDENTIALS_EXPIRY_ALERT, CRON_DAILY_2AM, null );
            credentialsExpiryAlert.setLeaderOnlyJob( true );
            credentialsExpiryAlert.setUid( DEFAULT_CREDENTIALS_EXPIRY_ALERT_UID );
            addAndScheduleJob( credentialsExpiryAlert );
        }

        if ( verifyNoJobExist( DEFAULT_DATA_SET_NOTIFICATION, jobConfigurations ) )
        {
            JobConfiguration dataSetNotification = new JobConfiguration( DEFAULT_DATA_SET_NOTIFICATION,
                DATA_SET_NOTIFICATION, CRON_DAILY_2AM, null );
            dataSetNotification.setLeaderOnlyJob( true );
            dataSetNotification.setUid( DEFAULT_DATA_SET_NOTIFICATION_UID );
            addAndScheduleJob( dataSetNotification );
        }

        if ( verifyNoJobExist( DEFAULT_REMOVE_EXPIRED_RESERVED_VALUES, jobConfigurations ) )
        {
            JobConfiguration removeExpiredReservedValues = new JobConfiguration( DEFAULT_REMOVE_EXPIRED_RESERVED_VALUES,
                REMOVE_EXPIRED_RESERVED_VALUES, CRON_HOURLY, null );
            removeExpiredReservedValues.setLeaderOnlyJob( true );
            removeExpiredReservedValues.setUid( DEFAULT_REMOVE_EXPIRED_RESERVED_VALUES_UID );
            addAndScheduleJob( removeExpiredReservedValues );
        }

        if ( verifyNoJobExist( DEFAULT_LEADER_ELECTION, jobConfigurations ) && "true".equalsIgnoreCase( redisEnabled ) )
        {
            JobConfiguration leaderElectionJobConfiguration = new JobConfiguration( DEFAULT_LEADER_ELECTION,
                LEADER_ELECTION, String.format( LEADER_JOB_CRON_FORMAT, leaderElectionTime ), null );
            leaderElectionJobConfiguration.setLeaderOnlyJob( false );
            leaderElectionJobConfiguration.setUid( DEFAULT_LEADER_ELECTION_UID );
            addAndScheduleJob( leaderElectionJobConfiguration );
        }
        else
        {
            checkLeaderElectionJobConfiguration( jobConfigurations );
        }
    }

    private void checkLeaderElectionJobConfiguration( List<JobConfiguration> jobConfigurations )
    {
        Optional<JobConfiguration> leaderElectionJobConfigurationOptional = jobConfigurations.stream()
            .filter( jobConfiguration -> jobConfiguration.getName().equals( DEFAULT_LEADER_ELECTION ) ).findFirst();
        if ( leaderElectionJobConfigurationOptional.isPresent() )
        {
            JobConfiguration leaderElectionJobConfiguration = leaderElectionJobConfigurationOptional.get();
            leaderElectionJobConfiguration
                .setCronExpression( String.format( LEADER_JOB_CRON_FORMAT, leaderElectionTime ) );
            if ( "true".equalsIgnoreCase( redisEnabled ) )
            {
                leaderElectionJobConfiguration.setEnabled( true );
            }
            else
            {
                leaderElectionJobConfiguration.setEnabled( false );
            }
            jobConfigurationService.updateJobConfiguration( leaderElectionJobConfiguration );
        }
    }

    private boolean verifyNoJobExist( String name, List<JobConfiguration> jobConfigurations )
    {
        return jobConfigurations.stream().noneMatch( jobConfiguration -> jobConfiguration.getName().equals( name ) );
    }

    private void addAndScheduleJob( JobConfiguration jobConfiguration )
    {
        jobConfigurationService.addJobConfiguration( jobConfiguration );
        schedulingManager.scheduleJob( jobConfiguration );
    }


    private static void portJob(SystemSettingManager systemSettingManager, JobConfiguration jobConfiguration, SettingKey systemKey)
    {
        Date lastSuccessfulRun = (Date) systemSettingManager.getSystemSetting( systemKey );

        if ( lastSuccessfulRun != null )
        {
            jobConfiguration.setLastExecuted( lastSuccessfulRun );
            jobConfiguration.setLastExecutedStatus( JobStatus.COMPLETED );
        }
    }
}
