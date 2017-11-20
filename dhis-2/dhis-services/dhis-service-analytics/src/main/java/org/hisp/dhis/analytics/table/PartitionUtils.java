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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.Program;
import org.joda.time.DateTime;

import com.google.common.collect.Lists;

/**
 * Utilities for analytics table partition handling.
 * 
 * @author Lars Helge Overland
 */
public class PartitionUtils
{
    private static final YearlyPeriodType PERIODTYPE = new YearlyPeriodType();

    public static final String SEP = "_";

    public static Period getPeriod( Calendar calendar, Integer year )
    {
        DateTimeUnit startOfYear = calendar.isoStartOfYear( year );
        DateTime time = new DateTime( year, startOfYear.getMonth(), startOfYear.getDay(), 1, 1 );

        return PERIODTYPE.createPeriod( time.toDate(), calendar );
    }
    
    public static Date getEarliestDate( Integer lastYears )
    {
        Date earliest = null;

        if ( lastYears != null )
        {
            Calendar calendar = PeriodType.getCalendar();
            DateTimeUnit dateTimeUnit = calendar.today();
            dateTimeUnit = calendar.minusYears( dateTimeUnit, lastYears - 1 );
            dateTimeUnit.setMonth( 1 );
            dateTimeUnit.setDay( 1 );

            earliest = dateTimeUnit.toJdkDate();
        }

        return earliest;
    }
    
    public static String getTableName( String baseName, Program program )
    {
        return baseName + SEP + program.getUid().toLowerCase();
    }

    public static Partitions getPartitions( List<DimensionalItemObject> periods )
    {
        final Set<Integer> years = new HashSet<>();
        
        periods.forEach( p -> {
            Period period = (Period) p;
            years.addAll( getYears( period ) );
        } );
        
        return new Partitions( years );
    }

    public static Partitions getPartitions( Period period )
    {
        return new Partitions( getYears( period ) );
    }
    
    public static Partitions getPartitions( Date startDate, Date endDate )
    {
        Period period = new Period();
        period.setStartDate( startDate );
        period.setEndDate( endDate );
        
        return getPartitions( period );        
    }

    private static Set<Integer> getYears( Period period )
    {
        Set<Integer> years = new HashSet<>();

        int startYear = PeriodType.getCalendar().fromIso( period.getStartDate() ).getYear();
        int endYear = PeriodType.getCalendar().fromIso( period.getEndDate() ).getYear();

        while ( startYear <= endYear )
        {
            years.add( startYear );
            startYear++;
        }

        return years;
    }
    
    /**
     * Creates a mapping between period type name and period for the given periods.
     */
    public static ListMap<String, DimensionalItemObject> getPeriodTypePeriodMap( Collection<DimensionalItemObject> periods )
    {
        ListMap<String, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject period : periods )
        {
            String periodTypeName = ((Period) period).getPeriodType().getName();

            map.putValue( periodTypeName, period );
        }

        return map;
    }

    /**
     * Returns a list of table partitions based on the given analytics tables. For
     * master tables with no partitions, a fake partition representing the master
     * table is used.
     * 
     * @param tables the list of {@link AnalyticsTable}.
     * @return a list of {@link AnalyticsTablePartition}.
     */
    public static List<AnalyticsTablePartition> getTablePartitions( List<AnalyticsTable> tables )
    {
        final List<AnalyticsTablePartition> partitions = Lists.newArrayList();
        
        for ( AnalyticsTable table : tables )
        {
            if ( table.hasPartitionTables() )
            {
                partitions.addAll( table.getPartitionTables() );
            }
            else
            {
                // Fake partition representing the master table
                
                partitions.add( new AnalyticsTablePartition( table, null, null, null, false ) );
            }
        }
        
        return partitions;
    }
}
