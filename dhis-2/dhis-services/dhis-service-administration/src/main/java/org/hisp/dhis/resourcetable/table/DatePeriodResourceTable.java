package org.hisp.dhis.resourcetable.table;

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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.period.Cal;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTable;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class DatePeriodResourceTable
    extends ResourceTable<Period>
{
    public DatePeriodResourceTable( List<Period> objects, String columnQuote )
    {
        super( objects, columnQuote );
    }
    
    @Override
    public String getTableName()
    {
        return "_dateperiodstructure";
    }

    @Override
    public String getCreateTempTableStatement()
    {
        String sql = "create table " + getTempTableName() + " (dateperiod date not null primary key";
        
        for ( PeriodType periodType : PeriodType.PERIOD_TYPES )
        {
            sql += ", " + columnQuote + periodType.getName().toLowerCase() + columnQuote + " varchar(15)";
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
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();

        List<Object[]> batchArgs = new ArrayList<>();

        Date startDate = new Cal( 1975, 1, 1, true ).time(); //TODO
        Date endDate = new Cal( 2025, 1, 1, true ).time();

        List<Period> days = new UniqueArrayList<>( new DailyPeriodType().generatePeriods( startDate, endDate ) );

        Calendar calendar = PeriodType.getCalendar();

        for ( Period day : days )
        {
            List<Object> values = new ArrayList<>();

            values.add( day.getStartDate() );

            for ( PeriodType periodType : periodTypes )
            {
                values.add( periodType.createPeriod( day.getStartDate(), calendar ).getIsoDate() );
            }

            batchArgs.add( values.toArray() );
        }

        return Optional.of( batchArgs );
    }

    @Override
    public List<String> getCreateIndexStatements()
    {
        return Lists.newArrayList();
    }
}
