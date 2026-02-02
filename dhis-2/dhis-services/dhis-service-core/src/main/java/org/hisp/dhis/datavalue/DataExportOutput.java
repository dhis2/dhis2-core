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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.dxf2.adx.AdxPeriod;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.period.Period;
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
    try (json) {
      JsonBuilder.streamObject(
          JsonBuilder.MINIMIZED_FULL,
          json,
          dvs -> {
            // group level IDs...
            dvs.addString("dataSet", group.dataSet());
            dvs.addString("period", group.period());
            dvs.addString("orgUnit", group.orgUnit());
            if (group.attributeOptionCombo() != null) {
              dvs.addString("attributeOptionCombo", group.attributeOptionCombo());
            } else if (group.attributeOptions() != null) {
              dvs.addObject(
                  "attributeOptionCombo", map -> group.attributeOptions().forEach(map::addString));
            }

            // ID schemes
            DataExportGroup.Ids ids = group.ids();
            if (ids.dataSets().isNotUID())
              dvs.addString("dataSetIdScheme", ids.dataSets().toString());
            if (ids.dataElements().isNotUID())
              dvs.addString("dataElementIdScheme", ids.dataElements().toString());
            if (ids.orgUnits().isNotUID())
              dvs.addString("orgUnitIdScheme", ids.orgUnits().toString());
            if (ids.categoryOptionCombos().isNotUID())
              dvs.addString("categoryOptionComboIdScheme", ids.categoryOptionCombos().toString());
            if (ids.attributeOptionCombos().isNotUID())
              dvs.addString("attributeOptionComboIdScheme", ids.attributeOptionCombos().toString());
            if (ids.categories().isNotUID())
              dvs.addString("categoryIdScheme", ids.categories().toString());
            if (ids.categoryOptions().isNotUID())
              dvs.addString("categoryOptionIdScheme", ids.categoryOptions().toString());

            // deletion scope
            DataExportGroup.Scope deletion = group.deletion();
            if (deletion != null) {
              dvs.addObject(
                  "deletion",
                  del -> {
                    del.addArray("orgUnits", arr -> deletion.orgUnits().forEach(arr::addString));
                    del.addArray("periods", arr -> deletion.periods().forEach(arr::addString));
                    del.addArray(
                        "elements",
                        arr -> {
                          for (DataExportGroup.Scope.Element elem : deletion.elements())
                            arr.addObject(
                                e -> {
                                  e.addString("dataElement", elem.dataElement());
                                  e.addString("categoryOptionCombo", elem.categoryOptionCombo());
                                  e.addString("attributeOptionCombo", elem.attributeOptionCombo());
                                });
                        });
                  });
            }

            // values...
            dvs.addArray(
                "dataValues",
                values -> {
                  Iterator<DataExportValue.Output> iter = group.values().iterator();
                  while (iter.hasNext()) {
                    DataExportValue.Output dv = iter.next();
                    values.addObject(
                        val -> {
                          val.addString("dataElement", dv.dataElement());
                          val.addString("period", dv.period());
                          val.addString("orgUnit", dv.orgUnit());
                          if (dv.categoryOptionCombo() != null) {
                            val.addString("categoryOptionCombo", dv.categoryOptionCombo());
                          } else if (dv.categoryOptions() != null) {
                            val.addObject(
                                "categoryOptionCombo",
                                map -> dv.categoryOptions().forEach(map::addString));
                          }
                          val.addString("attributeOptionCombo", dv.attributeOptionCombo());
                          val.addString("value", dv.value());
                          val.addString("storedBy", dv.storedBy());
                          val.addString("created", toLongGmtDate(dv.created()));
                          val.addString("lastUpdated", toLongGmtDate(dv.lastUpdated()));
                          val.addString("comment", dv.comment());
                          val.addBoolean("followup", dv.followUp());
                          if (dv.deleted()) val.addBoolean("deleted", true);
                        });
                  }
                });
          });
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
      writer.close();
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write CSV data", ex);
    }
  }

  static void toXml(DataExportGroup.Output group, OutputStream xml) {
    XMLWriter out = XMLFactory.getXMLWriter(xml);

    out.openDocument();
    out.openElement("dataValueSet");
    out.writeAttribute("xmlns", "http://dhis2.org/schema/dxf/2.0");
    // group level IDs
    if (group.dataSet() != null) out.writeAttribute("dataSet", group.dataSet());
    if (group.period() != null) out.writeAttribute("period", toAdxPeriod(group.period()));
    if (group.orgUnit() != null) out.writeAttribute("orgUnit", group.orgUnit());
    if (group.attributeOptionCombo() != null)
      out.writeAttribute("attributeOptionCombo", group.attributeOptionCombo());
    if (group.attributeOptions() != null) group.attributeOptions().forEach(out::writeAttribute);

    // ID schemes
    DataExportGroup.Ids ids = group.ids();
    if (ids.dataSets().isNotUID()) out.writeAttribute("dataSetIdScheme", ids.dataSets().toString());
    if (ids.dataElements().isNotUID())
      out.writeAttribute("dataElementIdScheme", ids.dataElements().toString());
    if (ids.orgUnits().isNotUID()) out.writeAttribute("orgUnitIdScheme", ids.orgUnits().toString());
    if (ids.categoryOptionCombos().isNotUID())
      out.writeAttribute("categoryOptionComboIdScheme", ids.categoryOptionCombos().toString());
    if (ids.attributeOptionCombos().isNotUID())
      out.writeAttribute("attributeOptionComboIdScheme", ids.attributeOptionCombos().toString());
    if (ids.categories().isNotUID())
      out.writeAttribute("categoryIdScheme", ids.categories().toString());
    if (ids.categoryOptions().isNotUID())
      out.writeAttribute("categoryOptionIdScheme", ids.categoryOptions().toString());

    // values...
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
    return AdxPeriod.serialize(Period.of(isoPeriod));
  }
}
