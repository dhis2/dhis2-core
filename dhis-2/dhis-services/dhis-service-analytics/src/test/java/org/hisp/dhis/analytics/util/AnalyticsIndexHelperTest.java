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
package org.hisp.dhis.analytics.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.AnalyticsTableType.EVENT;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexName;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexes;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.IndexType.BTREE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.junit.jupiter.api.Test;

/**
 * @author maikel arabori
 */
class AnalyticsIndexHelperTest {
  @Test
  void testGetIndexes() {
    List<AnalyticsTablePartition> stubPartitions = List.of(stubAnalyticsTablePartition());

    List<Index> indexes = getIndexes(stubPartitions);

    assertThat(indexes, hasSize(1));
    assertThat(indexes.get(0).getTableName(), is(equalTo("analytics_event_2022_temp")));
    assertThat(indexes.get(0).getColumns(), hasSize(1));
    assertThat(indexes.get(0).getIndexType(), is(equalTo(BTREE)));
  }

  @Test
  void testGetIndexNameA() {
    String statement = getIndexName("table", List.of("column"), EVENT);

    assertThat(statement, containsString("in_column_table"));
  }

  @Test
  void testGetIndexNameB() {
    String nameA =
        getIndexName("analytics_2017_temp", List.of("quarterly"), AnalyticsTableType.DATA_VALUE);
    String nameB =
        getIndexName("analytics_2018_temp", List.of("ax", "co"), AnalyticsTableType.DATA_VALUE);
    String nameC =
        getIndexName("analytics_2019_temp", List.of("YtbsuPPo010"), AnalyticsTableType.DATA_VALUE);

    assertTrue(nameA.startsWith("in_quarterly_ax_2017_"), nameA);
    assertTrue(nameB.startsWith("in_ax_co_ax_2018_"), nameB);
    assertTrue(nameC.startsWith("in_YtbsuPPo010_ax_2019_"), nameC);
  }

  private AnalyticsTablePartition stubAnalyticsTablePartition() {
    AnalyticsTablePartition analyticsTablePartitionStub =
        new AnalyticsTablePartition(stubAnalyticsTable(), List.of(), 2022, new Date(), new Date());

    return analyticsTablePartitionStub;
  }

  private AnalyticsTable stubAnalyticsTable() {
    List<AnalyticsTableColumn> columns =
        List.of(
            AnalyticsTableColumn.builder()
                .name("column")
                .dataType(TEXT)
                .selectExpression("c")
                .indexType(BTREE)
                .build());

    return new AnalyticsTable(EVENT, columns, List.of(), Logged.UNLOGGED);
  }
}
