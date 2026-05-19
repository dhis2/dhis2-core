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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Unit tests for {@link S3BlobStoreService} behaviours that the MinIO-backed contract test can't
 * easily exercise — pagination across multiple {@code ListObjectsV2} pages, and {@code
 * DeleteObjects} responses that report per-key errors. Uses Mockito-mocked {@link S3Client} via the
 * package-private constructor.
 */
class S3BlobStoreServiceTest {

  private final BlobContainerName container = new BlobContainerName("dhis2");

  @Test
  void listKeys_paginatesAcrossMultiplePages() {
    S3Client s3 = mock(S3Client.class);
    when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(page(List.of("apps/a", "apps/b"), true, "tok-1"))
        .thenReturn(page(List.of("apps/c"), false, null));

    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));

    List<String> keys = new ArrayList<>();
    svc.listKeys(BlobKeyPrefix.of("apps")).forEach(k -> keys.add(k.value()));

    assertEquals(List.of("apps/a", "apps/b", "apps/c"), keys);

    ArgumentCaptor<ListObjectsV2Request> captor =
        ArgumentCaptor.forClass(ListObjectsV2Request.class);
    verify(s3, times(2)).listObjectsV2(captor.capture());
    List<ListObjectsV2Request> calls = captor.getAllValues();
    assertNull(calls.get(0).continuationToken(), "first call carries no continuation token");
    assertEquals(
        "tok-1",
        calls.get(1).continuationToken(),
        "second call must pass the token returned by the first response");
  }

  @Test
  void listFolders_paginatesAcrossMultiplePages() {
    S3Client s3 = mock(S3Client.class);
    when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(commonPrefixPage(List.of("apps/foo/", "apps/bar/"), true, "tok-1"))
        .thenReturn(commonPrefixPage(List.of("apps/baz/"), false, null));

    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));

    List<String> folders = new ArrayList<>();
    svc.listFolders(BlobKeyPrefix.of("apps")).forEach(p -> folders.add(p.value()));

    assertEquals(List.of("apps/foo", "apps/bar", "apps/baz"), folders);
    verify(s3, times(2)).listObjectsV2(any(ListObjectsV2Request.class));
  }

  @Test
  void deleteDirectory_paginatesAcrossMultiplePages() {
    S3Client s3 = mock(S3Client.class);
    when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(page(List.of("apps/a", "apps/b"), true, "tok-1"))
        .thenReturn(page(List.of("apps/c"), false, null));
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(DeleteObjectsResponse.builder().build());

    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));
    svc.deleteDirectory(BlobKeyPrefix.of("apps"));

    // Two list calls (paginated), two delete calls (one per page since each page had <BATCH_SIZE).
    verify(s3, times(2)).listObjectsV2(any(ListObjectsV2Request.class));
    verify(s3, times(2)).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  void deleteDirectory_partialFailure_logsAndContinues() {
    S3Client s3 = mock(S3Client.class);
    when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(page(List.of("apps/a", "apps/b"), false, null));
    when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
        .thenReturn(
            DeleteObjectsResponse.builder()
                .errors(
                    S3Error.builder().key("apps/a").code("AccessDenied").message("nope").build())
                .build());

    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));

    // Per the bulkDelete policy this must not throw — the WARN summary is the contract.
    assertDoesNotThrow(() -> svc.deleteDirectory(BlobKeyPrefix.of("apps")));
    verify(s3, times(1)).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  void listKeys_filtersDirectoryMarkers() {
    S3Client s3 = mock(S3Client.class);
    when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(page(List.of("apps/", "apps/a", "apps/b/", "apps/b/c"), false, null));

    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));

    List<String> keys = new ArrayList<>();
    svc.listKeys(BlobKeyPrefix.of("apps")).forEach(k -> keys.add(k.value()));

    assertEquals(List.of("apps/a", "apps/b/c"), keys);
    assertTrue(keys.stream().noneMatch(k -> k.endsWith("/")));
  }

  @Test
  void keyPrefix_prependedOnListAndStrippedFromReturnedKeys() {
    // With keyPrefix="dev", listObjectsV2 is issued with prefix "dev/apps" and any keys returned
    // from the store come back stripped to the caller-visible "apps/..." form.
    S3Client s3 = mock(S3Client.class);
    when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(page(List.of("dev/apps/a", "dev/apps/b"), false, null));

    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class), "dev");

    List<String> keys = new ArrayList<>();
    svc.listKeys(BlobKeyPrefix.of("apps")).forEach(k -> keys.add(k.value()));
    assertEquals(List.of("apps/a", "apps/b"), keys);

    ArgumentCaptor<ListObjectsV2Request> listCaptor =
        ArgumentCaptor.forClass(ListObjectsV2Request.class);
    verify(s3).listObjectsV2(listCaptor.capture());
    assertEquals("dev/apps", listCaptor.getValue().prefix());
  }

  @Test
  void createDirectory_writesMarkerUnderPrefix() {
    // The synthetic directory marker must also live under the configured prefix so it's covered
    // by the IAM scoped to that prefix.
    S3Client s3 = mock(S3Client.class);
    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class), "dev");

    svc.createDirectory(BlobKeyPrefix.of("apps/my-app"));

    ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3)
        .putObject(putCaptor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
    assertEquals("dev/apps/my-app/", putCaptor.getValue().key());
  }

  @Test
  void normalizeKeyPrefix_stripsLeadingAndTrailingSlashes() {
    assertEquals("", S3BlobStoreService.normalizeKeyPrefix(null));
    assertEquals("", S3BlobStoreService.normalizeKeyPrefix(""));
    assertEquals("", S3BlobStoreService.normalizeKeyPrefix("  "));
    assertEquals("dev", S3BlobStoreService.normalizeKeyPrefix("dev"));
    assertEquals("dev", S3BlobStoreService.normalizeKeyPrefix("/dev"));
    assertEquals("dev", S3BlobStoreService.normalizeKeyPrefix("dev/"));
    assertEquals("dev", S3BlobStoreService.normalizeKeyPrefix("/dev/"));
    assertEquals("a/b", S3BlobStoreService.normalizeKeyPrefix("/a/b/"));
  }

  private static ListObjectsV2Response page(
      List<String> keys, boolean truncated, String nextToken) {
    return ListObjectsV2Response.builder()
        .contents(keys.stream().map(k -> S3Object.builder().key(k).build()).toList())
        .isTruncated(truncated)
        .nextContinuationToken(nextToken)
        .build();
  }

  private static ListObjectsV2Response commonPrefixPage(
      List<String> prefixes, boolean truncated, String nextToken) {
    return ListObjectsV2Response.builder()
        .commonPrefixes(
            prefixes.stream()
                .map(
                    p ->
                        software.amazon.awssdk.services.s3.model.CommonPrefix.builder()
                            .prefix(p)
                            .build())
                .toList())
        .isTruncated(truncated)
        .nextContinuationToken(nextToken)
        .build();
  }
}
