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
package org.hisp.dhis.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.appmanager.ResourceResult.Redirect;
import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.appmanager.ResourceResult.ResourceNotFound;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.test.junit.MinIOTestExtension;
import org.hisp.dhis.test.junit.MinIOTestExtension.DhisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
@ExtendWith(MinIOTestExtension.class)
@ContextConfiguration(classes = {DhisConfig.class})
class AppManagerMinIOTest extends PostgresIntegrationTestBase {

  private static final String MOCK_CONTEXT_PATH = "/context";

  @Autowired AppManager appManager;

  @Test
  @DisplayName("Can install and then update an App using MinIO storage")
  void canUpdateAppUsingMinIOStorageTest() throws IOException {
    // install an app for the 1st time (version 1)
    App installedApp =
        appManager.installApp(
            new ClassPathResource("app/test-app-minio-v1.zip").getFile(), "test-app-minio-v1.zip");

    AppStatus appStatus = installedApp.getAppState();

    assertTrue(appStatus.ok());
    assertEquals("ok", appStatus.getMessage());

    // install version 2 of the same app
    App updatedApp =
        assertDoesNotThrow(
            () ->
                appManager.installApp(
                    new ClassPathResource("app/test-app-minio-v2.zip").getFile(),
                    "test-app-minio-v2.zip"));

    assertTrue(updatedApp.getAppState().ok());
    assertEquals("ok", appStatus.getMessage());

    // get app version & index.html
    App app = appManager.getApp("test minio");
    ResourceFound resource =
        (ResourceFound) appManager.getAppResource(app, "index.html", MOCK_CONTEXT_PATH);

    assertEquals("2.0.0", app.getVersion());
    assertEquals(64, appManager.getUriContentLength(resource.resource()));
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

  @ParameterizedTest
  @MethodSource("validPathParams")
  @DisplayName("Calls with valid app resource paths should resolve correctly")
  void appPathResolveTest(String path, String expectedPath) throws IOException {
    // given an app is installed in object storage
    App installedApp =
        appManager.installApp(
            new ClassPathResource("app/test-app-minio-v1.zip").getFile(), "test-app-minio-v1.zip");

    AppStatus appStatus = installedApp.getAppState();

    assertTrue(appStatus.ok());
    assertEquals("ok", appStatus.getMessage());

    // when an app resource is retrieved with a valid path
    App app = appManager.getApp("test minio");
    ResourceFound resource =
        (ResourceFound) appManager.getAppResource(app, path, MOCK_CONTEXT_PATH);

    // then the resource path returned is the full resource path which ends with `/index.html`
    assertEquals(
        expectedPath,
        resource.resource().getURI().getPath(),
        "resource path should match expected format");
  }

  @ParameterizedTest
  @MethodSource("redirectPathParams")
  @DisplayName(
      "Calls to valid directories with no trailing slash should return with a trailing slash")
  void appPathRedirectTest(String path, String expectedPath) throws IOException {
    // given an app is installed in object storage
    App installedApp =
        appManager.installApp(
            new ClassPathResource("app/test-app-minio-v1.zip").getFile(), "test-app-minio-v1.zip");

    AppStatus appStatus = installedApp.getAppState();

    assertTrue(appStatus.ok());
    assertEquals("ok", appStatus.getMessage());

    // when an app resource is retrieved with a redirect path
    App app = appManager.getApp("test minio");
    Redirect resource = (Redirect) appManager.getAppResource(app, path, MOCK_CONTEXT_PATH);

    // then the path returned should end in a trailing slash
    assertEquals(expectedPath, resource.path(), "redirect path should have trailing slash");
  }

  @ParameterizedTest
  @MethodSource("notFoundPathParams")
  @DisplayName("When resources are not found, return null")
  void appPathNotFoundTest(String path) throws IOException {
    // given an app is installed in object storage
    App installedApp =
        appManager.installApp(
            new ClassPathResource("app/test-app-minio-v1.zip").getFile(), "test-app-minio-v1.zip");

    AppStatus appStatus = installedApp.getAppState();

    assertTrue(appStatus.ok());
    assertEquals("ok", appStatus.getMessage());

    // when non-existent an app resource path is retrieved
    App app = appManager.getApp("test minio");
    ResourceNotFound resource =
        (ResourceNotFound) appManager.getAppResource(app, path, MOCK_CONTEXT_PATH);

    // then the path returned should be null
    assertEquals(path, resource.path());
  }

  private static Stream<Arguments> validPathParams() {
    return Stream.of(
        Arguments.of("index.html", "/dhis2/apps/test-app-minio-v1/index.html"),
        Arguments.of("/index.html", "/dhis2/apps/test-app-minio-v1/index.html"),
        Arguments.of("subDir/", "/dhis2/apps/test-app-minio-v1/subDir/index.html"),
        Arguments.of("subDir/index.html", "/dhis2/apps/test-app-minio-v1/subDir/index.html"),
        Arguments.of(
            "subDir/test-page.html", "/dhis2/apps/test-app-minio-v1/subDir/test-page.html"),
        Arguments.of(
            "subDir/subSubDir/", "/dhis2/apps/test-app-minio-v1/subDir/subSubDir/index.html"));
  }

  private static Stream<Arguments> redirectPathParams() {
    return Stream.of(
        Arguments.of("", "/"),
        Arguments.of("subDir", "subDir/"),
        Arguments.of("emptyDir", "emptyDir/"),
        Arguments.of("subDir/subSubDir", "subDir/subSubDir/"));
  }

  private static Stream<Arguments> notFoundPathParams() {
    return Stream.of(
        Arguments.of("noFile.html"),
        Arguments.of("invalidDir"),
        Arguments.of("invalidDir/"),
        Arguments.of("emptyDir/"),
        Arguments.of("subDir/invalidDir"));
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
