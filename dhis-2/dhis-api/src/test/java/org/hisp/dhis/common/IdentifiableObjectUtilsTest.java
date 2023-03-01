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
package org.hisp.dhis.common;

import static org.hisp.dhis.common.IdentifiableObjectUtils.SEPARATOR_JOIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.impl.Iso8601Calendar;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
class IdentifiableObjectUtilsTest
{
    @Test
    void testJoin()
    {
        DataElement deA = new DataElement( "DEA" );
        DataElement deB = new DataElement( "DEB" );
        DataElement deC = new DataElement( "DEC" );
        String expected = deA.getDisplayName() + SEPARATOR_JOIN + deB.getDisplayName() + SEPARATOR_JOIN
            + deC.getDisplayName();
        assertEquals( expected, IdentifiableObjectUtils.join( Lists.newArrayList( deA, deB, deC ) ) );
        assertNull( IdentifiableObjectUtils.join( null ) );
        assertNull( IdentifiableObjectUtils.join( Lists.newArrayList() ) );
    }

    @Test
    void testGetUidsBySet()
    {
        DataElement deA = new DataElement( "DEA" );
        deA.setUid( "rAFWf6BDJxX" );
        DataElement deB = new DataElement( "DEB" );
        deB.setUid( "r6PuyLmXQBp" );
        Set<String> uids = IdentifiableObjectUtils.getUidsAsSet( Lists.newArrayList( deA, null, deB ) );
        assertEquals( Set.of( "rAFWf6BDJxX", "r6PuyLmXQBp" ), uids );
    }

    @Test
    void testGetIdMap()
    {
        DataElement deA = new DataElement( "NameA" );
        DataElement deB = new DataElement( "NameB" );
        DataElement deC = new DataElement( "NameC" );
        deA.setCode( "CodeA" );
        deB.setCode( "CodeB" );
        deC.setCode( "CodeC" );
        deA.setUid( "A123456789A" );
        deB.setUid( "A123456789B" );
        deC.setUid( "A123456789C" );
        List<DataElement> elements = Lists.newArrayList( deA, deB, deC );
        Map<String, DataElement> map = IdentifiableObjectUtils.getIdMap( elements,
            IdScheme.from( IdentifiableProperty.NAME ) );
        assertEquals( deA, map.get( "NameA" ) );
        assertEquals( deB, map.get( "NameB" ) );
        assertEquals( deC, map.get( "NameC" ) );
        assertNull( map.get( "NameD" ) );
        map = IdentifiableObjectUtils.getIdMap( elements, IdScheme.from( IdentifiableProperty.UID ) );
        assertEquals( deA, map.get( "A123456789A" ) );
        assertEquals( deB, map.get( "A123456789B" ) );
        assertEquals( deC, map.get( "A123456789C" ) );
        assertNull( map.get( "A123456789D" ) );
        map = IdentifiableObjectUtils.getIdMap( elements, IdScheme.from( IdentifiableProperty.CODE ) );
        assertEquals( deA, map.get( "CodeA" ) );
        assertEquals( deB, map.get( "CodeB" ) );
        assertEquals( deC, map.get( "CodeC" ) );
        assertNull( map.get( "CodeD" ) );
    }

    @Test
    void testGetUidMapIdentifiableProperty()
    {
        DataElement deA = new DataElement( "NameA" );
        DataElement deB = new DataElement( "NameB" );
        DataElement deC = new DataElement( "NameC" );
        deA.setUid( "A123456789A" );
        deB.setUid( "A123456789B" );
        deC.setUid( "A123456789C" );
        deA.setCode( "CodeA" );
        deB.setCode( "CodeB" );
        deC.setCode( null );
        List<DataElement> elements = Lists.newArrayList( deA, deB, deC );
        Map<String, String> map = IdentifiableObjectUtils.getUidPropertyMap( elements, IdentifiableProperty.CODE );
        assertEquals( 3, map.size() );
        assertEquals( "CodeA", map.get( "A123456789A" ) );
        assertEquals( "CodeB", map.get( "A123456789B" ) );
        assertEquals( null, map.get( "A123456789C" ) );
    }

    @Test
    void testGetPeriodByPeriodType()
    {
        Calendar calendar = Iso8601Calendar.getInstance();
        WeeklyPeriodType weekly = new WeeklyPeriodType();
        MonthlyPeriodType monthly = new MonthlyPeriodType();
        QuarterlyPeriodType quarterly = new QuarterlyPeriodType();
        YearlyPeriodType yearly = new YearlyPeriodType();
        assertEquals( PeriodType.getPeriodFromIsoString( "2017W10" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "20170308" ), weekly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "2017W9" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "20170301" ), weekly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201702" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017W8" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201703" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017W9" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201705" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017W21" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201706" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017W22" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201708" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017W35" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201702" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017WedW8" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201703" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017WedW9" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201702" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017ThuW8" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201703" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017ThuW10" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201702" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017SatW7" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201703" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017SatW10" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201702" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017SunW7" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201703" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017SunW9" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201702" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017SunW7" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "201703" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017SunW9" ), monthly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "2017Q1" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "201703" ), quarterly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "2017Q2" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "201704" ), quarterly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "2016" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2016Q4" ), yearly, calendar ) );
        assertEquals( PeriodType.getPeriodFromIsoString( "2017" ), IdentifiableObjectUtils
            .getPeriodByPeriodType( PeriodType.getPeriodFromIsoString( "2017Q1" ), yearly, calendar ) );
        assertNull( PeriodType.getPeriodFromIsoString( "u3847847" ) );
    }

    @Test
    void testSortById()
    {
        DataElement deA = new DataElement();
        DataElement deB = new DataElement();
        DataElement deC = new DataElement();
        deA.setAutoFields();
        deB.setAutoFields();
        deC.setAutoFields();
        deA.setId( 1 );
        deB.setId( 2 );
        deC.setId( 3 );

        Set<DataElement> set = Set.of( deB, deC, deA );

        List<DataElement> list = IdentifiableObjectUtils.sortById( set );

        assertEquals( List.of( deA, deB, deC ), list );
    }

    @Test
    void testEqualsByUid()
    {
        DataElement deA = new DataElement();
        deA.setUid( "UIDA" );
        DataElement deA1 = new DataElement();
        deA1.setUid( "UIDA" );
        DataElement deB = new DataElement();
        deB.setUid( "UIDB" );
        DataElement deC = new DataElement();
        deC.setUid( null );
        DataElement deD = null;
        assertFalse( IdentifiableObjectUtils.equalsByUid( deA, deB ) );
        assertTrue( IdentifiableObjectUtils.equalsByUid( deA, deA1 ) );
        assertFalse( IdentifiableObjectUtils.equalsByUid( deA, deC ) );
        assertFalse( IdentifiableObjectUtils.equalsByUid( deC, deA ) );
        assertFalse( IdentifiableObjectUtils.equalsByUid( deC, deD ) );
        assertTrue( IdentifiableObjectUtils.equalsByUid( deD, deD ) );
        assertTrue( IdentifiableObjectUtils.equalsByUid( deC, deC ) );
        assertFalse( IdentifiableObjectUtils.equalsByUid( deA, deD ) );
    }
}
