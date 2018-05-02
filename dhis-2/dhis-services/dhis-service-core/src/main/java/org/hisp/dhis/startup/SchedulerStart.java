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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hisp.dhis.scheduling.JobStatus.FAILED;
import static org.hisp.dhis.scheduling.JobStatus.SCHEDULED;
import static org.hisp.dhis.scheduling.JobType.*;

/**
 *
 * Reschedule old jobs and execute jobs which were scheduled when the server was not running.
 *
 * @author Henning Håkonsen
 */
public class SchedulerStart
    extends AbstractStartupRoutine
{
    private static final Log log = LogFactory.getLog( SchedulerStart.class );

    private final String CRON_HOURLY = "0 0 * ? * *";
    private final String CRON_DAILY_2AM = "0 0 2 ? * *";
    private final String CRON_DAILY_7AM = "0 0 7 ? * *";

    private final String DEFAULT_FILE_RESOURCE_CLEANUP = "File resource clean up";
    private final String DEFAULT_DATA_STATISTICS = "Data statistics";
    private final String DEFAULT_VALIDATION_RESULTS_NOTIFICATION = "Validation result notification";
    private final String DEFAULT_CREDENTIALS_EXPIRY_ALERT = "Credentials expiry alert";
    private final String DEFAULT_DATA_SET_NOTIFICATION = "Dataset notification";
    private final String DEFAULT_REMOVE_EXPIRED_RESERVED_VALUES = "Remove expired reserved values";

    @Autowired
    private SystemSettingManager systemSettingManager;

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

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    @Override
    public void execute( )
        throws Exception
    {
        Date now = new Date();
        List<String> unexecutedJobs = new ArrayList<>( );

        List<JobConfiguration> jobConfigurations = jobConfigurationService.getAllJobConfigurations();
        addDefaultJobs( jobConfigurations );

        jobConfigurations.forEach( (jobConfig -> {
            if ( jobConfig.isEnabled() )
            {
                Date oldExecutionTime = jobConfig.getNextExecutionTime();

                jobConfig.setNextExecutionTime( null );
                jobConfig.setJobStatus( SCHEDULED );
                jobConfigurationService.updateJobConfiguration( jobConfig );

                if ( jobConfig.getLastExecutedStatus() == FAILED ||
                    ( !jobConfig.isContinuousExecution() && oldExecutionTime.compareTo( now ) < 0 ) )
                {
                    unexecutedJobs.add( "Job [" + jobConfig.getUid() + ", " + jobConfig.getName() + "] has status failed or was scheduled in server downtime. Actual execution time was supposed to be: " + oldExecutionTime );
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

            messageService.sendSystemErrorNotification( "Scheduler startup", new Exception( "Scheduler started with one or more unexecuted jobs:\n" + jobs ) );
        }
    }

    private void addDefaultJobs( List<JobConfiguration> jobConfigurations)
    {
        log.info( "Setting up default jobs." );
        if ( addDefaultJob( DEFAULT_FILE_RESOURCE_CLEANUP, jobConfigurations ) )
        {
            JobConfiguration fileResourceCleanUp = new JobConfiguration( DEFAULT_FILE_RESOURCE_CLEANUP,
                FILE_RESOURCE_CLEANUP, CRON_DAILY_2AM, null, false, true );
            addAndScheduleJob( fileResourceCleanUp );
        }

        if ( addDefaultJob( DEFAULT_DATA_STATISTICS, jobConfigurations ) )
        {
            JobConfiguration dataStatistics = new JobConfiguration( DEFAULT_DATA_STATISTICS, DATA_STATISTICS,
                CRON_DAILY_2AM, null, false, true );
            SchedulerUpgrade.portJob( systemSettingManager, dataStatistics,"lastSuccessfulDataStatistics" );

            addAndScheduleJob( dataStatistics );
        }

        if ( addDefaultJob( DEFAULT_VALIDATION_RESULTS_NOTIFICATION, jobConfigurations ) )
        {
            JobConfiguration validationResultNotification = new JobConfiguration( DEFAULT_VALIDATION_RESULTS_NOTIFICATION,
                VALIDATION_RESULTS_NOTIFICATION, CRON_DAILY_7AM, null, false, true );
            addAndScheduleJob( validationResultNotification );
        }

        if ( addDefaultJob( DEFAULT_CREDENTIALS_EXPIRY_ALERT, jobConfigurations ) )
        {
            JobConfiguration credentialsExpiryAlert = new JobConfiguration( DEFAULT_CREDENTIALS_EXPIRY_ALERT,
                CREDENTIALS_EXPIRY_ALERT, CRON_DAILY_2AM, null, false, true );
            addAndScheduleJob( credentialsExpiryAlert );
        }

        if ( addDefaultJob( DEFAULT_DATA_SET_NOTIFICATION, jobConfigurations ) )
        {
            JobConfiguration dataSetNotification = new JobConfiguration( DEFAULT_DATA_SET_NOTIFICATION,
                DATA_SET_NOTIFICATION, CRON_DAILY_2AM, null, false, true );
            addAndScheduleJob( dataSetNotification );
        }

        if ( addDefaultJob( DEFAULT_REMOVE_EXPIRED_RESERVED_VALUES, jobConfigurations ) )
        {
            JobConfiguration removeExpiredReservedValues = new JobConfiguration( DEFAULT_REMOVE_EXPIRED_RESERVED_VALUES,
                REMOVE_EXPIRED_RESERVED_VALUES, CRON_HOURLY, null, false, true );
            addAndScheduleJob( removeExpiredReservedValues );
        }
    }

    private boolean addDefaultJob( String name, List<JobConfiguration> jobConfigurations )
    {
        return jobConfigurations.stream().noneMatch( jobConfiguration -> jobConfiguration.getName().equals( name ) );
    }

    private void addAndScheduleJob( JobConfiguration jobConfiguration )
    {
        jobConfigurationService.addJobConfiguration( jobConfiguration );
        schedulingManager.scheduleJob( jobConfiguration );
    }
}
