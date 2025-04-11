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
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MinIOContainer;

/**
 * Use this configuration for tests relying on MinIO storage running in a Docker container. The
 * container is stopped after the tests in the class have completed. Just add to test class like
 *
 * <p>@ExtendWith(MinIOTestExtension.class)
 *
 * <p>@ContextConfiguration(classes = {MinIOConfig.class})
 *
 * <p>If there are many uses of this extension then it should be considered whether keeping the
 * container up for the entirety of the tests is more preferable, rather than starting/stopping
 * multiple containers.
 *
 * @author david mackessy
 */
public class MinIOTestExtension implements AfterAllCallback {

  private static final String S3_URL;
  private static final String MINIO_USER = "testuser";
  private static final String MINIO_PASSWORD = "testpassword";
  private static final MinIOContainer MIN_IO_CONTAINER;

  static {
    MIN_IO_CONTAINER =
        new MinIOContainer("minio/minio:RELEASE.2024-07-16T23-46-41Z")
            .withUserName(MINIO_USER)
            .withPassword(MINIO_PASSWORD);
    MIN_IO_CONTAINER.start();
    S3_URL = MIN_IO_CONTAINER.getS3URL();
  }

  public static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      Properties properties = new Properties();
      properties.put("filestore.provider", "s3");
      properties.put("filestore.container", "dhis2");
      properties.put("filestore.location", "eu-west-1");
      properties.put("filestore.endpoint", S3_URL);
      properties.put("filestore.identity", MINIO_USER);
      properties.put("filestore.secret", MINIO_PASSWORD);

      PostgresDhisConfigurationProvider pgDhisConfig = new PostgresDhisConfigurationProvider(null);
      pgDhisConfig.addProperties(properties);
      return pgDhisConfig;
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    MIN_IO_CONTAINER.stop();
  }
}
