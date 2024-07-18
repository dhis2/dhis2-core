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

import java.util.Properties;
import org.testcontainers.containers.MinIOContainer;

/**
 * Config provider for MinIO store usage. It extends the Postgres config, resulting in Postgres
 * setup + MinIO storage (overriding the default file system storage).
 *
 * @author david mackessy
 */
public class MinIOConfigurationProvider extends PostgresDhisConfigurationProvider {
  private static final String MINIO_VERSION = "minio/minio:RELEASE.2023-09-04T19-57-37Z";
  private static final MinIOContainer MINIO_CONTAINER;
  private static final String MINIO_USER = "testuser";
  private static final String MINIO_PASSWORD = "testpassword";

  static {
    MINIO_CONTAINER =
        new MinIOContainer(MINIO_VERSION).withUserName(MINIO_USER).withPassword(MINIO_PASSWORD);
    MINIO_CONTAINER.start();
  }

  public MinIOConfigurationProvider() {
    Properties dhisConfig = super.getProperties();
    dhisConfig.putAll(getMinIOProperties());
    this.properties = dhisConfig;
  }

  private static Properties getMinIOProperties() {
    Properties properties = new Properties();
    properties.put("filestore.provider", "s3");
    properties.put("filestore.container", "dhis2");
    properties.put("filestore.location", "eu-west-1");
    properties.put("filestore.endpoint", MINIO_CONTAINER.getS3URL());
    properties.put("filestore.identity", MINIO_USER);
    properties.put("filestore.secret", MINIO_PASSWORD);
    return properties;
  }
}
