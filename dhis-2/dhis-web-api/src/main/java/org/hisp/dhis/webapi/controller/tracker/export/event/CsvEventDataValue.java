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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.util.Assert;

/**
 * @author Enrico Colasante
 */
@JsonPropertyOrder({
  "event",
  "status",
  "program",
  "programStage",
  "enrollment",
  "orgUnit",
  "orgUnitName",
  "occurredAt",
  "scheduledAt",
  "geometry",
  "latitude",
  "longitude",
  "followup",
  "deleted",
  "createdAt",
  "createdAtClient",
  "updatedAt",
  "updatedAtClient",
  "completedBy",
  "completedAt",
  "updatedBy",
  "attributeOptionCombo",
  "attributeCategoryOptions",
  "assignedUser",
  "dataElement",
  "value",
  "storedBy",
  "providedElsewhere",
  "storedByDataValue",
  "createAtDataValue",
  "updatedAtDataValue"
})
class CsvEventDataValue {
  private String event;

  private String status;

  private String program;

  private String programStage;

  private String orgUnit;

  private String orgUnitName;

  private String enrollment;

  private String occurredAt;

  private String scheduledAt;

  private boolean followup;

  private boolean deleted;

  private String createdAt;

  private String createdAtClient;

  private String updatedAt;

  private String updatedAtClient;

  private String completedBy;

  private String completedAt;

  private String updatedBy;

  private String geometry;

  private Double latitude;

  private Double longitude;

  private String attributeOptionCombo;

  private String attributeCategoryOptions;

  private String assignedUser;

  private String dataElement;

  private String value;

  private String storedBy;

  private Boolean providedElsewhere;

  private String createdAtDataValue;

  private String updatedAtDataValue;

  private String storedByDataValue;

  public CsvEventDataValue() {}

  public CsvEventDataValue(CsvEventDataValue dataValue) {
    Assert.notNull(dataValue, "A non-null CsvOutputEventDataValue must be given as a parameter.");

    event = dataValue.getEvent();
    status = dataValue.getStatus();
    program = dataValue.getProgram();
    programStage = dataValue.getProgramStage();
    enrollment = dataValue.getEnrollment();
    orgUnit = dataValue.getOrgUnit();
    orgUnitName = dataValue.getOrgUnitName();
    occurredAt = dataValue.getOccurredAt();
    scheduledAt = dataValue.getScheduledAt();
    followup = dataValue.isFollowup();
    deleted = dataValue.isDeleted();
    createdAt = dataValue.getCreatedAt();
    updatedAt = dataValue.getUpdatedAt();
    completedBy = dataValue.getCompletedBy();
    completedAt = dataValue.getCompletedAt();
    updatedBy = dataValue.getUpdatedBy();
    geometry = dataValue.getGeometry();
    latitude = dataValue.getLatitude();
    longitude = dataValue.getLongitude();
    dataElement = dataValue.getDataElement();
    value = dataValue.getValue();
    storedBy = dataValue.getStoredBy();
    providedElsewhere = dataValue.getProvidedElsewhere();
    createdAtDataValue = dataValue.getCreatedAtDataValue();
    updatedAtDataValue = dataValue.getUpdatedAtDataValue();
    storedByDataValue = dataValue.getStoredByDataValue();
  }

  @JsonProperty
  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  @JsonProperty
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @JsonProperty
  public String getProgram() {
    return program;
  }

  public void setProgram(String program) {
    this.program = program;
  }

  @JsonProperty
  public String getProgramStage() {
    return programStage;
  }

  public void setProgramStage(String programStage) {
    this.programStage = programStage;
  }

  @JsonProperty
  public String getEnrollment() {
    return enrollment;
  }

  public void setEnrollment(String enrollment) {
    this.enrollment = enrollment;
  }

  @JsonProperty
  public String getOrgUnit() {
    return orgUnit;
  }

