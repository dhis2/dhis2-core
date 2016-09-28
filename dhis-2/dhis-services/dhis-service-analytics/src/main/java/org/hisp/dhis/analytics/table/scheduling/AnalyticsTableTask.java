package org.hisp.dhis.analytics.table.scheduling;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

import java.util.Date;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.security.NoSecurityContextRunnable;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsTableTask
    extends NoSecurityContextRunnable
{
    private static final Log log = LogFactory.getLog( AnalyticsTableTask.class );

    @Resource( name = "org.hisp.dhis.analytics.AnalyticsTableService" )
    private AnalyticsTableService analyticsTableService;

    @Resource( name = "org.hisp.dhis.analytics.CompletenessTableService" )
    private AnalyticsTableService completenessTableService;

    @Resource( name = "org.hisp.dhis.analytics.CompletenessTargetTableService" )
    private AnalyticsTableService completenessTargetTableService;

    @Resource( name = "org.hisp.dhis.analytics.OrgUnitTargetTableService" )
    private AnalyticsTableService orgUnitTargetTableService;

    @Resource( name = "org.hisp.dhis.analytics.EventAnalyticsTableService" )
    private AnalyticsTableService eventAnalyticsTableService;

    @Autowired
    private Notifier notifier;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SystemSettingManager systemSettingManager;
    
    private Integer lastYears;

    public void setLastYears( Integer lastYears )
    {
        this.lastYears = lastYears;
    }

    private boolean skipResourceTables = false;

    public void setSkipResourceTables( boolean skipResourceTables )
    {
        this.skipResourceTables = skipResourceTables;
    }

    private boolean skipAggregate = false;

    public void setSkipAggregate( boolean skipAggregate )
    {
        this.skipAggregate = skipAggregate;
    }

    private boolean skipEvents = false;

    public void setSkipEvents( boolean skipEvents )
    {
        this.skipEvents = skipEvents;
    }

    private TaskId taskId;

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }

    // -------------------------------------------------------------------------
    // Runnable implementation
    // -------------------------------------------------------------------------

    @Override
    public void call()
    {
        final Date startTime = new Date();
        final Clock clock = new Clock( log ).startClock();

        notifier.clear( taskId ).notify( taskId, "Analytics table update process started" );

        try
        {
            if ( !skipResourceTables )
            {
                notifier.notify( taskId, "Updating resource tables" );
                analyticsTableService.generateResourceTables();
            }

            if ( !skipAggregate )
            {
                notifier.notify( taskId, "Updating analytics tables" );
                analyticsTableService.update( lastYears, taskId );

                notifier.notify( taskId, "Updating completeness table" );
                completenessTableService.update( lastYears, taskId );

                notifier.notify( taskId, "Updating completeness target table" );
                completenessTargetTableService.update( lastYears, taskId );

                notifier.notify( taskId, "Updating organisation unit target table" );
                orgUnitTargetTableService.update( lastYears, taskId );
            }

            if ( !skipEvents )
            {
                notifier.notify( taskId, "Updating event analytics table" );
                eventAnalyticsTableService.update( lastYears, taskId );
            }

            clock.logTime( "Analytics tables updated" );
            notifier.notify( taskId, INFO, "Analytics tables updated: " + clock.time(), true );
        }
        catch ( RuntimeException ex )
        {
            notifier.notify( taskId, ERROR, "Process failed: " + ex.getMessage(), true );

            messageService.sendSystemErrorNotification( "Analytics table process failed", ex );

            throw ex;
        }

        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE, startTime );
        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME, DateUtils.getPrettyInterval( clock.getSplitTime() ) );
    }
}
