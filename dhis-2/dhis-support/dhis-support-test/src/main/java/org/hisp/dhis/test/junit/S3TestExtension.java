/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.test.junit;

import java.util.Properties;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.config.PostgresDhisConfigurationProvider;
import org.junit.jupiter.api.extension.Extension;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Use this configuration for tests relying on S3 storage running in a Docker container. The
 * container runs <a href="https://github.com/seaweedfs/seaweedfs">SeaweedFS</a> with only its S3
 * gateway enabled. The container is started once per JVM and shared across all test classes that
 * use this extension; Testcontainers' Ryuk reaper handles teardown on JVM exit. Just add to test
 * class like
 *
 * <p>@ExtendWith(S3TestExtension.class)
 *
 * <p>@ContextConfiguration(classes = {S3TestExtension.DhisConfig.class})
 *
 * @author david mackessy
 */
public class S3TestExtension implements Extension {

  public static final String S3_ACCESS_KEY = "testuser";
  public static final String S3_SECRET_KEY = "testpassword";

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

  /**
   * Endpoint URL the running S3 container is reachable on (e.g. {@code http://localhost:32812}).
   */
  public static String s3Url() {
    return S3_URL;
  }

  public static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      Properties properties = new Properties();
      properties.put("filestore.provider", "s3");
      properties.put("filestore.container", "dhis2");
      properties.put("filestore.location", "eu-west-1");
      properties.put("filestore.endpoint", S3_URL);
      properties.put("filestore.identity", S3_ACCESS_KEY);
      properties.put("filestore.secret", S3_SECRET_KEY);

      PostgresDhisConfigurationProvider pgDhisConfig = new PostgresDhisConfigurationProvider(null);
      pgDhisConfig.addProperties(properties);
      return pgDhisConfig;
    }
  }
}
