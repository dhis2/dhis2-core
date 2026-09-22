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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
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

  /** Test clock so the timeout assertions do not depend on how long the test itself takes. */
  private long nanos = TimeUnit.SECONDS.toNanos(1_000);

  private void elapse(Duration elapsed) {
    nanos += elapsed.toNanos();
  }

  /** Options bounded by {@code budget} from the test clock's current reading. */
  private BlobReadOptions remainingFrom(Duration budget) {
    long deadline = nanos + budget.toNanos();
    return BlobReadOptions.remaining(() -> deadline - nanos);
  }

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
  void openStream_passesTheTimeoutOnToTheRequest() {
    S3Client s3 = mock(S3Client.class);
    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));

    svc.openStream(BlobKey.of("apps/a"), remainingFrom(Duration.ofSeconds(7)));

    assertEquals(
        Optional.of(Duration.ofSeconds(7)),
        capturedRequest(s3).overrideConfiguration().flatMap(o -> o.apiCallTimeout()),
        "the caller's remaining budget must bound the fetch");
  }

  @Test
  void openStream_boundsASecondCallByTheRemainderOfTheSameOptions() {
    // One read through the content store makes several calls, headObject then getObject, sharing
    // one BlobReadOptions. Each must get what is left rather than the whole limit again.
    S3Client s3 = mock(S3Client.class);
    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));
    BlobReadOptions options = remainingFrom(Duration.ofSeconds(10));

    svc.blobExists(BlobKey.of("apps/a"), options);
    elapse(Duration.ofSeconds(4));
    svc.openStream(BlobKey.of("apps/a"), options);

    assertEquals(
        Optional.of(Duration.ofSeconds(6)),
        capturedRequest(s3).overrideConfiguration().flatMap(o -> o.apiCallTimeout()),
        "the second call must be bounded by the remaining budget, not by the full one");
  }

  @Test
  void openStream_doesNotCallTheStoreWithNoneOfTheBudgetLeft() {
    // The SDK truncates apiCallTimeout to whole milliseconds and treats 0 as no timeout at all, so
    // there is no bound left to ask for and the call could only time out. It is not made.
    S3Client s3 = mock(S3Client.class);
    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));
    BlobKey key = BlobKey.of("apps/a");
    BlobReadOptions options = remainingFrom(Duration.ofMillis(1));

    elapse(Duration.ofNanos(999_999));

    assertThrows(BlobReadTimeoutException.class, () -> svc.openStream(key, options));
    verifyNoInteractions(s3);
  }

  @Test
  void openStream_withoutATimeoutLeavesTheRequestUnbounded() {
    S3Client s3 = mock(S3Client.class);
    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));

    svc.openStream(BlobKey.of("apps/a"));

    assertEquals(
        Optional.empty(),
        capturedRequest(s3).overrideConfiguration().flatMap(o -> o.apiCallTimeout()),
        "callers that pass no options must keep the client configuration");
  }

  @Test
  void openStream_translatesTheSdkTimeoutSoCallersNeedNoSdkTypes() {
    S3Client s3 = mock(S3Client.class);
    when(s3.getObject(any(Consumer.class)))
        .thenThrow(ApiCallTimeoutException.builder().message("timed out").build());
    BlobStoreService svc = new S3BlobStoreService(container, s3, mock(S3Presigner.class));
    BlobKey key = BlobKey.of("apps/a");
    BlobReadOptions options = remainingFrom(Duration.ofSeconds(1));

    assertThrows(BlobReadTimeoutException.class, () -> svc.openStream(key, options));
  }

  /**
   * The service calls the {@code Consumer<Builder>} overload, so what Mockito records is the lambda
   * rather than a request. Applying it to a builder yields the request the SDK would have built.
   */
  @SuppressWarnings("unchecked")
  private static GetObjectRequest capturedRequest(S3Client s3) {
    ArgumentCaptor<Consumer<GetObjectRequest.Builder>> captor =
        ArgumentCaptor.forClass(Consumer.class);
    verify(s3).getObject(captor.capture());
    GetObjectRequest.Builder builder = GetObjectRequest.builder();
    captor.getValue().accept(builder);
    return builder.build();
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
