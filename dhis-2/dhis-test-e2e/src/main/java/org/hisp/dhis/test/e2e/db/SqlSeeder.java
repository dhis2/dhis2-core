/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.test.e2e.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;

/**
 * Applies SQL scripts to the database of the instance under test, so tests can rely on data the
 * static e2e database dump does not contain.
 */
public class SqlSeeder {
  /** Directory holding the scripts, relative to the module root. */
  public static final String SEED_DIR = "src/test/resources/db/seed";

  /** Applies when {@code db.seed.timeout.seconds} is not set. */
  private static final int DEFAULT_TIMEOUT_SECONDS = 1800;

  private static final Logger logger = LogManager.getLogger(SqlSeeder.class.getName());

  private SqlSeeder() {}

  /**
   * Applies every {@code .sql} file in {@link #SEED_DIR}, ordered by file name. Each file is sent
   * as a single multi-statement query, which PostgreSQL applies atomically.
   *
   * @return the number of scripts applied
   * @throws IllegalStateException if the scripts cannot be listed, read or applied
   */
  public static int applySeedScripts() {
    // Checked before the directory is looked at, so that disabling seeding cannot fail on a
    // missing directory.
    if (!Optional.ofNullable(TestConfiguration.get().shouldApplySeedScripts()).orElse(true)) {
      logger.info("SQL seeding is disabled by db.seed.enabled=false; skipping {}", SEED_DIR);
      return 0;
    }

    List<Path> scripts = seedScripts(Path.of(SEED_DIR));

    if (scripts.isEmpty()) {
      logger.info("No SQL seed scripts in {}", SEED_DIR);
      return 0;
    }

    String url = TestConfiguration.get().databaseUrl();
    logger.info("Applying {} SQL seed script(s) to {}", scripts.size(), url);

    try (Connection connection =
        DriverManager.getConnection(
            url,
            TestConfiguration.get().databaseUsername(),
            TestConfiguration.get().databasePassword())) {
      for (Path script : scripts) {
        apply(connection, script, timeoutSeconds());
      }
    } catch (SQLException e) {
      throw new IllegalStateException(
          "Could not connect to "
              + url
              + " to apply the SQL seed scripts in "
              + SEED_DIR
              + ". Pass -Ddb.url, -Ddb.username and -Ddb.password when the database of the instance"
              + " under test is not the one at that url.",
          e);
    }

    logger.info("Applied {} SQL seed script(s)", scripts.size());

    return scripts.size();
  }

  private static void apply(Connection connection, Path script, int timeoutSeconds) {
    logger.info("Applying SQL seed script {}", script);

    try (Statement statement = connection.createStatement()) {
      // A script blocked on a lock would otherwise run for as long as the CI job allows: nothing
      // else bounds it, since the analytics profile disables JUnit's timeouts and they would not
      // cover an extension callback anyway.
      statement.setQueryTimeout(timeoutSeconds);
      statement.execute(Files.readString(script));
    } catch (IOException | SQLException e) {
      throw new IllegalStateException("Failed to apply SQL seed script " + script, e);
    }
  }

  private static int timeoutSeconds() {
    return Optional.ofNullable(TestConfiguration.get().seedTimeoutSeconds())
        .orElse(DEFAULT_TIMEOUT_SECONDS);
  }

  static List<Path> seedScripts(Path dir) {
    if (!Files.isDirectory(dir)) {
      throw new IllegalStateException(
          "The SQL seed directory "
              + dir
              + " does not exist. It is committed to the repository, so this normally means the"
              + " working directory is not the dhis-test-e2e module root. Current working directory:"
              + " "
              + Path.of("").toAbsolutePath()
              + ".");
    }

    try (Stream<Path> files = Files.list(dir)) {
      return files
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".sql"))
          .sorted(Comparator.comparing(path -> path.getFileName().toString()))
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list the SQL seed scripts in " + dir, e);
    }
  }
}
