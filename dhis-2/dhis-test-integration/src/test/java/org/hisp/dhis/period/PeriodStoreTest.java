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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Torgeir Lorange Ostby
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class PeriodStoreTest extends PostgresIntegrationTestBase {
  @Autowired private PeriodStore periodStore;

  // -------------------------------------------------------------------------
  // Period
  // -------------------------------------------------------------------------
  @Test
  void testAddPeriod() {
    List<PeriodType> periodTypes = periodStore.getAllPeriodTypes();
    Iterator<PeriodType> it = periodTypes.iterator();
    PeriodType periodTypeA = it.next();
    PeriodType periodTypeB = it.next();
    Period periodA = new Period(periodTypeA, getDay(1), getDay(2));
    Period periodB = new Period(periodTypeA, getDay(2), getDay(3));
    Period periodC = new Period(periodTypeB, getDay(2), getDay(3));
    periodStore.addPeriod(periodA);
    long idA = periodA.getId();
    periodStore.addPeriod(periodB);
    long idB = periodB.getId();
    periodStore.addPeriod(periodC);
    long idC = periodC.getId();
    periodA = periodStore.get(idA);
    assertNotNull(periodA);
    assertEquals(idA, periodA.getId());
    assertEquals(periodTypeA, periodA.getPeriodType());
    assertEquals(getDay(1), periodA.getStartDate());
    assertEquals(getDay(2), periodA.getEndDate());
    periodB = periodStore.get(idB);
    assertNotNull(periodB);
    assertEquals(idB, periodB.getId());
    assertEquals(periodTypeA, periodB.getPeriodType());
    assertEquals(getDay(2), periodB.getStartDate());
    assertEquals(getDay(3), periodB.getEndDate());
    periodC = periodStore.get(idC);
    assertNotNull(periodC);
    assertEquals(idC, periodC.getId());
    assertEquals(periodTypeB, periodC.getPeriodType());
    assertEquals(getDay(2), periodC.getStartDate());
    assertEquals(getDay(3), periodC.getEndDate());
  }

  @Test
  void testGetAllPeriods() {
    PeriodType periodType = periodStore.getAllPeriodTypes().iterator().next();
    Period periodA = new Period(periodType, getDay(1), getDay(1));
    Period periodB = new Period(periodType, getDay(2), getDay(2));
    Period periodC = new Period(periodType, getDay(3), getDay(3));
    periodStore.addPeriod(periodA);
    periodStore.addPeriod(periodB);
    periodStore.addPeriod(periodC);
    List<Period> periods = periodStore.getAll();
    assertNotNull(periods);
    assertEquals(3, periods.size());
    assertTrue(periods.contains(periodA));
    assertTrue(periods.contains(periodB));
    assertTrue(periods.contains(periodC));
  }
}
