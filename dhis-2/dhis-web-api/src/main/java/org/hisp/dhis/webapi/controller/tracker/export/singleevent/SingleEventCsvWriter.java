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
package org.hisp.dhis.webapi.controller.tracker.export.singleevent;

import static org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig.csvMapper;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.webapi.controller.tracker.export.CompressionUtil;
import org.hisp.dhis.webapi.controller.tracker.view.DataValue;
import org.hisp.dhis.webapi.controller.tracker.view.SingleEvent;

class SingleEventCsvWriter {
  private SingleEventCsvWriter() {
    throw new IllegalStateException("Utility class");
  }

  public static void write(OutputStream outputStream, List<SingleEvent> events, boolean withHeader)
      throws IOException {
    ObjectWriter writer = getObjectWriter(withHeader);

    writer.writeValue(outputStream, getCsvEventDataValues(events));
  }

  public static void writeZip(
      OutputStream outputStream, List<SingleEvent> toCompress, boolean withHeader, String file)
      throws IOException {
    CompressionUtil.writeZip(
        outputStream, getCsvEventDataValues(toCompress), getObjectWriter(withHeader), file);
  }

  public static void writeGzip(
      OutputStream outputStream, List<SingleEvent> toCompress, boolean withHeader)
      throws IOException {
    CompressionUtil.writeGzip(
        outputStream, getCsvEventDataValues(toCompress), getObjectWriter(withHeader));
  }

  private static ObjectWriter getObjectWriter(boolean withHeader) {
    final CsvSchema csvSchema =
        csvMapper
            .schemaFor(SingleEventCsvRow.class)
            .withLineSeparator("\n")
            .withUseHeader(withHeader);

    return csvMapper.writer(csvSchema.withUseHeader(withHeader));
  }

  private static List<SingleEventCsvRow> getCsvEventDataValues(List<SingleEvent> events) {
    List<SingleEventCsvRow> dataValues = new ArrayList<>();

    for (SingleEvent event : events) {
      SingleEventCsvRow templateDataValue = map(event);

      if (event.getDataValues().isEmpty()) {
        dataValues.add(templateDataValue);
        continue;
      }

      for (DataValue value : event.getDataValues()) {
        dataValues.add(map(value, templateDataValue));
      }
    }
    return dataValues;
  }

  private static SingleEventCsvRow map(SingleEvent event) {
    SingleEventCsvRow result = new SingleEventCsvRow();
    result.setEvent(event.getEvent());
    result.setStatus(event.getStatus() != null ? event.getStatus().name() : null);
    result.setProgram(event.getProgram());
    result.setOrgUnit(event.getOrgUnit());
    result.setOccurredAt(event.getOccurredAt() == null ? null : event.getOccurredAt().toString());
    result.setDeleted(event.isDeleted());
    result.setCreatedAt(event.getCreatedAt() == null ? null : event.getCreatedAt().toString());
    result.setCreatedAtClient(
        event.getCreatedAtClient() == null ? null : event.getCreatedAtClient().toString());
    result.setUpdatedAt(event.getUpdatedAt() == null ? null : event.getUpdatedAt().toString());
    result.setUpdatedAtClient(
        event.getUpdatedAtClient() == null ? null : event.getUpdatedAtClient().toString());
    result.setUpdatedBy(event.getUpdatedBy() == null ? null : event.getUpdatedBy().getUsername());
    result.setStoredBy(event.getStoredBy());
    result.setCompletedAt(
        event.getCompletedAt() == null ? null : event.getCompletedAt().toString());
    result.setCompletedBy(event.getCompletedBy());
    result.setAttributeOptionCombo(event.getAttributeOptionCombo());
    result.setAssignedUser(
        event.getAssignedUser() == null ? null : event.getAssignedUser().getUsername());

    if (event.getGeometry() != null) {
      result.setGeometry(event.getGeometry().toText());

      if (event.getGeometry().getGeometryType().equals("Point")) {
        result.setLongitude(event.getGeometry().getCoordinate().x);
        result.setLatitude(event.getGeometry().getCoordinate().y);
      }
    }
    return result;
  }

  private static SingleEventCsvRow map(DataValue value, SingleEventCsvRow base) {
    SingleEventCsvRow result = new SingleEventCsvRow(base);
    result.setDataElement(value.getDataElement());
    result.setValue(value.getValue());
    result.setProvidedElsewhere(value.isProvidedElsewhere());
    result.setCreatedAtDataValue(
        value.getCreatedAt() == null ? null : value.getCreatedAt().toString());
    result.setUpdatedAtDataValue(
        value.getUpdatedAt() == null ? null : value.getUpdatedAt().toString());

    if (value.getStoredBy() != null) {
      result.setStoredBy(value.getStoredBy());
    }

    return result;
  }
}
