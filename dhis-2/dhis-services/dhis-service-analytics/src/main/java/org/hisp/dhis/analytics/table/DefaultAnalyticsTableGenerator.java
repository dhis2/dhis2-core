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
package org.hisp.dhis.analytics.table;

import static org.hisp.dhis.commons.collection.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.Clock;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.analytics.AnalyticsTableGenerator" )
@RequiredArgsConstructor
public class DefaultAnalyticsTableGenerator
    implements AnalyticsTableGenerator
{
    private final List<AnalyticsTableService> analyticsTableServices;

    private final ResourceTableService resourceTableService;

    private final SystemSettingManager systemSettingManager;

    private final AnalyticsCache analyticsCache;

    // TODO introduce last successful timestamps per table type

    @Override
    public void generateTables( AnalyticsTableUpdateParams params0, JobProgress progress )
    {
        Clock clock = new Clock( log ).startClock();
        Date lastSuccessfulUpdate = systemSettingManager
            .getDateSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );

        Set<AnalyticsTableType> availableTypes = analyticsTableServices.stream()
            .map( AnalyticsTableService::getAnalyticsTableType )
            .collect( Collectors.toSet() );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder( params0 )
            .withLastSuccessfulUpdate( lastSuccessfulUpdate )
            .build();

        log.info( "Found {} analytics table types: {}", availableTypes.size(), availableTypes );
        log.info( "Analytics table update: {}", params );
        log.info( "Last successful analytics table update: '{}'",
            getLongDateString( lastSuccessfulUpdate ) );

        progress.startingProcess( "Analytics table update process"
            + (params.isLatestUpdate() ? "(latest partition)" : "") );

        if ( !params.isSkipResourceTables() && !params.isLatestUpdate() )
        {
            generateResourceTablesInternal( progress );
        }

        Set<AnalyticsTableType> skipTypes = emptyIfNull( params.getSkipTableTypes() );

        for ( AnalyticsTableService service : analyticsTableServices )
        {
            AnalyticsTableType tableType = service.getAnalyticsTableType();

            if ( !skipTypes.contains( tableType ) )
            {
                service.update( params, progress );
            }
        }

        progress.startingStage( "Updating settings" );
        progress.runStage( () -> updateLastSuccessfulSystemSettings( params, clock ) );

        progress.startingStage( "Invalidate analytics caches", SKIP_STAGE );
        progress.runStage( analyticsCache::invalidateAll );
        progress.completedProcess( "Analytics tables updated" );
    }

    private void updateLastSuccessfulSystemSettings( AnalyticsTableUpdateParams params, Clock clock )
    {
        if ( params.isLatestUpdate() )
        {
            systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE,
                params.getStartTime() );
            systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_RUNTIME,
                clock.time() );
        }
        else
        {
            systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE,
                params.getStartTime() );
            systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME,
                clock.time() );
        }
    }

    @Override
    public void generateResourceTables( JobProgress progress )
    {
        Clock clock = new Clock().startClock();

        progress.startingProcess( "Generating resource tables" );

        try
        {
            generateResourceTablesInternal( progress );

            progress.completedProcess( "Resource tables generated: " + clock.time() );
        }
        catch ( RuntimeException ex )
        {
            progress.failedProcess( "Resource tables generation: " + ex.getMessage() );
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void generateResourceTablesInternal( JobProgress progress )
    {
        final Date startTime = new Date();

        resourceTableService.dropAllSqlViews( progress );

        Map<String, Runnable> generators = new LinkedHashMap<>();
        generators.put( "generating OrganisationUnit structures",
            resourceTableService::generateOrganisationUnitStructures );
        generators.put( "generating DataSetOrganisationUnitCategory table",
            resourceTableService::generateDataSetOrganisationUnitCategoryTable );
        generators.put( "generating CategoryOptionCombo names",
            resourceTableService::generateCategoryOptionComboNames );
        generators.put( "generating DataElementGroupSet table",
            resourceTableService::generateDataElementGroupSetTable );
        generators.put( "generating IndicatorGroupSet table", resourceTableService::generateIndicatorGroupSetTable );
        generators.put( "generating OrganisationUnitGroupSet table",
            resourceTableService::generateOrganisationUnitGroupSetTable );
        generators.put( "generating Category table", resourceTableService::generateCategoryTable );
        generators.put( "generating DataElement table", resourceTableService::generateDataElementTable );
        generators.put( "generating Period table", resourceTableService::generatePeriodTable );
        generators.put( "generating DatePeriod table", resourceTableService::generateDatePeriodTable );
        generators.put( "generating CategoryOptionCombo table",
            resourceTableService::generateCategoryOptionComboTable );
        progress.startingStage( "Generating resource tables", generators.size() );
        progress.runStage( generators );

        resourceTableService.createAllSqlViews( progress );

        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE, startTime );
    }
}
