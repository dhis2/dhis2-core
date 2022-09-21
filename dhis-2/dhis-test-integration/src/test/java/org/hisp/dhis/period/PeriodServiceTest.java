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
package org.hisp.dhis.period;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kristian Nordal
 */
class PeriodServiceTest extends SingleSetupIntegrationTestBase
{
    @Autowired
    private PeriodService periodService;

    @Autowired
    private SessionFactory sessionFactory;

    // -------------------------------------------------------------------------
    // Period
    // -------------------------------------------------------------------------
    @Test
    void testAddPeriod()
    {
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
        Iterator<PeriodType> it = periodTypes.iterator();
        PeriodType periodTypeA = it.next();
        PeriodType periodTypeB = it.next();
        Period periodA = new Period( periodTypeA, getDay( 1 ), getDay( 2 ) );
        Period periodB = new Period( periodTypeA, getDay( 2 ), getDay( 3 ) );
        Period periodC = new Period( periodTypeB, getDay( 2 ), getDay( 3 ) );
        long idA = periodService.addPeriod( periodA );
        long idB = periodService.addPeriod( periodB );
        long idC = periodService.addPeriod( periodC );
        periodA = periodService.getPeriod( idA );
        assertNotNull( periodA );
        assertEquals( idA, periodA.getId() );
        assertEquals( periodTypeA, periodA.getPeriodType() );
        assertEquals( getDay( 1 ), periodA.getStartDate() );
        assertEquals( getDay( 2 ), periodA.getEndDate() );
        periodB = periodService.getPeriod( idB );
        assertNotNull( periodB );
        assertEquals( idB, periodB.getId() );
        assertEquals( periodTypeA, periodB.getPeriodType() );
        assertEquals( getDay( 2 ), periodB.getStartDate() );
        assertEquals( getDay( 3 ), periodB.getEndDate() );
        periodC = periodService.getPeriod( idC );
        assertNotNull( periodC );
        assertEquals( idC, periodC.getId() );
        assertEquals( periodTypeB, periodC.getPeriodType() );
        assertEquals( getDay( 2 ), periodC.getStartDate() );
        assertEquals( getDay( 3 ), periodC.getEndDate() );
    }

    @Test
    void testDeleteAndGetPeriod()
    {
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
        Iterator<PeriodType> it = periodTypes.iterator();
        PeriodType periodTypeA = it.next();
        PeriodType periodTypeB = it.next();
        Period periodA = new Period( periodTypeA, getDay( 1 ), getDay( 2 ) );
        Period periodB = new Period( periodTypeA, getDay( 2 ), getDay( 3 ) );
        Period periodC = new Period( periodTypeB, getDay( 2 ), getDay( 3 ) );
        Period periodD = new Period( periodTypeB, getDay( 3 ), getDay( 4 ) );
        long idA = periodService.addPeriod( periodA );
        long idB = periodService.addPeriod( periodB );
        long idC = periodService.addPeriod( periodC );
        long idD = periodService.addPeriod( periodD );
        assertNotNull( periodService.getPeriod( idA ) );
        assertNotNull( periodService.getPeriod( idB ) );
        assertNotNull( periodService.getPeriod( idC ) );
        assertNotNull( periodService.getPeriod( idD ) );
        periodService.deletePeriod( periodA );
        assertNull( periodService.getPeriod( idA ) );
        assertNotNull( periodService.getPeriod( idB ) );
        assertNotNull( periodService.getPeriod( idC ) );
        assertNotNull( periodService.getPeriod( idD ) );
        periodService.deletePeriod( periodB );
        assertNull( periodService.getPeriod( idA ) );
        assertNull( periodService.getPeriod( idB ) );
        assertNotNull( periodService.getPeriod( idC ) );
        assertNotNull( periodService.getPeriod( idD ) );
        periodService.deletePeriod( periodC );
        assertNull( periodService.getPeriod( idA ) );
        assertNull( periodService.getPeriod( idB ) );
        assertNull( periodService.getPeriod( idC ) );
        assertNotNull( periodService.getPeriod( idD ) );
        periodService.deletePeriod( periodD );
        assertNull( periodService.getPeriod( idA ) );
        assertNull( periodService.getPeriod( idB ) );
        assertNull( periodService.getPeriod( idC ) );
        assertNull( periodService.getPeriod( idD ) );
    }

