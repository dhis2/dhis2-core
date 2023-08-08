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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.webapi.controller.tracker.export.CsvService;
import org.hisp.dhis.webapi.controller.tracker.view.Attribute;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;
import org.springframework.stereotype.Service;

@Service("org.hisp.dhis.webapi.controller.tracker.export.trackedentity.CsvTrackedEntityService")
class CsvTrackedEntityService implements CsvService<TrackedEntity> {
  private static final CsvMapper CSV_MAPPER =
      new CsvMapper().enable(CsvParser.Feature.WRAP_AS_ARRAY);

  @Override
  public void write(
      OutputStream outputStream, List<TrackedEntity> trackedEntities, boolean withHeader)
      throws IOException {
    final CsvSchema csvSchema =
        CSV_MAPPER
            .schemaFor(CsvTrackedEntity.class)
            .withLineSeparator("\n")
            .withUseHeader(withHeader);

    ObjectWriter writer = CSV_MAPPER.writer(csvSchema.withUseHeader(withHeader));

    List<CsvTrackedEntity> attributes = new ArrayList<>();

    for (TrackedEntity trackedEntity : trackedEntities) {
      CsvTrackedEntity trackedEntityValue = new CsvTrackedEntity();
      trackedEntityValue.setTrackedEntity(trackedEntity.getTrackedEntity());
      trackedEntityValue.setTrackedEntityType(trackedEntity.getTrackedEntityType());
      trackedEntityValue.setCreatedAt(checkForNull(trackedEntity.getCreatedAt()));
      trackedEntityValue.setCreatedAtClient(checkForNull(trackedEntity.getCreatedAtClient()));
      trackedEntityValue.setUpdatedAt(checkForNull(trackedEntity.getUpdatedAt()));
      trackedEntityValue.setUpdatedAtClient(checkForNull(trackedEntity.getUpdatedAtClient()));
      trackedEntityValue.setOrgUnit(trackedEntity.getOrgUnit());
      trackedEntityValue.setInactive(trackedEntity.isInactive());
      trackedEntityValue.setDeleted(trackedEntity.isDeleted());
      trackedEntityValue.setPotentialDuplicate(trackedEntity.isPotentialDuplicate());
      trackedEntityValue.setStoredBy(trackedEntity.getStoredBy());
      trackedEntityValue.setCreatedBy(
          trackedEntity.getCreatedBy() == null ? null : trackedEntity.getCreatedBy().getUsername());
      trackedEntityValue.setUpdatedBy(
          trackedEntity.getUpdatedBy() == null ? null : trackedEntity.getUpdatedBy().getUsername());

      if (trackedEntity.getGeometry() != null) {
        trackedEntityValue.setGeometry(trackedEntity.getGeometry().toText());

        if (trackedEntity.getGeometry().getGeometryType().equals("Point")) {
          trackedEntityValue.setLongitude(trackedEntity.getGeometry().getCoordinate().x);
          trackedEntityValue.setLatitude(trackedEntity.getGeometry().getCoordinate().y);
        }
      }

      if (trackedEntity.getAttributes().isEmpty()) {
        attributes.add(trackedEntityValue);
      } else {
        addAttributes(trackedEntity, trackedEntityValue, attributes);
      }
    }

    writer.writeValue(outputStream, attributes);
  }

  private String checkForNull(Instant instant) {
    if (instant == null) {
      return null;
    }

    return instant.toString();
  }

  private void addAttributes(
      TrackedEntity trackedEntity,
      CsvTrackedEntity currentDataValue,
      List<CsvTrackedEntity> attributes) {
    for (Attribute attribute : trackedEntity.getAttributes()) {
      CsvTrackedEntity trackedEntityValue = new CsvTrackedEntity(currentDataValue);
      trackedEntityValue.setAttribute(attribute.getAttribute());
      trackedEntityValue.setDisplayName(attribute.getDisplayName());
      trackedEntityValue.setAttrCreatedAt(checkForNull(attribute.getCreatedAt()));
      trackedEntityValue.setAttrUpdatedAt(checkForNull(attribute.getUpdatedAt()));
      trackedEntityValue.setValueType(attribute.getValueType().toString());
      trackedEntityValue.setValue(attribute.getValue());
      attributes.add(trackedEntityValue);
    }
  }

  @Override
  public List<TrackedEntity> read(InputStream inputStream, boolean skipFirst) {
    return Collections.emptyList();
  }
}
