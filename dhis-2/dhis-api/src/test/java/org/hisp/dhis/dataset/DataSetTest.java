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
package org.hisp.dhis.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DataSetTest {

  @Test
  void testAddDataSetElement() {
    DataSet dsA = new DataSet("DataSetA");
    DataSet dsB = new DataSet("DataSetB");
    DataElement deA = new DataElement("DataElementA");
    DataElement deB = new DataElement("DataElementB");
    dsA.addDataSetElement(deA);
    dsA.addDataSetElement(deB);
    dsB.addDataSetElement(deA);
    assertEquals(2, dsA.getDataSetElements().size());
    assertEquals(1, dsB.getDataSetElements().size());
    assertEquals(2, deA.getDataSetElements().size());
    assertEquals(1, deB.getDataSetElements().size());
  }

  @Test
  void testUpdateOrganisationUnits() {
    DataSet dsA = new DataSet("dsA");
    OrganisationUnit ouA = new OrganisationUnit("ouA");
    OrganisationUnit ouB = new OrganisationUnit("ouB");
    OrganisationUnit ouC = new OrganisationUnit("ouC");
    OrganisationUnit ouD = new OrganisationUnit("ouD");
    dsA.addOrganisationUnit(ouA);
    dsA.addOrganisationUnit(ouB);
    assertEquals(2, dsA.getSources().size());
    assertTrue(dsA.getSources().containsAll(Sets.newHashSet(ouA, ouB)));
    assertTrue(ouA.getDataSets().contains(dsA));
    assertTrue(ouB.getDataSets().contains(dsA));
    assertTrue(ouC.getDataSets().isEmpty());
    assertTrue(ouD.getDataSets().isEmpty());
    dsA.updateOrganisationUnits(Sets.newHashSet(ouB, ouC));
    assertEquals(2, dsA.getSources().size());
    assertTrue(dsA.getSources().containsAll(Sets.newHashSet(ouB, ouC)));
    assertTrue(ouA.getDataSets().isEmpty());
    assertTrue(ouB.getDataSets().contains(dsA));
    assertTrue(ouC.getDataSets().contains(dsA));
    assertTrue(ouD.getDataSets().isEmpty());
  }

  @Test
  void testAddIndicator() {
    DataSet dsA = new DataSet("DataSetA");
    Indicator indicatorA = new Indicator();
    Indicator indicatorB = new Indicator();
    indicatorA.setName("Indicator A");
    indicatorB.setName("Indicator B");
    dsA.addIndicator(indicatorA);
    assertEquals(1, dsA.getIndicators().size());
    assertTrue(dsA.getIndicators().contains(indicatorA));
    assertEquals(1, indicatorA.getDataSets().size());
    assertTrue(indicatorA.getDataSets().contains(dsA));
    dsA.addIndicator(indicatorB);
    assertEquals(2, dsA.getIndicators().size());
    assertTrue(dsA.getIndicators().contains(indicatorA));
    assertTrue(dsA.getIndicators().contains(indicatorB));
    assertEquals(1, indicatorA.getDataSets().size());
    assertEquals(1, indicatorB.getDataSets().size());
    assertTrue(indicatorA.getDataSets().contains(dsA));
    assertTrue(indicatorB.getDataSets().contains(dsA));
  }

  @Test
  void testIsLocked_BeforeFirstDayOfPeriod() {
    assertIsLocked(false, period -> new Date(period.getStartDate().getTime() - 1L));
  }

  @Test
  void testIsLocked_FirstDayOfPeriod() {
    assertIsLocked(false, Period::getStartDate);
  }

  @Test
  void testIsLocked_LastDayOfPeriod() {
    assertIsLocked(false, Period::getEndDate);
  }

  private static void assertIsLocked(boolean expected, Function<Period, Date> actual) {
    Date now = new Date();
    Period thisMonth = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY).createPeriod(now);
    DataSet ds = new DataSet();
    ds.setExpiryDays(1);
    assertEquals(expected, ds.isLocked(null, thisMonth, actual.apply(thisMonth)));
  }

  private static Date createDateFromSimpleDateFormat(String date) {
    try {
      return new SimpleDateFormat("MMM d yyyy HH:mm:ss").parse(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void isLockedAtCertainTime() {
    Date someTimeAgo = createDateFromSimpleDateFormat("Jan 1 2022 12:15:56");
    Period thisMonth = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY).createPeriod(someTimeAgo);
    Date periodEndDate = createDateFromSimpleDateFormat("Jan 31 2022 00:00:00");
    assertEquals("202201", thisMonth.getIsoDate());
    assertEquals(periodEndDate, thisMonth.getEndDate());
    DataSet ds = new DataSet();
    ds.setExpiryDays(1.5);
    Date dataEntryClosed = createDateFromSimpleDateFormat("Feb 2 2022 12:00:01");
    assertTrue(ds.isLocked(null, thisMonth, dataEntryClosed));
    Date dataEntryOpen = createDateFromSimpleDateFormat("Feb 2 2022 12:00:00");
    assertFalse(ds.isLocked(null, thisMonth, dataEntryOpen));
  }
}
