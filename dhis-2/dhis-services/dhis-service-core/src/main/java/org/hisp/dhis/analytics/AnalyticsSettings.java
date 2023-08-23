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
package org.hisp.dhis.analytics;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_UNLOGGED;
import static org.hisp.dhis.external.conf.ConfigurationKey.CITUS_EXTENSION_DISABLED;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Component responsible for exposing analytics table export settings. Can hold settings living in
 * configuration files (ie. dhis.conf) or in system settings.
 *
 * @author maikel arabori
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSettings {
  private final DhisConfigurationProvider dhisConfigurationProvider;

  private static final String UNLOGGED = "unlogged";
  private Boolean citusEnabled;

  /**
   * Returns the respective string that represents the table type to be exported. Two types are
   * supported: UNLOGGED and DEFAULT. See {@link AnalyticsTableType}
   *
   * @return the string representation of {@link AnalyticsTableType}.
   */
  public String getTableType() {
    if (dhisConfigurationProvider.isEnabled(ANALYTICS_TABLE_UNLOGGED)) {
      return UNLOGGED;
    }

    return EMPTY;
  }

  private boolean isCitusDisabledByConfig() {
    return dhisConfigurationProvider.isEnabled(CITUS_EXTENSION_DISABLED);
  }

  public synchronized boolean isCitusEnabled(JdbcTemplate jdbcTemplate) {
    if (Objects.isNull(citusEnabled)) {
      if (isCitusDisabledByConfig()) {
        log.info("Citus extension is disabled in dhis.conf");
        citusEnabled = false;
      } else {
        citusEnabled = isCitusExtensionInstalledAndEnabled(jdbcTemplate);
      }
    }
    return citusEnabled;
  }

  private boolean isCitusExtensionInstalledAndEnabled(JdbcTemplate jdbcTemplate) {
    List<PgExtension> installedExtensions = getPostgresExtensions(jdbcTemplate);

    logFoundExtensions(installedExtensions);

    return installedExtensions.stream().anyMatch(this::isCitusEnabled);
  }

  private void logFoundExtensions(List<PgExtension> installedExtensions) {
    if (!installedExtensions.isEmpty()) {
      log.info(
          "Found citus extensions in the database: {}",
          StringUtils.join(installedExtensions, ", "));
      return;
    }
    log.info(
        "Citus extension software is not installed in the database. You need to install it first, depending on the OS: https://docs.citusdata.com/en/latest/installation/multi_node.html");
  }

  private List<PgExtension> getPostgresExtensions(JdbcTemplate jdbcTemplate) {
    return jdbcTemplate.query(
        "select name, installed_version "
            + "from pg_available_extensions "
            + "where lower(name) like 'citus%'",
        new DataClassRowMapper<>(PgExtension.class));
  }

  private boolean isCitusEnabled(PgExtension e) {
    boolean isCitusExtensionEnabled =
        Optional.of(e)
            .map(PgExtension::installedVersion)
            .filter(StringUtils::isNotBlank)
            .isPresent();

    if (!isCitusExtensionEnabled) {
      log.info(
          "Citus extension was not created in the DHIS database. You need to run 'CREATE EXTENSION citus;' after the creation of the database");
      return false;
    }

    log.info("Citus extension detected in the DHIS database");
    return true;
  }

  public record PgExtension(String name, String installedVersion) {}
}
