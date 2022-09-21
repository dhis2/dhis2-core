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

import static java.util.Calendar.DECEMBER;
import static java.util.Calendar.JANUARY;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.minusOneDay;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.jdbc.batchhandler.MappingBatchHandler;

/**
 * Writer of rows to the analytics_ownership temp tables.
 *
 * @author Jim Grace
 */
@RequiredArgsConstructor( access = AccessLevel.PRIVATE )
public class JdbcOwnershipWriter
{
    private final MappingBatchHandler batchHandler;

    /**
     * Row of the previous write if any, possibly modified.
     */
    private Map<String, Object> prevRow = null;

    /**
     * Row of the current write, possibly modified.
     */
    private Map<String, Object> newRow;

    public static final String TEIUID = quote( "teiuid" );

    public static final String STARTDATE = quote( "startdate" );

    public static final String ENDDATE = quote( "enddate" );

    public static final String OU = quote( "ou" );

    private static final Date FAR_PAST_DATE = new GregorianCalendar( 1000, JANUARY, 1 ).getTime();

    private static final Date FAR_FUTURE_DATE = new GregorianCalendar( 9999, DECEMBER, 31 ).getTime();

    /**
     * Gets instance by a factory method (so it can be mocked).
     */
    public static JdbcOwnershipWriter getInstance( MappingBatchHandler batchHandler )
    {
        return new JdbcOwnershipWriter( batchHandler );
    }

    /**
     * Write a row to an analytics_ownership temp table. Work on a copy of the
     * row so we do not change the original row. We cannot use immutable maps
     * because the orgUnit levels contain nulls when the orgUnit is not at the
     * lowest level, and immutable maps do not allow null values.
     *
     * @param row map of values to write
     */
    public void write( Map<String, Object> row )
    {
        this.newRow = new HashMap<>( row );

        if ( prevRow != null )
        {
            processPreviousRow();
        }

        adjustNewRow();

        prevRow = newRow;
    }

    /**
     * Flush any final row to the analytics_ownership temp table.
     */
    public void flush()
    {
        if ( prevRow != null )
        {
            prevRow.put( ENDDATE, FAR_FUTURE_DATE );

            conditionallyWritePreviousRow();
        }

        batchHandler.flush();
    }

    /**
     * Process the previous, saved row now that we know what the new row
     * contains. If the ownership hasn't changed, just continue the previous row
     * to include the new date range. If the ownership orgUnit has changed, and
     * it's the same TEI, then end the previous row one day before the start of
     * this row. If the TEI has changed, then the previous row was the last row
     * for that TEI, so the previous row ends far in the future.
     */
    private void processPreviousRow()
    {
        if ( sameValue( TEIUID ) )
        {
            if ( sameValue( OU ) )
            {
                // Make this row a continuation of the previous row:
                newRow.put( STARTDATE, prevRow.get( STARTDATE ) );

                return;
            }

            prevRow.put( ENDDATE, minusOneDay( (Date) newRow.get( STARTDATE ) ) );
        }
        else
        {
            prevRow.put( ENDDATE, FAR_FUTURE_DATE );
        }

        conditionallyWritePreviousRow();
    }

    /**
     * Write the previous row to the analytics_ownership temp table. However, if
     * the previous row was the only one for this TEI then we don't need to
     * write it. This is because the ownership never changed, so the enrollment
     * orgUnit can be used as the ownership orgUnit.
     */
    private void conditionallyWritePreviousRow()
    {
        if ( !FAR_PAST_DATE.equals( prevRow.get( STARTDATE ) ) ||
            !FAR_FUTURE_DATE.equals( prevRow.get( ENDDATE ) ) )
        {
            batchHandler.addObject( prevRow );
        }
    }

    /**
     * Adjust the row. If this is the first row for this TEI, set the start date
     * to the far past.
     */
    private void adjustNewRow()
    {
        if ( prevRow == null || !sameValue( TEIUID ) )
        {
            newRow.put( STARTDATE, FAR_PAST_DATE );
        }
    }

    /**
     * Returns true if the column has the same value between the row and the
     * previous row.
     */
    private boolean sameValue( String colName )
    {
        return newRow.get( colName ).equals( prevRow.get( colName ) );
    }
}
