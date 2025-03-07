/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.test.config;

import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Provides a shared PostgreSQL container that can be reused across tests. This class implements the
 * singleton pattern to ensure a single container instance.
 */
public class SharedPostgresContainer {

  /**
   * Refers to the {@code postgis/postgis:13-3.4-alpine} image which contains PostgreSQL 13 and
   * PostGIS 3.4.2.
   */
  private static final String POSTGRES_POSTGIS_VERSION = "13-3.4-alpine";

  private static final DockerImageName POSTGIS_IMAGE_NAME =
      DockerImageName.parse("postgis/postgis").asCompatibleSubstituteFor("postgres");

  public static final String POSTGRES_DATABASE_NAME = "dhis";
  public static final String POSTGRES_USERNAME = "dhis";
  public static final String POSTGRES_PASSWORD = "dhis";

  /** The singleton instance of the PostgreSQL container. */
  private static PostgreSQLContainer<?> instance;

  /**
   * Get the shared PostgreSQL container instance. If the container is not yet started, it
   * initializes and starts it.
   *
   * @return The PostgreSQL container instance
   */
  public static synchronized PostgreSQLContainer<?> getInstance() {
    if (instance == null) {
      instance = createAndStartContainer();
    }
    return instance;
  }

  /**
   * Creates and starts a new PostgreSQL container.
   *
   * @return The started PostgreSQL container
   */
  private static PostgreSQLContainer<?> createAndStartContainer() {
    PostgreSQLContainer<?> container =
        new PostgreSQLContainer<>(POSTGIS_IMAGE_NAME.withTag(POSTGRES_POSTGIS_VERSION))
            .withDatabaseName(POSTGRES_DATABASE_NAME)
            .withUsername(POSTGRES_USERNAME)
            .withPassword(POSTGRES_PASSWORD)
            .withTmpFs(Map.of("/testtmpfs", "rw"))
            .withEnv("LC_COLLATE", "C")
            // Performance optimization settings
            .withCommand(
                "postgres",
                "-c",
                "shared_buffers=256MB",
                "-c",
                "max_connections=200",
                "-c",
                "fsync=off",
                "-c",
                "synchronous_commit=off",
                "-c",
                "full_page_writes=off")
            // Container reuse label
            .withLabel("reuse.dhis2", "true");

    if (initDbScriptIsPresent()) {
      container.withCopyFileToContainer(
          MountableFile.forClasspathResource("db/init-db.sql"), "/docker-entrypoint-initdb.d/");
    } else {
      container.withInitScript("db/extensions.sql");
    }

    container.start();
    return container;
  }

  /**
   * Checks whether an init DB script is present on the classpath.
   *
   * @return true if file is present
   */
  private static boolean initDbScriptIsPresent() {
    ClassPathResource resource = new ClassPathResource("db/init-db.sql");
    return resource.exists();
  }

  // Private constructor to prevent instantiation
  private SharedPostgresContainer() {}
}
