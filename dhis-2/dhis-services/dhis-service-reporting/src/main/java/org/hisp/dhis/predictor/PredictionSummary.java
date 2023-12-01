/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.predictor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.webmessage.AbstractWebMessageResponse;

/**
 * @author Jim Grace
 */
@JacksonXmlRootElement(localName = "predictionSummary", namespace = DxfNamespaces.DXF_2_0)
public class PredictionSummary extends AbstractWebMessageResponse {
  private PredictionStatus status = PredictionStatus.SUCCESS;

  private String description;

  private int predictors = 0;

  private int inserted = 0;

  private int updated = 0;

  private int deleted = 0;

  private int unchanged = 0;

  public PredictionSummary() {}

  public PredictionSummary(PredictionStatus status, String description) {
    this.status = status;
    this.description = description;
  }

  public void incrementInserted() {
    inserted += 1;
  }

  public void incrementPredictors() {
    predictors += 1;
  }

  public void incrementUpdated() {
    updated += 1;
  }

  public void incrementDeleted() {
    deleted += 1;
  }

  public void incrementUnchanged() {
    unchanged += 1;
  }

  public int getPredictions() {
    return inserted + updated + unchanged;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public PredictionStatus getStatus() {
    return status;
  }

  public PredictionSummary setStatus(PredictionStatus status) {
    this.status = status;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  public PredictionSummary setDescription(String description) {
    this.description = description;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getPredictors() {
    return predictors;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getInserted() {
    return inserted;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getUpdated() {
    return updated;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getDeleted() {
    return deleted;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getUnchanged() {
    return unchanged;
  }

  @Override
  public String toString() {
    return "PredictionSummary{"
        + "status="
        + status
        + ", description='"
        + description
        + '\''
        + ", predictors="
        + predictors
        + ", inserted="
        + inserted
        + ", updated="
        + updated
        + ", deleted="
        + deleted
        + ", unchanged="
        + unchanged
        + '}';
  }
}
