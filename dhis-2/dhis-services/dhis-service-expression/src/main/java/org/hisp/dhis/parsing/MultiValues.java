package org.hisp.dhis.parsing;

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

import org.hisp.dhis.period.Period;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Holds multiple values for expression evaluation.
 *
 * @author Jim Grace
 */
public class MultiValues
{
    private List<Object> values = new ArrayList<>();

    private List<Period> periods = null;

    // -------------------------------------------------------------------------
    // Business logic
    // -------------------------------------------------------------------------

    /**
     * Adds a value to the MultiValues.
     *
     * @param value the value to add.
     */
    public void addValue( Object value )
    {
        if ( value instanceof MultiValues )
        {
            MultiValues multiValues = (MultiValues) value;

            if ( multiValues.hasPeriods() )
            {
                for ( int i = 0; i < multiValues.periods.size(); i++ )
                {
                    addPeriodValue( multiValues.periods.get( i ), multiValues.values.get( i ) );
                }
            }
            else
            {
                values.addAll( multiValues.values );
            }
        }
        else if ( value != null )
        {
            values.add( value );
        }
    }

    /**
     * Adds a value with period information to the MultiValues.
     *
     * @param period the period associated with the value.
     * @param value the value to add.
     */
    public void addPeriodValue( Period period, Object value )
    {
        if ( value instanceof MultiValues )
        {
            MultiValues multiValues = (MultiValues) value;

            if ( multiValues.hasPeriods() )
            {
                throw new ParsingException( "Can't chain two period functions with no aggregation function between." );
            }

            for ( int i = 0; i < multiValues.periods.size(); i++ )
            {
                addPeriodValue( multiValues.periods.get( i ), multiValues.values.get( i ) );
            }
        }
        else if ( value != null )
        {
            values.add( value );

            if ( periods == null )
            {
                periods = new ArrayList<>();
            }

            periods.add( period );
        }
    }

    /**
     * Tells whether this MultiValues has periods.
     *
     * @return true if peridos are present, otherwise false.
     */
    public boolean hasPeriods()
    {
        return periods != null;
    }

    /**
     * Returns the first or last n values, sorted by period.
     *
     * @param limit how many values (maximum) to return.
     * @param isFirst whether to return first values (or last).
     * @return the limited MultiValues.
     */
    public MultiValues firstOrLast( int limit, boolean isFirst )
    {
        SortedMap<Period, List<Object>> sortedValues = buildSortedValues( isFirst );

        return firstOrLastValues( sortedValues, limit );
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    public List<Object> getValues()
    {
        return values;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Build a SortedMap of values, orderd by period, in either ascending or
     * descending order.
     *
     * @param isFirst whether to put earliest periods first (or last).
     * @return the sorted values.
     */
    private SortedMap<Period, List<Object>> buildSortedValues( boolean isFirst )
    {
        SortedMap<Period, List<Object>> sortedValues = isFirst ? new TreeMap<>()
            : new TreeMap<>( Collections.reverseOrder() );

        for ( int i = 0; i < periods.size(); i++ )
        {
            if ( !sortedValues.containsKey( periods.get( i ) ) )
            {
                sortedValues.put( periods.get( i ), new ArrayList<Object>() );
            }

            sortedValues.get( periods.get( i ) ).add( values.get( i ) );
        }

        return sortedValues;
    }

    /**
     * Returns the first n values from the SortedMap.
     *
     * @param sortedValues the SortedMap of values.
     * @param limit the largest number of values to return.
     * @return the limited MultiValues.
     */
    private MultiValues firstOrLastValues( SortedMap<Period, List<Object>> sortedValues, int limit )
    {
        MultiValues selectedValues = new MultiValues();

        int count = 0;

        for ( Period period : sortedValues.keySet() )
        {
            for ( Object value : sortedValues.get( period ) )
            {
                if ( ++count > limit )
                {
                    return selectedValues;
                }

                selectedValues.addPeriodValue( period, value );
            }
        }

        return selectedValues;
    }
}
