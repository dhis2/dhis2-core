package org.hisp.dhis.dataadmin.action.scheduling;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.opensymphony.xwork2.Action;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.scheduling.ScheduledTaskStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.hisp.dhis.scheduling.SchedulingManager.TASK_ANALYTICS_ALL;
import static org.hisp.dhis.scheduling.SchedulingManager.TASK_ANALYTICS_LAST_3_YEARS;
import static org.hisp.dhis.scheduling.SchedulingManager.TASK_DATA_SYNCH;
import static org.hisp.dhis.scheduling.SchedulingManager.TASK_META_DATA_SYNC;
import static org.hisp.dhis.scheduling.SchedulingManager.TASK_MONITORING_LAST_DAY;
import static org.hisp.dhis.scheduling.SchedulingManager.TASK_RESOURCE_TABLE;
import static org.hisp.dhis.scheduling.SchedulingManager.TASK_RESOURCE_TABLE_15_MINS;
import static org.hisp.dhis.scheduling.SchedulingManager.TASK_SCHEDULED_PROGRAM_NOTIFICATIONS;
import static org.hisp.dhis.system.scheduling.Scheduler.CRON_DAILY_0AM;
import static org.hisp.dhis.system.scheduling.Scheduler.CRON_DAILY_11PM;
import static org.hisp.dhis.system.scheduling.Scheduler.CRON_DAILY_5AM;
import static org.hisp.dhis.system.scheduling.Scheduler.CRON_DAILY_6AM;
import static org.hisp.dhis.system.scheduling.Scheduler.CRON_DAILY_7AM;
import static org.hisp.dhis.system.scheduling.Scheduler.CRON_DAILY_8AM;
import static org.hisp.dhis.system.scheduling.Scheduler.CRON_EVERY_15MIN;

/**
 * @author Lars Helge Overland
 */