    @Test
    void testGetPeriod()
    {
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
        Iterator<PeriodType> it = periodTypes.iterator();
        PeriodType periodTypeA = it.next();
        PeriodType periodTypeB = it.next();
        Period periodA = new Period( periodTypeA, getDay( 1 ), getDay( 2 ) );
        Period periodB = new Period( periodTypeA, getDay( 2 ), getDay( 3 ) );
        Period periodC = new Period( periodTypeB, getDay( 2 ), getDay( 3 ) );
        Period periodD = new Period( periodTypeB, getDay( 3 ), getDay( 4 ) );
        Period periodE = new Period( periodTypeA, getDay( 3 ), getDay( 4 ) );
        long idA = periodService.addPeriod( periodA );
        long idB = periodService.addPeriod( periodB );
        long idC = periodService.addPeriod( periodC );
        long idD = periodService.addPeriod( periodD );
        long idE = periodService.addPeriod( periodE );
        periodA = periodService.getPeriod( getDay( 1 ), getDay( 2 ), periodTypeA );
        assertNotNull( periodA );
        assertEquals( idA, periodA.getId() );
        assertEquals( periodTypeA, periodA.getPeriodType() );
        assertEquals( getDay( 1 ), periodA.getStartDate() );
        assertEquals( getDay( 2 ), periodA.getEndDate() );
        periodB = periodService.getPeriod( getDay( 2 ), getDay( 3 ), periodTypeA );
        assertNotNull( periodB );
        assertEquals( idB, periodB.getId() );
        assertEquals( periodTypeA, periodB.getPeriodType() );
        assertEquals( getDay( 2 ), periodB.getStartDate() );
        assertEquals( getDay( 3 ), periodB.getEndDate() );
        periodC = periodService.getPeriod( getDay( 2 ), getDay( 3 ), periodTypeB );
        assertNotNull( periodC );
        assertEquals( idC, periodC.getId() );
        assertEquals( periodTypeB, periodC.getPeriodType() );
        assertEquals( getDay( 2 ), periodC.getStartDate() );
        assertEquals( getDay( 3 ), periodC.getEndDate() );
        periodD = periodService.getPeriod( getDay( 3 ), getDay( 4 ), periodTypeB );
        assertNotNull( periodD );
        assertEquals( idD, periodD.getId() );
        assertEquals( periodTypeB, periodD.getPeriodType() );
        assertEquals( getDay( 3 ), periodD.getStartDate() );
        assertEquals( getDay( 4 ), periodD.getEndDate() );
        periodE = periodService.getPeriod( getDay( 3 ), getDay( 4 ), periodTypeA );
        assertNotNull( periodE );
        assertEquals( idE, periodE.getId() );
        assertEquals( periodTypeA, periodE.getPeriodType() );
        assertEquals( getDay( 3 ), periodE.getStartDate() );
        assertEquals( getDay( 4 ), periodE.getEndDate() );
        assertNull( periodService.getPeriod( getDay( 1 ), getDay( 2 ), periodTypeB ) );
        assertNull( periodService.getPeriod( getDay( 4 ), getDay( 5 ), periodTypeA ) );
        assertNull( periodService.getPeriod( getDay( 1 ), getDay( 5 ), periodTypeB ) );
        assertNull( periodService.getPeriod( getDay( 4 ), getDay( 3 ), periodTypeB ) );
        assertNull( periodService.getPeriod( getDay( 5 ), getDay( 6 ), periodTypeA ) );
    }

