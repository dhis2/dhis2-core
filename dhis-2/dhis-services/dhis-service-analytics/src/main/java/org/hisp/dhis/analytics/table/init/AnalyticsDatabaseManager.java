package org.hisp.dhis.analytics.table.init;

import javax.annotation.PostConstruct;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manager responsible for performing work for initialization of an analytics database, if configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsDatabaseManager {
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
    
    switch(database) {
      case DORIS:
        initDoris();
      case POSTGRESQL:
        initPostgreSql();
    }
    
    log.info("Initialized analytics database: '{}'", database);
  }
  
  /**
   * Work for initializing a Doris analytics database.
   */
  private void initDoris() {
    
    String connectionUrl = config.getProperty(ConfigurationKey.CONNECTION_URL);
    String username = config.getProperty(ConfigurationKey.CONNECTION_USERNAME);
    String password = config.getProperty(ConfigurationKey.CONNECTION_PASSWORD);
    
    jdbcTemplate.execute(sqlBuilder.dropCatalogIfExists());
    jdbcTemplate.execute(sqlBuilder.createCatalog(connectionUrl, username, password));
  }
  
  /**
   * Work for initializing a PostgreSQL analytics database.
   */
  private void initPostgreSql() {
  }
}
