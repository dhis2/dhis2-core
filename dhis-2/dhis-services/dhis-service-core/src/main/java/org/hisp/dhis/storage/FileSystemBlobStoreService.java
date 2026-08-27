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
package org.hisp.dhis.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;

/**
 * NIO-based implementation of {@link BlobStoreService} for the {@code filesystem} file store
 * provider. Blobs are stored as plain files under {@code <DHIS2 external dir>/<container>/}.
 *
 * <p>The backend is unsigned and unmetadata-aware: {@code contentType}, {@code contentDisposition},
 * and {@code contentHash} are accepted but not persisted (no native filesystem support, and the
 * contract only requires they not throw). {@link #signedGetUri} returns {@code null}.
 */
@Slf4j
public class FileSystemBlobStoreService implements BlobStoreService {

  private final BlobContainerName container;
  private final Path baseDir;

  public FileSystemBlobStoreService(
      DhisConfigurationProvider configurationProvider, LocationManager locationManager) {
    if (!locationManager.externalDirectorySet()) {
      throw new IllegalStateException(
          "File system blob store cannot start: external directory is not set.");
    }
    String containerName = configurationProvider.getProperty(ConfigurationKey.FILESTORE_CONTAINER);
    this.container = new BlobContainerName(containerName);
    this.baseDir = Paths.get(locationManager.getExternalDirectoryPath(), containerName);
  }

  @PostConstruct
  public void init() {
    try {
      Files.createDirectories(baseDir);
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to create file store directory at " + baseDir, e);
    }
    log.info("Filesystem blob store rooted at: '{}'", baseDir);
  }

  @Override
  public boolean blobExists(BlobKey key) {
    if (key == null) return false;
    Path p = resolve(key);
    return Files.isRegularFile(p);
  }

  @Override
  @CheckForNull
  public InputStream openStream(BlobKey key) {
    Path p = resolve(key);
    if (!Files.isRegularFile(p)) return null;
    try {
      return Files.newInputStream(p);
    } catch (IOException e) {
      log.warn("Unable to open stream for key: {}. {}", key, e.getMessage());
      return null;
    }
  }

  @Override
  public long contentLength(BlobKey key) {
    Path p = resolve(key);
    if (!Files.isRegularFile(p)) return 0L;
    try {
      return Files.size(p);
    } catch (IOException e) {
      return 0L;
    }
  }

  @Override
  public void putBlob(
      BlobKey key,
      InputStream content,
      long contentLength,
      @CheckForNull String contentType,
      @CheckForNull ContentDisposition contentDisposition,
      @CheckForNull ContentHash contentHash) {
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to write blob to " + target, e);
    }
  }

  @Override
  public void deleteBlob(BlobKey key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to delete blob at " + resolve(key), e);
    }
  }

  @Override
  public void deleteDirectory(BlobKeyPrefix prefix) {
    Path dir = baseDir.resolve(prefix.value());
    if (!Files.isDirectory(dir)) return;
    try (var paths = Files.walk(dir)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException e) {
                  throw new UncheckedIOException("Unable to delete " + p, e);
                }
              });
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to walk directory " + dir, e);
    }
  }

  @Override
  public Iterable<BlobKeyPrefix> listFolders(BlobKeyPrefix prefix) {
    Path dir = baseDir.resolve(prefix.value());
    if (!Files.isDirectory(dir)) return List.of();
    List<BlobKeyPrefix> result = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isDirectory)) {
      for (Path child : stream) {
        Path relative = baseDir.relativize(child);
        result.add(BlobKeyPrefix.of(toBlobPath(relative)));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to list folders under " + dir, e);
    }
    return result;
  }

  @Override
  public Iterable<BlobKey> listKeys(BlobKeyPrefix prefix) {
    Path dir = baseDir.resolve(prefix.value());
    if (!Files.isDirectory(dir)) return List.of();
    List<BlobKey> result = new ArrayList<>();
    try {
      Files.walkFileTree(
          dir,
          new SimpleFileVisitor<>() {
            @Nonnull
            @Override
            public FileVisitResult visitFile(
                @Nonnull Path file, @Nonnull BasicFileAttributes attrs) {
              Path relative = baseDir.relativize(file);
              result.add(BlobKey.of(toBlobPath(relative)));
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to walk directory " + dir, e);
    }
    return result;
  }

  @Override
  public boolean directoryExists(BlobKeyPrefix prefix) {
    return Files.isDirectory(baseDir.resolve(prefix.value()));
  }

  @Override
  public void createDirectory(BlobKeyPrefix prefix) {
    Path dir = baseDir.resolve(prefix.value());
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to create directory " + dir, e);
    }
  }

  @Override
  @CheckForNull
  public URI signedGetUri(BlobKey key, long expirationSeconds) {
    return null;
  }

  @Override
  public BlobContainerName container() {
    return container;
  }

  @Override
  public boolean isFilesystem() {
    return true;
  }

  private Path resolve(BlobKey key) {
    // Path-traversal is prevented at construction time by BlobKey (rejects `..` segments).
    return baseDir.resolve(key.value());
  }

  /** Converts a relative {@link Path} to a blob-key path using {@code /} regardless of platform. */
  private static String toBlobPath(Path relative) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < relative.getNameCount(); i++) {
      if (i > 0) sb.append('/');
      sb.append(relative.getName(i));
    }
    return sb.toString();
  }
}
