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
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * @author Luciano Fiandesio
 */
public class PostgresDhisConfigurationProvider extends TestDhisConfigurationProvider {
  private static final String DEFAULT_CONFIGURATION_FILE_NAME = "postgresTestDhis.conf";

  /**
   * Refers to the {@code postgis/postgis:14-3.5-alpine} image which contains PostgreSQL 16 and
   * PostGIS 3.4.2.
   */
  private static final String POSTGRES_POSTGIS_VERSION = "14-3.5-alpine";

  private static final DockerImageName POSTGIS_IMAGE_NAME =
      DockerImageName.parse("postgis/postgis").asCompatibleSubstituteFor("postgres");
  private static final String POSTGRES_DATABASE_NAME = "dhis";
  private static final String POSTGRES_USERNAME = "dhis";
  private static final String POSTGRES_PASSWORD = "dhis";

  /**
   * The Postgres container will be initialized in one of two ways:<br>
   * 1. If the `init-db.sql` file is present on the classpath it will use this file to fully
   * initialize the DB (extensions + base schema + migrations)<br>
   * 2. If the `init-db.sql` file is not present, it will just initialize the DB extensions. All
   * migrations should then be applied as normal on start-up.<br>
   * <br>
   *
   * <p>If using `init-db.sql`, the expectation is that this file should include all extensions, the
   * base DB schema and all applied existing migrations. Creating a new baseline essentially. <br>
   * This is purely for testing purposes and using a new baseline allows integration tests to
   * start-up quicker (~49s -> ~24s).<br>
   * An example of creating a new baseline schema (after starting the App with a blank DB +
   * extensions):
   *
   * <pre>
   * pg_dump --schema-only --no-owner dhis2 > /Users/user/code/dhis2-core/dhis-2/dhis-support/dhis-support-test/src/main/resources/db/init-db.sql
   * </pre>
   *
   * When using the init-db.sql script, the test property `flyway.skip_migration=true` needs to be
   * used. They must be used in conjunction with each other.
   */
  private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

  static {
    POSTGRES_CONTAINER =
        new PostgreSQLContainer<>(POSTGIS_IMAGE_NAME.withTag(POSTGRES_POSTGIS_VERSION))
            .withDatabaseName(POSTGRES_DATABASE_NAME)
            .withUsername(POSTGRES_USERNAME)
            .withPassword(POSTGRES_PASSWORD)
            .withTmpFs(Map.of("/testtmpfs", "rw"))
            .withCommand("postgres -c idle_session_timeout=30000")
            .withEnv("LC_COLLATE", "C");

    if (initDbScriptIsPresent()) {
      POSTGRES_CONTAINER.withCopyFileToContainer(
          MountableFile.forClasspathResource("db/init-db.sql"), "/docker-entrypoint-initdb.d/");
    } else {
      POSTGRES_CONTAINER.withInitScript("db/extensions.sql");
    }

    POSTGRES_CONTAINER.start();
  }

  /**
   * checks whether an init DB script is present on the classpath. See {@link
   * PostgresDhisConfigurationProvider#POSTGRES_CONTAINER} for more info.
   *
   * @return true if file is present
   */
  private static boolean initDbScriptIsPresent() {
    ClassPathResource resource = new ClassPathResource("db/init-db.sql");
    return resource.exists();
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
