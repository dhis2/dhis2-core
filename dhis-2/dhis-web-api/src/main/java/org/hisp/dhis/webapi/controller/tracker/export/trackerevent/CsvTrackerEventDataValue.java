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
package org.hisp.dhis.webapi.controller.tracker.export.trackerevent;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.UID;
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
  "occurredAt",
  "scheduledAt",
  "geometry",
  "latitude",
  "longitude",
  "followUp",
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
  "createdAtDataValue",
  "updatedAtDataValue"
})
@Getter
@Setter
class CsvTrackerEventDataValue {
  private UID event;

  private String status;

  private String program;

  private String programStage;

  private String orgUnit;

  private UID enrollment;

  private String occurredAt;

  private String scheduledAt;

  private boolean followUp;

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

  public CsvTrackerEventDataValue() {}

  public CsvTrackerEventDataValue(CsvTrackerEventDataValue dataValue) {
    Assert.notNull(dataValue, "A non-null CsvOutputEventDataValue must be given as a parameter.");

    event = dataValue.getEvent();
    status = dataValue.getStatus();
    program = dataValue.getProgram();
    programStage = dataValue.getProgramStage();
    enrollment = dataValue.getEnrollment();
    orgUnit = dataValue.getOrgUnit();
    occurredAt = dataValue.getOccurredAt();
    attributeOptionCombo = dataValue.getAttributeOptionCombo();
    attributeCategoryOptions = dataValue.getAttributeCategoryOptions();
    scheduledAt = dataValue.getScheduledAt();
    followUp = dataValue.isFollowUp();
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
}
