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
package org.hisp.dhis.system.database;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Component("databaseInfoProvider")
public class HibernateDatabaseInfoProvider implements DatabaseInfoProvider {
  private static final String EXTENSION_MISSING_ERROR =
      "%s extension is not installed. Execute \"CREATE EXTENSION %s;\" as a superuser and restart the application.";

  private static final String POSTGIS_EXTENSION = "postgis";

  private static final String PG_TRIGRAM_EXTENSION = "pg_trgm";

  private static final String BTREE_GIN_EXTENSION = "btree_gin";

  private static final String POSTGRES_VERSION_REGEX = "^([a-zA-Z_-]+ \\d+\\.+\\d+)?[ ,].*$";

  private static final Pattern POSTGRES_VERSION_PATTERN = Pattern.compile(POSTGRES_VERSION_REGEX);

  private DatabaseInfo info;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final DhisConfigurationProvider config;

  private final JdbcTemplate jdbcTemplate;

  private final Environment environment;

  public HibernateDatabaseInfoProvider(
      DhisConfigurationProvider config, JdbcTemplate jdbcTemplate, Environment environment) {
    checkNotNull(config);
    checkNotNull(jdbcTemplate);
    checkNotNull(environment);

    this.config = config;
    this.jdbcTemplate = jdbcTemplate;
    this.environment = environment;
  }

  @PostConstruct
  public void init() {
    checkDatabaseConnectivity();

    boolean spatialSupport = false;

    boolean trigramSupport = false;

    boolean btreeGinSupport = false;

    if (!SystemUtils.isTestRun(environment.getActiveProfiles())) {
      // Check if postgis extension is installed, fail startup if not
      spatialSupport = isSpatialSupport();

      if (!spatialSupport) {
        log.error(String.format(EXTENSION_MISSING_ERROR, POSTGIS_EXTENSION, POSTGIS_EXTENSION));
        throw new IllegalStateException(
            String.format(EXTENSION_MISSING_ERROR, POSTGIS_EXTENSION, POSTGIS_EXTENSION));
      }

      // Check if pg_trgm extension is installed, fail startup if not
      trigramSupport = isTrigramExtensionCreated();

      if (!trigramSupport) {
        log.error(
            String.format(EXTENSION_MISSING_ERROR, PG_TRIGRAM_EXTENSION, PG_TRIGRAM_EXTENSION));
        throw new IllegalStateException(
            String.format(EXTENSION_MISSING_ERROR, PG_TRIGRAM_EXTENSION, PG_TRIGRAM_EXTENSION));
      }

      // Check if btree_gin extension is installed, fail startup if not
      btreeGinSupport = isBtreeGinExtensionCreated();

      if (!btreeGinSupport) {
        log.error(String.format(EXTENSION_MISSING_ERROR, BTREE_GIN_EXTENSION, BTREE_GIN_EXTENSION));
        throw new IllegalStateException(
            String.format(EXTENSION_MISSING_ERROR, BTREE_GIN_EXTENSION, BTREE_GIN_EXTENSION));
      }
    }

    String url = config.getProperty(ConfigurationKey.CONNECTION_URL);
    String user = config.getProperty(ConfigurationKey.CONNECTION_USERNAME);
    String password = config.getProperty(ConfigurationKey.CONNECTION_PASSWORD);
    InternalDatabaseInfo internalDatabaseInfo = getInternalDatabaseInfo();

    info = new DatabaseInfo();
    info.setName(internalDatabaseInfo.getDatabase());
    info.setUser(StringUtils.defaultIfEmpty(internalDatabaseInfo.getUser(), user));
    info.setPassword(password);
    info.setUrl(url);
    info.setSpatialSupport(spatialSupport);
    info.setDatabaseVersion(internalDatabaseInfo.getVersion());
  }

  // -------------------------------------------------------------------------
  // DatabaseInfoProvider implementation
  // -------------------------------------------------------------------------

