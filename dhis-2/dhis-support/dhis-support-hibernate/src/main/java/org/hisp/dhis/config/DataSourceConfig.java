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
package org.hisp.dhis.config;

import com.google.common.base.MoreObjects;
import io.micrometer.core.instrument.MeterRegistry;
import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.dsproxy.transform.TransformInfo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.datasource.DatabasePoolUtils;
import org.hisp.dhis.datasource.ReadOnlyDataSourceManager;
import org.hisp.dhis.datasource.model.DbPoolConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

  private final MeterRegistry meterRegistry;

  @Primary
  @Bean
  public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }

  @Primary
  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.setFetchSize(1000);
    return jdbcTemplate;
  }

  @Bean
  public JdbcTemplate readOnlyJdbcTemplate(
      DhisConfigurationProvider config, DataSource dataSource) {
    ReadOnlyDataSourceManager manager = new ReadOnlyDataSourceManager(config, meterRegistry);

    JdbcTemplate jdbcTemplate =
        new JdbcTemplate(MoreObjects.firstNonNull(manager.getReadOnlyDataSource(), dataSource));
    jdbcTemplate.setFetchSize(1000);

    return jdbcTemplate;
  }

  @Primary
  @Bean("actualDataSource")
  public DataSource dataSource(DhisConfigurationProvider config) {
    return createProxyDataSource(config, actualDataSource(config));
  }

  private DataSource actualDataSource(DhisConfigurationProvider config) {
    String jdbcUrl = config.getProperty(ConfigurationKey.CONNECTION_URL);
    String username = config.getProperty(ConfigurationKey.CONNECTION_USERNAME);
    String dbPoolType = config.getProperty(ConfigurationKey.DB_POOL_TYPE);

    DbPoolConfig poolConfig =
        DbPoolConfig.builder("actual").dhisConfig(config).dbPoolType(dbPoolType).build();

    try {
      return DatabasePoolUtils.createDbPool(poolConfig, meterRegistry);
    } catch (SQLException | PropertyVetoException e) {
      String message =
          String.format(
              "Connection test failed for main database pool, jdbcUrl: '%s', user: '%s'",
              jdbcUrl, username);

      log.error(message);
      log.error(DebugUtils.getStackTrace(e));

      throw new IllegalStateException(message, e);
    }
  }

  /** Maps MDC keys to SQL comment keys. LinkedHashMap for deterministic comment order. */
  private static final LinkedHashMap<String, String> MDC_TO_SQL_KEY = new LinkedHashMap<>();

  static {
    MDC_TO_SQL_KEY.put("controller", "controller");
    MDC_TO_SQL_KEY.put("method", "method");
    MDC_TO_SQL_KEY.put("xRequestID", "request_id");
    MDC_TO_SQL_KEY.put("sessionId", "session_id");
  }

  static DataSource createProxyDataSource(
      DhisConfigurationProvider dhisConfig, DataSource actualDataSource) {
    boolean queryLogging = dhisConfig.isEnabled(ConfigurationKey.ENABLE_QUERY_LOGGING);
    boolean queryComments = dhisConfig.isEnabled(ConfigurationKey.LOGGING_QUERY_COMMENTS);

    if (!queryLogging && !queryComments) {
      return actualDataSource;
    }

    ProxyDataSourceBuilder builder =
        ProxyDataSourceBuilder.create(actualDataSource)
            .name(
                "ProxyDS_DHIS2_"
                    + dhisConfig.getProperty(ConfigurationKey.DB_POOL_TYPE)
                    + "_"
                    + CodeGenerator.generateCode(5));

    if (queryLogging) {
      SingleLineQueryEntryCreator creator = new SingleLineQueryEntryCreator();

      SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
      listener.setLogger("org.hisp.dhis.datasource.query");
      listener.setLogLevel(SLF4JLogLevel.INFO);
      listener.setQueryLogEntryCreator(creator);

      builder
          .logSlowQueryBySlf4j(
              Integer.parseInt(
                  dhisConfig.getProperty(ConfigurationKey.SLOW_QUERY_LOGGING_THRESHOLD_TIME_MS)),
              TimeUnit.MILLISECONDS,
              SLF4JLogLevel.WARN)
          .listener(listener);
    }

    if (queryComments) {
      builder.queryTransformer(DataSourceConfig::addMdcComment);
    }

    return builder.build();
  }

  /**
   * Prepends a SQL comment with MDC context to the query for observability in pg_stat_activity and
   * logs. Values are embedded verbatim so they MUST be validated before being put into MDC.
   * Unvalidated input (e.g. containing {@code * /}) would close the comment early and allow SQL
   * injection. See {@link org.hisp.dhis.webapi.filter.RequestIdFilter} and {@link
   * org.hisp.dhis.webapi.mvc.interceptor.SqlCommentInterceptor} for the validation of each key.
   */
  static String addMdcComment(TransformInfo transformInfo) {
    String query = transformInfo.getQuery();
    Map<String, String> mdc = MDC.getCopyOfContextMap();
    if (mdc == null || mdc.isEmpty()) {
      return query;
    }

    StringJoiner joiner = new StringJoiner(",");
    for (Map.Entry<String, String> entry : MDC_TO_SQL_KEY.entrySet()) {
      String value = mdc.get(entry.getKey());
      if (value != null) {
        joiner.add(entry.getValue() + "='" + value + "'");
      }
    }

    if (joiner.length() == 0) {
      return query;
    }

    return "/* " + joiner + " */ " + query;
  }

  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  /** Collapses multi-line SQL into a single line for log parsing. */
  private static class SingleLineQueryEntryCreator extends DefaultQueryLogEntryCreator {
    @Override
    protected String formatQuery(String query) {
      return WHITESPACE.matcher(query).replaceAll(" ");
    }
  }
}
