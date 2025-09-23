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

import java.net.URI;
import java.net.URISyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbcUtils {
  /**
   * Extracts the database name from a JDBC connection URL.
   *
   * @param jdbcUrl The JDBC URL string.
   * @return The database name, or null if it cannot be extracted.
   */
  public static String getDatabaseFromUrl(String jdbcUrl) {
    if (jdbcUrl == null || jdbcUrl.isBlank()) {
      return null;
    }

    // Handle the unique PostgreSQL simple format without host/port.
    if (jdbcUrl.startsWith("jdbc:postgresql:") && !jdbcUrl.contains("//")) {
      String dbPart = jdbcUrl.substring("jdbc:postgresql:".length());
      int queryIndex = dbPart.indexOf('?');
      if (queryIndex != -1) {
        return dbPart.substring(0, queryIndex);
      }
      return dbPart;
    }

    // For all other supported drivers (MySQL, ClickHouse, Doris),
    // and the full PostgreSQL format, the structure is standard.
    try {
      // Remove the "jdbc:" prefix to make it a valid URI for parsing.
      URI uri = new URI(jdbcUrl.substring("jdbc:".length()));
      String path = uri.getPath();

      // The path is typically "/<database_name>".
      if (path != null && path.length() > 1) {
        // Remove the leading slash and return the database name.
        // The URI class correctly handles query parameters.
        return path.substring(1);
      }
    } catch (URISyntaxException e) {
      // The URL is malformed, so we can't extract the name.
    }

    return null;
  }
}
