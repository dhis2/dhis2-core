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
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.commons.util.CronUtils;
import org.hisp.dhis.pushanalysis.PushAnalysis;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hisp.dhis.scheduling.JobType.*;

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

    private JobConfigurationService jobConfigurationService;

    public void setJobConfigurationService( JobConfigurationService jobConfigurationService )
    {
        this.jobConfigurationService = jobConfigurationService;
    }

    private GenericIdentifiableObjectStore<PushAnalysis> pushAnalysisStore;

    public void setPushAnalysisStore( GenericIdentifiableObjectStore<PushAnalysis> store )
    {
        this.pushAnalysisStore = store;
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
            .getSystemSetting( "keySchedTasks" );

        if ( scheduledSystemSettings != null && scheduledSystemSettings.containsKey( "ported" ) )
        {
            log.info( "Scheduler ported" );
            return;
        }

        if ( scheduledSystemSettings != null )
        {
            log.info( "Porting old jobs" );
            JobConfiguration resourceTable = new JobConfiguration( "Resource table", RESOURCE_TABLE, null, null, false, true );
            portJob( systemSettingManager, resourceTable, "keyLastSuccessfulResourceTablesUpdate" );

            JobConfiguration analytics = new JobConfiguration( "Analytics", ANALYTICS_TABLE, null,
                new AnalyticsJobParameters( null, Sets.newHashSet(), false ), false, true );
            portJob( systemSettingManager, analytics, "keyLastSuccessfulAnalyticsTablesUpdate" );

            JobConfiguration monitoring = new JobConfiguration( "Monitoring", MONITORING, null, null, false, true );
            portJob( systemSettingManager, monitoring, "keyLastSuccessfulMonitoring" );

            JobConfiguration dataSync = new JobConfiguration( "Data synchronization", DATA_SYNC, null, null, false, true );
            portJob( systemSettingManager, dataSync, "keyLastSuccessfulDataSynch" );

            JobConfiguration metadataSync = new JobConfiguration( "Metadata sync", META_DATA_SYNC, null, null, false, true );
            portJob( systemSettingManager, metadataSync, "keyLastMetaDataSyncSuccess" );

            JobConfiguration sendScheduledMessage = new JobConfiguration( "Send scheduled messages",
                SEND_SCHEDULED_MESSAGE, null, null, false, true );

            JobConfiguration scheduledProgramNotifications = new JobConfiguration( "Scheduled program notifications",
                PROGRAM_NOTIFICATIONS, null, null, false, true );
            portJob( systemSettingManager, scheduledProgramNotifications, "keyLastSuccessfulScheduledProgramNotifications" );

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

            log.info("Moving existing Push Analysis jobs." );

            pushAnalysisStore
                .getAll()
                .forEach( ( pa ) -> {
                    String cron;

                    switch ( pa.getSchedulingFrequency() )
                    {
                    case DAILY:
                        cron = CronUtils.getDailyCronExpression( 0, 4 );
                        break;
                    case WEEKLY:
                        cron = CronUtils.getWeeklyCronExpression( 0, 4, pa.getSchedulingDayOfFrequency() );
                        break;
                    case MONTHLY:
                        cron = CronUtils.getMonthlyCronExpression( 0, 4, pa.getSchedulingDayOfFrequency() );
                        break;
                    default:
                        cron = "";
                        break;
                    }

                    jobConfigurationService.addJobConfiguration( new JobConfiguration( "PushAnalysis: " + pa.getUid(), PUSH_ANALYSIS, cron,
                            new PushAnalysisJobParameters( pa.getUid() ), true, pa.getEnabled() ) );
                } );

            ListMap<String, String> emptySystemSetting = new ListMap<>();
            emptySystemSetting.putValue( "ported", "" );

            log.info( "Porting to new scheduler finished. Setting system settings key 'keySchedTasks' to 'ported'." );
            systemSettingManager.saveSystemSetting( "keySchedTasks", emptySystemSetting );
        }
    }

    public static void portJob( SystemSettingManager systemSettingManager, JobConfiguration jobConfiguration, String systemKey )
    {
        Date lastSuccessfulRun = (Date) systemSettingManager.getSystemSetting( systemKey );

        if ( lastSuccessfulRun != null )
        {
            jobConfiguration.setLastExecuted( lastSuccessfulRun );
            jobConfiguration.setLastExecutedStatus( JobStatus.COMPLETED );
        }
    }
}
