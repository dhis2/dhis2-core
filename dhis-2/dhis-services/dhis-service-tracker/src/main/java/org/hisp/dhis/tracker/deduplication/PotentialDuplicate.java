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
package org.hisp.dhis.tracker.deduplication;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.UID;

public class PotentialDuplicate extends BaseIdentifiableObject {
  /**
   * original represents the UID of a TrackedEntity. original is required. original is a potential
   * duplicate of duplicate.
   */
  private UID original;

  /**
   * duplicate represents the UID of a TrackedEntity. duplicate is required. duplicate is a
   * potential duplicate of original.
   */
  private UID duplicate;

  protected String lastUpdatedByUserName;

  protected String createdByUserName;

  /**
   * status represents the state of the PotentialDuplicate. all new Potential duplicates are OPEN by
   * default.
   */
  private DeduplicationStatus status = DeduplicationStatus.OPEN;

  public PotentialDuplicate() {}

  public PotentialDuplicate(UID original, UID duplicate) {
    this.original = original;
    this.duplicate = duplicate;
  }

  @JsonProperty
  @JacksonXmlProperty
  public UID getOriginal() {
    return original;
  }

  public void setOriginal(UID original) {
    this.original = original;
  }

  @JsonProperty
  @JacksonXmlProperty
  public UID getDuplicate() {
    return duplicate;
  }

  public void setDuplicate(UID duplicate) {
    this.duplicate = duplicate;
  }

  @JsonProperty
  @JacksonXmlProperty
  public DeduplicationStatus getStatus() {
    return status;
  }

  public void setStatus(DeduplicationStatus status) {
    this.status = status;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLastUpdatedByUserName() {
    return lastUpdatedByUserName;
  }

  public void setLastUpdatedByUserName(String lastUpdatedByUserName) {
    this.lastUpdatedByUserName = lastUpdatedByUserName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getCreatedByUserName() {
    return createdByUserName;
  }

  public void setCreatedByUserName(String createdByUserName) {
    this.createdByUserName = createdByUserName;
  }
}
