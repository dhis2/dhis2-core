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
package org.hisp.dhis.test.config;

import java.util.Map;
import java.util.Properties;
import javax.annotation.CheckForNull;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Luciano Fiandesio
 */
public class PostgresDhisConfigurationProvider extends TestDhisConfigurationProvider {
  private static final String DEFAULT_CONFIGURATION_FILE_NAME = "postgresTestDhis.conf";

  /**
   * Refers to the {@code postgis/postgis:16-3.5-alpine} image which contains PostgreSQL 16 and
   * PostGIS 3.4.2.
   */
  private static final String POSTGRES_POSTGIS_VERSION = "16-3.5-alpine";

  private static final DockerImageName POSTGIS_IMAGE_NAME =
      DockerImageName.parse("imresamu/postgis").asCompatibleSubstituteFor("postgres");
  private static final String POSTGRES_DATABASE_NAME = "dhis";
  private static final String POSTGRES_USERNAME = "dhis";
  private static final String POSTGRES_PASSWORD = "dhis";

  private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

  static {
    POSTGRES_CONTAINER =
        new PostgreSQLContainer<>(POSTGIS_IMAGE_NAME.withTag(POSTGRES_POSTGIS_VERSION))
            .withDatabaseName(POSTGRES_DATABASE_NAME)
            .withUsername(POSTGRES_USERNAME)
            .withPassword(POSTGRES_PASSWORD)
            .withInitScript("db/extensions.sql")
            .withTmpFs(Map.of("/var/lib/postgresql/data", "rw,size=512m"))
            .withCommand(
                "postgres",
                "-c",
                "idle_session_timeout=35000",
                // Disable durability (safe for tests, data is disposable)
                "-c",
                "fsync=off",
                "-c",
                "synchronous_commit=off",
                "-c",
                "full_page_writes=off",
                // WAL tuning
                "-c",
                "wal_level=minimal",
                "-c",
                "max_wal_senders=0",
                // Memory (conservative for CI/dev machines)
                "-c",
                "shared_buffers=128MB",
                "-c",
                "work_mem=8MB",
                "-c",
                "maintenance_work_mem=64MB",
                "-c",
                "effective_cache_size=256MB",
                // I/O tuning for tmpfs
                "-c",
                "random_page_cost=1.1",
                // Reduce logging overhead
                "-c",
                "log_statement=none",
                "-c",
                "logging_collector=off")
            .withEnv("LC_COLLATE", "C");

    POSTGRES_CONTAINER.start();
  }

  public PostgresDhisConfigurationProvider(@CheckForNull Properties overrides) {
    Properties dhisConfig = new Properties();
    dhisConfig.putAll(getPropertiesFromFile(DEFAULT_CONFIGURATION_FILE_NAME));
    dhisConfig.putAll(getConnectionProperties());

    if (overrides != null) dhisConfig.putAll(overrides);

    this.properties = dhisConfig;
  }

  /**
   * Returns the DHIS2 DB connection properties that are only known after the DB container has been
   * started.
   */
  private static Properties getConnectionProperties() {
    Properties properties = new Properties();
    properties.setProperty(
        "connection.dialect", "org.hisp.dhis.hibernate.dialect.DhisPostgresDialect");
    properties.setProperty("connection.driver_class", POSTGRES_CONTAINER.getDriverClassName());
    properties.setProperty("connection.url", POSTGRES_CONTAINER.getJdbcUrl());
    properties.setProperty("connection.username", POSTGRES_USERNAME);
    properties.setProperty("connection.password", POSTGRES_PASSWORD);
    return properties;
  }
}
