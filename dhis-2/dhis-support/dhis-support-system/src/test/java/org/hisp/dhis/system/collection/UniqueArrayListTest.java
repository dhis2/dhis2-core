package org.hisp.dhis.system.collection;

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
import java.util.List;

import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* @author Lars Helge Overland
*/
public class UniqueArrayListTest
{
    @Test
    public void testAdd()
    {
        List<Period> list = new UniqueArrayList<>();
        
        PeriodType periodType = new MonthlyPeriodType();
        Period peA = periodType.createPeriod( new DateTime( 2000, 1, 1, 0, 0 ).toDate() );
        Period peB = periodType.createPeriod( new DateTime( 2000, 1, 1, 0, 0 ).toDate() ); // Duplicate
        Period peC = periodType.createPeriod( new DateTime( 2000, 2, 1, 0, 0 ).toDate() );
        
        list.add( peA );
        list.add( peB );
        list.add( peC );
        
        assertEquals( 2, list.size() );
        assertTrue( list.contains( peA ) );
        assertTrue( list.contains( peB ) );
        assertTrue( list.contains( peC ) );
    }

    @Test
    public void testAddAll()
    {
        List<Period> list = new ArrayList<>();
        
        PeriodType periodType = new MonthlyPeriodType();
        Period peA = periodType.createPeriod( new DateTime( 2000, 1, 1, 0, 0 ).toDate() );
        Period peB = periodType.createPeriod( new DateTime( 2000, 1, 1, 0, 0 ).toDate() ); // Duplicate
        Period peC = periodType.createPeriod( new DateTime( 2000, 2, 1, 0, 0 ).toDate() );
        
        list.add( peA );
        list.add( peB );
        list.add( peC );
        
        assertEquals( 3, list.size() );
        
        List<Period> uniqueList = new UniqueArrayList<>();
        uniqueList.addAll( list );
        
        assertEquals( 2, uniqueList.size() );
        assertTrue( uniqueList.contains( peA ) );
        assertTrue( uniqueList.contains( peB ) );
        assertTrue( uniqueList.contains( peC ) );
    }
    
    @Test
    public void testCollectionConstructor()
    {
        List<Period> list = new ArrayList<>();
        
        PeriodType periodType = new MonthlyPeriodType();
        Period peA = periodType.createPeriod( new DateTime( 2000, 1, 1, 0, 0 ).toDate() );
        Period peB = periodType.createPeriod( new DateTime( 2000, 1, 1, 0, 0 ).toDate() ); // Duplicate
        Period peC = periodType.createPeriod( new DateTime( 2000, 2, 1, 0, 0 ).toDate() );
        
        list.add( peA );
        list.add( peB );
        list.add( peC );
        
        assertEquals( 3, list.size() );
        
        List<Period> uniqueList = new UniqueArrayList<>( list );
        
        assertEquals( 2, uniqueList.size() );
        assertTrue( uniqueList.contains( peA ) );
        assertTrue( uniqueList.contains( peB ) );
        assertTrue( uniqueList.contains( peC ) );
    }
}
