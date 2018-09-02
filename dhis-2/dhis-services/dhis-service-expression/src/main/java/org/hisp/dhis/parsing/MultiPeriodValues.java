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
 * Holds multiple period values for expression evaluation.
 *
 * @author Jim Grace
 */
public class MultiPeriodValues extends MultiValues
{
    private List<Period> periods = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Business logic
    // -------------------------------------------------------------------------

    public void addPeriodValue( Object value, Period period )
    {
        if ( value != null )
        {
            if ( value instanceof MultiPeriodValues )
            {
                MultiPeriodValues multiPeriodValues = (MultiPeriodValues) value;

                for ( int i = 0; i < multiPeriodValues.periods.size(); i++ )
                {
                    addPeriodValue( multiPeriodValues.getValues().get( i ), multiPeriodValues.getPeriods().get( i ) );
                }
            }
            else if ( value instanceof MultiValues )
            {
                MultiValues multiValues = (MultiValues) value;

                for ( Object val : multiValues.getValues() )
                {
                    addPeriodValue( val, period );
                }
            }
            else
            {
                addValue( value );
                periods.add( period );
            }
        }
    }

    public MultiPeriodValues firstOrLast( int limit, boolean isFirst )
    {
        SortedMap<Period, List<Object>> sortedValues = isFirst ? new TreeMap<>()
            : new TreeMap<>( Collections.reverseOrder() );

        for ( int i = 0; i < periods.size(); i++ )
        {
            if ( !sortedValues.containsKey( periods.get( i ) ) )
            {
                sortedValues.put( periods.get( i ), new ArrayList<Object>() );
            }

            sortedValues.get( periods.get( i ) ).add( getValues().get( i ) );
        }

        MultiPeriodValues selectedValues = new MultiPeriodValues();

        int count = 0;

        for ( Period period : sortedValues.keySet() )
        {
            for ( Object o : sortedValues.get( period ) )
            {
                if ( ++count > limit )
                {
                    return selectedValues;
                }

                selectedValues.addPeriodValue( o, period );
            }
        }

        return selectedValues;
    }

    // -------------------------------------------------------------------------
    // Getter
    // -------------------------------------------------------------------------

    public List<Period> getPeriods()
    {
        return periods;
    }
}
