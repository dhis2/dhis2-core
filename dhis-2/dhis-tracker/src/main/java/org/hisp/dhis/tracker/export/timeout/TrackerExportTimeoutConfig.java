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
package org.hisp.dhis.tracker.export.timeout;

import java.time.Duration;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Wires the tracker export query timeout.
 *
 * <p>The template is a separate bean, not a setting on the shared {@code @Primary} or {@code
 * readOnlyJdbcTemplate} beans, so the timeout cannot reach any other product.
 */
@Slf4j
@Configuration
public class TrackerExportTimeoutConfig {

  /** Name of the bean the tracker export stores inject. */
  public static final String TRACKER_EXPORT_JDBC_TEMPLATE = "trackerExportJdbcTemplate";

  /**
   * The only {@link JdbcTemplate} in DHIS2 that enforces the tracker export deadline. Exposed as a
   * {@link NamedParameterJdbcTemplate} because that is what the export stores use; it delegates to
   * the {@link DeadlineAwareJdbcTemplate} it wraps.
   */
  @Bean(TRACKER_EXPORT_JDBC_TEMPLATE)
  public NamedParameterJdbcTemplate trackerExportJdbcTemplate(DataSource dataSource) {
    JdbcTemplate jdbcTemplate = new DeadlineAwareJdbcTemplate(dataSource);
    jdbcTemplate.setFetchSize(1000);
    return new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  /**
   * The configured export budget, or null when {@code 0} turned the timeout off.
   *
   * @throws IllegalStateException if the value is not a whole number of seconds, or is negative.
   *     Failing startup beats disabling the timeout on a typo, which would leave exports unbounded
   *     for a deployment that asked for the opposite.
   */
  @Bean
  public TrackerExportTimeout trackerExportTimeout(DhisConfigurationProvider config) {
    String key = ConfigurationKey.TRACKER_EXPORT_TIMEOUT.getKey();
    String value = config.getProperty(ConfigurationKey.TRACKER_EXPORT_TIMEOUT);

    long seconds;
    try {
      seconds = Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Invalid %s='%s', expected a whole number of seconds".formatted(key, value), e);
    }
    if (seconds < 0) {
      throw new IllegalStateException(
          "Invalid %s='%s', expected a positive number of seconds or 0 to disable"
              .formatted(key, value));
    }

    if (seconds == 0) {
      log.warn("Tracker export timeout is disabled ({}=0), exports will run unbounded", key);
      return new TrackerExportTimeout(null);
    }
    return new TrackerExportTimeout(Duration.ofSeconds(seconds));
  }
}
