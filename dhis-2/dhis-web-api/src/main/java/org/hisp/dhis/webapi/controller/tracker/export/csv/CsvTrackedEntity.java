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
package org.hisp.dhis.webapi.controller.tracker.export.csv;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonPropertyOrder({
  "trackedEntity",
  "trackedEntityType",
  "createdAt",
  "createdAtClient",
  "updatedAt",
  "updatedAtClient",
  "orgUnit",
  "inactive",
  "deleted",
  "potentialDuplicate",
  "geometry",
  "latitude",
  "longitude",
  "storedBy",
  "createdBy",
  "updatedBy"
})
@Data
@NoArgsConstructor
class CsvTrackedEntity {

  private String trackedEntity;

  private String trackedEntityType;

  private String createdAt;

  private String createdAtClient;

  private String updatedAt;

  private String updatedAtClient;

  private String orgUnit;

  private boolean inactive;

  private boolean deleted;

  private boolean potentialDuplicate;

  private String attribute;

  private String displayName;

  private String attrCreatedAt;

  private String attrUpdatedAt;

  private String valueType;

  private String value;

  private String geometry;

  private Double longitude;

  private Double latitude;

  private String storedBy;

  private String createdBy;

  private String updatedBy;

  public CsvTrackedEntity(CsvTrackedEntity entity) {
    this.trackedEntity = entity.getTrackedEntity();
    this.trackedEntityType = entity.getTrackedEntityType();
    this.createdAt = entity.getCreatedAt();
    this.createdAtClient = entity.getCreatedAtClient();
    this.updatedAt = entity.getUpdatedAt();
    this.updatedAtClient = entity.getUpdatedAtClient();
    this.orgUnit = entity.getOrgUnit();
    this.inactive = entity.isInactive();
    this.deleted = entity.isDeleted();
    this.potentialDuplicate = entity.isPotentialDuplicate();
    this.geometry = entity.getGeometry();
    this.longitude = entity.getLongitude();
    this.latitude = entity.getLatitude();
    this.storedBy = entity.getStoredBy();
    this.createdBy = entity.getCreatedBy();
    this.updatedBy = entity.getUpdatedBy();
  }
}
