/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.data.sql.from.strategy;

import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultSubqueryTest {

  private DefaultSubquery defaultSubquery;
  private DataQueryParams params;
  private SqlBuilder sqlBuilder;

  @BeforeEach
  void setUp() {
    sqlBuilder = new PostgreSqlBuilder();
  }

  @Test
  void testBuildSubqueryWithPartitions() {
    // Arrange
    params = createParams(false, "analytics", createPartitions(2022, 2023));

    defaultSubquery = new DefaultSubquery(params, sqlBuilder);

    // Expected SQL query based on the partition names
    String expectedSubquery =
        "(select ap.* from analytics_2022 as ap union all "
            + "select ap.* from analytics_2023 as ap )";

    // Act
    String subquery = defaultSubquery.buildSubquery();

    // Assert
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithSinglePartition() {
    // Arrange
    params = createParams(false, "analytics", createPartitions(2022));

    // Initialize the DefaultSubquery with real data
    defaultSubquery = new DefaultSubquery(params, sqlBuilder);

    // Expected SQL query, single partition should return directly
    String expectedSubquery = "analytics_2022";

    // Act
    String subquery = defaultSubquery.buildSubquery();

    // Assert
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWhenPartitioningIsSkipped() {
    // Arrange
    params = createParams(true, "analytics", null);

    // Initialize the DefaultSubquery with real data
    defaultSubquery = new DefaultSubquery(params, sqlBuilder);

    // Expected: partitioning skipped, should use direct table name
    String expectedSubquery = "analytics";

    // Act
    String subquery = defaultSubquery.buildSubquery();

    // Assert
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithNoPartitionAndPartitioningNotSkipped() {
    // Arrange (No partitions, but don't skip partitioning)
    params = createParams(false, "analytics", null);

    // Initialize the DefaultSubquery with real data
    defaultSubquery = new DefaultSubquery(params, sqlBuilder);

    // Expected: no partitions, should use direct table name
    String expectedSubquery = "analytics";

    // Act
    String subquery = defaultSubquery.buildSubquery();

    // Assert
    assertEquals(expectedSubquery, subquery);
  }

  private DataQueryParams createParams(
      boolean skipPartitioning, String tableName, Partitions partitions) {
    return DataQueryParams.newBuilder()
        .withSkipPartitioning(skipPartitioning)
        .withTableName(tableName)
        .withPartitions(partitions)
        .build();
  }

  private Partitions createPartitions(Integer... years) {
    Partitions partitions = new Partitions();
    if (years != null) {
      for (Integer year : years) {
        partitions.add(year);
      }
    }
    return partitions;
  }
}
