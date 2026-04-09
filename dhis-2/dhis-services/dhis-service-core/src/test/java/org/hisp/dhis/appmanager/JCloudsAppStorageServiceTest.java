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
package org.hisp.dhis.appmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.hisp.dhis.appmanager.ResourceResult.Redirect;
import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.appmanager.ResourceResult.ResourceNotFound;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.storage.BlobContainerName;
import org.hisp.dhis.storage.BlobKey;
import org.hisp.dhis.storage.BlobKeyPrefix;
import org.hisp.dhis.storage.BlobStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JCloudsAppStorageServiceTest {

  private static final String FOLDER = "apps/test-app_abc123";
  private static final BlobContainerName CONTAINER = new BlobContainerName("dhis2-store");

  @Mock private BlobStoreService blobStore;
  @Mock private LocationManager locationManager;
  @Mock private FileResourceContentStore fileResourceContentStore;

  @TempDir File tempDir;

  private JCloudsAppStorageService service;
  private App app;

  @BeforeEach
  void setUp() {
    service =
        new JCloudsAppStorageService(
            blobStore, locationManager, new ObjectMapper(), fileResourceContentStore);

    app = new App();
    app.setAppStorageSource(AppStorageSource.JCLOUDS);
    app.setFolderName(FOLDER);
  }

  @Test
  void getAppResource_nullApp_returnsNotFound() throws IOException {
    ResourceResult result = service.getAppResource(null, "index.html");
    assertInstanceOf(ResourceNotFound.class, result);
    assertEquals("index.html", ((ResourceNotFound) result).path());
  }

  @Test
  void getAppResource_emptyString_redirectsToSlash() throws IOException {
    ResourceResult result = service.getAppResource(app, "");
    assertInstanceOf(Redirect.class, result);
    assertEquals("/", ((Redirect) result).path());
  }

  /** A leading-slash-only resource ("/") should serve {@code index.html}. */
  @Test
  void getAppResource_rootSlash_servesIndexHtml() throws IOException {
    BlobKey indexKey = BlobKey.of(FOLDER, "index.html");
    File indexFile = new File(tempDir, "index.html");
    when(blobStore.blobExists(indexKey)).thenReturn(true);
    when(blobStore.isFilesystem()).thenReturn(true);
    when(blobStore.container()).thenReturn(CONTAINER);
    when(locationManager.getFileForReading(CONTAINER.resolve(indexKey))).thenReturn(indexFile);

    ResourceResult result = service.getAppResource(app, "/");

    assertInstanceOf(
        ResourceFound.class,
        result,
        "GET '/' must serve index.html, not redirect or 404. " + "Actual result: " + result);
    verify(locationManager).getFileForReading(CONTAINER.resolve(indexKey));
    assertEquals(
        indexFile.getAbsolutePath(),
        ((ResourceFound) result).resource().getFile().getAbsolutePath());
  }

  @Test
  void getAppResource_explicitIndexHtml_withLeadingSlash_returnsFound() throws IOException {
    BlobKey indexKey = BlobKey.of(FOLDER, "index.html");
    File indexFile = new File(tempDir, "index.html");
    when(blobStore.blobExists(indexKey)).thenReturn(true);
    when(blobStore.isFilesystem()).thenReturn(true);
    when(blobStore.container()).thenReturn(CONTAINER);
    when(locationManager.getFileForReading(CONTAINER.resolve(indexKey))).thenReturn(indexFile);

    ResourceResult result = service.getAppResource(app, "/index.html");

    assertInstanceOf(ResourceFound.class, result);
    verify(locationManager).getFileForReading(CONTAINER.resolve(indexKey));
    assertEquals(
        indexFile.getAbsolutePath(),
        ((ResourceFound) result).resource().getFile().getAbsolutePath());
  }

  @Test
  void getAppResource_explicitIndexHtml_withoutLeadingSlash_returnsFound() throws IOException {
    BlobKey indexKey = BlobKey.of(FOLDER, "index.html");
    File indexFile = new File(tempDir, "index.html");
    when(blobStore.blobExists(indexKey)).thenReturn(true);
    when(blobStore.isFilesystem()).thenReturn(true);
    when(blobStore.container()).thenReturn(CONTAINER);
    when(locationManager.getFileForReading(CONTAINER.resolve(indexKey))).thenReturn(indexFile);

    ResourceResult result = service.getAppResource(app, "index.html");

    assertInstanceOf(ResourceFound.class, result);
    verify(locationManager).getFileForReading(CONTAINER.resolve(indexKey));
    assertEquals(
        indexFile.getAbsolutePath(),
        ((ResourceFound) result).resource().getFile().getAbsolutePath());
  }

  @Test
  void getAppResource_trailingSlashSubDir_servesIndexHtml() throws IOException {
    BlobKey subIndexKey = BlobKey.of(FOLDER, "subDir", "index.html");
    File subIndexFile = new File(tempDir, "subDir/index.html");
    when(blobStore.blobExists(subIndexKey)).thenReturn(true);
    when(blobStore.isFilesystem()).thenReturn(true);
    when(blobStore.container()).thenReturn(CONTAINER);
    when(locationManager.getFileForReading(CONTAINER.resolve(subIndexKey)))
        .thenReturn(subIndexFile);

    ResourceResult result = service.getAppResource(app, "subDir/");

    assertInstanceOf(ResourceFound.class, result);
    verify(locationManager).getFileForReading(CONTAINER.resolve(subIndexKey));
    assertEquals(
        subIndexFile.getAbsolutePath(),
        ((ResourceFound) result).resource().getFile().getAbsolutePath());
  }

  @Test
  void getAppResource_trailingSlashSubDir_withLeadingSlash_servesIndexHtml() throws IOException {
    BlobKey subIndexKey = BlobKey.of(FOLDER, "subDir", "index.html");
    File subIndexFile = new File(tempDir, "subDir/index.html");
    when(blobStore.blobExists(subIndexKey)).thenReturn(true);
    when(blobStore.isFilesystem()).thenReturn(true);
    when(blobStore.container()).thenReturn(CONTAINER);
    when(locationManager.getFileForReading(CONTAINER.resolve(subIndexKey)))
        .thenReturn(subIndexFile);

    ResourceResult result = service.getAppResource(app, "/subDir/");

    assertInstanceOf(ResourceFound.class, result);
    verify(locationManager).getFileForReading(CONTAINER.resolve(subIndexKey));
    assertEquals(
        subIndexFile.getAbsolutePath(),
        ((ResourceFound) result).resource().getFile().getAbsolutePath());
  }

  @Test
  void getAppResource_bareSubDirName_redirectsToTrailingSlash() throws IOException {
    // blob "subDir" does not exist as a file, but keys exist under that prefix
    when(blobStore.blobExists(BlobKey.of(FOLDER, "subDir"))).thenReturn(false);
    when(blobStore.listKeys(BlobKeyPrefix.of(BlobKey.of(FOLDER, "subDir").value())))
        .thenReturn(List.of(BlobKey.of(FOLDER, "subDir", "index.html")));

    ResourceResult result = service.getAppResource(app, "subDir");

    assertInstanceOf(Redirect.class, result);
    assertEquals("subDir/", ((Redirect) result).path());
  }

  @Test
  void getAppResource_missingResource_returnsNotFound() throws IOException {
    when(blobStore.blobExists(any())).thenReturn(false);
    when(blobStore.listKeys(any())).thenReturn(List.of());

    ResourceResult result = service.getAppResource(app, "missing.js");

    assertInstanceOf(ResourceNotFound.class, result);
    assertEquals("missing.js", ((ResourceNotFound) result).path());
  }
}
