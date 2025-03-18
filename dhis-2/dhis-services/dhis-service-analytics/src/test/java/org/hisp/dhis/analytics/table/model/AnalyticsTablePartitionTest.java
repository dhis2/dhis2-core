/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.table.model;

import static org.hisp.dhis.db.model.DataType.BIGINT;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.db.model.Logged;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class AnalyticsTablePartitionTest {
  private final List<AnalyticsTableColumn> columnsA =
      List.of(
          AnalyticsTableColumn.builder().name("id").dataType(BIGINT).selectExpression("id").build(),
          AnalyticsTableColumn.builder()
              .name("data")
              .dataType(CHARACTER_11)
              .selectExpression("data")
              .build(),
          AnalyticsTableColumn.builder()
              .name("value")
              .dataType(DOUBLE)
              .selectExpression("value")
              .build());

  private final List<String> sortKeyA = List.of("data");

  @Test
  void testGetName() {
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.DATA_VALUE, columnsA, sortKeyA, Logged.UNLOGGED);

    List<String> checks = List.of("value = 2023");

    AnalyticsTablePartition partition =
        new AnalyticsTablePartition(
            table,
            checks,
            2023,
            new LocalDate(2023, 1, 1).toDate(),
            new LocalDate(2023, 12, 31).toDate());

    assertEquals("analytics_2023_temp", partition.getName());
    assertEquals("analytics_2023", partition.getMainName());
    assertTrue(partition.hasChecks());
    assertFalse(partition.isLatestPartition());
  }

  @Test
  void testIsLatestPartition() {
    AnalyticsTable table =
        new AnalyticsTable(AnalyticsTableType.COMPLETENESS, columnsA, sortKeyA, Logged.UNLOGGED);

    AnalyticsTablePartition partition =
        new AnalyticsTablePartition(
            table, List.of(), AnalyticsTablePartition.LATEST_PARTITION, new Date(), new Date());

    assertTrue(partition.isLatestPartition());
  }
}
