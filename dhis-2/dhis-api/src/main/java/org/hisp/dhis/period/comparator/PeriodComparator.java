package org.hisp.dhis.period.comparator;

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

import java.util.Comparator;

import org.hisp.dhis.period.Period;

/**
 * Sorts periods descending based on the start date, then the end date.
 * 
 * @author Lars Helge Overland
 */
public class PeriodComparator
    implements Comparator<Period>
{
    public static final PeriodComparator INSTANCE = new PeriodComparator();
    
    @Override
    public int compare( Period period1, Period period2 )
    {
        if ( period1.getStartDate() == null )
        {
            return -1;
        }
        
        if ( period2.getStartDate() == null )
        {
            return 1;
        }
        
        if ( period1.getStartDate().compareTo( period2.getStartDate() ) != 0 )
        {
            return period1.getStartDate().compareTo( period2.getStartDate() ) * -1;
        }
        
        if ( period1.getEndDate() == null )
        {
            return -1;
        }
        
        if ( period2.getEndDate() == null )
        {
            return 1;
        }
        
        return period1.getEndDate().compareTo( period2.getEndDate() ) * -1;
    }
}
