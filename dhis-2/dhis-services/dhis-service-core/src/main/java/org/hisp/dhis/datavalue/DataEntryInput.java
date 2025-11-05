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

import static java.lang.Boolean.parseBoolean;
import static org.hisp.dhis.commons.util.StreamUtils.wrapAndCheckCompressionFormat;

import com.lowagie.text.exceptions.InvalidPdfException;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.csv.CSV;
import org.hisp.dhis.dxf2.adx.AdxPeriod;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.staxwax.factory.XMLFactory;
import org.hisp.staxwax.reader.XMLReader;

/**
 * A utility to convert between binary and text formats and the processing records {@link
 * DataEntryGroup.Input} and {@link DataEntryValue.Input}.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataEntryInput {

  @Nonnull
  public static List<DataEntryGroup.Input> fromCsv(
      @Nonnull InputStream in, @Nonnull ImportOptions options) throws IOException {
    List<DataEntryValue.Input> values =
        CSV.of(wrapAndCheckCompressionFormat(in)).as(DataEntryValue.Input.class).list();
    String ds = options.getDataSet();
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(options.getIdSchemes());
    return List.of(new DataEntryGroup.Input(ids, ds, values));
  }

  @Nonnull
  public static List<DataEntryGroup.Input> fromXml(
      @Nonnull InputStream in, @Nonnull ImportOptions options)
      throws IOException, BadRequestException {
    try {
      XMLReader dvs = XMLFactory.getXMLReader(wrapAndCheckCompressionFormat(in));
      XMLStreamReader reader = dvs.getXmlStreamReader();
      while (reader.hasNext()) {
        if (reader.next() == XMLStreamConstants.START_ELEMENT) {
          String rootTag = reader.getLocalName();
          if ("dataValueSet".equals(rootTag)) return fromDxf2Xml(dvs, options);
          if ("adx".equals(rootTag)) return fromAdxXml(dvs, options);
          throw new BadRequestException(
              ErrorCode.E8007,
              "Neither <dataValueSet> nor <adx> root tag found but: `<" + rootTag + ">`");
        }
      }
      throw new BadRequestException(ErrorCode.E8007, "No root tag found");
    } catch (XMLStreamException ex) {
      throw new BadRequestException(ErrorCode.E8007, ex.getMessage());
    }
  }

  @Nonnull
  private static List<DataEntryGroup.Input> fromDxf2Xml(
      @Nonnull XMLReader dvs, @Nonnull ImportOptions options) {
    IdSchemes schemes = options.getIdSchemes();
    if (!"dataValueSet".equals(dvs.getElementName())) dvs.moveToStartElement("dataValueSet");
    String ds = dvs.getAttributeValue("dataSet");
    // keys that are common for all values
    String ou = dvs.getAttributeValue("orgUnit");
    String pe = dvs.getAttributeValue("period");
    String aoc = dvs.getAttributeValue("attributeOptionCombo");
    String dryRun = dvs.getAttributeValue("dryRun");
    if (dryRun != null) options.setDryRun(parseBoolean(dryRun));
    // ID schemes
    String scheme = dvs.getAttributeValue("idScheme");
    if (scheme != null) schemes.setIdScheme(scheme);
    scheme = dvs.getAttributeValue("dataElementIdScheme");
    if (scheme != null) schemes.setDataElementIdScheme(scheme);
    scheme = dvs.getAttributeValue("orgUnitIdScheme");
    if (scheme != null) schemes.setOrgUnitIdScheme(scheme);
    scheme = dvs.getAttributeValue("categoryOptionComboIdScheme");
    if (scheme != null) schemes.setCategoryOptionComboIdScheme(scheme);
    scheme = dvs.getAttributeValue("dataSetIdScheme");
    if (scheme != null) schemes.setDataSetIdScheme(scheme);
    // values...
    List<DataEntryValue.Input> values = new ArrayList<>();
    while (dvs.moveToStartElement("dataValue", "dataValueSet")) {
      String followUp = dvs.getAttributeValue("followUp");
      String deleted = dvs.getAttributeValue("deleted");
      values.add(
          new DataEntryValue.Input(
              dvs.getAttributeValue("dataElement"),
              dvs.getAttributeValue("orgUnit"),
              dvs.getAttributeValue("categoryOptionCombo"),
              null,
              dvs.getAttributeValue("attributeOptionCombo"),
              null,
              null,
              dvs.getAttributeValue("period"),
              dvs.getAttributeValue("value"),
              dvs.getAttributeValue("comment"),
              followUp == null ? null : parseBoolean(followUp),
              deleted == null ? null : parseBoolean(deleted)));
    }
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(schemes);
    return List.of(new DataEntryGroup.Input(ids, ds, null, ou, pe, aoc, null, values));
  }

  @Nonnull
  private static List<DataEntryGroup.Input> fromAdxXml(
      @Nonnull XMLReader dvs, @Nonnull ImportOptions options) {
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(options.getIdSchemes());
    String ns = "urn:ihe:qrph:adx:2015";
    if (!"adx".equals(dvs.getElementName())) dvs.moveToStartElement("adx", ns);
    DataEntryGroup.Input last = null;
    List<DataEntryGroup.Input> res = new ArrayList<>();
    while (dvs.moveToStartElement("group", ns)) {
      Map<String, String> group = dvs.readAttributes();
      String ds = group.get("dataSet");
      // keys common for all values in group
      String ou = group.get("orgUnit");
      String pe = AdxPeriod.parse(group.get("period")).getIsoDate();
      String aoc = group.get("attributeOptionCombo");
      Map<String, String> aco = null;
      if (!group.containsKey("attributeOptionCombo")) {
        aco = group;
        // remove the attributes that are known to not be category=option
        List.of("orgUnit", "period", "dataSet").forEach(aco::remove);
        if (aco.isEmpty()) aco = null; // if there is no other attr => AOC not used
      }
      // values...
      List<DataEntryValue.Input> values = new ArrayList<>();
      while (dvs.moveToStartElement("dataValue", "group")) {
        Map<String, String> dv = dvs.readAttributes();
        String de = dv.get("dataElement");
        String value = dv.get("value");
        String comment = dv.get("comment");
        String followupStr = dv.get("followup");
        String deletedStr = dv.get("deleted");
        String coc = dv.get("categoryOptionCombo");
        Map<String, String> co = null;
        if (!dv.containsKey("categoryOptionCombo")) {
          co = dv;
          // remove the attributes that are known to not be category=option
          List.of("dataElement", "value", "comment", "followup").forEach(co::remove);
        }
        Boolean followup = followupStr == null ? null : "true".equalsIgnoreCase(followupStr);
        Boolean deleted = deletedStr == null ? null : "true".equalsIgnoreCase(deletedStr);
        if (dvs.moveToStartElement("annotation", "dataValue")) value = dvs.getElementValue();
        // values also get group ou, pe set because of potential merge
        values.add(
            new DataEntryValue.Input(
                de, ou, coc, co, null, null, null, pe, value, comment, followup, deleted));
      }
      DataEntryGroup.Input adxGroup =
          new DataEntryGroup.Input(ids, ds, null, ou, pe, aoc, aco, values);
      // if this ADX group has same DS group properties...
      if (last != null && last.isSameDsAoc(adxGroup)) {
        // auto-merge ADX group into a DS group purely for faster decode
        // (less DB queries due to fewer groups)
        // but only merge in-order to maintain value order
        res.remove(res.size() - 1); // old last
        last = last.mergedSameDsAoc(adxGroup);
        res.add(last);
      } else {
        res.add(adxGroup);
        last = adxGroup;
      }
    }
    return res;
  }

  @Nonnull
  public static List<DataEntryGroup.Input> fromJson(
      @Nonnull InputStream in, @Nonnull ImportOptions options) throws IOException {
    IdSchemes schemes = options.getIdSchemes();
    JsonObject dvs =
        JsonValue.of(new InputStreamReader(wrapAndCheckCompressionFormat(in))).asObject();
    String ds = dvs.getString("dataSet").string();
    // keys that are common for all values
    String ou = dvs.getString("orgUnit").string();
    String pe = dvs.getString("period").string();
    JsonString aoc = dvs.getString("attributeOptionCombo");
    String aocId = aoc.isString() ? aoc.string() : null;
    Map<String, String> aocMap =
        aoc.isObject() ? aoc.asMap(JsonString.class).toMap(JsonString::string) : null;
    Boolean dryRun = dvs.getBoolean("dryRun").bool();
    if (dryRun != null) options.setDryRun(dryRun);
    // ID schemes
    if (!dvs.get("idScheme").isUndefined()) schemes.setIdScheme(dvs.getString("idScheme").string());
    if (!dvs.get("dataElementIdScheme").isUndefined())
      schemes.setDataSetIdScheme(dvs.getString("dataElementIdScheme").string());
    if (!dvs.get("orgUnitIdScheme").isUndefined())
      schemes.setOrgUnitIdScheme(dvs.getString("orgUnitIdScheme").string());
    if (!dvs.get("categoryOptionComboIdScheme").isUndefined())
      schemes.setCategoryOptionComboIdScheme(dvs.getString("categoryOptionComboIdScheme").string());
    if (!dvs.get("dataSetIdScheme").isUndefined())
      schemes.setDataSetIdScheme(dvs.getString("dataSetIdScheme").string());
    // values...
    List<DataEntryValue.Input> values = new ArrayList<>();
    // Note that this uses JsonNode API to iterate without indexing
    // to make the processing memory footprint smaller
    JsonValue dataValues = dvs.get("dataValues");
    if (dataValues.exists())
      dataValues
          .node()
          .elements(false)
          .forEachRemaining(
              node -> {
                JsonObject dv = JsonMixed.of(node);
                JsonString coc = dv.getString("categoryOptionCombo");
                values.add(
                    new DataEntryValue.Input(
                        dv.getString("dataElement").string(),
                        dv.getString("orgUnit").string(),
                        coc.isString() ? coc.string() : null,
                        coc.isObject()
                            ? coc.asMap(JsonString.class).toMap(JsonString::string)
                            : null,
                        dv.getString("attributeOptionCombo").string(),
                        null,
                        null,
                        dv.getString("period").string(),
                        dv.getString("value").string(),
                        dv.getString("comment").string(),
                        dv.getBoolean("followUp").bool(),
                        dv.getBoolean("deleted").bool()));
              });
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(schemes);
    return List.of(new DataEntryGroup.Input(ids, ds, null, ou, pe, aocId, aocMap, values));
  }

  @Nonnull
  public static List<DataEntryGroup.Input> fromPdf(
      @Nonnull InputStream in, @Nonnull ImportOptions options)
      throws IOException, BadRequestException {
    try (PdfReader dvs = new PdfReader(wrapAndCheckCompressionFormat(in))) {
      AcroFields form = dvs.getAcroFields();
      if (form == null) throw new BadRequestException(ErrorCode.E8001);
      // keys that are common for all values
      String ou = form.getField("TXFD_OrgID").trim();
      String pe = form.getField("TXFD_PeriodID").trim();
      // values...
      Set<String> fields = form.getAllFields().keySet();
      List<DataEntryValue.Input> values = new ArrayList<>();
      for (String field : fields) {
        if (field.startsWith("TXFDDV_")) {
          String[] parts = field.split("_");
          String de = parts[1];
          String coc = parts[2];
          String value = form.getField(field);
          if (parts.length >= 4 && parts[3].startsWith("T4")) { // T4=checkbox
            if ("On".equalsIgnoreCase(value)) value = "true";
            if ("Off".equalsIgnoreCase(value)) value = "false";
          }
          values.add(
              new DataEntryValue.Input(
                  de, null, coc, null, null, null, null, null, value, null, null, null));
        }
      }
      DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(options.getIdSchemes());
      return List.of(new DataEntryGroup.Input(ids, null, null, ou, pe, null, null, values));
    } catch (InvalidPdfException ex) {
      throw new BadRequestException(ErrorCode.E8006, ex.getMessage());
    }
  }
}
