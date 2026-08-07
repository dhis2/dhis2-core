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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.awaitility.Awaitility;
import org.hisp.dhis.storage.BlobStoreService.ContentDisposition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Behavioural contract every {@link BlobStoreService} implementation must satisfy.
 *
 * <p>Subclasses wire a concrete {@link BlobStoreService} (e.g. {@code S3BlobStoreService}, {@code
 * FileSystemBlobStoreService}, {@code TransientBlobStoreService}) and the contract tests run
 * unchanged.
 *
 * <p>Tests that don't apply to a given backend are skipped via {@link
 * org.junit.jupiter.api.Assumptions} on capability hooks subclasses override:
 *
 * <ul>
 *   <li>{@link #supportsRequestSigning()} — backend can produce presigned GET URLs (false on the
 *       filesystem and transient backends).
 *   <li>{@link #validatesContentMd5()} — backend rejects an upload whose payload disagrees with the
 *       supplied {@link ContentHash} (true on real S3, false on local backends).
 * </ul>
 *
 * <p>Lifecycle: subclasses use {@code @BeforeAll}/{@code @AfterAll} to start and stop the backend
 * (one per class). Per-test data is namespaced under a unique {@link #testPrefix} and removed in
 * {@link #cleanUpTestData()}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BlobStoreServiceContractTest {

  /** Implementation under test. Lifecycle is owned by the subclass. */
  protected abstract BlobStoreService service();

  protected abstract boolean supportsRequestSigning();

  protected boolean validatesContentMd5() {
    return false;
  }

  private final BlobKeyPrefix testPrefix = BlobKeyPrefix.of("blobStoreContract");

  @AfterEach
  void cleanUpTestData() {
    // deleteDirectory removes both real blobs and any synthetic directory markers (which
    // listKeys deliberately excludes) so tests that exercise createDirectory leave no
    // residue for subsequent tests.
    assertDoesNotThrow(() -> service().deleteDirectory(testPrefix));
  }

  // -------------------------------------------------------------------- existence + read

  @Test
  void blobExists_unknownKey_returnsFalse() {
    assertFalse(service().blobExists(key("missing.txt")));
  }

  @Test
  void putBlob_then_blobExists_returnsTrue() {
    putString(key("hello.txt"), "hello");
    assertTrue(service().blobExists(key("hello.txt")));
  }

  @Test
  void openStream_unknownKey_returnsNull() {
    assertNull(service().openStream(key("missing.txt")));
  }

  @Test
  void openStream_existingKey_returnsContent() throws IOException {
    putString(key("hello.txt"), "hello world");
    try (InputStream in = service().openStream(key("hello.txt"))) {
      assertNotNull(in);
      assertEquals("hello world", new String(in.readAllBytes(), UTF_8));
    }
  }

  @Test
  void contentLength_unknownKey_returnsZero() {
    assertEquals(0L, service().contentLength(key("missing.txt")));
  }

  @Test
  void contentLength_existingKey_returnsByteCount() {
    byte[] payload = "hello".getBytes(UTF_8);
    putBytes(key("hello.txt"), payload);
    assertEquals(payload.length, service().contentLength(key("hello.txt")));
  }

  // -------------------------------------------------------------------- write

  @Test
  void putBlob_overwrite_replacesContent() throws IOException {
    putString(key("k"), "first");
    putString(key("k"), "second");
    try (InputStream in = service().openStream(key("k"))) {
      assertNotNull(in);
      assertEquals("second", new String(in.readAllBytes(), UTF_8));
    }
  }

  @Test
  void putBlob_emptyPayload_succeeds() throws IOException {
    putBytes(key("empty"), new byte[0]);
    assertTrue(service().blobExists(key("empty")));
    assertEquals(0L, service().contentLength(key("empty")));
    try (InputStream in = service().openStream(key("empty"))) {
      assertNotNull(in);
      assertEquals(0, in.readAllBytes().length);
    }
  }

  @Test
  void putBlob_withCorrectMd5_succeeds() {
    byte[] payload = "verify-me".getBytes(UTF_8);
    ContentHash md5 = ContentHash.of(Hashing.md5().hashBytes(payload));
    service()
        .putBlob(
            key("md5"), new ByteArrayInputStream(payload), payload.length, "text/plain", null, md5);
    assertTrue(service().blobExists(key("md5")));
  }

  @Test
  void putBlob_withMismatchedMd5_isRejected() {
    assumeTrue(validatesContentMd5(), "backend does not validate Content-MD5");
    byte[] payload = "verify-me".getBytes(UTF_8);
    BlobKey badKey = key("bad-md5");
    ByteArrayInputStream stream = new ByteArrayInputStream(payload);
    ContentHash wrong = new ContentHash("00000000000000000000000000000000");
    BlobStoreService svc = service();

    assertThrows(
        RuntimeException.class,
        () -> svc.putBlob(badKey, stream, payload.length, "text/plain", null, wrong));
  }

  @Test
  void putBlob_withContentDisposition_doesNotThrow() {
    // Persistence of the disposition header is asserted via the signed-URL test below
    // (BlobStoreService doesn't expose a HEAD/metadata read directly). On signing-incapable
    // backends we can only assert the put succeeds and the blob is readable.
    putWithDisposition(key("with-disposition"), "report.pdf", "data".getBytes(UTF_8));
    assertTrue(service().blobExists(key("with-disposition")));
  }

  // -------------------------------------------------------------------- delete

  @Test
  void deleteBlob_existing_removesIt() {
    putString(key("doomed"), "x");
    service().deleteBlob(key("doomed"));
    assertFalse(service().blobExists(key("doomed")));
  }

  @Test
  void deleteBlob_unknown_isNoOp() {
    assertDoesNotThrow(() -> service().deleteBlob(key("never-existed")));
  }

  @Test
  void deleteDirectory_removesEverythingUnderPrefix_butLeavesSiblings() {
    putString(key("trash/a"), "a");
    putString(key("trash/sub/b"), "b");
    putString(key("trash/sub/c"), "c");
    putString(key("keep/x"), "x");

    service().deleteDirectory(BlobKeyPrefix.of(key("trash").value()));

    assertFalse(service().blobExists(key("trash/a")));
    assertFalse(service().blobExists(key("trash/sub/b")));
    assertFalse(service().blobExists(key("trash/sub/c")));
    assertTrue(service().blobExists(key("keep/x")));
  }

  // -------------------------------------------------------------------- list

  @Test
  void listFolders_returnsOnlyImmediateChildPrefixes() {
    putString(key("alpha/file"), "1");
    putString(key("alpha/sub/file"), "2");
    putString(key("beta/file"), "3");
    putString(key("file-at-root"), "4");

    Set<String> folderNames = new HashSet<>();
    service().listFolders(testPrefix).forEach(p -> folderNames.add(p.value()));

    assertTrue(
        folderNames.contains(testPrefix.value() + "/alpha"),
        "expected alpha as immediate child folder, got: " + folderNames);
    assertTrue(
        folderNames.contains(testPrefix.value() + "/beta"),
        "expected beta as immediate child folder, got: " + folderNames);
    assertFalse(
        folderNames.contains(testPrefix.value() + "/alpha/sub"),
        "listFolders must not recurse, but found alpha/sub: " + folderNames);
  }

  @Test
  void listFolders_unknownPrefix_returnsEmpty() {
    Iterable<BlobKeyPrefix> result = service().listFolders(BlobKeyPrefix.of("does-not-exist"));
    assertFalse(result.iterator().hasNext());
  }

  @Test
  void listKeys_returnsAllKeysRecursively() {
    putString(key("a/1"), "1");
    putString(key("a/b/2"), "2");
    putString(key("a/b/c/3"), "3");

    Set<String> keys = new HashSet<>();
    service().listKeys(BlobKeyPrefix.of(key("a").value())).forEach(k -> keys.add(k.value()));

    assertEquals(Set.of(key("a/1").value(), key("a/b/2").value(), key("a/b/c/3").value()), keys);
  }

  @Test
  void listKeys_filtersByPrefix() {
    putString(key("p1/x"), "1");
    putString(key("p2/y"), "2");

    Set<String> keys = new HashSet<>();
    service().listKeys(BlobKeyPrefix.of(key("p1").value())).forEach(k -> keys.add(k.value()));

    assertEquals(Set.of(key("p1/x").value()), keys);
  }

  // -------------------------------------------------------------------- directories

  @Test
  void directoryExists_unknown_returnsFalse() {
    assertFalse(service().directoryExists(BlobKeyPrefix.of(key("never-existed").value())));
  }

  @Test
  void createDirectory_then_directoryExists_returnsTrue() {
    BlobKeyPrefix dir = BlobKeyPrefix.of(key("empty-dir").value());
    service().createDirectory(dir);
    assertTrue(service().directoryExists(dir));
  }

  @Test
  void directoryExists_returnsTrue_whenBlobStoredUnderPrefix() {
    putString(key("populated/inside"), "x");
    assertTrue(service().directoryExists(BlobKeyPrefix.of(key("populated").value())));
  }

  @Test
  void createDirectory_isIdempotent() {
    BlobKeyPrefix dir = BlobKeyPrefix.of(key("repeat").value());
    service().createDirectory(dir);
    assertDoesNotThrow(() -> service().createDirectory(dir));
    assertTrue(service().directoryExists(dir));
  }

  @Test
  void listKeys_excludesDirectoryMarkers() {
    BlobKeyPrefix dir = BlobKeyPrefix.of(key("filtered").value());
    service().createDirectory(dir);
    putString(key("filtered/file"), "x");

    Set<String> keys = new HashSet<>();
    service().listKeys(dir).forEach(k -> keys.add(k.value()));

    assertEquals(Set.of(key("filtered/file").value()), keys);
  }

  /**
   * Regression test for the bundled-app cleanup bug where {@code listKeys} returned only the
   * provider's first page (1000 keys on S3) and {@code JCloudsAppStorageService#deleteApp} silently
   * stopped after deleting that page, leaving the rest of the app's files behind as orphans.
   */
  @Test
  void listKeys_paginatesPastDefaultPageSize() {
    int total = 1100; // above the 1000-key default page size of S3-compatible stores
    for (int i = 0; i < total; i++) {
      putString(key("page/" + String.format("%05d", i)), "x");
    }

    Set<String> keys = new HashSet<>();
    service().listKeys(BlobKeyPrefix.of(key("page").value())).forEach(k -> keys.add(k.value()));

    assertEquals(
        total,
        keys.size(),
        "listKeys must return every blob across all pages, not just the first page");
  }

  // -------------------------------------------------------------------- presigning

  @Test
  void signedGetUri_signingIncapableBackend_returnsNull() {
    assumeFalse(supportsRequestSigning(), "backend supports request signing");
    putString(key("any"), "any");
    assertNull(service().signedGetUri(key("any"), 60));
  }

  @Test
  void signedGetUri_returnsUriThatServesContentWithDisposition() throws Exception {
    assumeTrue(supportsRequestSigning(), "backend does not support request signing");
    putWithDisposition(key("signed"), "sample.txt", "signed-content".getBytes(UTF_8));

    URI uri = service().signedGetUri(key("signed"), 60);
    assertNotNull(uri, "signed URI must be non-null on signing-capable backends");

    HttpResponse<String> response = httpGet(uri);
    assertEquals(200, response.statusCode(), "signed URL should serve content; uri=" + uri);
    assertEquals("signed-content", response.body());
    assertTrue(
        response.headers().firstValue("Content-Disposition").orElse("").contains("sample.txt"),
        "Content-Disposition must round-trip through the backend; headers="
            + response.headers().map());
  }

  @Test
  void signedGetUri_expiredUri_isRejected() {
    assumeTrue(supportsRequestSigning(), "backend does not support request signing");
    putString(key("expires-fast"), "x");
    URI uri = service().signedGetUri(key("expires-fast"), 1);
    assertNotNull(uri);
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(
            () ->
                assertNotEquals(
                    200, httpGet(uri).statusCode(), "expired presigned URL should not return 200"));
  }

  // -------------------------------------------------------------------- meta

  @Test
  void container_isStableAndNonEmpty() {
    BlobContainerName first = service().container();
    assertNotNull(first);
    assertNotNull(first.value());
    assertFalse(first.value().isBlank());
    assertEquals(first.value(), service().container().value());
  }

  @Test
  void isFilesystem_isStableBetweenCalls() {
    assertEquals(service().isFilesystem(), service().isFilesystem());
  }

  // -------------------------------------------------------------------- helpers

  private BlobKey key(String tail) {
    return BlobKey.of(testPrefix.value(), tail);
  }

  private void putString(BlobKey key, String value) {
    putBytes(key, value.getBytes(UTF_8));
  }

  private void putBytes(BlobKey key, byte[] payload) {
    service()
        .putBlob(
            key,
            new ByteArrayInputStream(payload),
            payload.length,
            "application/octet-stream",
            null,
            null);
  }

  private void putWithDisposition(BlobKey key, String filename, byte[] payload) {
    service()
        .putBlob(
            key,
            new ByteArrayInputStream(payload),
            payload.length,
            "application/octet-stream",
            ContentDisposition.filename(filename),
            null);
  }

  private static HttpResponse<String> httpGet(URI uri) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    return client.send(
        HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
        HttpResponse.BodyHandlers.ofString(UTF_8));
  }
}
