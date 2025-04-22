/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.appmanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Component that installs bundled app ZIP files during server startup. This detects ZIP files in
 * the bundled apps directory and installs them using the AppManager.
 */
@Component
public class BundledAppInstaller implements ApplicationListener<ContextRefreshedEvent> {
  private static final Logger logger = LoggerFactory.getLogger(BundledAppInstaller.class);
  private static final String APPS_PATH = "classpath:static/*.zip";

  @Autowired private AppManager appManager;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    logger.info("Checking for bundled app ZIP files to install");
    //        try {
    //            installBundledApps();
    //        } catch (Exception e) {
    //            logger.error("Error installing bundled apps: {}", e.getMessage(), e);
    //        }
  }

  /**
   * Installs all bundled app ZIP files found in the classpath.
   *
   * @throws IOException if there's an error accessing the bundled app files
   */
  private void installBundledApps() throws IOException {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources(APPS_PATH);

    logger.info("Found {} bundled app ZIP files", resources.length);

    for (Resource resource : resources) {
      try {
        installAppFromResource(resource);
      } catch (Exception e) {
        logger.error("Error installing app from {}: {}", resource.getFilename(), e.getMessage(), e);
      }
    }
  }

  /**
   * Installs an app from a resource.
   *
   * @param resource the resource containing the app ZIP file
   * @throws IOException if there's an error accessing the resource
   */
  private void installAppFromResource(Resource resource) throws IOException {
    String fileName = resource.getFilename();
    if (fileName == null) {
      logger.warn("Skipping resource with no filename");
      return;
    }

    logger.info("Installing bundled app: {}", fileName);

    // Create a temporary file from the resource
    Path tempFile = Files.createTempFile("bundled-app-", fileName);
    Files.copy(resource.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

    // Install the app using the AppManager
    appManager.installApp(tempFile.toFile(), fileName);

    // Clean up the temporary file
    try {
      Files.deleteIfExists(tempFile);
    } catch (IOException e) {
      logger.warn("Failed to delete temporary file: {}", tempFile, e);
    }
  }
}
