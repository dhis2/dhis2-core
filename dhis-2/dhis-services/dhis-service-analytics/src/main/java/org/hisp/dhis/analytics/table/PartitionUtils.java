package org.hisp.dhis.analytics.table;

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
import org.hisp.dhis.program.Program;

import com.google.common.collect.Lists;

/**
 * Utilities for analytics table partition handling.
 * 
 * @author Lars Helge Overland
 */
public class PartitionUtils
{
    public static final String SEP = "_";

    /**
     * Returns the start date for the given year, inclusive.
     * 
     * @param calendar the calendar to base the date on.
     * @param year the year.
     * @return the start date.
     */
    public static Date getStartDate( Calendar calendar, Integer year )
    {
        return calendar.isoStartOfYear( year ).toJdkDate();
    }

    /**
     * Returns the end date for the given year, exclusive, i.e.
     * the start date of the year after the given year.
     * 
     * @param calendar the calendar to base the date on.
     * @param year the year.
     * @return the start date.
     */
    public static Date getEndDate( Calendar calendar, Integer year )
    {
        Integer nextYear = year + 1;
        return getStartDate( calendar, nextYear );
    }
    
    /**
     * Returns the start date of the year which occurred 
     * {@code lastYears} ago.
     * 
     * @param lastYears the number of years ago to base the 
     *         returned date.
     * @return the start date of the given year.
     */
    public static Date getStartDate( Integer lastYears )
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
    
    /**
     * Returns the table name of the table with the given base name and program.
     * 
     * @param baseName the table base name.
     * @param program the program.
     * @return the table name.
     */
    public static String getTableName( String baseName, Program program )
    {
        return baseName + SEP + program.getUid().toLowerCase();
    }

    /**
     * Returns partitions for the given list of periods.
     * 
     * @param period the period.
     * @return partitions for the given list of periods.
     */
    public static Partitions getPartitions( List<DimensionalItemObject> periods )
    {
        final Set<Integer> years = new HashSet<>();
        
        periods.forEach( p -> {
            Period period = (Period) p;
            years.addAll( getYears( period ) );
        } );
        
        return new Partitions( years );
    }

    /**
     * Returns partitions for the given period.
     * 
     * @param period the period.
     * @return partitions for the given period.
     */
    public static Partitions getPartitions( Period period )
    {
        return new Partitions( getYears( period ) );
    }
    
    /**
     * Returns partitions for the given start and end date.
     * 
     * @param startDate the start date.
     * @param endDate the end date.
     * @return partitions for the given start and end date.
     */
    public static Partitions getPartitions( Date startDate, Date endDate )
    {
        Period period = new Period();
        period.setStartDate( startDate );
        period.setEndDate( endDate );
        
        return getPartitions( period );        
    }

    /**
     * Returns the years which the given period spans.
     * 
     * @param period the period.
     * @return a set of years.
     */
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
