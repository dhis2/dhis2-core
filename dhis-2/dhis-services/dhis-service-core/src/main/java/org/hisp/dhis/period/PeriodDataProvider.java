/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.period;

import static java.time.LocalDate.now;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Component responsible for fetching, extracting and providing specific data
 * related to existing periods in the database.
 *
 * @author maikel arabori
 */
@Component
@RequiredArgsConstructor
public class PeriodDataProvider
{
    private static final int BEFORE_AND_AFTER_DATA_YEARS_SUPPORTED = 5;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Returns a distinct union of all years available in the
     * "programstageinstance" table + "period" table, both from aggregate and
     * tracker, with 5 years previous and future additions.
     *
     * ie: [extra_5_previous_years, data_years, extra_5_future_year]
     *
     * @return an unmodifiable list of distinct years and the respective
     *         additions.
     */
    public List<Integer> getAvailableYears()
    {
        List<Integer> availableDataYears = new ArrayList<>( fetchAvailableYears() );

        if ( availableDataYears.isEmpty() )
        {
            availableDataYears.add( now().getYear() );
        }

        int firstYear = availableDataYears.get( 0 );
        int lastYear = availableDataYears.get( availableDataYears.size() - 1 );

        for ( int i = 0; i < BEFORE_AND_AFTER_DATA_YEARS_SUPPORTED; i++ )
        {
            availableDataYears.add( --firstYear );
            availableDataYears.add( ++lastYear );
        }

        sort( availableDataYears );

        return unmodifiableList( availableDataYears );
    }

    /**
     * Queries the database in order to fetch all years available in the
     * "period" and "programstageinstance" tables.
     *
     * @return the list of distinct years found.
     */
    private List<Integer> fetchAvailableYears()
    {
        String dueDateOrExecutionDate = "(case when 'SCHEDULE' = psi.status then psi.duedate else psi.executiondate end)";

        String sql = "( select distinct (extract(year from pe.startdate)) as datayear from period pe )" +
            " union" +
            " ( select distinct (extract(year from pe.enddate)) as datayear from period pe )" +
            " union" +
            " ( select distinct (extract(year from " + dueDateOrExecutionDate + ")) as datayear" +
            " from programstageinstance psi" +
            " where " + dueDateOrExecutionDate + " is not null" +
            " and psi.deleted is false ) order by datayear asc";

        return jdbcTemplate.queryForList( sql, Integer.class );
    }
}
