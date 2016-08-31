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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTable;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class PeriodResourceTable
    extends ResourceTable<Period>
{
    public PeriodResourceTable( List<Period> objects, String columnQuote )
    {
        super( objects, columnQuote );
    }

    @Override
    public String getTableName()
    {
        return "_periodstructure";
    }

    @Override
    public String getCreateTempTableStatement()
    {
        String sql = 
            "CREATE TABLE " + getTempTableName() + 
            " (periodid INTEGER NOT NULL PRIMARY KEY, iso VARCHAR(15) NOT NULL, daysno INTEGER NOT NULL";
        
        for ( PeriodType periodType : PeriodType.PERIOD_TYPES )
        {
            sql += ", " + columnQuote + periodType.getName().toLowerCase() + columnQuote + " VARCHAR(15)";
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
                final Date startDate = period.getStartDate();
                final PeriodType rowType = period.getPeriodType();
                final String isoDate = period.getIsoDate();

                if ( !uniqueIsoDates.add( isoDate ) )
                {
                    log.warn( "Duplicate ISO date for period, ignoring: " + period + ", ISO date: " + isoDate );
                    continue;
                }
                
                List<Object> values = new ArrayList<>();

                values.add( period.getId() );
                values.add( isoDate );
                values.add( period.getDaysInPeriod() );

                for ( PeriodType periodType : PeriodType.PERIOD_TYPES )
                {
                    if ( rowType.getFrequencyOrder() <= periodType.getFrequencyOrder() )
                    {
                        values.add( IdentifiableObjectUtils.getLocalPeriodIdentifier( startDate, periodType, calendar ) );
                    }
                    else
                    {
                        values.add( null );
                    }
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
}
