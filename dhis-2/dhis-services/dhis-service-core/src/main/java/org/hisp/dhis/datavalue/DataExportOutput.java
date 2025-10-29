/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.util.DateUtils.toLongGmtDate;

import com.csvreader.CsvWriter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.dxf2.adx.AdxPeriod;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.CsvUtils;
import org.hisp.staxwax.factory.XMLFactory;
import org.hisp.staxwax.writer.XMLWriter;

/**
 * Utilities to write aggregate data values in different output formats.
 *
 * @since 2.43
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DataExportOutput {

  static void toJson(DataExportGroup.Output group, OutputStream json) {
    try {
      JsonFactory factory = new ObjectMapper().getFactory();
      // Disables flushing every time that an object property is written
      // to the stream
      factory.disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);
      // Do not attempt to balance unclosed tags
      factory.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
      JsonGenerator out = factory.createGenerator(json);

      out.writeStartObject();
      if (group.dataSet() != null) out.writeStringField("dataSet", group.dataSet());
      if (group.period() != null) out.writeStringField("period", group.period());
      if (group.orgUnit() != null) out.writeStringField("orgUnit", group.orgUnit());
      if (group.attributeOptionCombo() != null)
        out.writeStringField("attributeOptionCombo", group.attributeOptionCombo());
      if (group.attributeOptions() != null)
        for (Map.Entry<String, String> e : group.attributeOptions().entrySet())
          out.writeStringField(e.getKey(), e.getValue());
      out.writeArrayFieldStart("dataValues");
      Iterator<DataExportValue.Output> iter = group.values().iterator();
      while (iter.hasNext()) {
        DataExportValue.Output dv = iter.next();
        out.writeStartObject();
        out.writeStringField("dataElement", dv.dataElement());
        if (dv.period() != null) out.writeStringField("period", dv.period());
        if (dv.orgUnit() != null) out.writeStringField("orgUnit", dv.orgUnit());
        if (dv.categoryOptionCombo() != null)
          out.writeStringField("categoryOptionCombo", dv.categoryOptionCombo());
        if (dv.categoryOptions() != null)
          for (Map.Entry<String, String> e : dv.categoryOptions().entrySet())
            out.writeStringField(e.getKey(), e.getValue());
        if (dv.attributeOptionCombo() != null)
          out.writeStringField("attributeOptionCombo", dv.attributeOptionCombo());
        if (dv.value() != null) out.writeStringField("value", dv.value());
        if (dv.storedBy() != null) out.writeStringField("storedBy", dv.storedBy());
        if (dv.created() != null) out.writeStringField("created", toLongGmtDate(dv.created()));
        if (dv.lastUpdated() != null)
          out.writeStringField("lastUpdated", toLongGmtDate(dv.lastUpdated()));
        if (dv.comment() != null) out.writeStringField("comment", dv.comment());
        if (dv.followUp() != null) out.writeBooleanField("followup", dv.followUp());
        if (dv.deleted()) out.writeBooleanField("deleted", true);
        out.writeEndObject();
      }
      out.writeEndArray();
      out.writeEndObject();
      out.close();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static final String[] CSV_HEADER_ROW = {
    "dataelement",
    "period",
    "orgunit",
    "categoryoptioncombo",
    "attributeoptioncombo",
    "value",
    "storedby",
    "lastupdated",
    "comment",
    "followup",
    "deleted"
  };

  static void toCsv(DataExportGroup.Output group, OutputStream csv) {
    try {
      CsvWriter writer = CsvUtils.getWriter(new PrintWriter(csv));
      writer.writeRecord(CSV_HEADER_ROW);
      Iterator<DataExportValue.Output> iter = group.values().iterator();
      String groupOrgUnit = group.orgUnit();
      String groupPeriod = group.period();
      String groupAoc = group.attributeOptionCombo();
      while (iter.hasNext()) {
        DataExportValue.Output dv = iter.next();
        writer.writeRecord(
            new String[] {
              dv.dataElement(),
              dv.period() == null ? groupPeriod : dv.period(),
              dv.orgUnit() == null ? groupOrgUnit : dv.orgUnit(),
              dv.categoryOptionCombo(),
              dv.attributeOptionCombo() == null ? groupAoc : dv.attributeOptionCombo(),
              dv.value(),
              dv.storedBy(),
              dv.lastUpdated() == null ? null : toLongGmtDate(dv.lastUpdated()),
              dv.comment(),
              dv.followUp() == null ? null : String.valueOf(dv.followUp()),
              String.valueOf(dv.deleted())
            });
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write CSV data", ex);
    }
  }

  static void toXml(DataExportGroup.Output group, OutputStream xml) {
    XMLWriter out = XMLFactory.getXMLWriter(xml);

    out.openDocument();
    out.openElement("dataValueSet");
    out.writeAttribute("xmlns", "http://dhis2.org/schema/dxf/2.0");
    if (group.dataSet() != null) out.writeAttribute("dataSet", group.dataSet());
    if (group.period() != null) out.writeAttribute("period", toAdxPeriod(group.period()));
    if (group.orgUnit() != null) out.writeAttribute("orgUnit", group.orgUnit());
    if (group.attributeOptionCombo() != null)
      out.writeAttribute("attributeOptionCombo", group.attributeOptionCombo());
    if (group.attributeOptions() != null) group.attributeOptions().forEach(out::writeAttribute);
    group
        .values()
        .forEach(
            dv -> {
              out.openElement("dataValue");
              out.writeAttribute("dataElement", dv.dataElement());
              if (dv.period() != null) out.writeAttribute("period", dv.period());
              if (dv.orgUnit() != null) out.writeAttribute("orgUnit", dv.orgUnit());
              out.writeAttribute("categoryOptionCombo", dv.categoryOptionCombo());
              if (dv.attributeOptionCombo() != null)
                out.writeAttribute("attributeOptionCombo", dv.attributeOptionCombo());
              if (dv.value() != null) out.writeAttribute("value", dv.value());
              if (dv.storedBy() != null) out.writeAttribute("storedBy", dv.storedBy());
              if (dv.created() != null) out.writeAttribute("created", toLongGmtDate(dv.created()));
              if (dv.lastUpdated() != null)
                out.writeAttribute("lastUpdated", toLongGmtDate(dv.lastUpdated()));
              if (dv.comment() != null) out.writeAttribute("comment", dv.comment());
              if (Boolean.TRUE.equals(dv.followUp())) out.writeAttribute("followUp", "true");
              if (dv.deleted()) out.writeAttribute("deleted", "true");
              out.closeElement();
            });
    out.closeElement();
    out.closeDocument();
    out.closeWriter();
  }

  /**
   * Writes data in ADX XML format
   *
   * @param groups data to export
   * @param xml target XML output stream to write to
   */
  static void toXml(Stream<DataExportGroup.Output> groups, OutputStream xml) {
    XMLWriter out = XMLFactory.getXMLWriter(xml);

    out.openElement("adx");
    out.writeAttribute("xmlns", "urn:ihe:qrph:adx:2015");
    groups.forEach(
        group -> {
          out.openElement("group");
          if (group.dataSet() != null) out.writeAttribute("dataSet", group.dataSet());
          if (group.period() != null) out.writeAttribute("period", toAdxPeriod(group.period()));
          if (group.orgUnit() != null) out.writeAttribute("orgUnit", group.orgUnit());
          if (group.attributeOptionCombo() != null)
            out.writeAttribute("attributeOptionCombo", group.attributeOptionCombo());
          if (group.attributeOptions() != null)
            group.attributeOptions().forEach(out::writeAttribute);
          group
              .values()
              .forEach(
                  dv -> {
                    out.openElement("dataValue");
                    out.writeAttribute("dataElement", dv.dataElement());
                    if (dv.period() != null) out.writeAttribute("period", toAdxPeriod(dv.period()));
                    if (dv.orgUnit() != null) out.writeAttribute("orgUnit", dv.orgUnit());
                    if (dv.categoryOptionCombo() != null)
                      out.writeAttribute("categoryOptionCombo", dv.categoryOptionCombo());
                    if (dv.categoryOptions() != null)
                      dv.categoryOptions().forEach(out::writeAttribute);
                    if (dv.attributeOptionCombo() != null)
                      out.writeAttribute("attributeOptionCombo", dv.attributeOptionCombo());
                    if (dv.type().isNumeric()) {
                      out.writeAttribute("value", dv.value());
                    } else {
                      out.writeAttribute("value", "0");
                      out.openElement("annotation");
                      out.writeCharacters(dv.value());
                      out.closeElement(); // ANNOTATION
                    }
                    out.closeElement(); // DATAVALUE
                  });
          out.closeElement(); // GROUP
        });
    out.closeElement(); // ADX
    out.closeWriter();
  }

  @Nonnull
  private static String toAdxPeriod(String isoPeriod) {
    return AdxPeriod.serialize(PeriodType.getPeriodFromIsoString(isoPeriod));
  }
}
