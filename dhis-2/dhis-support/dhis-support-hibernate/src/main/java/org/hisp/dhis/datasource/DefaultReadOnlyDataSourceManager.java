/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.datasource;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_PASSWORD;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.CONNECTION_USERNAME;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class DefaultReadOnlyDataSourceManager
    implements ReadOnlyDataSourceManager, InitializingBean {
  private static final String FORMAT_READ_PREFIX = "read%d.";

  private static final String FORMAT_CONNECTION_URL = FORMAT_READ_PREFIX + CONNECTION_URL.getKey();

  private static final String FORMAT_CONNECTION_USERNAME =
      FORMAT_READ_PREFIX + CONNECTION_USERNAME.getKey();

  private static final String FORMAT_CONNECTION_PASSWORD =
      FORMAT_READ_PREFIX + CONNECTION_PASSWORD.getKey();

  private static final int VAL_ACQUIRE_INCREMENT = 6;

  private static final int VAL_MAX_IDLE_TIME = 21600;

  private static final int MAX_READ_REPLICAS = 5;

  private final DhisConfigurationProvider config;

  public DefaultReadOnlyDataSourceManager(DhisConfigurationProvider config) {
    checkNotNull(config);
    this.config = config;
  }

  /** State holder for the resolved read only data source. */
  private DataSource internalReadOnlyDataSource;

  /** State holder for explicitly defined read only data sources. */
  private List<DataSource> internalReadOnlyInstanceList;

  @Override
  public void afterPropertiesSet() {
    List<DataSource> ds = getReadOnlyDataSources();

    this.internalReadOnlyInstanceList = ds;
    this.internalReadOnlyDataSource = !ds.isEmpty() ? new CircularRoutingDataSource(ds) : null;
  }

  // -------------------------------------------------------------------------
  // DataSourceManager implementation
  // -------------------------------------------------------------------------

  @Override
  public DataSource getReadOnlyDataSource() {
    return internalReadOnlyDataSource;
  }

  @Override
  public int getReadReplicaCount() {
    return internalReadOnlyInstanceList != null ? internalReadOnlyInstanceList.size() : 0;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private List<DataSource> getReadOnlyDataSources() {
    String mainUser = config.getProperty(ConfigurationKey.CONNECTION_USERNAME);
    String mainPassword = config.getProperty(ConfigurationKey.CONNECTION_PASSWORD);
    String driverClass = config.getProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS);
    String maxPoolSize = config.getProperty(ConfigurationKey.CONNECTION_POOL_MAX_SIZE);
    String dbPoolType = config.getProperty(ConfigurationKey.DB_POOL_TYPE);

    Properties props = config.getProperties();

    List<DataSource> dataSources = new ArrayList<>();

    for (int i = 1; i <= MAX_READ_REPLICAS; i++) {
      String jdbcUrl = props.getProperty(String.format(FORMAT_CONNECTION_URL, i));
      String username = props.getProperty(String.format(FORMAT_CONNECTION_USERNAME, i));
      String password = props.getProperty(String.format(FORMAT_CONNECTION_PASSWORD, i));

      username = StringUtils.defaultIfEmpty(username, mainUser);
      password = StringUtils.defaultIfEmpty(password, mainPassword);

      DatabasePoolUtils.PoolConfig.PoolConfigBuilder builder =
          DatabasePoolUtils.PoolConfig.builder();
      builder.dhisConfig(config);
      builder.password(password);
      builder.username(username);
      builder.jdbcUrl(jdbcUrl);
      builder.dbPoolType(dbPoolType);
      builder.maxPoolSize(maxPoolSize);
      builder.acquireIncrement(String.valueOf(VAL_ACQUIRE_INCREMENT));
      builder.maxIdleTime(String.valueOf(VAL_MAX_IDLE_TIME));

      if (ObjectUtils.allNonNull(jdbcUrl, username, password)) {
        try {
          dataSources.add(DatabasePoolUtils.createDbPool(builder.build()));
        } catch (SQLException | PropertyVetoException e) {
          String message =
              String.format(
                  "Connection test failed for read replica database pool, "
                      + "driver class: '%s', URL: '%s', user: '%s'",
                  driverClass, jdbcUrl, username);

          log.error(message);
          log.error(DebugUtils.getStackTrace(e));

          throw new IllegalStateException(message, e);
        }
      }
    }

    log.info("Read only configuration initialized, read replicas found: " + dataSources.size());

    config
        .getProperties()
        .setProperty(
            ConfigurationKey.ACTIVE_READ_REPLICAS.getKey(), String.valueOf(dataSources.size()));

    return dataSources;
  }
}