  @Override
  public DatabaseInfo getDatabaseInfo() {
    // parts of returned object may be reset due to security reasons
    // (clone must be created to preserve original values)
    return info == null ? null : info.instance();
  }

  @Override
  public boolean isInMemory() {
    return info.getUrl() != null && info.getUrl().contains(":mem:");
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  @Nonnull
  private InternalDatabaseInfo getInternalDatabaseInfo() {
    if (SystemUtils.isH2(environment.getActiveProfiles())) {
      return new InternalDatabaseInfo();
    } else {
      try {
        final InternalDatabaseInfo internalDatabaseInfo =
            jdbcTemplate.queryForObject(
                "select version(),current_catalog,current_user",
                (rs, rowNum) -> {
                  String version = rs.getString(1);
                  final Matcher versionMatcher = POSTGRES_VERSION_PATTERN.matcher(version);

                  if (versionMatcher.find()) {
                    version = versionMatcher.group(1);
                  }

                  return new InternalDatabaseInfo(version, rs.getString(2), rs.getString(3));
                });

        return internalDatabaseInfo == null ? new InternalDatabaseInfo() : internalDatabaseInfo;
      } catch (Exception ex) {
        log.error("An error occurred when retrieving database info.", ex);

        return new InternalDatabaseInfo();
      }
    }
  }

  private void checkDatabaseConnectivity() {
    jdbcTemplate.queryForObject("select 'checking db connection';", String.class);
  }

  /**
   * Attempts to create a spatial database extension. Checks if spatial operations are supported.
   */
  private boolean isSpatialSupport() {
    try {
      jdbcTemplate.execute("create extension postgis;");
    } catch (Exception ex) {
      // swallowing exception as this is just an attempt to create the
      // extension if possible.
    }

    try {
      String version = jdbcTemplate.queryForObject("select postgis_full_version();", String.class);

      return version != null;
    } catch (Exception ex) {
      log.error("Exception when checking postgis_full_version(), PostGIS not available");
      log.debug("Exception when checking postgis_full_version()", ex);
      return false;
    }
  }

  /** Attempts to create a pg_trgm database extension. Checks if extension is created */
  private boolean isTrigramExtensionCreated() {
    try {
      jdbcTemplate.execute("create extension pg_trgm;");
    } catch (Exception ex) {
      // swallowing exception as this is just an attempt to create the
      // extension if possible.
    }

    try {
      String pg_trgm_ext_name =
          jdbcTemplate.queryForObject(
              "SELECT extname from pg_extension where extname='pg_trgm';", String.class);

      return PG_TRIGRAM_EXTENSION.equals(pg_trgm_ext_name);
    } catch (Exception ex) {
      log.error("Exception when checking pg_trgm extension. Extension may not be created", ex);
      return false;
    }
  }

  /** Attempts to create a btree_gin database extension. Checks if extension is created */
  private boolean isBtreeGinExtensionCreated() {
    try {
      jdbcTemplate.execute("create extension btree_gin;");
    } catch (Exception ex) {
      // swallowing exception as this is just an attempt to create the
      // extension if possible.
    }

    try {
      String btree_ext_name =
          jdbcTemplate.queryForObject(
              "SELECT extname from pg_extension where extname='btree_gin';", String.class);

      return BTREE_GIN_EXTENSION.equals(btree_ext_name);
    } catch (Exception ex) {
      log.error("Exception when checking btree_gin extension. Extension may not be created", ex);
      return false;
    }
  }

  protected static class InternalDatabaseInfo {
    private final String version;

    private final String database;

    private final String user;

    public InternalDatabaseInfo() {
      this(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    public InternalDatabaseInfo(String version, String database, String user) {
      this.version = version;
      this.database = database;
      this.user = user;
    }

    public String getVersion() {
      return version;
    }

    public String getDatabase() {
      return database;
    }

    public String getUser() {
      return user;
    }
  }
}
