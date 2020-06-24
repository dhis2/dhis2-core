package org.hisp.dhis.resourcetable.table;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyAbstractPeriodType;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import static org.hisp.dhis.system.util.SqlUtils.quote;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class PeriodResourceTable
    extends ResourceTable<Period>
{
    public PeriodResourceTable( List<Period> objects )
    {
        super( objects );
    }

    @Override
    public ResourceTableType getTableType()
    {
        return ResourceTableType.PERIOD_STRUCTURE;
    }

    @Override
    public String getCreateTempTableStatement()
    {
        String sql =
            "create table " + getTempTableName() +
            " (periodid bigint not null primary key, iso varchar(15) not null, daysno integer not null, startdate date not null, enddate date not null, year integer not null";

        for ( PeriodType periodType : PeriodType.PERIOD_TYPES )
        {
            sql += ", " + quote( periodType.getName().toLowerCase() ) + " varchar(15)";
        }

        sql += ")";

        return sql;
    }

    @Override
    public Optional<String> getPopulateTempTableStatement()
    {
        return Optional.empty();
    }

    @Override
    public Optional<List<Object[]>> getPopulateTempTableContent()
    {
        Calendar calendar = PeriodType.getCalendar();

        List<Object[]> batchArgs = new ArrayList<>();

        Set<String> uniqueIsoDates = new HashSet<>();

        for ( Period period : objects )
        {
            if ( period != null && period.isValid() )
            {
                final String isoDate = period.getIsoDate();
                final int year = resolveYearFromPeriod( period );

                if ( !uniqueIsoDates.add( isoDate ) )
                {
                    // Protect against duplicates produced by calendar implementations
                    log.warn( "Duplicate ISO date for period, ignoring: " + period + ", ISO date: " + isoDate );
                    continue;
                }

                List<Object> values = new ArrayList<>();

                values.add( period.getId() );
                values.add( isoDate );
                values.add( period.getDaysInPeriod() );
                values.add( period.getStartDate() );
                values.add( period.getEndDate() );
                values.add( year );

                for ( Period pe : PeriodType.getPeriodTypePeriods( period, calendar ) )
                {
                    values.add( pe != null ? IdentifiableObjectUtils.getLocalPeriodIdentifier( pe, calendar ) : null );
                }

                batchArgs.add( values.toArray() );
            }
        }

        return Optional.of( batchArgs );
    }

    @Override
    public List<String> getCreateIndexStatements()
    {
        String name = "in_periodstructure_iso_" + getRandomSuffix();

        String sql = "create unique index " + name + " on " + getTempTableName() + "(iso)";

        return Lists.newArrayList( sql );
    }
    
    private int resolveYearFromPeriod( Period period )
    {
        // Weekly type has to be treated separately from other Period types.
        // In order to handle all weekly types uniformly, 3 days are added to the week start day and
        // the year of the modified start date is used as reference year for the Period

        if ( WeeklyAbstractPeriodType.class.isAssignableFrom( period.getPeriodType().getClass() ) )
        {
            return new DateTime( period.getStartDate().getTime() ).plusDays( 3 ).getYear();
        }
        else
        {
            return PeriodType.getCalendar().fromIso( period.getStartDate() ).getYear();
        }
    }
}
