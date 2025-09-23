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
  public static String getDatabaseFromConnectionUrl(String jdbcUrl) {
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
