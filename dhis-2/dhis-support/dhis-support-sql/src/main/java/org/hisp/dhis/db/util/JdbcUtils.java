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

import static org.apache.commons.lang3.StringUtils.isBlank;
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
  private static final String PREFIX_POSTGRESQL = "jdbc:postgresql:";
  private static final String PREFIX_JDBC = "jdbc:";
  private static final String SLASH = "//";

  /**
   * Extracts the database name from a JDBC connection URL.
   *
   * @param jdbcUrl The JDBC URL connection URL.
   * @return The database name, or null if it cannot be extracted.
   */
  public static String getDatabaseFromUrl(String jdbcUrl) {
    if (isBlank(jdbcUrl) || !jdbcUrl.startsWith(PREFIX_JDBC)) {
      return null;
    }

    // Handle PostgreSQL simple format without host and port
    if (jdbcUrl.startsWith(PREFIX_POSTGRESQL) && !jdbcUrl.contains(SLASH)) {
      String databasePart = jdbcUrl.substring(PREFIX_POSTGRESQL.length());

      int queryIndex = databasePart.indexOf('?');

      if (queryIndex != -1) {
        return databasePart.substring(0, queryIndex);
      }

      return trimToNull(databasePart);
    }

    // Handle standard format for all other drivers (MySQL, ClickHouse, Doris)
    try {
      // Remove "jdbc:" prefix to make it a valid URI for parsing
      URI uri = new URI(jdbcUrl.substring(PREFIX_JDBC.length()));
      String path = uri.getPath();

      // Path is typically "/<database_name>"
      if (path != null && path.length() > 1) {
        // Remove leading slash and return the database name
        return trimToNull(path.substring(1));
      }
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Malformed JDBC connection URL: " + jdbcUrl, ex);
    }

    return null;
  }
}
