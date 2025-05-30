/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.fileresource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Halvdan Hoem Grelland
 */
public class FileResource extends BaseIdentifiableObject {
  public static final String DEFAULT_FILENAME = "untitled";

  public static final String DEFAULT_CONTENT_TYPE = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

  public static final Set<String> IMAGE_CONTENT_TYPES =
      Set.of("image/jpg", "image/png", "image/jpeg");

  public static FileResource ofKey(FileResourceDomain domain, String key, String contentType) {
    return new FileResource(
        key, domain.getContainerName() + "_" + key, contentType, 0, null, domain);
  }

  /** MIME type. */
  private String contentType;

  /** Byte size of content, non negative. */
  private long contentLength;

  /** MD5 digest of content. */
  private String contentMd5;

  /** Key used for content storage at external location. */
  private String storageKey;

  /**
   * Flag indicating whether the resource is assigned (e.g. to a DataValue) or not. Unassigned
   * FileResources are generally safe to delete when reaching a certain age (unassigned objects
   * might be in staging).
   */
  private boolean assigned = false;

  /** The domain which this FileResource belongs to. */
  private FileResourceDomain domain;

  /**
   * To keep track of those files which are not pre-generated and need to be processed later. Flag
   * will be set to true for FileResource having more than one file associated with it (e.g. images)
   */
  private boolean hasMultipleStorageFiles;

  /** Current storage status of content. */
  private transient FileResourceStorageStatus storageStatus = FileResourceStorageStatus.NONE;

  private String fileResourceOwner;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public FileResource() {}

  public FileResource(
      String name,
      String contentType,
      long contentLength,
      String contentMd5,
      FileResourceDomain domain) {
    this.name = name;
    this.contentType = contentType;
    this.contentLength = contentLength;
    this.contentMd5 = contentMd5;
    this.domain = domain;
    this.storageKey = FileResourceKeyUtil.makeKey(domain, Optional.empty());
  }

  public FileResource(
      String key,
      String name,
      String contentType,
      long contentLength,
      String contentMd5,
      FileResourceDomain domain) {
    this.name = name;
    this.contentType = contentType;
    this.contentLength = contentLength;
    this.contentMd5 = contentMd5;
    this.domain = domain;
    this.storageKey = FileResourceKeyUtil.makeKey(domain, Optional.of(key));
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Indicates whether the given content type is not null and a valid image content type.
   *
   * @param contentType the content type.
   * @return true if the given content type is a valid image content type.
   */
  public static boolean isImage(String contentType) {
    return contentType != null && IMAGE_CONTENT_TYPES.contains(contentType);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public long getContentLength() {
    return contentLength;
  }

  public void setContentLength(long contentLength) {
    this.contentLength = contentLength;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getContentMd5() {
    return contentMd5;
  }

  public void setContentMd5(String contentMd5) {
    this.contentMd5 = contentMd5;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public void setStorageKey(String storageKey) {
    this.storageKey = storageKey;
  }

  public boolean isAssigned() {
    return assigned;
  }

  public void setAssigned(boolean assigned) {
    this.assigned = assigned;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public FileResourceStorageStatus getStorageStatus() {
    return storageStatus;
  }

  public void setStorageStatus(FileResourceStorageStatus storageStatus) {
    this.storageStatus = storageStatus;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public FileResourceDomain getDomain() {
    return domain;
  }

  public void setDomain(FileResourceDomain domain) {
    this.domain = domain;
  }

  public String getFormat() {
    return this.contentType.split("[/;]")[1];
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isHasMultipleStorageFiles() {
    return hasMultipleStorageFiles;
  }

  public void setHasMultipleStorageFiles(boolean hasMultipleStorageFiles) {
    this.hasMultipleStorageFiles = hasMultipleStorageFiles;
  }

  public String getFileResourceOwner() {
    return fileResourceOwner;
  }

  public void setFileResourceOwner(String fileResourceOwner) {
    this.fileResourceOwner = fileResourceOwner;
  }
}
