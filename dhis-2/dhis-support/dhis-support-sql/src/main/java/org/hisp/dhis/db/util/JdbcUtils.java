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
package org.hisp.dhis.db.util;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.net.URI;
import java.net.URISyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utilities for JDBC operations.
 *
 * @author Lars Helge Overland
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbcUtils {
  private static final String PREFIX_JDBC = "jdbc:";
  private static final String PREFIX_POSTGRESQL = PREFIX_JDBC + "postgresql:";
  private static final String SLASH = "//";
  private static final String SEP_PARAM = "?";

  /**
   * Extracts the host from a JDBC connection URL.
   *
   * @param jdbcUrl The JDBC URL connection URL.
   * @return the host, or null if it cannot be extracted.
   */
  public static String getHostFromUrl(String jdbcUrl) {
    if (!isJdbcUrl(jdbcUrl)) {
      return null;
    }

    if (isPostgreSqlSimpleFormat(jdbcUrl)) {
      return "localhost";
    }

    return toUri(jdbcUrl).getHost();
  }

  /**
   * Extracts the database name from a JDBC connection URL.
   *
   * @param jdbcUrl The JDBC URL connection URL.
   * @return the database name, or null if it cannot be extracted.
   */
  public static String getDatabaseFromUrl(String jdbcUrl) {
    if (!isJdbcUrl(jdbcUrl)) {
      return null;
    }

    // Handle PostgreSQL simple format without host and port
    if (isPostgreSqlSimpleFormat(jdbcUrl)) {
      String databasePart = jdbcUrl.substring(PREFIX_POSTGRESQL.length());
      int queryIndex = databasePart.indexOf(SEP_PARAM);

      if (queryIndex != -1) {
        return databasePart.substring(0, queryIndex);
      }

      return trimToNull(databasePart);
    }

    // Handle standard format for all other drivers (MySQL, ClickHouse, Doris)
    URI uri = toUri(jdbcUrl);
    String path = uri.getPath();

    // Path is typically "/<database_name>"
    if (isNotBlank(path) && path.length() > 1) {
      // Remove leading slash and return the database name
      return path.substring(1);
    }

    return null;
  }

  /**
   * Determines if the given string is a valid JDBC URL.
   *
   * @param jdbcUrl The JDBC URL connection URL.
   * @return true if the URL is a valid JDBC URL, false otherwise.
   */
  static boolean isJdbcUrl(String jdbcUrl) {
    return isNotBlank(jdbcUrl) && jdbcUrl.startsWith(PREFIX_JDBC);
  }

  /**
   * Determines if the given JDBC URL is for PostgreSQL in simple format without host and port.
   *
   * @param jdbcUrl The JDBC URL connection URL.
   * @return true if the URL PostgreSQL simple format, false otherwise.
   */
  static boolean isPostgreSqlSimpleFormat(String jdbcUrl) {
    return jdbcUrl.startsWith(PREFIX_POSTGRESQL) && !jdbcUrl.contains(SLASH);
  }

  /**
   * Converts a JDBC connection URL to a URI. Removes "jdbc:" prefix to make it a valid URI before
   * parsing.
   *
   * @param jdbcUrl The JDBC connection URL.
   * @return The corresponding URI.
   * @throws IllegalArgumentException if the URL is malformed.
   */
  static URI toUri(String jdbcUrl) throws IllegalArgumentException {
    try {
      return new URI(jdbcUrl.substring(PREFIX_JDBC.length()));
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Malformed JDBC connection URL: " + jdbcUrl, ex);
    }
  }
}
