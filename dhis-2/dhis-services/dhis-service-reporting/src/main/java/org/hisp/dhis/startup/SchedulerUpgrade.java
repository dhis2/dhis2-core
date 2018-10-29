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

import com.google.api.client.util.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.pushanalysis.PushAnalysisService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.scheduling.JobType.ANALYTICS_TABLE;
import static org.hisp.dhis.scheduling.JobType.DATA_SYNC;
import static org.hisp.dhis.scheduling.JobType.META_DATA_SYNC;
import static org.hisp.dhis.scheduling.JobType.MONITORING;
import static org.hisp.dhis.scheduling.JobType.PROGRAM_NOTIFICATIONS;
import static org.hisp.dhis.scheduling.JobType.PUSH_ANALYSIS;
import static org.hisp.dhis.scheduling.JobType.RESOURCE_TABLE;
import static org.hisp.dhis.scheduling.JobType.SEND_SCHEDULED_MESSAGE;

/**
 * Handles porting from the old scheduler to the new.
 *
 * @author Henning Håkonsen
 */
@Transactional
public class SchedulerUpgrade
    extends AbstractStartupRoutine
{
    private static final Log log = LogFactory.getLog( SchedulerUpgrade.class );

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private PushAnalysisService pushAnalysisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JobConfigurationService jobConfigurationService;

    public void setJobConfigurationService( JobConfigurationService jobConfigurationService )
    {
        this.jobConfigurationService = jobConfigurationService;
    }

    /**
     * Method which ports the jobs in the system from the old scheduler to the new.
     * Collects all old jobs and adds them.
     */
    @Override
    public void execute()
        throws Exception
    {
        @SuppressWarnings( "unchecked" )
        ListMap<String, String> scheduledSystemSettings = (ListMap<String, String>) systemSettingManager
            .getSystemSetting( SettingKey.KEY_SCHED_TASKS );

        if ( scheduledSystemSettings != null && ( scheduledSystemSettings.containsKey( "ported" ) || scheduledSystemSettings.containsKey( "portedV31" ) ) )
        {
            if ( scheduledSystemSettings.containsKey( "portedV31" ) )
            {
                log.info( "Scheduler ported" );
            }
            else
            {
                v31upgrade();
            }
            return;
        }

        if ( scheduledSystemSettings != null )
        {
            log.info( "Porting old jobs" );
            JobConfiguration resourceTable = new JobConfiguration( "Resource table", RESOURCE_TABLE, null, null, false, true );
            SchedulerStart.portJob( systemSettingManager, resourceTable, SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE );

            JobConfiguration analytics = new JobConfiguration( "Analytics", ANALYTICS_TABLE, null,
                new AnalyticsJobParameters( null, Sets.newHashSet(), false ), false, true );
            SchedulerStart.portJob( systemSettingManager, analytics, SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );

            JobConfiguration monitoring = new JobConfiguration( "Monitoring", MONITORING, null, null, false, true );
            SchedulerStart.portJob( systemSettingManager, monitoring, SettingKey.LAST_SUCCESSFUL_MONITORING );

            JobConfiguration dataSync = new JobConfiguration( "Data synchronization", DATA_SYNC, null, null, false, true );
            SchedulerStart.portJob( systemSettingManager, dataSync, SettingKey.LAST_SUCCESSFUL_DATA_SYNC );

            JobConfiguration metadataSync = new JobConfiguration( "Metadata sync", META_DATA_SYNC, null, null, false, true );
            SchedulerStart.portJob( systemSettingManager, metadataSync, SettingKey.LAST_SUCCESSFUL_METADATA_SYNC );

            JobConfiguration sendScheduledMessage = new JobConfiguration( "Send scheduled messages",
                SEND_SCHEDULED_MESSAGE, null, null, false, true );

            JobConfiguration scheduledProgramNotifications = new JobConfiguration( "Scheduled program notifications",
                PROGRAM_NOTIFICATIONS, null, null, false, true );
            SchedulerStart.portJob( systemSettingManager, scheduledProgramNotifications, SettingKey.LAST_SUCCESSFUL_SCHEDULED_PROGRAM_NOTIFICATIONS );

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
                        }
                        break;
                    }

                    log.error( "Could not map job type '" + jobType + "' with cron '" + cron + "'" );
                }
            } ) );

            pushAnalysisService
                .getAll()
                .forEach( ( pa ) -> {
                    String cron = "";

                    switch ( pa.getSchedulingFrequency() )
                    {
                    case DAILY:
                        cron = "0 0 4 */1 * *";
                        break;
                    case WEEKLY:
                        cron = "0 0 4 * * " + ( pa.getSchedulingDayOfFrequency() % 7 );
                        break;
                    case MONTHLY:
                        cron = "0 0 4 " + pa.getSchedulingDayOfFrequency() + " */1 *";
                        break;
                    default:
                        break;
                    }

                    if( !cron.isEmpty() )
                    {
                        jobConfigurationService.addJobConfiguration(
                            new JobConfiguration( "PushAnalysis: " + pa.getUid(), PUSH_ANALYSIS, cron,
                                new PushAnalysisJobParameters( pa.getUid() ), true, pa.getEnabled() ) );
                    }
                } );

            ListMap<String, String> emptySystemSetting = new ListMap<>();
            emptySystemSetting.putValue( "ported", "" );

            log.info( "Porting to new scheduler finished. Setting system settings key 'keySchedTasks' to 'ported'." );
            systemSettingManager.saveSystemSetting( SettingKey.KEY_SCHED_TASKS, emptySystemSetting );
        }
    }

    private void v31upgrade()
    {
        log.info( "Running V31 scheduler upgrade" );

        List<JobConfiguration> jobConfigurations = jobConfigurationService.getAllJobConfigurations();

        jobConfigurations.forEach(jobConfiguration -> {
            byte[] jobParametersByte = jdbcTemplate.queryForObject(
                "select jobparameters from jobconfiguration where jobconfigurationid=" + jobConfiguration.getId(),
                (rs, rowNum) -> rs.getBytes(1));

            if ( jobParametersByte != null )
            {
                Object jParaB = null;
                try
                {
                    jParaB = toObject( jobParametersByte );
                }
                catch ( IOException | ClassNotFoundException e )
                {
                    e.printStackTrace();
                }

                JobParameters jobParameters = jobConfiguration.getJobType().getJobParameters().cast( jParaB );
                jobConfiguration.setJobParameters( jobParameters );

                jobConfigurationService.updateJobConfiguration( jobConfiguration );
            }
        });

        ListMap<String, String> emptySystemSetting = new ListMap<>();
        emptySystemSetting.putValue( "portedV31", "" );

        log.info( "Upgrade to V31 scheduler finished. Setting system settings key 'keySchedTasks' to 'portedV31'." );
        systemSettingManager.saveSystemSetting( SettingKey.KEY_SCHED_TASKS, emptySystemSetting );
    }

    public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
        Object obj;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return obj;
    }
}
