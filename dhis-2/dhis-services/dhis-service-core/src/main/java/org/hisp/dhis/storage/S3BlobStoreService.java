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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * AWS SDK v2 implementation of {@link BlobStoreService} for the {@code s3} and {@code aws-s3} file
 * store providers.
 *
 * <p>Targets both real AWS S3 and S3-compatible endpoints (e.g. MinIO, Ceph). When {@link
 * ConfigurationKey#FILESTORE_ENDPOINT} is set, the client is pointed at that endpoint with
 * path-style addressing enabled (the only mode supported by most S3-compatible servers).
 */
@Slf4j
public class S3BlobStoreService implements BlobStoreService {

  /** Hard cap on objects per S3 {@code DeleteObjects} request; enforced by the service. */
  private static final int DELETE_BATCH_SIZE = 1000;

  private final BlobContainerName container;
  private final S3Client s3;
  private final S3Presigner presigner;

  public S3BlobStoreService(DhisConfigurationProvider configurationProvider) {
    String containerName =
        configurationProvider.getProperty(ConfigurationKey.FILESTORE_CONTAINER);
    String location = configurationProvider.getProperty(ConfigurationKey.FILESTORE_LOCATION);
    String endpoint = configurationProvider.getProperty(ConfigurationKey.FILESTORE_ENDPOINT);
    String identity = configurationProvider.getProperty(ConfigurationKey.FILESTORE_IDENTITY);
    String secret = configurationProvider.getProperty(ConfigurationKey.FILESTORE_SECRET);

    this.container = new BlobContainerName(containerName);

    // SDK v2 requires a region even when an endpoint override is set; MinIO ≥ 2025-04 rejects a
    // region that disagrees with the bucket's LocationConstraint, so fall back to us-east-1 (the
    // S3 default that translates to no LocationConstraint on bucket creation) when unset.
    Region region = StringUtils.isNotBlank(location) ? Region.of(location) : Region.US_EAST_1;

    // Fall back to the SDK's default credential chain (env vars, system props, IAM role, EC2
    // metadata, ...) when identity/secret are not explicitly configured — typical for AWS-hosted
    // deployments that rely on IAM. Static credentials are used otherwise.
    AwsCredentialsProvider credentials =
        StringUtils.isNotBlank(identity) && StringUtils.isNotBlank(secret)
            ? StaticCredentialsProvider.create(AwsBasicCredentials.create(identity, secret))
            : DefaultCredentialsProvider.builder().build();

    // Path-style addressing is required for custom endpoints (MinIO/Ceph) and applied uniformly
    // here for aws-s3 as well, matching the addressing mode used historically.
    S3Configuration s3Config = S3Configuration.builder().pathStyleAccessEnabled(true).build();

    // SDK ≥ 2.30 dropped legacy Content-MD5 in favour of x-amz-checksum-* headers for
    // httpChecksumRequired operations. Older MinIO/Ceph builds reject DeleteObjects without
    // Content-MD5, so we re-add it via an interceptor below.
    S3ClientBuilder clientBuilder =
        S3Client.builder()
            .region(region)
            .credentialsProvider(credentials)
            .serviceConfiguration(s3Config)
            .overrideConfiguration(
                o -> o.addExecutionInterceptor(new DeleteObjectsContentMd5Interceptor()));
    Builder presignerBuilder =
        S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentials)
            .serviceConfiguration(s3Config);

    if (StringUtils.isNotBlank(endpoint)) {
      URI endpointUri = URI.create(endpoint);
      clientBuilder.endpointOverride(endpointUri);
      presignerBuilder.endpointOverride(endpointUri);
    }

    this.s3 = clientBuilder.build();
    this.presigner = presignerBuilder.build();
  }

  @PostConstruct
  public void init() {
    String bucket = container.value();
    try {
      s3.headBucket(b -> b.bucket(bucket));
    } catch (NoSuchBucketException e) {
      try {
        s3.createBucket(b -> b.bucket(bucket));
      } catch (BucketAlreadyOwnedByYouException ignored) {
        // Concurrent startup or external creation — bucket exists, nothing to do.
      }
    }
    log.info("S3 file store configured with bucket: '{}'.", bucket);
  }

  @PreDestroy
  public void cleanUp() {
    s3.close();
    presigner.close();
  }

  @Override
  public boolean blobExists(BlobKey key) {
    if (key == null) return false;
    try {
      s3.headObject(b -> b.bucket(container.value()).key(key.value()));
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  @Override
  @CheckForNull
  public InputStream openStream(BlobKey key) {
    try {
      return s3.getObject(b -> b.bucket(container.value()).key(key.value()));
    } catch (NoSuchKeyException e) {
      return null;
    }
  }

  @Override
  public long contentLength(BlobKey key) {
    try {
      HeadObjectResponse head = s3.headObject(b -> b.bucket(container.value()).key(key.value()));
      return head.contentLength();
    } catch (NoSuchKeyException e) {
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
    PutObjectRequest.Builder put =
        PutObjectRequest.builder()
            .bucket(container.value())
            .key(key.value())
            .contentLength(contentLength);
    if (StringUtils.isNotEmpty(contentType)) {
      put.contentType(contentType);
    }
    if (contentDisposition != null) {
      put.contentDisposition(contentDisposition.value());
    }
    if (contentHash != null) {
      // S3 expects the Content-MD5 header as base64-encoded bytes; ContentHash stores hex.
      put.contentMD5(hexToBase64(contentHash.hex()));
    }
    s3.putObject(put.build(), RequestBody.fromInputStream(content, contentLength));
  }

  @Override
  public void deleteBlob(BlobKey key) {
    s3.deleteObject(b -> b.bucket(container.value()).key(key.value()));
  }

  @Override
  public void deleteDirectory(BlobKeyPrefix prefix) {
    String bucket = container.value();
    String listPrefix = prefix.value() + "/";
    String continuationToken = null;
    do {
      ListObjectsV2Request.Builder list =
          ListObjectsV2Request.builder().bucket(bucket).prefix(listPrefix);
      if (continuationToken != null) {
        list.continuationToken(continuationToken);
      }
      ListObjectsV2Response resp = s3.listObjectsV2(list.build());

      List<ObjectIdentifier> ids = new ArrayList<>(resp.contents().size());
      for (S3Object obj : resp.contents()) {
        ids.add(ObjectIdentifier.builder().key(obj.key()).build());
        if (ids.size() == DELETE_BATCH_SIZE) {
          bulkDelete(bucket, ids);
          ids.clear();
        }
      }
      if (!ids.isEmpty()) {
        bulkDelete(bucket, ids);
      }

      continuationToken = Boolean.TRUE.equals(resp.isTruncated()) ? resp.nextContinuationToken() : null;
    } while (continuationToken != null);
  }

  @Override
  public Iterable<BlobKeyPrefix> listFolders(BlobKeyPrefix prefix) {
    String listPrefix = prefix.value() + "/";
    ListObjectsV2Response resp =
        s3.listObjectsV2(
            b -> b.bucket(container.value()).prefix(listPrefix).delimiter("/"));
    return resp.commonPrefixes().stream()
        .map(CommonPrefix::prefix)
        .map(BlobKeyPrefix::of)
        .toList();
  }

  @Override
  public Iterable<BlobKey> listKeys(BlobKeyPrefix prefix) {
    List<BlobKey> result = new ArrayList<>();
    String bucket = container.value();
    String continuationToken = null;
    do {
      ListObjectsV2Request.Builder list =
          ListObjectsV2Request.builder().bucket(bucket).prefix(prefix.value());
      if (continuationToken != null) {
        list.continuationToken(continuationToken);
      }
      ListObjectsV2Response resp = s3.listObjectsV2(list.build());
      for (S3Object obj : resp.contents()) {
        // Skip synthetic directory markers — some S3-compatible backends emit them.
        if (obj.key().endsWith("/")) continue;
        result.add(new BlobKey(obj.key()));
      }
      continuationToken = Boolean.TRUE.equals(resp.isTruncated()) ? resp.nextContinuationToken() : null;
    } while (continuationToken != null);
    return result;
  }

  @Override
  @CheckForNull
  public URI signedGetUri(BlobKey key, long expirationSeconds) {
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(expirationSeconds))
            .getObjectRequest(r -> r.bucket(container.value()).key(key.value()).build())
            .build();
    try {
      return presigner.presignGetObject(presignRequest).url().toURI();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(
          "Presigner produced a malformed URL for key: " + key.value(), e);
    }
  }

  @Override
  public BlobContainerName container() {
    return container;
  }

  @Override
  public boolean isFilesystem() {
    return false;
  }

  private void bulkDelete(String bucket, List<ObjectIdentifier> ids) {
    DeleteObjectsResponse resp =
        s3.deleteObjects(
            DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(d -> d.objects(ids).build())
                .build());
    log.debug(
        "bulkDelete: requested {} keys, deleted {}, errors {}",
        ids.size(),
        resp.deleted().size(),
        resp.errors().size());
    resp.errors()
        .forEach(
            err ->
                log.warn(
                    "bulkDelete error: key='{}' code='{}' message='{}'",
                    err.key(),
                    err.code(),
                    err.message()));
  }

  private static String hexToBase64(String hex) {
    return Base64.getEncoder().encodeToString(HexFormat.of().parseHex(hex));
  }

  /**
   * Adds a Content-MD5 header to S3 {@code DeleteObjects} requests for compatibility with
   * S3-compatible servers (e.g. older MinIO) that reject the new {@code x-amz-checksum-*} mechanism
   * introduced by AWS SDK v2 ≥ 2.30. Real AWS S3 accepts either header, so this is a no-op for
   * vanilla AWS.
   */
  private static final class DeleteObjectsContentMd5Interceptor implements ExecutionInterceptor {
    private static final String OPERATION = "DeleteObjects";
    private static final String HEADER = "Content-MD5";

    @Override
    public SdkHttpRequest modifyHttpRequest(
        Context.ModifyHttpRequest context, ExecutionAttributes attrs) {
      SdkHttpRequest request = context.httpRequest();
      if (!OPERATION.equals(attrs.getAttribute(SdkExecutionAttribute.OPERATION_NAME))) {
        return request;
      }
      if (request.firstMatchingHeader(HEADER).isPresent()) {
        return request;
      }
      return context
          .requestBody()
          .map(body -> request.toBuilder().putHeader(HEADER, md5Base64(body)).build())
          .orElse(request);
    }

    private static String md5Base64(RequestBody body) {
      try (InputStream in = body.contentStreamProvider().newStream()) {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
        return Base64.getEncoder().encodeToString(md.digest());
      } catch (IOException | NoSuchAlgorithmException e) {
        throw new IllegalStateException("Failed to compute Content-MD5 for DeleteObjects body", e);
      }
    }
  }
}
