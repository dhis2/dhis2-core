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
package org.hisp.dhis.config;

import java.util.Properties;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Config provider for S3 store usage, backed by a <a
 * href="https://github.com/seaweedfs/seaweedfs">SeaweedFS</a> container with only its S3 gateway
 * enabled. It extends the Postgres config to make use of that setup.
 *
 * @author david mackessy
 */
public class S3ConfigurationProvider extends PostgresDhisConfigurationProvider {
  private static final String S3_ACCESS_KEY = "testuser";
  private static final String S3_SECRET_KEY = "testpassword";
  private static final int S3_PORT = 8333;

  private static final String S3_URL;
  private static final GenericContainer<?> S3_CONTAINER;

  static {
    S3_CONTAINER =
        new GenericContainer<>("chrislusf/seaweedfs:4.47")
            .withCommand(
                "server",
                "-dir=/data",
                "-s3",
                "-s3.port.iceberg=0",
                "-s3.port.lance=0",
                "-master.telemetry=false")
            // SeaweedFS registers these as the admin S3 identity.
            .withEnv("AWS_ACCESS_KEY_ID", S3_ACCESS_KEY)
            .withEnv("AWS_SECRET_ACCESS_KEY", S3_SECRET_KEY)
            .withExposedPorts(S3_PORT)
            .waitingFor(Wait.forHttp("/healthz").forPort(S3_PORT));
    S3_CONTAINER.start();
    S3_URL = "http://" + S3_CONTAINER.getHost() + ":" + S3_CONTAINER.getMappedPort(S3_PORT);
  }

  public S3ConfigurationProvider(Properties dhisConfig) {
    setS3Properties(dhisConfig);
  }

  public void setS3Properties(Properties properties) {
    properties.put("filestore.provider", "s3");
    properties.put("filestore.container", "dhis2");
    properties.put("filestore.location", "eu-west-1");
    properties.put("filestore.endpoint", S3_URL);
    properties.put("filestore.identity", S3_ACCESS_KEY);
    properties.put("filestore.secret", S3_SECRET_KEY);
    this.properties = properties;
  }
}
