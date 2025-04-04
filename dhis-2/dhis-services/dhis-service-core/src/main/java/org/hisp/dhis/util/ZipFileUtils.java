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
package org.hisp.dhis.util;

import static org.hisp.dhis.appmanager.AppStorageService.MANIFEST_FILENAME;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.appmanager.AppStorageSource;
import org.hisp.dhis.jclouds.JCloudsStore;
import org.jclouds.blobstore.domain.Blob;

/**
 * @author Austin McGee
 */
@Slf4j
public class ZipFileUtils {
  private ZipFileUtils() {
    throw new UnsupportedOperationException("util");
  }

  // 1 GB total max size
  public static final long MAX_TOTAL_UNCOMPRESSED_SIZE = (long) 1024 * 1024 * 1024;
  // Max number of entries
  public static final int MAX_ENTRIES = 10000;
  // Max compression ratio (uncompressed / compressed)
  public static final double MAX_COMPRESSION_RATIO = 100.0;
  private static final Pattern TOP_LEVEL_DIRECTORY_PREFIX_PATTERN =
      Pattern.compile("^([^/\\\\]+[/\\\\]).*");

  /**
   * Finds the top level directory in a zip file with 'TOP_LEVEL_DIRECTORY_PREFIX_PATTERN' that
   * matches then extract like this: 1. home/user/file.txt -> home/ 2. data\archive.zip -> data\ 3.
   * project/src/main.java -> project/ 4. dir/ -> dir/
   *
   * <p>It looks at the first entry and checks if all other files starts with the same folder name.
   * If not all entries are in the same root folder, it returns an empty string.
   *
   * @param zipEntries `Iterator` of `ZipEntry` objects
   * @return the top level directory or an empty string if there is no same top level directory for
   *     all entries
   */
  public static String getTopLevelDirectory(Iterator<? extends ZipEntry> zipEntries) {
    if (!zipEntries.hasNext()) {
      return "";
    }
    ZipEntry firstEntry = zipEntries.next();

    Matcher rootFolderMatch = TOP_LEVEL_DIRECTORY_PREFIX_PATTERN.matcher(firstEntry.getName());
    if (rootFolderMatch.find()) {
      final String rootFolderName = rootFolderMatch.group(1);

      Stream<ZipEntry> stream =
          StreamSupport.stream(
              Spliterators.spliteratorUnknownSize(zipEntries, Spliterator.ORDERED), false);

      boolean allMatch = stream.allMatch((ZipEntry ze) -> ze.getName().startsWith(rootFolderName));
      if (allMatch) {
        return rootFolderName;
      }
    }

    return "";
  }

  private static void unzipAllFiles(
      ZipFile zipFile,
      String topLevelFolder,
      String appFolder,
      JCloudsStore jCloudsStore,
      AtomicBoolean zipBombDetected,
      AtomicBoolean unzipFailure) {

    final LongAdder totalUncompressedSize = new LongAdder();
    final AtomicInteger entryCount = new AtomicInteger(0);

    zipFile.stream()
        .forEach(
            (Consumer<ZipEntry>)
                zipEntry -> {
                  // Skip the rest if we have failed (general failure or zip bomb detected)!
                  if (unzipFailure.get() || zipBombDetected.get()) return;

                  if (entryCount.incrementAndGet() > MAX_ENTRIES) {
                    log.error("Maximum number of entries ({}) exceeded.", MAX_ENTRIES);
                    zipBombDetected.set(true);
                    return;
                  }

                  String filePath;
                  try {
                    filePath = validateFilePath(topLevelFolder, appFolder, zipEntry);
                    // If it's the root folder, skip
                    if (filePath == null) return;

                  } catch (IOException e) {
                    log.error("Unable validate zip file's name or path", e);
                    unzipFailure.set(true);
                    return;
                  }

                  try {
                    validateSizes(zipEntry, totalUncompressedSize);
                  } catch (IOException e) {
                    log.error("Error during ZipBomb protection checks", e);
                    zipBombDetected.set(true);
                    return;
                  }

                  try (InputStream zipInputStream = zipFile.getInputStream(zipEntry)) {
                    Blob blob =
                        jCloudsStore
                            .getBlobStore()
                            .blobBuilder(filePath)
                            .payload(zipInputStream)
                            .contentLength(zipEntry.getSize())
                            .build();
                    jCloudsStore.putBlob(blob);

                  } catch (IOException e) {
                    log.error("Unable to store app file from zip manifestEntry", e);
                    unzipFailure.set(true);
                  } catch (RuntimeException e) {
                    log.error("Runtime error processing app file from zip manifestEntry", e);
                    unzipFailure.set(true);
                  }
                });
  }

  private static @CheckForNull String validateFilePath(
      String topLevelFolder, String appFolder, ZipEntry zipEntry) throws IOException {
    String normalizedName = validateFilePaths(topLevelFolder, zipEntry.getName());
    if (normalizedName.isBlank()) {
      return null;
    }
    String sanitizedName = getSanitizedName(appFolder, normalizedName);
    return appFolder + File.separator + sanitizedName;
  }

