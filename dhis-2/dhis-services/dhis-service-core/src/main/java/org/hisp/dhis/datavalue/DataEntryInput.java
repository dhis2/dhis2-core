/*
 * Copyright (c) 2004-2026, University of Oslo
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import javax.annotation.CheckForNull;
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
import org.hisp.dhis.jsontree.JsonAccessException;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonNode.Index;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonPathException;
import org.hisp.dhis.jsontree.JsonTreeException;
import org.hisp.dhis.jsontree.Text;
import org.hisp.staxwax.XMLException;
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
      @Nonnull InputStream in, @Nonnull ImportOptions options)
      throws IOException, BadRequestException {
    List<DataEntryValue.Input> values;
    try {
      values = CSV.of(wrapAndCheckCompressionFormat(in)).as(DataEntryValue.Input.class).list();
    } catch (IllegalArgumentException ex) {
      // CSV reports all input problems as IllegalArgumentException
      throw new BadRequestException(ErrorCode.E8036, ex.getMessage());
    }
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
    } catch (XMLStreamException | XMLException ex) {
      // XMLException: staxwax wraps parse errors that occur while reading the values
      throw new BadRequestException(ErrorCode.E8007, ex.getMessage());
    }
  }

  @Nonnull
  private static List<DataEntryGroup.Input> fromDxf2Xml(
      @Nonnull XMLReader dvs, @Nonnull ImportOptions options) {
    IdSchemes schemes = options.getIdSchemes();
    if (!"dataValueSet".equals(dvs.getElementName())) dvs.moveToStartElement("dataValueSet");
    String ds = dvs.getAttributeValue("dataSet");
    String completionDate = dvs.getAttributeValue("completeDate");
    // group level IDs
    String ou = dvs.getAttributeValue("orgUnit");
    String pe = dvs.getAttributeValue("period");
    String aoc = dvs.getAttributeValue("attributeOptionCombo");
    String dryRun = dvs.getAttributeValue("dryRun");
    if (dryRun != null) options.setDryRun(parseBoolean(dryRun));

    setIdSchemes(schemes, dvs::getAttributeValue);

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
    return List.of(
        new DataEntryGroup.Input(ids, ds, completionDate, null, ou, pe, aoc, null, null, values));
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
      String completionDate = group.get("completeDate");
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
          new DataEntryGroup.Input(ids, ds, completionDate, null, ou, pe, aoc, aco, null, values);
      // if this ADX group has same DS group properties...
      if (last != null && last.canMergeWith(adxGroup)) {
        // auto-merge ADX group into a DS group purely for faster decode
        // (less DB queries due to fewer groups)
        // but only merge in-order to maintain value order
        res.remove(res.size() - 1); // old last
        last = last.mergedWith(adxGroup);
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
      @Nonnull InputStream in, @Nonnull ImportOptions options)
      throws IOException, BadRequestException {
    byte[] json = wrapAndCheckCompressionFormat(in).readAllBytes();
    try {
      return fromJson(
          JsonMixed.of(JsonNode.of(json, StandardCharsets.UTF_8, null, Index.AUTO_SKIP)), options);
    } catch (JsonTreeException
        | JsonPathException
        | JsonAccessException
        | IllegalArgumentException ex) {
      // JsonFormatException and JsonSchemaException are IllegalArgumentExceptions,
      // so are invalid ID schemes
      throw new BadRequestException(ErrorCode.E8036, ex.getMessage());
    }
  }

  @Nonnull
  private static List<DataEntryGroup.Input> fromJson(
      @Nonnull JsonMixed root, @Nonnull ImportOptions options) {
    // the index is built lazily, this checks the syntax of the entire document so that
    // incomplete input (e.g. a lone "{") and trailing content after the root are rejected
    root.node().endIndex();
    if (!root.isObject())
      throw new IllegalArgumentException("Root must be an object but was: " + root.type());
    JsonObject dvs = root.asObject();
    IdSchemes schemes = options.getIdSchemes();
    String ds = dvs.get("dataSet").string();
    String completionDate = dvs.get("completeDate").string();
    // keys that are common for all values
    String ou = dvs.get("orgUnit").string();
    String pe = dvs.get("period").string();
    JsonMixed aoc = dvs.get("attributeOptionCombo");
    String aocId = !aoc.isObject() ? aoc.string() : null;
    Map<String, String> aocMap = toStringMap(aoc);
    Boolean dryRun = bool(dvs.get("dryRun"));
    if (dryRun != null) options.setDryRun(dryRun);
    setIdSchemes(schemes, name -> dvs.get(name).string());
    DataEntryGroup.Input.Scope deletion = deletionScope(dvs.get("deletion"));

    // values...
    List<DataEntryValue.Input> values = new ArrayList<>();
    for (JsonObject dv : dvs.get("dataValues").values()) {
      if (!dv.isObject())
        throw new IllegalArgumentException(
            "Each data value must be an object but was: " + dv.type());
      JsonMixed coc = dv.get("categoryOptionCombo");
      JsonMixed followup = !dv.isUndefined("followUp") ? dv.get("followUp") : dv.get("followup");
      values.add(
          new DataEntryValue.Input(
              dv.get("dataElement").string(),
              dv.get("orgUnit").string(),
              !coc.isObject() ? coc.string() : null,
              toStringMap(coc),
              dv.get("attributeOptionCombo").string(),
              null,
              null,
              dv.get("period").string(),
              dv.get("value").string(),
              dv.get("comment").string(),
              bool(followup),
              bool(dv.get("deleted"))));
    }
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(schemes);
    return List.of(
        new DataEntryGroup.Input(
            ids, ds, completionDate, null, ou, pe, aocId, aocMap, deletion, values));
  }

  /**
   * Sets the ID schemes given in the input, the same property names for all formats.
   *
   * @param property returns the value of the named property, null if absent
   */
  private static void setIdSchemes(
      @Nonnull IdSchemes schemes, @Nonnull UnaryOperator<String> property) {
    setIfPresent(property.apply("idScheme"), schemes::setIdScheme);
    setIfPresent(property.apply("dataSetIdScheme"), schemes::setDataSetIdScheme);
    setIfPresent(property.apply("dataElementIdScheme"), schemes::setDataElementIdScheme);
    setIfPresent(property.apply("orgUnitIdScheme"), schemes::setOrgUnitIdScheme);
    setIfPresent(
        property.apply("categoryOptionComboIdScheme"), schemes::setCategoryOptionComboIdScheme);
    setIfPresent(
        property.apply("attributeOptionComboIdScheme"), schemes::setAttributeOptionComboIdScheme);
    setIfPresent(property.apply("categoryIdScheme"), schemes::setCategoryIdScheme);
    setIfPresent(property.apply("categoryOptionIdScheme"), schemes::setCategoryOptionIdScheme);
  }

  private static void setIfPresent(@CheckForNull String value, @Nonnull Consumer<String> setter) {
    if (value != null) setter.accept(value);
  }

  @CheckForNull
  private static DataEntryGroup.Input.Scope deletionScope(@Nonnull JsonObject scope) {
    if (scope.isUndefined() || (scope.isObject() && scope.isEmpty())) return null;
    return new DataEntryGroup.Input.Scope(
        scope.getArray("orgUnits").stringValues(),
        scope.getArray("periods").stringValues(),
        scope
            .get("elements")
            .values()
            .map(
                e ->
                    new DataEntryGroup.Input.Scope.Element(
                        e.get("dataElement").string(),
                        e.get("categoryOptionCombo").string(),
                        e.get("attributeOptionCombo").string()))
            .toList());
  }

  /**
   * Accepts what 2.42 (Jackson) accepted for a boolean: JSON booleans, the strings {@code true} and
   * {@code false} (any case), the numbers {@code 1} and {@code 0}, and an empty string as null.
   * This also allows strings "0", "1" as well as "yes" and "no".
   *
   * @return the boolean value or null if the value is undefined, JSON null or an empty string
   * @throws IllegalArgumentException for any other value
   */
  @CheckForNull
  private static Boolean bool(@Nonnull JsonMixed value) {
    Text str = value.text();
    if (str == null || str.isEmpty()) return null;
    if (str.contentEquals("1")
        || str.contentEqualsIgnoreCaseASCII("true")
        || str.contentEqualsIgnoreCaseASCII("yes")) return true;
    if (str.contentEquals("0")
        || str.contentEqualsIgnoreCaseASCII("false")
        || str.contentEqualsIgnoreCaseASCII("no")) return false;
    throw new IllegalArgumentException(
        "Property `%s` must be a boolean but was: %s".formatted(value.path(), value.toJson()));
  }

  /**
   * @return a category to option map if the value is an object, otherwise null
   */
  @CheckForNull
  private static Map<String, String> toStringMap(@Nonnull JsonMixed value) {
    if (!value.isObject()) return null;
    Map<String, String> res = new LinkedHashMap<>(value.size());
    for (JsonMixed e : value.entries()) res.put(e.getKey().toString(), e.string());
    return res;
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
      return List.of(
          new DataEntryGroup.Input(ids, null, null, null, ou, pe, null, null, null, values));
    } catch (InvalidPdfException ex) {
      throw new BadRequestException(ErrorCode.E8006, ex.getMessage());
    }
  }
}
