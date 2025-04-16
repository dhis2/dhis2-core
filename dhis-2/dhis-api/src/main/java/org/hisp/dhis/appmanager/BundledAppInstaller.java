package org.hisp.dhis.appmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Component that installs bundled app ZIP files during server startup.
 * This detects ZIP files in the bundled apps directory and installs them
 * using the AppManager.
 */
@Component
public class BundledAppInstaller implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(BundledAppInstaller.class);
    private static final String APPS_PATH = "classpath:static/*.zip";

    @Autowired
    private AppManager appManager;

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