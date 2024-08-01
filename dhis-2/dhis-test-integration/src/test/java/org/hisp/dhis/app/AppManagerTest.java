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
package org.hisp.dhis.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.config.MinIOConfiguration;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test class configured for use cases when DHIS2 is configured to use MinIO storage. The default
 * storage is the local filesystem.
 */
@ContextConfiguration(classes = {MinIOConfiguration.class})
class AppManagerTest extends SingleSetupIntegrationTestBase {

  @Autowired AppManager appManager;
  @Autowired private UserService _userService;

  @Test
  @DisplayName("Can install and then update an App using MinIO storage")
  void canUpdateAppUsingMinIoTest() throws IOException {
    // install an app for the 1st time (version 1)
    userService = _userService;
    createAndInjectAdminUser();
    AppStatus appStatus =
        appManager.installApp(
            new ClassPathResource("app/test-app-minio-v1.zip").getFile(), "test-app-minio-v1.zip");

    assertTrue(appStatus.ok());
    assertEquals("ok", appStatus.getMessage());

    // install version 2 of the same app
    AppStatus update =
        assertDoesNotThrow(
            () ->
                appManager.installApp(
                    new ClassPathResource("app/test-app-minio-v2.zip").getFile(),
                    "test-app-minio-v2.zip"));

    assertTrue(update.ok());
    assertEquals("ok", appStatus.getMessage());

    // get app version & index.html
    App app = appManager.getApp("test minio");
    Resource resource = appManager.getAppResource(app, "index.html");

    assertEquals("2.0.0", app.getVersion());
    assertEquals(63, appManager.getUriContentLength(resource));
  }

  @Test
  @DisplayName("File resource content size is 38")
  void fileResourceContentLengthTest() {
    // given
    Resource resource =
        new FileSystemResource("./src/test/resources/app/test-file-content-length.txt");

    // when
    int uriContentLength = appManager.getUriContentLength(resource);

    // then
    assertEquals(38, uriContentLength);
  }

  @Test
  @DisplayName("File resource content size is -1 when exception thrown")
  void fileResourceContentLengthUnknownTest() throws IOException {
    // given
    Resource mockResource = mock(UrlResource.class);
    when(mockResource.getURL()).thenThrow(IOException.class);

    // when
    int uriContentLength = appManager.getUriContentLength(mockResource);

    // then
    assertEquals(-1, uriContentLength);
  }
}
