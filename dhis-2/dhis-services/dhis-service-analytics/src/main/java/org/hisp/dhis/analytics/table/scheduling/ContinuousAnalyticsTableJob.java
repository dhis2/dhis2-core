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
package org.hisp.dhis.analytics.table.scheduling;

import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.Date;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

/**
 * Job for continuous update of analytics tables. Performs analytics table
 * update on a schedule where the full analytics table update is done once per
 * day, and the latest analytics partition update is done with a fixed delay.
 * <p>
 * When to run the full update is determined by
 * {@link ContinuousAnalyticsJobParameters#getHourOfDay()}, which specifies the
 * hour of day to run the full update. The next scheduled full analytics table
 * update time is persisted using a system setting. A full analytics table
 * update is performed when the current time is after the next scheduled full
 * update time. Otherwise, a partial update of the latest analytics partition
 * table is performed.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContinuousAnalyticsTableJob implements Job
{
    private static final int DEFAULT_HOUR_OF_DAY = 0;

    private final AnalyticsTableGenerator analyticsTableGenerator;

    private final SystemSettingManager systemSettingManager;

    @Override
    public JobType getJobType()
    {
        return JobType.CONTINUOUS_ANALYTICS_TABLE;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration, JobProgress progress )
    {
        ContinuousAnalyticsJobParameters parameters = (ContinuousAnalyticsJobParameters) jobConfiguration
            .getJobParameters();

        Integer fullUpdateHourOfDay = ObjectUtils.firstNonNull( parameters.getFullUpdateHourOfDay(),
            DEFAULT_HOUR_OF_DAY );

        Date now = new Date();
        Date defaultNextFullUpdate = DateUtils.getNextDate( fullUpdateHourOfDay, now );
        Date nextFullUpdate = systemSettingManager.getSystemSetting( SettingKey.NEXT_ANALYTICS_TABLE_UPDATE,
            defaultNextFullUpdate );

        log.info(
            "Starting continuous analytics table update, current time: '{}', default next full update: '{}', next full update: '{}'",
            getLongDateString( now ), getLongDateString( defaultNextFullUpdate ), getLongDateString( nextFullUpdate ) );

        Preconditions.checkNotNull( nextFullUpdate );

        if ( now.after( nextFullUpdate ) )
        {
            log.info( "Performing full analytics table update" );

            AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
                .withLastYears( parameters.getLastYears() )
                .withSkipResourceTables( false )
                .withSkipTableTypes( parameters.getSkipTableTypes() )
                .withJobId( jobConfiguration )
                .withStartTime( now )
                .build();

            try
            {
                analyticsTableGenerator.generateTables( params, progress );
            }
            finally
            {
                Date nextUpdate = DateUtils.getNextDate( fullUpdateHourOfDay, now );
                systemSettingManager.saveSystemSetting( SettingKey.NEXT_ANALYTICS_TABLE_UPDATE, nextUpdate );
                log.info( "Next full analytics table update: '{}'", getLongDateString( nextUpdate ) );
            }
        }
        else
        {
            log.info( "Performing latest analytics table partition update" );

            AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
                .withLatestPartition()
                .withSkipResourceTables( true )
                .withSkipTableTypes( parameters.getSkipTableTypes() )
                .withJobId( jobConfiguration )
                .withStartTime( now )
                .build();

            analyticsTableGenerator.generateTables( params, progress );
        }
    }
}
