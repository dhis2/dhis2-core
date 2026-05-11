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
package org.hisp.dhis.test.junit;

import java.util.Properties;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.config.PostgresDhisConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link Configuration} that wires the active {@link DhisConfigurationProvider} to point at
 * the MinIO container managed by {@link MinIOTestExtension}.
 *
 * <p>Use alongside {@code @ExtendWith(MinIOTestExtension.class)} on Spring-aware integration test
 * classes:
 *
 * <pre>{@code
 * @ExtendWith(MinIOTestExtension.class)
 * @ContextConfiguration(classes = MinIOTestConfig.class)
 * class MyTest extends PostgresIntegrationTestBase { ... }
 * }</pre>
 */
@Configuration
public class MinIOTestConfig {

  @Bean
  public DhisConfigurationProvider dhisConfigurationProvider() {
    Properties properties = new Properties();
    properties.put("filestore.provider", "s3");
    properties.put("filestore.container", "dhis2");
    // No filestore.location: jclouds otherwise sends a CreateBucketConfiguration with a
    // LocationConstraint, which newer MinIO releases (>= RELEASE.2025-04-22) reject when the
    // value doesn't match the server's region. With no location, jclouds uses the server default.
    properties.put("filestore.endpoint", MinIOTestExtension.s3Url());
    properties.put("filestore.identity", MinIOTestExtension.MINIO_USER);
    properties.put("filestore.secret", MinIOTestExtension.MINIO_PASSWORD);

    PostgresDhisConfigurationProvider provider = new PostgresDhisConfigurationProvider(null);
    provider.addProperties(properties);
    return provider;
  }
}
