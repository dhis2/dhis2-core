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
package org.hisp.dhis.dxf2.datavalueset;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;

/**
 * Write {@link DataValueSet}s as CSV data.
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
final class JsonDataValueSetWriter implements DataValueSetWriter {
  private final JsonGenerator generator;

  JsonDataValueSetWriter(OutputStream out) {
    this(createGenerator(out));
  }

  private static JsonGenerator createGenerator(OutputStream out) {
    try {
      JsonFactory factory = new ObjectMapper().getFactory();
      // Disables flushing every time that an object property is written
      // to the stream
      factory.disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);
      // Do not attempt to balance unclosed tags
      factory.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
      return factory.createGenerator(out);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public void writeHeader() {
    try {
      generator.writeStartObject();
      generator.writeArrayFieldStart("dataValues");
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public void writeHeader(
      String dataSetId, String completeDate, String isoPeriod, String orgUnitId) {
    try {
      generator.writeStartObject();
      generator.writeStringField("dataSet", dataSetId);
      generator.writeStringField("completeDate", completeDate);
      generator.writeStringField("period", isoPeriod);
      generator.writeStringField("orgUnit", orgUnitId);
      generator.writeArrayFieldStart("dataValues");
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public void writeValue(DataValueEntry entry) {
    try {
      generator.writeStartObject();
      generator.writeStringField("dataElement", entry.getDataElement());
      generator.writeStringField("period", entry.getPeriod());
      generator.writeStringField("orgUnit", entry.getOrgUnit());
      generator.writeStringField("categoryOptionCombo", entry.getCategoryOptionCombo());
      generator.writeStringField("attributeOptionCombo", entry.getAttributeOptionCombo());
      generator.writeStringField("value", entry.getValue());
      generator.writeStringField("storedBy", entry.getStoredBy());
      generator.writeStringField("created", entry.getCreated());
      generator.writeStringField("lastUpdated", entry.getLastUpdated());
      generator.writeStringField("comment", entry.getComment());
      generator.writeBooleanField("followup", entry.getFollowup());
      Boolean deleted = entry.getDeleted();
      if (deleted != null) {
        generator.writeBooleanField("deleted", deleted);
      }
      generator.writeEndObject();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public void close() {
    try {
      generator.writeEndArray();
      generator.writeEndObject();
      generator.close();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
