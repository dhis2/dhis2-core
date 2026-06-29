/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.db.model.Database;
import org.junit.jupiter.api.Test;

/** Tests the ClickHouse-only JDBC URL augmentation in {@link AnalyticsDataSourceConfig}. */
class AnalyticsDataSourceConfigTest {

  @Test
  void appendsJoinUseNullsForClickHouseWithoutExistingParams() {
    assertEquals(
        "jdbc:clickhouse://localhost:8123/dhis2?clickhouse_setting_join_use_nulls=1",
        AnalyticsDataSourceConfig.withClickHouseConnectionSettings(
            Database.CLICKHOUSE, "jdbc:clickhouse://localhost:8123/dhis2"));
  }

  @Test
  void appendsJoinUseNullsForClickHouseWithExistingParams() {
    assertEquals(
        "jdbc:clickhouse://localhost:8123/dhis2?ssl=true&clickhouse_setting_join_use_nulls=1",
        AnalyticsDataSourceConfig.withClickHouseConnectionSettings(
            Database.CLICKHOUSE, "jdbc:clickhouse://localhost:8123/dhis2?ssl=true"));
  }

  @Test
  void appendsJoinUseNullsForClickHouseWithTrailingQuestionMark() {
    assertEquals(
        "jdbc:clickhouse://localhost:8123/dhis2?clickhouse_setting_join_use_nulls=1",
        AnalyticsDataSourceConfig.withClickHouseConnectionSettings(
            Database.CLICKHOUSE, "jdbc:clickhouse://localhost:8123/dhis2?"));
  }

  @Test
  void appendsJoinUseNullsForClickHouseWithTrailingAmpersand() {
    assertEquals(
        "jdbc:clickhouse://localhost:8123/dhis2?ssl=true&clickhouse_setting_join_use_nulls=1",
        AnalyticsDataSourceConfig.withClickHouseConnectionSettings(
            Database.CLICKHOUSE, "jdbc:clickhouse://localhost:8123/dhis2?ssl=true&"));
  }

  @Test
  void leavesClickHouseUrlUnchangedWhenAlreadyPresent() {
    String url = "jdbc:clickhouse://localhost:8123/dhis2?clickhouse_setting_join_use_nulls=1";
    assertEquals(
        url, AnalyticsDataSourceConfig.withClickHouseConnectionSettings(Database.CLICKHOUSE, url));
  }

  @Test
  void leavesPostgresUrlUnchanged() {
    String url = "jdbc:postgresql://localhost:5432/dhis2";
    assertEquals(
        url, AnalyticsDataSourceConfig.withClickHouseConnectionSettings(Database.POSTGRESQL, url));
  }

  @Test
  void leavesDorisUrlUnchanged() {
    String url = "jdbc:mysql://localhost:9030/dhis2";
    assertEquals(
        url, AnalyticsDataSourceConfig.withClickHouseConnectionSettings(Database.DORIS, url));
  }

  @Test
  void handlesNullUrl() {
    assertNull(
        AnalyticsDataSourceConfig.withClickHouseConnectionSettings(Database.CLICKHOUSE, null));
  }
}