  private static @Nonnull String validateFilePaths(String topLevelFolder, String filename)
      throws IOException {
    // Check for relative (..) and absolute paths in the file name
    File file = new File(filename);
    if (file.isAbsolute() || file.getPath().contains("..")) {
      throw new IOException("Invalid zip manifestEntry name, contains possible path traversal");
    }

    // Remove the prefix from the filename
    filename = filename.substring(topLevelFolder.length());

    // Normalize the path to resolve '.' and '..' components
    String normalizedName = FilenameUtils.normalize(filename);
    if (normalizedName == null) {
      // Normalization failed, likely due to invalid characters/sequences
      throw new IOException("Invalid zip manifestEntry name, failed to normalize");
    }

    return normalizedName;
  }

  private static @Nonnull String getSanitizedName(String appFolder, String normalizedName)
      throws IOException {
    String sanitizedName = normalizedName.replaceAll("^[./\\\\]+", "").replace('\\', '/');

    // Check sanitizedName is not trying to escape the appFolder
    String canonicalBasePath = new File(appFolder).getCanonicalPath();
    String canonicalDestPath = new File(appFolder, sanitizedName).getCanonicalPath();
    if (!canonicalDestPath.startsWith(canonicalBasePath + File.separator)) {
      throw new IOException(
          "Invalid zip manifestEntry path after sanitization, potential traversal attempt");
    }
    return sanitizedName;
  }

  private static void validateSizes(ZipEntry entry, LongAdder totalUncompressedSize)
      throws IOException {
    long entrySize = entry.getSize(); // Uncompressed size
    long compressedSize = entry.getCompressedSize();
    if (entrySize < 0) {
      String formatted =
          "Invalid zip manifestEntry: Negative uncompressed size (%s) for manifestEntry"
              .formatted(entrySize);
      log.error(formatted);
      throw new IOException(formatted);
    }

    totalUncompressedSize.add(entrySize);
    if (totalUncompressedSize.sum() > MAX_TOTAL_UNCOMPRESSED_SIZE) {
      String formatted =
          "Zip bomb detected: Maximum total uncompressed size (%s) exceeded."
              .formatted(MAX_COMPRESSION_RATIO);
      log.error(formatted);
      throw new IOException(formatted);
    }

    if (compressedSize > 0) {
      double compressionRatio = (double) entrySize / compressedSize;
      if (compressionRatio > MAX_COMPRESSION_RATIO) {
        String formatted =
            "Zip bomb detected: Maximum compression ratio (%s) exceeded for manifestEntry, (Ratio: %s)."
                .formatted(MAX_COMPRESSION_RATIO, String.format("%.2f", compressionRatio));
        log.error(formatted);
        throw new IOException(formatted);
      }
    }
  }

  public static String getTopLevelFolder(File file) throws IOException {
    try (ZipFile zip = new ZipFile(file)) {
      // Determine top-level directory name, if the zip file contains one
      return ZipFileUtils.getTopLevelDirectory(zip.entries().asIterator());
    }
  }

  public static App readManifest(File file, ObjectMapper jsonMapper, String topLevelFolder)
      throws IOException {
    App app = new App();
    try (ZipFile zip = new ZipFile(file)) {
      // Parse manifest.webapp file from ZIP archive.
      ZipEntry manifestEntry = zip.getEntry(topLevelFolder + MANIFEST_FILENAME);
      if (manifestEntry == null) {
        log.error("Failed to install app: Missing manifest.webapp in zip");
        app.setAppState(AppStatus.MISSING_MANIFEST);
      }
      InputStream inputStream = zip.getInputStream(manifestEntry);
      app = jsonMapper.readValue(inputStream, App.class);
      app.setAppStorageSource(AppStorageSource.JCLOUDS);
    }

    return app;
  }

  public static App unzip(
      File file, App app, String appFolder, String topLevelFolder, JCloudsStore storage) {

    try (ZipFile zipFile = new ZipFile(file)) {
      final AtomicBoolean zipBombDetected = new AtomicBoolean(false);
      final AtomicBoolean unzipFailure = new AtomicBoolean(false);

      unzipAllFiles(zipFile, topLevelFolder, appFolder, storage, zipBombDetected, unzipFailure);

      if (zipBombDetected.get()) {
        log.error("Failed to install app: Zip bomb detected during processing.");
        app.setAppState(AppStatus.INVALID_ZIP_FORMAT);
        return app;
      }

      if (unzipFailure.get()) {
        log.error("Failed to install app: Failure during unzipping");
        app.setAppState(AppStatus.INVALID_ZIP_FORMAT);
        return app;
      }

      // Set the app state to OK and return the app
      app.setAppState(AppStatus.OK);
      return app;

    } catch (ZipException e) {
      log.error("Failed to install app: Invalid ZIP format", e);
      app.setAppState(AppStatus.INVALID_ZIP_FORMAT);
    } catch (JsonParseException e) {
      log.error("Failed to install app: Invalid manifest.webapp", e);
      app.setAppState(AppStatus.INVALID_MANIFEST_JSON);
    } catch (IOException e) {
      log.error("Failed to install app: Could not save app", e);
      app.setAppState(AppStatus.INSTALLATION_FAILED);
    }

    // Failed return
    return app;
  }
}
