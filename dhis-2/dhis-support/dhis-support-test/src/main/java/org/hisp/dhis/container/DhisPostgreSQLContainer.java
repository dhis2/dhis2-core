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
package org.hisp.dhis.container;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Custom {@link PostgreSQLContainer} that provides additional fluent API to customize PostgreSQL
 * configuration.
 *
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
public class DhisPostgreSQLContainer<SELF extends DhisPostgreSQLContainer<SELF>>
    extends PostgreSQLContainer<SELF> {
  private Set<String> customPostgresConfigs = new HashSet<>();

  public DhisPostgreSQLContainer(DockerImageName dockerImageName) {
    super(dockerImageName);
  }

  @Override
  protected void configure() {
    addExposedPort(POSTGRESQL_PORT);
    addEnv("POSTGRES_DB", getDatabaseName());
    addEnv("POSTGRES_USER", getUsername());
    addEnv("POSTGRES_PASSWORD", getPassword());
    setCommand(getPostgresCommandWithCustomConfigs());
    withInitScript("db/extensions.sql");
  }

  private String getPostgresCommandWithCustomConfigs() {
    StringBuilder builder = new StringBuilder();
    builder.append("postgres");

    if (!this.customPostgresConfigs.isEmpty()) {
      this.customPostgresConfigs.forEach(
          config -> {
            builder.append(" -c ");
            builder.append(config);
          });
    }
    return builder.toString();
  }

  /**
   * Append custom postgres configuration to be customized when starting the container. The
   * configAndValue should be of the form "configName=configValue". This method can be invoked
   * multiple times to add multiple custom commands.
   *
   * @param configAndValue The configuration and value of the form "configName=configValue"
   * @return the DhisPostgreSQLContainer
   */
  public SELF appendCustomPostgresConfig(String configAndValue) {
    if (!StringUtils.isBlank(configAndValue)) {
      this.customPostgresConfigs.add(configAndValue);
    }
    return self();
  }
}
