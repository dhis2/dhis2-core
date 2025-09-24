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
package org.hisp.dhis.db;

import java.util.Objects;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.db.setting.SqlBuilderSettings;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.ClickHouseAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.DorisAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.stereotype.Service;

/** Provider of {@link AnalyticsSqlBuilder} implementations. */
@Service
public class AnalyticsSqlBuilderProvider {
  private final AnalyticsSqlBuilder analyticsSqlBuilder;

  public AnalyticsSqlBuilderProvider(SqlBuilderSettings config) {
    Objects.requireNonNull(config);
    this.analyticsSqlBuilder = getSqlBuilder(config);
  }

  /**
   * Returns a {@link AnalyticsSqlBuilder} implementation based on the system configuration.
   *
   * @return a {@link AnalyticsSqlBuilder}.
   */
  public AnalyticsSqlBuilder getAnalyticsSqlBuilder() {
    return analyticsSqlBuilder;
  }

  /**
   * Returns the appropriate {@link AnalyticsSqlBuilder} implementation based on the system
   * configuration.
   *
   * @param config the {@link DhisConfigurationProvider}.
   * @return a {@link AnalyticsSqlBuilder}.
   */
  private AnalyticsSqlBuilder getSqlBuilder(SqlBuilderSettings config) {
    Database database = config.getAnalyticsDatabase();
    String catalog = config.getAnalyticsDatabaseCatalog();
    String driverFilename = config.getAnalyticsDatabaseDriverFilename();
    String databaseName = config.getAnalyticsDatabaseName();

    Objects.requireNonNull(database);

    return switch (database) {
      case DORIS -> new DorisAnalyticsSqlBuilder(catalog, driverFilename);
      case CLICKHOUSE -> new ClickHouseAnalyticsSqlBuilder(databaseName);
      default -> new PostgreSqlAnalyticsSqlBuilder();
    };
  }
}
