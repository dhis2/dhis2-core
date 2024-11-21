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
package org.hisp.dhis.analytics.table.init;

import static org.hisp.dhis.db.sql.ClickHouseSqlBuilder.NAMED_COLLECTION;

import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.db.sql.ClickHouseSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilderProvider;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Class responsible for performing work for initialization of an analytics database.
 *
 * <p>The following steps are required to introduce a new analytics database platform.
 *
 * <ul>
 *   <li>Add value to enum {@link Database}
 *   <li>Add implementation of interface {@link SqlBuilder}
 *   <li>Add entry to switch statement in {@link SqlBuilderProvider}
 *   <li>Add method to {@link AnalyticsDatabaseInit} if necessary
 * </ul>
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsDatabaseInit {
  private final DhisConfigurationProvider config;

  private final AnalyticsTableSettings settings;

  @Qualifier("analyticsJdbcTemplate")
  private final JdbcTemplate jdbcTemplate;

  private final SqlBuilder sqlBuilder;

  @PostConstruct
  public void init() {
    if (!config.isAnalyticsDatabaseConfigured()) {
      return;
    }

    Database database = settings.getAnalyticsDatabase();

    switch (database) {
      case POSTGRESQL -> initPostgreSql();
      case DORIS -> initDoris();
      case CLICKHOUSE -> initClickHouse();
    }

    log.info("Initialized analytics database: '{}'", database);
  }

  /** Work for initializing a PostgreSQL analytics database. */
  private void initPostgreSql() {
    // No work yet
  }

  /** Work for initializing a Doris analytics database. */
  private void initDoris() {
    createDorisJdbcCatalog();
  }

  /** Work for initializing a ClickHouse analytics database. */
  private void initClickHouse() {
    createClickHouseNamedCollection();
  }

  /**
   * Creates a Doris JDBC catalog which is used to connect to and read from the PostgreSQL
   * transaction database as an external data source.
   */
  private void createDorisJdbcCatalog() {
    String connectionUrl = config.getProperty(ConfigurationKey.CONNECTION_URL);
    String username = config.getProperty(ConfigurationKey.CONNECTION_USERNAME);
    String password = config.getProperty(ConfigurationKey.CONNECTION_PASSWORD);

    jdbcTemplate.execute(sqlBuilder.dropCatalogIfExists());
    jdbcTemplate.execute(sqlBuilder.createCatalog(connectionUrl, username, password));
  }

  /**
   * Creates a ClickHouse named collection with connection information for the DHIS 2 PostgreSQL
   * database.
   */
  private void createClickHouseNamedCollection() {
    Map<String, Object> keyValues =
        Map.of(
            "host", config.getProperty(ConfigurationKey.CONNECTION_HOST),
            "port", config.getIntProperty(ConfigurationKey.CONNECTION_PORT),
            "database", config.getProperty(ConfigurationKey.CONNECTION_DATABASE),
            "username", config.getProperty(ConfigurationKey.CONNECTION_USERNAME),
            "password", config.getProperty(ConfigurationKey.CONNECTION_PASSWORD));

    ClickHouseSqlBuilder clickHouseSqlBuilder = new ClickHouseSqlBuilder();

    jdbcTemplate.execute(clickHouseSqlBuilder.dropNamedCollectionIfExists(NAMED_COLLECTION));
    jdbcTemplate.execute(clickHouseSqlBuilder.createNamedCollection(NAMED_COLLECTION, keyValues));
  }
}
