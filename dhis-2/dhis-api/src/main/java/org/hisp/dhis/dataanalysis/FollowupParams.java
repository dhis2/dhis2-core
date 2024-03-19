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
package org.hisp.dhis.dataanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FollowupParams {
  private int dataElementId;

  private int periodId;

  private int organisationUnitId;

  private int categoryOptionComboId;

  private int attributeOptionComboId;

  private boolean followup;

  public FollowupParams() {}

  public FollowupParams(
      int dataElementId,
      int periodId,
      int organisationUnitId,
      int categoryOptionComboId,
      int attributeOptionComboId,
      boolean followup) {
    this.dataElementId = dataElementId;
    this.periodId = periodId;
    this.organisationUnitId = organisationUnitId;
    this.categoryOptionComboId = categoryOptionComboId;
    this.attributeOptionComboId = attributeOptionComboId;
    this.followup = followup;
  }

  @JsonProperty
  public int getDataElementId() {
    return dataElementId;
  }

  public void setDataElementId(int dataElementId) {
    this.dataElementId = dataElementId;
  }

  @JsonProperty
  public int getPeriodId() {
    return periodId;
  }

  public void setPeriodId(int periodId) {
    this.periodId = periodId;
  }

  @JsonProperty
  public int getOrganisationUnitId() {
    return organisationUnitId;
  }

  public void setOrganisationUnitId(int organisationUnitId) {
    this.organisationUnitId = organisationUnitId;
  }

  @JsonProperty
  public int getCategoryOptionComboId() {
    return categoryOptionComboId;
  }

  public void setCategoryOptionComboId(int categoryOptionComboId) {
    this.categoryOptionComboId = categoryOptionComboId;
  }

  @JsonProperty
  public int getAttributeOptionComboId() {
    return attributeOptionComboId;
  }

  public void setAttributeOptionComboId(int attributeOptionComboId) {
    this.attributeOptionComboId = attributeOptionComboId;
  }

  @JsonProperty
  public boolean isFollowup() {
    return followup;
  }

  public void setFollowup(boolean followup) {
    this.followup = followup;
  }

  @Override
  public String toString() {
    return "FollowupParams{"
        + "dataElementId="
        + dataElementId
        + ", periodId="
        + periodId
        + ", organisationUnitId="
        + organisationUnitId
        + ", categoryOptionComboId="
        + categoryOptionComboId
        + ", attributeOptionComboId="
        + attributeOptionComboId
        + ", followup="
        + followup
        + '}';
  }
}
