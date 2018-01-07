package org.hisp.dhis.period.comparator;

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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;

/**
 * @author Lars Helge Overland
 */
public class DescendingPeriodComparatorTest
{
    @Test
    public void testSort()
    {
        Period m03 = MonthlyPeriodType.getPeriodFromIsoString( "201603" );
        Period m04 = MonthlyPeriodType.getPeriodFromIsoString( "201604" );
        Period m05 = MonthlyPeriodType.getPeriodFromIsoString( "201605" );
        Period m06 = MonthlyPeriodType.getPeriodFromIsoString( "201606" );
        
        List<Period> periods = Lists.newArrayList( m04, m03, m06, m05 );
        List<Period> expected = Lists.newArrayList( m06, m05, m04, m03 );
        
        List<Period> sortedPeriods = periods.stream().sorted( new DescendingPeriodComparator() ).collect( Collectors.toList() );
        
        assertEquals( expected, sortedPeriods );
    }

    @Test
    public void testMin()
    {
        Period m03 = MonthlyPeriodType.getPeriodFromIsoString( "201603" );
        Period m04 = MonthlyPeriodType.getPeriodFromIsoString( "201604" );
        Period m05 = MonthlyPeriodType.getPeriodFromIsoString( "201605" );
        Period m06 = MonthlyPeriodType.getPeriodFromIsoString( "201606" );
        
        List<Period> periods = Lists.newArrayList( m04, m03, m06, m05 );
        
        Optional<Period> latest = periods.stream().min( DescendingPeriodComparator.INSTANCE );
        
        assertEquals( m06, latest.get() );
    }
}