public class ScheduleTasksAction
    implements Action
{
    private static final String TASK_STARTED = "task_started";
    private static final String TASK_ALREADY_RUNNING = "task_already_running";

    private static final String STRATEGY_ALL_DAILY = "allDaily";
    private static final String STRATEGY_ALL_15_MIN = "allEvery15Min";
    private static final String STRATEGY_LAST_3_YEARS_DAILY = "last3YearsDaily";
    private static final String STRATEGY_ENABLED = "enabled";

    private static final String STRATEGY_DAILY_5_AM = "dailyFiveAM";
    private static final String STRATEGY_DAILY_6_AM = "dailySixAM";
    private static final String STRATEGY_DAILY_7_AM = "dailySevenAM";
    private static final String STRATEGY_DAILY_8_AM = "dailyEightAM";

    private BiMap<String, String> STRATEGY_TO_CRON = new ImmutableBiMap.Builder<String, String>()
        .put( STRATEGY_DAILY_5_AM, CRON_DAILY_5AM )
        .put( STRATEGY_DAILY_6_AM, CRON_DAILY_6AM )
        .put( STRATEGY_DAILY_7_AM, CRON_DAILY_7AM )
        .put( STRATEGY_DAILY_8_AM, CRON_DAILY_8AM )
        .build();

    private static final Log log = LogFactory.getLog( ScheduleTasksAction.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private SynchronizationManager synchronizationManager;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private boolean executeNow;

    public void setExecuteNow( boolean executeNow )
    {
        this.executeNow = executeNow;
    }

    private boolean schedule;

    public void setSchedule( boolean schedule )
    {
        this.schedule = schedule;
    }

    private String taskKey;

    public void setTaskKey( String taskKey )
    {
        this.taskKey = taskKey;
    }

    private String resourceTableStrategy;

    public String getResourceTableStrategy()
    {
        return resourceTableStrategy;
    }

    public void setResourceTableStrategy( String resourceTableStrategy )
    {
        this.resourceTableStrategy = resourceTableStrategy;
    }

    private String analyticsStrategy;

    public String getAnalyticsStrategy()
    {
        return analyticsStrategy;
    }

    public void setAnalyticsStrategy( String analyticsStrategy )
    {
        this.analyticsStrategy = analyticsStrategy;
    }

    private String monitoringStrategy;

    public String getMonitoringStrategy()
    {
        return monitoringStrategy;
    }

    public void setMonitoringStrategy( String monitoringStrategy )
    {
        this.monitoringStrategy = monitoringStrategy;
    }

    private String dataSynchStrategy;


    public String getDataSynchStrategy()
    {
        return dataSynchStrategy;
    }

    public void setDataSynchStrategy( String dataSynchStrategy )
    {
        this.dataSynchStrategy = dataSynchStrategy;
    }

    private String smsSchedulerStrategy;

    public String getSmsSchedulerStrategy()
    {
        return smsSchedulerStrategy;
    }

    public void setSmsSchedulerStrategy( String smsSchedulerStrategy )
    {
        this.smsSchedulerStrategy = smsSchedulerStrategy;
    }

    private String programNotificationSchedulerStrategy;

    public String getProgramNotificationSchedulerStrategy()
    {
        return programNotificationSchedulerStrategy;
    }

    public void setProgramNotificationSchedulerStrategy( String programNotificationSchedulerStrategy )
    {
        this.programNotificationSchedulerStrategy = programNotificationSchedulerStrategy;
    }

    private String dataStatisticsStrategy;

    public String getDataStatisticsStrategy()
    {
        return dataStatisticsStrategy;
    }

    public void setDataStatisticsStrategy( String dataStatisticsStrategy )
    {
        this.dataStatisticsStrategy = dataStatisticsStrategy;
    }

    private String metadataSyncStrategy;

    public String getMetadataSyncStrategy()
    {
        return metadataSyncStrategy;
    }

    public void setMetadataSyncStrategy( String metadataSyncStrategy )
    {
        this.metadataSyncStrategy = metadataSyncStrategy;
    }

    private String metadataSyncCron;

    public String getMetadataSyncCron()
    {
        return metadataSyncCron;
    }

    public void setMetadataSyncCron( String metadataSyncCron )
    {
        this.metadataSyncCron = metadataSyncCron;
    }

    private String dataSyncCron;

    public String getDataSyncCron()
    {
        return dataSyncCron;
    }

    public void setDataSyncCron( String dataSyncCron )
    {
        this.dataSyncCron = dataSyncCron;
    }
    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private ScheduledTaskStatus status;

    public ScheduledTaskStatus getStatus()
    {
        return status;
    }

    private boolean running;

    public boolean isRunning()
    {
        return running;
    }

    private List<OrganisationUnitLevel> levels;

    public List<OrganisationUnitLevel> getLevels()
    {
        return levels;
    }

    private Date lastResourceTableSuccess;

    public Date getLastResourceTableSuccess()
    {
        return lastResourceTableSuccess;
    }

    private Date lastAnalyticsTableSuccess;

    public Date getLastAnalyticsTableSuccess()
    {
        return lastAnalyticsTableSuccess;
    }

    private Date lastMonitoringSuccess;

    public Date getLastMonitoringSuccess()
    {
        return lastMonitoringSuccess;
    }

    private Date lastDataSyncSuccess;

    private Date lastMetaDataSyncSuccess;

    public Date getLastDataSyncSuccess()
    {
        return lastDataSyncSuccess;
    }

    public Date getLastMetaDataSyncSuccess()
    {
        return lastMetaDataSyncSuccess;
    }

    private Date lastSmsSchedulerSuccess;

    public Date getLastSmsSchedulerSuccess()
    {
        return lastSmsSchedulerSuccess;
    }

    private Date lastProgramNotificationSchedulerSuccess;

    public Date getLastProgramNotificationSchedulerSuccess()
    {
        return lastProgramNotificationSchedulerSuccess;
    }

    private Date lastDataStatisticSuccess;

    public Date getLastDataStatisticSuccess()
    {
        return lastDataStatisticSuccess;
    }

    private String currentRunningTaskStatus;

    public String getCurrentRunningTaskStatus()
    {
        return currentRunningTaskStatus;
    }

    public void setCurrentRunningTaskStatus( String currentRunningTaskStatus )
    {
        this.currentRunningTaskStatus = currentRunningTaskStatus;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        if ( executeNow )
        {
            if ( schedulingManager.isTaskInProgress( taskKey ) )
            {
                currentRunningTaskStatus = TASK_ALREADY_RUNNING;
            }
            else
            {
                schedulingManager.executeTask( taskKey );
                currentRunningTaskStatus = TASK_STARTED;
            }
        }

        if ( schedule )
        {
            if ( ScheduledTaskStatus.RUNNING.equals( schedulingManager.getTaskStatus() ) )
            {
                schedulingManager.stopTasks();
            }
            else
            {

                // -------------------------------------------------------------
                // Build new schedule
                // -------------------------------------------------------------

                ListMap<String, String> cronKeyMap = new ListMap<>();

                // -------------------------------------------------------------
                // Resource tables
                // -------------------------------------------------------------

                if ( STRATEGY_ALL_DAILY.equals( resourceTableStrategy ) )
                {
                    cronKeyMap.putValue( CRON_DAILY_11PM, TASK_RESOURCE_TABLE );
                }
                else if ( STRATEGY_ALL_15_MIN.equals( resourceTableStrategy ) )
                {
                    cronKeyMap.putValue( CRON_EVERY_15MIN, TASK_RESOURCE_TABLE_15_MINS );
                }

                // -------------------------------------------------------------
                // Analytics
                // -------------------------------------------------------------

                if ( STRATEGY_ALL_DAILY.equals( analyticsStrategy ) )
                {
                    cronKeyMap.putValue( CRON_DAILY_0AM, TASK_ANALYTICS_ALL );
                }
                else if ( STRATEGY_LAST_3_YEARS_DAILY.equals( analyticsStrategy ) )
                {
                    cronKeyMap.putValue( CRON_DAILY_0AM, TASK_ANALYTICS_LAST_3_YEARS );
                }

                // -------------------------------------------------------------
                // Monitoring
                // -------------------------------------------------------------

                if ( STRATEGY_ALL_DAILY.equals( monitoringStrategy ) )
                {
                    cronKeyMap.putValue( CRON_DAILY_0AM, TASK_MONITORING_LAST_DAY );
                }

                // -------------------------------------------------------------
                // Data synch
                // -------------------------------------------------------------

                if ( STRATEGY_ENABLED.equals( dataSynchStrategy ) )
                {
                    cronKeyMap.putValue( dataSyncCron, TASK_DATA_SYNCH );
                    systemSettingManager.saveSystemSetting( SettingKey.DATA_SYNC_CRON, dataSyncCron );
                }

                if ( STRATEGY_ENABLED.equals( metadataSyncStrategy ) )
                {
                    cronKeyMap.putValue( metadataSyncCron, TASK_META_DATA_SYNC );
                    systemSettingManager.saveSystemSetting( SettingKey.METADATA_SYNC_CRON, metadataSyncCron );
                    systemSettingManager.saveSystemSetting( SettingKey.METADATAVERSION_ENABLED, true );
                }

                // -------------------------------------------------------------
                // Program notifications scheduler
                // -------------------------------------------------------------

                if ( StringUtils.isNotEmpty( programNotificationSchedulerStrategy ) )
                {
                    String cron = STRATEGY_TO_CRON.get( programNotificationSchedulerStrategy );

                    if ( cron == null )
                    {
                        log.warn( "Unrecognized scheduling strategy for program notifications: " +
                            programNotificationSchedulerStrategy );
                    }
                    else
                    {
                        cronKeyMap.putValue( cron, TASK_SCHEDULED_PROGRAM_NOTIFICATIONS );
                    }
                }

                // -------------------------------------------------------------
                // Commit new schedule
                // -------------------------------------------------------------

                schedulingManager.scheduleTasks( cronKeyMap );
            }
        }
        else
        {
            // -------------------------------------------------------------
            // Populate fields
            // -------------------------------------------------------------

            Collection<String> keys = schedulingManager.getScheduledKeys();

            // -----------------------------------------------------------------
            // Resource tables
            // -----------------------------------------------------------------

            if ( keys.contains( TASK_RESOURCE_TABLE ) )
            {
                resourceTableStrategy = STRATEGY_ALL_DAILY;
            }
            else if ( keys.contains( TASK_RESOURCE_TABLE_15_MINS ) )
            {
                resourceTableStrategy = STRATEGY_ALL_15_MIN;
            }

            // -----------------------------------------------------------------
            // Analytics
            // -----------------------------------------------------------------

            if ( keys.contains( TASK_ANALYTICS_ALL ) )
            {
                analyticsStrategy = STRATEGY_ALL_DAILY;
            }
            else if ( keys.contains( TASK_ANALYTICS_LAST_3_YEARS ) )
            {
                analyticsStrategy = STRATEGY_LAST_3_YEARS_DAILY;
            }

            // -------------------------------------------------------------
            // Monitoring
            // -------------------------------------------------------------

            if ( keys.contains( TASK_MONITORING_LAST_DAY ) )
            {
                monitoringStrategy = STRATEGY_ALL_DAILY;
            }

            // -------------------------------------------------------------
            // Data synch
            // -------------------------------------------------------------

            if ( keys.contains( TASK_DATA_SYNCH ) )
            {
                dataSynchStrategy = STRATEGY_ENABLED;
                dataSyncCron = (String) systemSettingManager.getSystemSetting( SettingKey.DATA_SYNC_CRON );
            }

            // -------------------------------------------------------------
            // Metadata sync Scheduler
            // -------------------------------------------------------------

            if ( keys.contains( TASK_META_DATA_SYNC ) )
            {
                metadataSyncStrategy = STRATEGY_ENABLED;
                metadataSyncCron = (String) systemSettingManager.getSystemSetting( SettingKey.METADATA_SYNC_CRON );
            }

            // -------------------------------------------------------------
            // Program notifications scheduler
            // -------------------------------------------------------------

            if ( keys.contains( TASK_SCHEDULED_PROGRAM_NOTIFICATIONS ) )
            {
                String cron = schedulingManager.getCronForTask( TASK_SCHEDULED_PROGRAM_NOTIFICATIONS );

                if ( cron != null )
                {
                    programNotificationSchedulerStrategy = STRATEGY_TO_CRON.inverse().get( cron );
                }
            }
        }

        status = schedulingManager.getTaskStatus();
        running = ScheduledTaskStatus.RUNNING.equals( status );
        levels = organisationUnitService.getOrganisationUnitLevels();

        lastResourceTableSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE );
        lastAnalyticsTableSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );
        lastMonitoringSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_MONITORING );
        lastDataStatisticSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_DATA_STATISTIC );
        lastDataSyncSuccess = synchronizationManager.getLastDataSynchSuccess();
        lastMetaDataSyncSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_METADATA_SYNC );
        lastProgramNotificationSchedulerSuccess = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_SCHEDULED_PROGRAM_NOTIFICATIONS );

        log.info( "Status: " + status );
        log.info( "Running: " + running );

        return SUCCESS;
    }
}
