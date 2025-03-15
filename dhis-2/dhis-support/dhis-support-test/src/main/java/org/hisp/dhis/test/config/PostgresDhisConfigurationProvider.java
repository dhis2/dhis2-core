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
package org.hisp.dhis.test.config;

import java.util.Properties;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author Luciano Fiandesio
 */
public class PostgresDhisConfigurationProvider extends TestDhisConfigurationProvider {
  private static final String DEFAULT_CONFIGURATION_FILE_NAME = "postgresTestDhis.conf";

  /** Using the shared PostgreSQL container instance that can be reused across tests. */
  private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
      SharedPostgresContainer.getInstance();

  public PostgresDhisConfigurationProvider() {
    Properties dhisConfig = new Properties();
    dhisConfig.putAll(getPropertiesFromFile(DEFAULT_CONFIGURATION_FILE_NAME));
    dhisConfig.putAll(getConnectionProperties());
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
    properties.setProperty("connection.username", SharedPostgresContainer.POSTGRES_USERNAME);
    properties.setProperty("connection.password", SharedPostgresContainer.POSTGRES_PASSWORD);
    return properties;
  }
}
