package org.hisp.dhis.analytics.table;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.joda.time.DateTime;

/**
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

    //TODO optimize by including required filter periods only

    public static Partitions getPartitions( Period period, String tablePrefix, String tableSuffix, Set<String> validPartitions )
    {
        tablePrefix = StringUtils.trimToEmpty( tablePrefix );
        tableSuffix = StringUtils.trimToEmpty( tableSuffix );

        Partitions partitions = new Partitions();

        int startYear = PeriodType.getCalendar().fromIso( period.getStartDate() ).getYear();
        int endYear = PeriodType.getCalendar().fromIso( period.getEndDate() ).getYear();

        while ( startYear <= endYear )
        {
            String name = tablePrefix + SEP + startYear + tableSuffix;
            partitions.add( name.toLowerCase() );
            startYear++;
        }

        return partitions.prunePartitions( validPartitions );
    }

    public static Partitions getPartitions( List<DimensionalItemObject> periods, 
        String tablePrefix, String tableSuffix, Set<String> validPartitions )
    {
        UniqueArrayList<String> partitions = new UniqueArrayList<>();

        for ( DimensionalItemObject period : periods )
        {
            partitions.addAll( getPartitions( (Period) period, tablePrefix, tableSuffix, null ).getPartitions() );
        }

        return new Partitions( new ArrayList<>( partitions ) ).prunePartitions( validPartitions );
    }

    public static ListMap<Partitions, DimensionalItemObject> getPartitionPeriodMap( 
        List<DimensionalItemObject> periods, String tablePrefix, String tableSuffix, Set<String> validPartitions )
    {
        ListMap<Partitions, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject period : periods )
        {
            map.putValue( getPartitions( (Period) period, tablePrefix, tableSuffix, null ).prunePartitions( validPartitions ), period );
        }

        return map;
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
}
