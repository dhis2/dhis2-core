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
package org.hisp.dhis.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.fileresource.FileResource;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "document", namespace = DxfNamespaces.DXF_2_0)
public class Document extends BaseIdentifiableObject implements MetadataObject {
  /**
   * Refers to a URL if the {@code external} property is true. Refers to the UID of the associated
   * {@link FileResource} if the {@code external} property is false.
   */
  private String url;

  /**
   * A reference to the {@link FileResource} associated with the document. Will be null if the
   * document represents a URL.
   */
  private FileResource fileResource;

  /** Indicates whether the document refers to a file (false) or external URL (true). */
  private boolean external;

  /**
   * The content type of the file referred to by the document, or null if the document refers to a
   * URL.
   */
  private String contentType;

  /**
   * Indicates whether the file should be displayed inline in the browser or be downloaded by the
   * browser as an attachment.
   */
  private Boolean attachment = false;

  public Document() {}

  public Document(String name, String url, boolean external, String contentType) {
    this.name = name;
    this.url = url;
    this.external = external;
    this.contentType = contentType;
  }

  @JsonIgnore
  public boolean isAttachmentDefaultFalse() {
    return BooleanUtils.isTrue(attachment);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isExternal() {
    return external;
  }

  public void setExternal(boolean external) {
    this.external = external;
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
  public Boolean getAttachment() {
    return attachment;
  }

  public void setAttachment(Boolean attachment) {
    this.attachment = attachment;
  }

  // Do not expose in API
  @JsonIgnore
  public FileResource getFileResource() {
    return fileResource;
  }

  public void setFileResource(FileResource fileResource) {
    this.fileResource = fileResource;
  }
}