  public void setOrgUnit(String orgUnit) {
    this.orgUnit = orgUnit;
  }

  @JsonProperty
  public String getOrgUnitName() {
    return orgUnitName;
  }

  public void setOrgUnitName(String orgUnitName) {
    this.orgUnitName = orgUnitName;
  }

  @JsonProperty
  public String getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(String occurredAt) {
    this.occurredAt = occurredAt;
  }

  @JsonProperty
  public String getScheduledAt() {
    return scheduledAt;
  }

  public void setScheduledAt(String scheduledAt) {
    this.scheduledAt = scheduledAt;
  }

  @JsonProperty
  public boolean isFollowup() {
    return followup;
  }

  public void setFollowup(boolean followup) {
    this.followup = followup;
  }

  @JsonProperty
  public boolean isDeleted() {
    return deleted;
  }

  @JsonProperty
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  @JsonProperty
  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  @JsonProperty
  public String getCreatedAtClient() {
    return createdAtClient;
  }

  public void setCreatedAtClient(String createdAtClient) {
    this.createdAtClient = createdAtClient;
  }

  @JsonProperty
  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  @JsonProperty
  public String getUpdatedAtClient() {
    return updatedAtClient;
  }

  public void setUpdatedAtClient(String updatedAtClient) {
    this.updatedAtClient = updatedAtClient;
  }

  @JsonProperty
  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  @JsonProperty
  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  @JsonProperty
  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  @JsonProperty
  public String getAttributeOptionCombo() {
    return attributeOptionCombo;
  }

  public void setAttributeOptionCombo(String attributeOptionCombo) {
    this.attributeOptionCombo = attributeOptionCombo;
  }

  @JsonProperty
  public String getAttributeCategoryOptions() {
    return attributeCategoryOptions;
  }

  public void setAttributeCategoryOptions(String attributeCategoryOptions) {
    this.attributeCategoryOptions = attributeCategoryOptions;
  }

  @JsonProperty
  public String getAssignedUser() {
    return assignedUser;
  }

  public void setAssignedUser(String assignedUser) {
    this.assignedUser = assignedUser;
  }

  @JsonProperty
  public String getDataElement() {
    return dataElement;
  }

  public void setDataElement(String dataElement) {
    this.dataElement = dataElement;
  }

  @JsonProperty
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @JsonProperty
  public Boolean getProvidedElsewhere() {
    return providedElsewhere;
  }

  public void setProvidedElsewhere(Boolean providedElsewhere) {
    this.providedElsewhere = providedElsewhere;
  }

  @JsonProperty
  public String getStoredBy() {
    return storedBy;
  }

  public void setStoredBy(String storedBy) {
    this.storedBy = storedBy;
  }

  @JsonProperty
  public String getCompletedAt() {
    return this.completedAt;
  }

  public void setCompletedAt(String completedAt) {
    this.completedAt = completedAt;
  }

  @JsonProperty
  public String getCompletedBy() {
    return this.completedBy;
  }

  public void setCompletedBy(String completedBy) {
    this.completedBy = completedBy;
  }

  @JsonProperty
  public String getGeometry() {
    return geometry;
  }

  public void setGeometry(String geometry) {
    this.geometry = geometry;
  }

  @JsonProperty
  public String getCreatedAtDataValue() {
    return createdAtDataValue;
  }

  public void setCreatedAtDataValue(String createdAtDataValue) {
    this.createdAtDataValue = createdAtDataValue;
  }

  @JsonProperty
  public String getUpdatedAtDataValue() {
    return updatedAtDataValue;
  }

  public void setUpdatedAtDataValue(String updatedAtDataValue) {
    this.updatedAtDataValue = updatedAtDataValue;
  }

  @JsonProperty
  public String getStoredByDataValue() {
    return storedByDataValue;
  }

  public void setStoredByDataValue(String storedByDataValue) {
    this.storedByDataValue = storedByDataValue;
  }
}