    @Test
    void testGetAllPeriods()
    {
        PeriodType periodType = PeriodType.getAvailablePeriodTypes().iterator().next();
        Period periodA = new Period( periodType, getDay( 1 ), getDay( 2 ) );
        Period periodB = new Period( periodType, getDay( 2 ), getDay( 3 ) );
        Period periodC = new Period( periodType, getDay( 3 ), getDay( 4 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        List<Period> periods = periodService.getAllPeriods();
        assertNotNull( periods );
        assertEquals( 3, periods.size() );
        assertTrue( periods.contains( periodA ) );
        assertTrue( periods.contains( periodB ) );
        assertTrue( periods.contains( periodC ) );
    }

    @Test
    void testGetPeriodsBetweenDates()
    {
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
        Iterator<PeriodType> it = periodTypes.iterator();
        PeriodType periodTypeA = it.next();
        PeriodType periodTypeB = it.next();
        Period periodA = new Period( periodTypeA, getDay( 1 ), getDay( 2 ) );
        Period periodB = new Period( periodTypeA, getDay( 2 ), getDay( 3 ) );
        Period periodC = new Period( periodTypeB, getDay( 2 ), getDay( 3 ) );
        Period periodD = new Period( periodTypeB, getDay( 3 ), getDay( 4 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        periodService.addPeriod( periodD );
        List<Period> periods = periodService.getPeriodsBetweenDates( getDay( 1 ), getDay( 1 ) );
        assertNotNull( periods );
        assertEquals( 0, periods.size() );
        periods = periodService.getPeriodsBetweenDates( getDay( 1 ), getDay( 2 ) );
        assertNotNull( periods );
        assertEquals( 1, periods.size() );
        assertEquals( periodA, periods.iterator().next() );
        periods = periodService.getPeriodsBetweenDates( getDay( 2 ), getDay( 4 ) );
        assertNotNull( periods );
        assertEquals( 3, periods.size() );
        assertTrue( periods.contains( periodB ) );
        assertTrue( periods.contains( periodC ) );
        assertTrue( periods.contains( periodD ) );
        periods = periodService.getPeriodsBetweenDates( getDay( 1 ), getDay( 5 ) );
        assertNotNull( periods );
        assertEquals( 4, periods.size() );
        assertTrue( periods.contains( periodA ) );
        assertTrue( periods.contains( periodB ) );
        assertTrue( periods.contains( periodC ) );
        assertTrue( periods.contains( periodD ) );
    }

    @Test
    void testGetIntersectingPeriods()
    {
        PeriodType type = PeriodType.getAvailablePeriodTypes().iterator().next();
        Period periodA = new Period( type, getDay( 1 ), getDay( 2 ) );
        Period periodB = new Period( type, getDay( 2 ), getDay( 4 ) );
        Period periodC = new Period( type, getDay( 4 ), getDay( 6 ) );
        Period periodD = new Period( type, getDay( 6 ), getDay( 8 ) );
        Period periodE = new Period( type, getDay( 8 ), getDay( 10 ) );
        Period periodF = new Period( type, getDay( 10 ), getDay( 12 ) );
        Period periodG = new Period( type, getDay( 12 ), getDay( 14 ) );
        Period periodH = new Period( type, getDay( 2 ), getDay( 6 ) );
        Period periodI = new Period( type, getDay( 8 ), getDay( 12 ) );
        Period periodJ = new Period( type, getDay( 2 ), getDay( 12 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        periodService.addPeriod( periodD );
        periodService.addPeriod( periodE );
        periodService.addPeriod( periodF );
        periodService.addPeriod( periodG );
        periodService.addPeriod( periodH );
        periodService.addPeriod( periodI );
        periodService.addPeriod( periodJ );
        List<Period> periods = periodService.getIntersectingPeriods( getDay( 4 ), getDay( 10 ) );
        assertEquals( periods.size(), 8 );
        assertTrue( periods.contains( periodB ) );
        assertTrue( periods.contains( periodC ) );
        assertTrue( periods.contains( periodD ) );
        assertTrue( periods.contains( periodE ) );
        assertTrue( periods.contains( periodF ) );
        assertTrue( periods.contains( periodH ) );
        assertTrue( periods.contains( periodI ) );
        assertTrue( periods.contains( periodJ ) );
    }

    @Test
    void testGetPeriodsByPeriodType()
    {
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
        Iterator<PeriodType> it = periodTypes.iterator();
        PeriodType periodTypeA = it.next();
        PeriodType periodTypeB = it.next();
        PeriodType periodTypeC = it.next();
        Period periodA = new Period( periodTypeA, getDay( 1 ), getDay( 2 ) );
        Period periodB = new Period( periodTypeA, getDay( 2 ), getDay( 3 ) );
        Period periodC = new Period( periodTypeA, getDay( 3 ), getDay( 4 ) );
        Period periodD = new Period( periodTypeB, getDay( 3 ), getDay( 4 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        periodService.addPeriod( periodD );
        List<Period> periodsARef = new ArrayList<>();
        periodsARef.add( periodA );
        periodsARef.add( periodB );
        periodsARef.add( periodC );
        List<Period> periodsA = periodService.getPeriodsByPeriodType( periodTypeA );
        assertNotNull( periodsA );
        assertEquals( periodsARef.size(), periodsA.size() );
        assertTrue( periodsA.containsAll( periodsARef ) );
        List<Period> periodsB = periodService.getPeriodsByPeriodType( periodTypeB );
        assertNotNull( periodsB );
        assertEquals( 1, periodsB.size() );
        assertEquals( periodD, periodsB.iterator().next() );
        List<Period> periodsC = periodService.getPeriodsByPeriodType( periodTypeC );
        assertNotNull( periodsC );
        assertEquals( 0, periodsC.size() );
    }

    @Test
    void testGetInclusivePeriods()
    {
        PeriodType periodType = PeriodType.getAvailablePeriodTypes().iterator().next();
        Period periodA = new Period( periodType, getDay( 5 ), getDay( 8 ) );
        Period periodB = new Period( periodType, getDay( 8 ), getDay( 11 ) );
        Period periodC = new Period( periodType, getDay( 11 ), getDay( 14 ) );
        Period periodD = new Period( periodType, getDay( 14 ), getDay( 17 ) );
        Period periodE = new Period( periodType, getDay( 17 ), getDay( 20 ) );
        Period periodF = new Period( periodType, getDay( 5 ), getDay( 20 ) );
        List<Period> periods = new ArrayList<>();
        periods.add( periodA );
        periods.add( periodB );
        periods.add( periodC );
        periods.add( periodD );
        periods.add( periodE );
        periods.add( periodF );
        Period basePeriod = new Period( periodType, getDay( 8 ), getDay( 20 ) );
        List<Period> inclusivePeriods = periodService.getInclusivePeriods( basePeriod, periods );
        assertTrue( inclusivePeriods.size() == 4 );
        assertTrue( inclusivePeriods.contains( periodB ) );
        assertTrue( inclusivePeriods.contains( periodC ) );
        assertTrue( inclusivePeriods.contains( periodD ) );
        assertTrue( inclusivePeriods.contains( periodE ) );
        basePeriod = new Period( periodType, getDay( 9 ), getDay( 18 ) );
        inclusivePeriods = periodService.getInclusivePeriods( basePeriod, periods );
        assertTrue( inclusivePeriods.size() == 2 );
        assertTrue( inclusivePeriods.contains( periodC ) );
        assertTrue( inclusivePeriods.contains( periodD ) );
        basePeriod = new Period( periodType, getDay( 2 ), getDay( 5 ) );
        inclusivePeriods = periodService.getInclusivePeriods( basePeriod, periods );
        assertTrue( inclusivePeriods.size() == 0 );
    }

    // -------------------------------------------------------------------------
    // PeriodType
    // -------------------------------------------------------------------------
    @Test
    void testGetAndGetAllPeriodTypes()
    {
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
        Iterator<PeriodType> it = periodTypes.iterator();
        PeriodType periodTypeA = it.next();
        PeriodType periodTypeB = it.next();
        PeriodType periodTypeC = it.next();
        PeriodType periodTypeD = it.next();
        assertNotNull( periodService.getPeriodTypeByName( periodTypeA.getName() ) );
        assertNotNull( periodService.getPeriodTypeByName( periodTypeB.getName() ) );
        assertNotNull( periodService.getPeriodTypeByName( periodTypeC.getName() ) );
        assertNotNull( periodService.getPeriodTypeByName( periodTypeD.getName() ) );
    }

    @Test
    void testGetPeriodTypeByName()
    {
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
        Iterator<PeriodType> it = periodTypes.iterator();
        PeriodType refA = it.next();
        PeriodType refB = it.next();
        PeriodType periodTypeA = periodService.getPeriodTypeByName( refA.getName() );
        assertNotNull( periodTypeA );
        assertEquals( refA.getName(), periodTypeA.getName() );
        PeriodType periodTypeB = periodService.getPeriodTypeByName( refB.getName() );
        assertNotNull( periodTypeB );
        assertEquals( refB.getName(), periodTypeB.getName() );
    }

    @Test
    void testDeleteAndGetPeriodType()
    {
        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
        Iterator<PeriodType> it = periodTypes.iterator();
        PeriodType periodTypeA = it.next();
        PeriodType periodTypeB = it.next();
        PeriodType periodTypeC = it.next();
        PeriodType periodTypeD = it.next();
        int idA = periodTypeA.getId();
        int idB = periodTypeB.getId();
        int idC = periodTypeC.getId();
        int idD = periodTypeD.getId();
        assertNotNull( periodService.getPeriodType( idA ) );
        assertNotNull( periodService.getPeriodType( idB ) );
        assertNotNull( periodService.getPeriodType( idC ) );
        assertNotNull( periodService.getPeriodType( idD ) );
        assertNotNull( periodService.getPeriodType( periodTypeA.getId() ) );
        assertNotNull( periodService.getPeriodType( periodTypeB.getId() ) );
        assertNotNull( periodService.getPeriodType( periodTypeC.getId() ) );
        assertNotNull( periodService.getPeriodType( periodTypeD.getId() ) );
    }

    @Test
    void testReloadPeriodInStatelessSession()
    {
        Period period = periodService.reloadIsoPeriodInStatelessSession( "202510" );
        assertNotNull( period );
        removeTestPeriod( "202510" );
    }

    private void removeTestPeriod( String period )
    {
        StatelessSession session = sessionFactory.openStatelessSession();
        session.beginTransaction();
        try
        {
            session.delete( periodService.getPeriod( period ) );
            session.getTransaction().commit();
        }
        catch ( Exception ex )
        {
            session.getTransaction().rollback();
        }
        finally
        {
            session.close();
        }
    }
}
