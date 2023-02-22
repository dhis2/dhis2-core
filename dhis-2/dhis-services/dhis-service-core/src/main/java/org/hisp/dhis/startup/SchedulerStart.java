/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.startup;

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobStatus.FAILED;
import static org.hisp.dhis.scheduling.JobStatus.SCHEDULED;
import static org.hisp.dhis.scheduling.JobType.FILE_RESOURCE_CLEANUP;
import static org.hisp.dhis.scheduling.JobType.REMOVE_USED_OR_EXPIRED_RESERVED_VALUES;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;

/**
 * Reschedule old jobs and execute jobs which were scheduled when the server was
 * not running.
 *
 * @author Henning Håkonsen
 */
@Slf4j
@RequiredArgsConstructor
public class SchedulerStart extends AbstractStartupRoutine
{

    // Execute at 3-5AM every night and, use a random min/sec, so we don't have
    // all servers in the world
    // requesting at the same time.
    private static final String CRON_DAILY_3AM_RANDOM_MIN_SEC = String.format( "%d %d %d ? * *",
        ThreadLocalRandom.current().nextInt( 59 + 1 ),
        ThreadLocalRandom.current().nextInt( 59 + 1 ),
        ThreadLocalRandom.current().nextInt( 3, 5 + 1 ) );

    private static final String CRON_DAILY_2AM = "0 0 2 ? * *";

    private static final String CRON_DAILY_7AM = "0 0 7 ? * *";

    private static final String LEADER_JOB_CRON_FORMAT = "0 0/%s * * * *";

    enum SystemJob
    {
        SYSTEM_VERSION_UPDATE_CHECK( CRON_DAILY_3AM_RANDOM_MIN_SEC, "vt21671bgno", JobType.SYSTEM_VERSION_UPDATE_CHECK,
            "System version update check notification" ),

        FILE_RESOURCE( CRON_DAILY_2AM, "pd6O228pqr0", FILE_RESOURCE_CLEANUP,
            "File resource clean up" ),
        DATA_STATISTICS( CRON_DAILY_2AM, "BFa3jDsbtdO", JobType.DATA_STATISTICS,
            "Data statistics" ),
        VALIDATION_RESULTS_NOTIFICATION( CRON_DAILY_7AM, "Js3vHn2AVuG", JobType.VALIDATION_RESULTS_NOTIFICATION,
            "Validation result notification" ),
        CREDENTIALS_EXPIRY_ALERT( CRON_DAILY_2AM, "sHMedQF7VYa", JobType.CREDENTIALS_EXPIRY_ALERT,
            "Credentials expiry alert" ),
        ACCOUNT_EXPIRY_ALERT( CRON_DAILY_2AM, "fUWM1At1TUx", JobType.ACCOUNT_EXPIRY_ALERT,
            "User account expiry alert" ),
        DATA_SET_NOTIFICATION( CRON_DAILY_2AM, "YvAwAmrqAtN", JobType.DATA_SET_NOTIFICATION,
            "Dataset notification" ),
        REMOVE_EXPIRED_OR_USED_RESERVED_VALUES( CRON_DAILY_2AM, "uwWCT2BMmlq", REMOVE_USED_OR_EXPIRED_RESERVED_VALUES,
            "Remove expired or used reserved values" ),
        LEADER_ELECTION( LEADER_JOB_CRON_FORMAT, "MoUd5BTQ3lY", JobType.LEADER_ELECTION,
            "Leader election in cluster" );

        final String cron;

        final String uid;

        final JobType type;

        final String name;

        SystemJob( final String cron, String uid, JobType type, String name )
        {
            this.type = type;
            this.uid = uid;
            this.cron = cron;
            this.name = name;
        }
    }

    private final SystemSettingManager systemSettingManager;

    private final boolean redisEnabled;

    private final String leaderElectionTime;

    private final JobConfigurationService jobConfigurationService;

    private final SchedulingManager schedulingManager;

    private final MessageService messageService;

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

