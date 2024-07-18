package org.hisp.dhis.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.config.MinIOConfiguration;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test class configured for use cases when DHIS2 is configured to use MinIO storage. The default
 * storage is the local filesystem.
 */
@ContextConfiguration(classes = {MinIOConfiguration.class})
class AppManagerTest extends TransactionalIntegrationTest {

  @Autowired AppManager appManager;

  @Test
  @DisplayName("Can install an App using MinIO storage")
  void canInstallAppUsingMinIoTest() throws IOException {
    AppStatus appStatus =
        appManager.installApp(
            new ClassPathResource("app/test-app-minio-v1.zip").getFile(), "test-app-minio-v1");

    assertTrue(appStatus.ok());
    assertEquals("ok", appStatus.getMessage());
  }

  @Test
  @DisplayName("Can update an App using MinIO storage")
  void canUpdateAppUsingMinIoTest() throws IOException {
    // install an app for the 1st time (version 1)
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
  }
}
