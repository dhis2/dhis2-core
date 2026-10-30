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
package org.hisp.dhis.test.tracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads files from a public S3 bucket using HTTP conditional GET (ETag). Files are cached
 * locally with a {@code .etag} sidecar file. Subsequent calls skip the download if the ETag
 * matches, costing only a single conditional GET per file (S3 returns 304 Not Modified with zero
 * transfer).
 */
class S3Fetcher {

  private static final Logger logger = LoggerFactory.getLogger(S3Fetcher.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(60);

  private S3Fetcher() {
    throw new UnsupportedOperationException("utility class");
  }

  /**
   * Ensures each file in {@code fileNames} at {@code cacheDir} is up to date with the S3 object at
   * {@code baseUrl/fileName}. Downloads only if the file does not exist locally or the remote ETag
   * has changed.
   *
   * @param baseUrl base HTTPS URL (without trailing slash)
   * @param cacheDir local directory to cache downloads
   * @param fileNames file names to fetch
   */
  static void fetchAll(String baseUrl, Path cacheDir, String... fileNames) {
    try {
      Files.createDirectories(cacheDir);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create cache directory: " + cacheDir, e);
    }

    HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    for (String fileName : fileNames) {
      fetch(client, baseUrl + "/" + fileName, cacheDir.resolve(fileName));
    }
  }

  private static void fetch(HttpClient client, String url, Path localPath) {
    try {
      Path etagPath = localPath.resolveSibling(localPath.getFileName() + ".etag");
      String cachedEtag = readEtag(etagPath);

      HttpRequest.Builder requestBuilder =
          HttpRequest.newBuilder().uri(URI.create(url)).timeout(TIMEOUT);
      if (cachedEtag != null && Files.exists(localPath)) {
        requestBuilder.header("If-None-Match", cachedEtag);
      }

      HttpResponse<InputStream> response =
          client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() == 304) {
        logger.info("Up to date: {}", localPath.getFileName());
        return;
      }

      if (response.statusCode() != 200) {
        String body = "";
        try (InputStream is = response.body()) {
          body = new String(is.readNBytes(1024));
        } catch (IOException ignored) {
        }
        throw new IOException(
            "HTTP " + response.statusCode() + " for " + url + ": " + body.strip());
      }

      Path tmpFile = localPath.resolveSibling(localPath.getFileName() + ".tmp");
      try (InputStream body = response.body()) {
        Files.copy(body, tmpFile, StandardCopyOption.REPLACE_EXISTING);
      }
      Files.move(tmpFile, localPath, StandardCopyOption.REPLACE_EXISTING);

      String newEtag = response.headers().firstValue("ETag").orElse(null);
      if (newEtag != null) {
        Files.writeString(etagPath, newEtag);
      }
      logger.info("Downloaded {} ({} bytes)", localPath.getFileName(), Files.size(localPath));
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to fetch " + url + " to " + localPath, e);
    }
  }

  private static String readEtag(Path etagPath) {
    try {
      if (Files.exists(etagPath)) {
        return Files.readString(etagPath).strip();
      }
    } catch (IOException e) {
      logger.info("Could not read ETag file {}: {}", etagPath.getFileName(), e.getMessage());
    }
    return null;
  }
}