                schedulingManager.schedule( jobConfig );
            }
        }) );

        if ( !unexecutedJobs.isEmpty() )
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
        addDefaultJob( SystemJob.FILE_RESOURCE, jobConfigurations );
        addDefaultJob( SystemJob.DATA_STATISTICS, jobConfigurations,
            config -> portJob( config, SettingKey.LAST_SUCCESSFUL_DATA_STATISTICS ) );
        addDefaultJob( SystemJob.VALIDATION_RESULTS_NOTIFICATION, jobConfigurations );
        addDefaultJob( SystemJob.CREDENTIALS_EXPIRY_ALERT, jobConfigurations );
        addDefaultJob( SystemJob.ACCOUNT_EXPIRY_ALERT, jobConfigurations );
        addDefaultJob( SystemJob.DATA_SET_NOTIFICATION, jobConfigurations );
        addDefaultJob( SystemJob.REMOVE_EXPIRED_OR_USED_RESERVED_VALUES, jobConfigurations );
        addDefaultJob( SystemJob.SYSTEM_VERSION_UPDATE_CHECK, jobConfigurations );

        if ( redisEnabled && verifyNoJobExist( SystemJob.LEADER_ELECTION.name, jobConfigurations ) )
        {
            JobConfiguration leaderElectionJobConfiguration = new JobConfiguration(
                SystemJob.LEADER_ELECTION.name,
                SystemJob.LEADER_ELECTION.type,
                format( SystemJob.LEADER_ELECTION.cron, leaderElectionTime ), null );
            leaderElectionJobConfiguration.setLeaderOnlyJob( false );
            leaderElectionJobConfiguration.setUid( SystemJob.LEADER_ELECTION.uid );
            addAndScheduleJob( leaderElectionJobConfiguration );
        }
        else
        {
            checkLeaderElectionJobConfiguration( jobConfigurations );
        }
    }

    private void addDefaultJob( SystemJob job, List<JobConfiguration> jobConfigurations )
    {
        addDefaultJob( job, jobConfigurations, null );
    }

    private void addDefaultJob( SystemJob job, List<JobConfiguration> jobConfigurations,
        Consumer<JobConfiguration> init )
    {
        if ( verifyNoJobExist( job.name, jobConfigurations ) )
        {
            JobConfiguration configuration = new JobConfiguration( job.name,
                job.type, job.cron, null );
            if ( init != null )
                init.accept( configuration );
            configuration.setUid( job.uid );
            configuration.setLeaderOnlyJob( true );
            addAndScheduleJob( configuration );
        }
    }

    private void checkLeaderElectionJobConfiguration( List<JobConfiguration> jobConfigurations )
    {
        Optional<JobConfiguration> maybeLeaderElection = jobConfigurations.stream()
            .filter( configuration -> configuration.getName().equals( SystemJob.LEADER_ELECTION.name ) )
            .findFirst();
        if ( maybeLeaderElection.isPresent() )
        {
            JobConfiguration leaderElection = maybeLeaderElection.get();
            leaderElection.setCronExpression( format( LEADER_JOB_CRON_FORMAT, leaderElectionTime ) );
            leaderElection.setEnabled( redisEnabled );
            jobConfigurationService.updateJobConfiguration( leaderElection );
        }
    }

    private boolean verifyNoJobExist( String name, List<JobConfiguration> jobConfigurations )
    {
        return jobConfigurations.stream().noneMatch( jobConfiguration -> jobConfiguration.getName().equals( name ) );
    }

    private void addAndScheduleJob( JobConfiguration jobConfiguration )
    {
        jobConfigurationService.addJobConfiguration( jobConfiguration );
        schedulingManager.schedule( jobConfiguration );
    }

    private void portJob( JobConfiguration jobConfiguration, SettingKey systemKey )
    {
        Date lastSuccessfulRun = systemSettingManager.getDateSetting( systemKey );

        if ( lastSuccessfulRun != null )
        {
            jobConfiguration.setLastExecuted( lastSuccessfulRun );
            jobConfiguration.setLastExecutedStatus( JobStatus.COMPLETED );
        }
    }
}
