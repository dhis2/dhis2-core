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
package org.hisp.dhis.analytics.table;

import static org.hisp.dhis.test.TestBase.createPeriod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.analytics.table.util.PartitionUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class PartitionUtilsTest {
  private final PeriodType quarterly = new QuarterlyPeriodType();

  private final PeriodDimension q1 =
      new PeriodDimension(quarterly.createPeriod(new DateTime(2018, 7, 1, 0, 0).toDate()));
  private final PeriodDimension q2 =
      new PeriodDimension(quarterly.createPeriod(new DateTime(2018, 10, 1, 0, 0).toDate()));
  private final PeriodDimension q3 =
      new PeriodDimension(quarterly.createPeriod(new DateTime(2019, 1, 1, 0, 0).toDate()));

  @Test
  void testGetPartitions() {
    assertEquals(
        new Partitions(Set.of(2000)), PartitionUtils.getPartitions(createPeriod("200001")));
    assertEquals(
        new Partitions(Set.of(2001)), PartitionUtils.getPartitions(createPeriod("200110")));
    assertEquals(
        new Partitions(Set.of(2002)), PartitionUtils.getPartitions(createPeriod("2002Q2")));
    assertEquals(
        new Partitions(Set.of(2003)), PartitionUtils.getPartitions(createPeriod("2003S2")));
    assertEquals(
        new Partitions(Set.of(2000, 2001)), PartitionUtils.getPartitions(createPeriod("2000July")));
    assertEquals(
        new Partitions(Set.of(2001, 2002)),
        PartitionUtils.getPartitions(createPeriod("2001April")));
  }

  @Test
  void getGetPartitionsLongPeriods() {
    Period period = new Period();
    period.setStartDate(new DateTime(2008, 3, 1, 0, 0).toDate());
    period.setEndDate(new DateTime(2011, 7, 1, 0, 0).toDate());
    Partitions expected = new Partitions(Set.of(2008, 2009, 2010, 2011));

    assertEquals(expected, PartitionUtils.getPartitions(period));

    period = new Period();
    period.setStartDate(new DateTime(2009, 8, 1, 0, 0).toDate());
    period.setEndDate(new DateTime(2010, 2, 1, 0, 0).toDate());
    expected = new Partitions(Set.of(2009, 2010));

    assertEquals(expected, PartitionUtils.getPartitions(period));
  }

  @Test
  void testGetTablePartitionsFromQuery() {
    DataQueryParams params = DataQueryParams.newBuilder().withPeriods(List.of(q1, q2, q3)).build();
    Partitions partitions = PartitionUtils.getPartitions(params, AnalyticsTableType.DATA_VALUE);

    assertEquals(3, partitions.getPartitions().size());
    assertTrue(partitions.getPartitions().contains(2018));
    assertTrue(partitions.getPartitions().contains(2019));
    assertTrue(partitions.getPartitions().contains(0));

    params = DataQueryParams.newBuilder().withPeriods(List.of(q1)).build();

    partitions = PartitionUtils.getPartitions(params, AnalyticsTableType.ORG_UNIT_TARGET);

    assertEquals(1, partitions.getPartitions().size());
    assertTrue(partitions.getPartitions().contains(2018));
  }
}
