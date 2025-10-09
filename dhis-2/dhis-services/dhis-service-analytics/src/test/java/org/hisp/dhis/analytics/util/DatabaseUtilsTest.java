/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseUtilsTest {

  @Mock private DhisConfigurationProvider config;

  private DatabaseUtils databaseUtils;

  @BeforeEach
  void setUp() {
    databaseUtils = new DatabaseUtils(config);
  }

  @Test
  void testSupportsOutliersWhenPostgres() {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("postgres");
    boolean result = databaseUtils.supportsOutliers();
    assertTrue(result, "Postgres should support outliers");
  }

  @Test
  void testSupportsOutliersWhenEmpty() {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("");
    boolean result = databaseUtils.supportsOutliers();
    assertTrue(
        result, "Empty database configuration should default to Postgres and support outliers");
  }

  @Test
  void testSupportsOutliersWhenDoris() {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("doris");
    boolean result = databaseUtils.supportsOutliers();
    assertFalse(result, "Doris should not support outliers");
  }

  @Test
  void testSupportsOutliersWhenDorisMixedCase() {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("DoRiS");
    boolean result = databaseUtils.supportsOutliers();
    assertFalse(result, "Doris (mixed case) should not support outliers");
  }

  @Test
  void testSupportsOutliersWhenClickhouse() {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("clickhouse");
    boolean result = databaseUtils.supportsOutliers();
    assertFalse(result, "ClickHouse should not support outliers");
  }

  @Test
  void testSupportsOutliersWhenClickhouseMixedCase() {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("ClickHouse");
    boolean result = databaseUtils.supportsOutliers();
    assertFalse(result, "ClickHouse (mixed case) should not support outliers");
  }

  @Test
  void testSupportsOutliersWhenDorisWithWhitespace() {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("  doris  ");
    boolean result = databaseUtils.supportsOutliers();
    assertFalse(result, "Doris with whitespace should not support outliers");
  }
}
