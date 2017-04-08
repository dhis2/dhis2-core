package org.hisp.dhis.analytics.table;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

/**
 * @author Lars Helge Overland
 */
public class DefaultAnalyticsTableGenerator
    implements AnalyticsTableGenerator
{
    private static final Log log = LogFactory.getLog( DefaultAnalyticsTableGenerator.class );

    @Autowired
    private List<AnalyticsTableService> analyticsTableServices;

    @Autowired
    private ResourceTableService resourceTableService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private Notifier notifier;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public void generateTables( Integer lastYears, TaskId taskId, Set<AnalyticsTableType> skipTableTypes, boolean skipResourceTables )
    {
        final Date startTime = new Date();
        final Clock clock = new Clock( log ).startClock();
        final Set<AnalyticsTableType> skipTypes = CollectionUtils.emptyIfNull( skipTableTypes );
        final Set<AnalyticsTableType> availableTypes = analyticsTableServices.
            stream().map( AnalyticsTableService::getAnalyticsTableType ).collect( Collectors.toSet() );

        log.info( String.format( "Found %d analytics table types: %s", availableTypes.size(), availableTypes ) );
        log.info( String.format( "Skip %d analytics table types: %s", skipTypes.size(), skipTypes ) );

        try
        {
            notifier.clear( taskId ).notify( taskId, "Analytics table update process started" );

            if ( !skipResourceTables )
            {
                notifier.notify( taskId, "Updating resource tables" );
                generateResourceTables();
            }

            for ( AnalyticsTableService service : analyticsTableServices )
            {
                AnalyticsTableType tableType = service.getAnalyticsTableType();

                if ( !skipTypes.contains( tableType ) )
                {
                    notifier.notify( taskId, "Updating tables: " + tableType );

                    service.update( lastYears, taskId );
                }
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

    @Override
    public void dropTables()
    {
        for ( AnalyticsTableService service : analyticsTableServices )
        {
            service.dropTables();
        }
    }

    @Override
    public void generateResourceTables( TaskId taskId )
    {
        final Clock clock = new Clock().startClock();

        notifier.notify( taskId, "Generating resource tables" );

        try
        {
            generateResourceTables();

            notifier.notify( taskId, NotificationLevel.INFO, "Resource tables generated: " + clock.time(), true );
        }
        catch ( RuntimeException ex )
        {
            notifier.notify( taskId, NotificationLevel.ERROR, "Process failed: " + ex.getMessage(), true );

            messageService.sendSystemErrorNotification( "Resource table process failed", ex );

            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void generateResourceTables()
    {
        final Date startTime = new Date();

        resourceTableService.dropAllSqlViews();
        resourceTableService.generateOrganisationUnitStructures();
        resourceTableService.generateDataSetOrganisationUnitCategoryTable();
        resourceTableService.generateCategoryOptionComboNames();
        resourceTableService.generateDataElementGroupSetTable();
        resourceTableService.generateIndicatorGroupSetTable();
        resourceTableService.generateOrganisationUnitGroupSetTable();
        resourceTableService.generateCategoryTable();
        resourceTableService.generateDataElementTable();
        resourceTableService.generatePeriodTable();
        resourceTableService.generateDatePeriodTable();
        resourceTableService.generateDataElementCategoryOptionComboTable();
        resourceTableService.createAllSqlViews();

        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE, startTime );
    }
}
