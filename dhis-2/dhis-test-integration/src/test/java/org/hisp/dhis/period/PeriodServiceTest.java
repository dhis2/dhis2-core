/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Nordal
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class PeriodServiceTest extends PostgresIntegrationTestBase {
  @Autowired private PeriodService periodService;

  @Autowired private EntityManagerFactory entityManagerFactory;

  // -------------------------------------------------------------------------
  // Period
  // -------------------------------------------------------------------------
  @Test
  void testAddPeriod() {
    List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
    Iterator<PeriodType> it = periodTypes.iterator();
    PeriodType periodTypeA = it.next();
    PeriodType periodTypeB = it.next();
    Period periodA = new Period(periodTypeA, getDay(1), getDay(2));
    Period periodB = new Period(periodTypeA, getDay(2), getDay(3));
    Period periodC = new Period(periodTypeB, getDay(2), getDay(3));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodC);
    assertNotEquals(0L, periodA.getId());
    assertEquals(periodTypeA, periodA.getPeriodType());
    assertEquals(getDay(1), periodA.getStartDate());
    assertEquals(getDay(2), periodA.getEndDate());
    assertNotEquals(0L, periodB.getId());
    assertEquals(periodTypeA, periodB.getPeriodType());
    assertEquals(getDay(2), periodB.getStartDate());
    assertEquals(getDay(3), periodB.getEndDate());
    assertNotEquals(0L, periodC.getId());
    assertEquals(periodTypeB, periodC.getPeriodType());
    assertEquals(getDay(2), periodC.getStartDate());
    assertEquals(getDay(3), periodC.getEndDate());
  }

  @Test
  void testDeleteAndGetPeriod() {
    List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
    Iterator<PeriodType> it = periodTypes.iterator();
    PeriodType periodTypeA = it.next();
    PeriodType periodTypeB = it.next();
    Period periodA = periodTypeA.createPeriod(getDay(1));
    Period periodB = periodA.next();
    Period periodC = periodTypeB.createPeriod(getDay(1));
    Period periodD = periodC.next();
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodC);
    periodService.addPeriod(periodD);
    assertNotEquals(0L, periodA.getId());
    assertNotEquals(0L, periodB.getId());
    assertNotEquals(0L, periodC.getId());
    assertNotEquals(0L, periodC.getId());
    periodService.deletePeriod(periodA);
    assertNull(periodService.getPeriod(periodA.getIsoDate()));
    assertNotNull(periodService.getPeriod(periodB.getIsoDate()));
    assertNotNull(periodService.getPeriod(periodC.getIsoDate()));
    assertNotNull(periodService.getPeriod(periodD.getIsoDate()));
    periodService.deletePeriod(periodB);
    assertNull(periodService.getPeriod(periodA.getIsoDate()));
    assertNull(periodService.getPeriod(periodB.getIsoDate()));
    assertNotNull(periodService.getPeriod(periodC.getIsoDate()));
    assertNotNull(periodService.getPeriod(periodD.getIsoDate()));
    periodService.deletePeriod(periodC);
    assertNull(periodService.getPeriod(periodA.getIsoDate()));
    assertNull(periodService.getPeriod(periodB.getIsoDate()));
    assertNull(periodService.getPeriod(periodC.getIsoDate()));
    assertNotNull(periodService.getPeriod(periodD.getIsoDate()));
    periodService.deletePeriod(periodD);
    assertNull(periodService.getPeriod(periodA.getIsoDate()));
    assertNull(periodService.getPeriod(periodB.getIsoDate()));
    assertNull(periodService.getPeriod(periodC.getIsoDate()));
    assertNull(periodService.getPeriod(periodD.getIsoDate()));
  }

  @Test
  void testGetAllPeriods() {
    PeriodType periodType = PeriodType.getAvailablePeriodTypes().iterator().next();
    Period periodA = new Period(periodType, getDay(1), getDay(2));
    Period periodB = new Period(periodType, getDay(2), getDay(3));
    Period periodC = new Period(periodType, getDay(3), getDay(4));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodC);
    List<Period> periods = periodService.getAllPeriods();
    assertNotNull(periods);
    assertEquals(3, periods.size());
    assertTrue(periods.contains(periodA));
    assertTrue(periods.contains(periodB));
    assertTrue(periods.contains(periodC));
  }

  @Test
  void testGetPeriodsBetweenDates() {
    Period periodA = PeriodType.getPeriodType(PeriodTypeEnum.DAILY).createPeriod(getDay(2));
    Period periodB = periodA.next();
    Period periodC = periodB.next();
    Period periodD = periodC.next();
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodC);
    periodService.addPeriod(periodD);
    List<Period> periods = periodService.getPeriodsBetweenDates(getDay(1), getDay(1));
    assertNotNull(periods);
    assertEquals(0, periods.size());
    periods = periodService.getPeriodsBetweenDates(getDay(1), getDay(2));
    assertNotNull(periods);
    assertEquals(1, periods.size());
    assertEquals(periodA, periods.iterator().next());
    periods = periodService.getPeriodsBetweenDates(getDay(2), getDay(4));
    assertNotNull(periods);
    assertEquals(3, periods.size());
    assertTrue(periods.contains(periodA));
    assertTrue(periods.contains(periodB));
    assertTrue(periods.contains(periodC));
    periods = periodService.getPeriodsBetweenDates(getDay(1), getDay(5));
    assertNotNull(periods);
    assertEquals(4, periods.size());
    assertTrue(periods.contains(periodA));
    assertTrue(periods.contains(periodB));
    assertTrue(periods.contains(periodC));
    assertTrue(periods.contains(periodD));
  }

  @Test
  void testGetIntersectingPeriods() {
    PeriodType type = PeriodType.getPeriodType(PeriodTypeEnum.DAILY);
    Period periodA = type.createPeriod(getDay(1));
    Period periodB = periodA.next();
    Period periodC = periodB.next();
    Period periodD = periodC.next();
    Period periodE = periodD.next();
    Period periodF = periodE.next();
    Period periodG = periodF.next();
    Period periodH = periodG.next();
    Period periodI = periodH.next();
    Period periodJ = periodI.next();
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    periodService.addPeriod(periodC);
    periodService.addPeriod(periodD);
    periodService.addPeriod(periodE);
    periodService.addPeriod(periodF);
    periodService.addPeriod(periodG);
    periodService.addPeriod(periodH);
    periodService.addPeriod(periodI);
    periodService.addPeriod(periodJ);
    List<Period> periods = periodService.getIntersectingPeriods(getDay(3), getDay(10));
    assertEquals(8, periods.size());
    assertTrue(periods.contains(periodC));
    assertTrue(periods.contains(periodD));
    assertTrue(periods.contains(periodE));
    assertTrue(periods.contains(periodF));
    assertTrue(periods.contains(periodG));
    assertTrue(periods.contains(periodH));
    assertTrue(periods.contains(periodI));
    assertTrue(periods.contains(periodJ));
  }

  @Test
  void testGetInclusivePeriods() {
    PeriodType periodType = PeriodType.getAvailablePeriodTypes().iterator().next();
    Period periodA = new Period(periodType, getDay(5), getDay(8));
    Period periodB = new Period(periodType, getDay(8), getDay(11));
    Period periodC = new Period(periodType, getDay(11), getDay(14));
    Period periodD = new Period(periodType, getDay(14), getDay(17));
    Period periodE = new Period(periodType, getDay(17), getDay(20));
    Period periodF = new Period(periodType, getDay(5), getDay(20));
    List<Period> periods = new ArrayList<>();
    periods.add(periodA);
    periods.add(periodB);
    periods.add(periodC);
    periods.add(periodD);
    periods.add(periodE);
    periods.add(periodF);
    Period basePeriod = new Period(periodType, getDay(8), getDay(20));
    List<Period> inclusivePeriods = periodService.getInclusivePeriods(basePeriod, periods);
    assertTrue(inclusivePeriods.size() == 4);
    assertTrue(inclusivePeriods.contains(periodB));
    assertTrue(inclusivePeriods.contains(periodC));
    assertTrue(inclusivePeriods.contains(periodD));
    assertTrue(inclusivePeriods.contains(periodE));
    basePeriod = new Period(periodType, getDay(9), getDay(18));
    inclusivePeriods = periodService.getInclusivePeriods(basePeriod, periods);
    assertTrue(inclusivePeriods.size() == 2);
    assertTrue(inclusivePeriods.contains(periodC));
    assertTrue(inclusivePeriods.contains(periodD));
    basePeriod = new Period(periodType, getDay(2), getDay(5));
    inclusivePeriods = periodService.getInclusivePeriods(basePeriod, periods);
    assertTrue(inclusivePeriods.size() == 0);
  }

  // -------------------------------------------------------------------------
  // PeriodType
  // -------------------------------------------------------------------------
  @Test
  void testGetAndGetAllPeriodTypes() {
    List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
    Iterator<PeriodType> it = periodTypes.iterator();
    PeriodType periodTypeA = it.next();
    PeriodType periodTypeB = it.next();
    PeriodType periodTypeC = it.next();
    PeriodType periodTypeD = it.next();
    assertNotNull(periodService.getPeriodTypeByName(periodTypeA.getName()));
    assertNotNull(periodService.getPeriodTypeByName(periodTypeB.getName()));
    assertNotNull(periodService.getPeriodTypeByName(periodTypeC.getName()));
    assertNotNull(periodService.getPeriodTypeByName(periodTypeD.getName()));
  }

  @Test
  void testGetPeriodTypeByName() {
    List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();
    Iterator<PeriodType> it = periodTypes.iterator();
    PeriodType refA = it.next();
    PeriodType refB = it.next();
    PeriodType periodTypeA = periodService.getPeriodTypeByName(refA.getName());
    assertNotNull(periodTypeA);
    assertEquals(refA.getName(), periodTypeA.getName());
    PeriodType periodTypeB = periodService.getPeriodTypeByName(refB.getName());
    assertNotNull(periodTypeB);
    assertEquals(refB.getName(), periodTypeB.getName());
  }

  @Test
  void testReloadPeriodInStatelessSession() {
    Period period = periodService.reloadIsoPeriodInStatelessSession("202510");
    assertNotNull(period);
    removeTestPeriod("202510");
  }

  private void removeTestPeriod(String period) {
    StatelessSession session =
        entityManagerFactory.unwrap(SessionFactory.class).openStatelessSession();
    session.beginTransaction();
    try {
      session.delete(periodService.getPeriod(period));
      session.getTransaction().commit();
    } catch (Exception ex) {
      session.getTransaction().rollback();
    } finally {
      session.close();
    }
  }
}
