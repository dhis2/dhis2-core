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
package org.hisp.dhis.db.setting;

import static org.hisp.dhis.commons.util.TextUtils.format;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE_CATALOG;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE_DRIVER_FILENAME;
import static org.hisp.dhis.util.ObjectUtils.isNull;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.stereotype.Component;

/**
 * Component responsible for exposing analytics table settings related to the SqlBuilder. The source
 * of the settings are configuration files (dhis.conf) and system settings.
 */
@Component
@RequiredArgsConstructor
public class SqlBuilderSettings {
  private final DhisConfigurationProvider config;

  /**
   * Returns the analytics database JDBC catalog name.
   *
   * @return the analytics database JDBC catalog name.
   */
  public String getAnalyticsDatabaseCatalog() {
    return config.getProperty(ANALYTICS_DATABASE_CATALOG);
  }

  /**
   * Returns the configured analytics {@link Database}. The default is {@link Database#POSTGRESQL}.
   *
   * @return the analytics {@link Database}.
   */
  public Database getAnalyticsDatabase() {
    String value = config.getProperty(ANALYTICS_DATABASE);
    String valueUpperCase = StringUtils.trimToEmpty(value).toUpperCase();
    return getAndValidateDatabase(valueUpperCase);
  }

  /**
   * Returns the analytics database JDBC driver filename.
   *
   * @return the analytics database JDBC driver filename.
   */
  public String getAnalyticsDatabaseDriverFilename() {
    return config.getProperty(ANALYTICS_DATABASE_DRIVER_FILENAME);
  }

  /**
   * Returns the {@link Database} matching the given value.
   *
   * @param value the string value.
   * @return the {@link Database}.
   * @throws IllegalArgumentException if the value does not match a valid option.
   */
  Database getAndValidateDatabase(String value) {
    Database database = EnumUtils.getEnum(Database.class, value);

    if (isNull(database)) {
      String message =
          format(
              "Property '{}' has illegal value: '{}', allowed options: {}",
              ANALYTICS_DATABASE.getKey(),
              value,
              StringUtils.join(Database.values(), ','));
      throw new IllegalArgumentException(message);
    }

    return database;
  }
}
