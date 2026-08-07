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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.hisp.dhis.test.junit.MinIOTestExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * {@link software.amazon.awssdk.http.auth.aws.internal.signer.AwsChunkedV4PayloadSigner#sign} (line
 * 91 in SDK 2.44.4) calls {@code payload.newStream()} exactly once per invocation. The whole
 * signing pipeline runs inside the SDK's {@code RetryableStage} — when a request is retried, the
 * pipeline re-runs and {@code sign()} is called again. The second call asks the wrapper from {@code
 * RequestBody.fromInputStream} for another stream; because the underlying {@link InputStream}
 * doesn't support {@code mark/reset} (e.g. {@code ZipFile.getInputStream(zipEntry)} used by app
 * install), the wrapper throws IllegalStateException("...mark/reset...").
 *
 * <p>Plain MinIO doesn't expose the bug because no retry happens. This test injects an {@link
 * ExecutionInterceptor} that throws {@link RetryableException} on the first {@code PutObject}
 * attempt, forcing the SDK to retry exactly like real AWS S3 does when the first attempt hits a
 * transient 5xx / socket error.
 */
@Tag("integration")
@ExtendWith(MinIOTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3BlobStoreNonResetableStreamTest {

  private static final int PAYLOAD_BYTES = 512 * 1024;

  private S3BlobStoreService store;
  private FailFirstPutObjectInterceptor failFirstPutObject;

  @BeforeAll
  void start() {
    failFirstPutObject = new FailFirstPutObjectInterceptor();

    StaticCredentialsProvider credentials =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                MinIOTestExtension.MINIO_USER, MinIOTestExtension.MINIO_PASSWORD));
    S3Configuration s3Config = S3Configuration.builder().pathStyleAccessEnabled(true).build();
    URI endpoint = URI.create(MinIOTestExtension.s3Url());

    S3Client s3 =
        S3Client.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(endpoint)
            .credentialsProvider(credentials)
            .serviceConfiguration(s3Config)
            .overrideConfiguration(o -> o.addExecutionInterceptor(failFirstPutObject))
            .build();
    S3Presigner presigner =
        S3Presigner.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(endpoint)
            .credentialsProvider(credentials)
            .serviceConfiguration(s3Config)
            .build();

    store = new S3BlobStoreService(new BlobContainerName("repro"), s3, presigner);
    store.init();
    // Arm only after init() so the bucket-create call isn't the one we force-retry.
    failFirstPutObject.armed.set(true);
  }

  @AfterAll
  void stop() {
    if (store != null) store.cleanUp();
  }

  @Test
  void putBlob_nonResetableStream_survivesRetry() throws IOException {
    byte[] payload = new byte[PAYLOAD_BYTES];
    ThreadLocalRandom.current().nextBytes(payload);
    InputStream nonResetable = nonResetableWrapper(payload);

    BlobKey key = BlobKey.of("forced-retry-test.bin");
    try {
      store.putBlob(key, nonResetable, payload.length, "application/octet-stream", null, null);

      assertTrue(
          failFirstPutObject.attempts.get() >= 2,
          "the SDK must have retried at least once — otherwise this test isn't actually"
              + " exercising the retry path it's meant to cover. attempts="
              + failFirstPutObject.attempts.get());
      assertTrue(store.blobExists(key), "blob must exist after a successful retry");
      assertEquals(payload.length, store.contentLength(key));
      try (InputStream in = store.openStream(key)) {
        assertNotNull(in);
        assertArrayEquals(payload, in.readAllBytes(), "uploaded bytes must round-trip intact");
      }
    } finally {
      // clean up
      if (store.blobExists(key)) store.deleteBlob(key);
    }
  }

  private static InputStream nonResetableWrapper(byte[] payload) {
    return new FilterInputStream(new ByteArrayInputStream(payload)) {
      @Override
      public boolean markSupported() {
        return false;
      }

      @Override
      public synchronized void mark(int readlimit) {
        // no-op
      }

      @Override
      public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
      }
    };
  }

  /**
   * Throws a {@link RetryableException} from {@code afterTransmission} on the first {@code
   * PutObject} call, then disarms. Mimics the transient-error path that production hits against
   * real AWS S3 (5xx / socket reset / etc.) and forces the SDK's {@code RetryableStage} to
   * re-execute the signing pipeline.
   */
  private static final class FailFirstPutObjectInterceptor implements ExecutionInterceptor {
    final AtomicInteger attempts = new AtomicInteger();
    final AtomicBoolean armed = new AtomicBoolean(false);

    @Override
    public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes attrs) {
      if (!armed.get()) return;
      if (!"PutObject".equals(attrs.getAttribute(SdkExecutionAttribute.OPERATION_NAME))) return;
      if (attempts.incrementAndGet() == 1) {
        throw RetryableException.create("forced retry for non-resetable stream reproduction");
      }
    }
  }
}
