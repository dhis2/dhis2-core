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
import org.hisp.dhis.jsontree.JsonBoolean;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonNode.Index;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonPathException;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonTreeException;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.jsontree.Streamable;
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
      throw new BadRequestException(ErrorCode.E8037, ex.getMessage());
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

    // ID schemes
    String scheme = dvs.getAttributeValue("idScheme");
    if (scheme != null) schemes.setIdScheme(scheme);
    scheme = dvs.getAttributeValue("dataSetIdScheme");
    if (scheme != null) schemes.setDataSetIdScheme(scheme);
    scheme = dvs.getAttributeValue("dataElementIdScheme");
    if (scheme != null) schemes.setDataElementIdScheme(scheme);
    scheme = dvs.getAttributeValue("orgUnitIdScheme");
    if (scheme != null) schemes.setOrgUnitIdScheme(scheme);
    scheme = dvs.getAttributeValue("categoryOptionComboIdScheme");
    if (scheme != null) schemes.setCategoryOptionComboIdScheme(scheme);
    scheme = dvs.getAttributeValue("attributeOptionComboIdScheme");
    if (scheme != null) schemes.setAttributeOptionComboIdScheme(scheme);
    scheme = dvs.getAttributeValue("categoryIdScheme");
    if (scheme != null) schemes.setCategoryIdScheme(scheme);
    scheme = dvs.getAttributeValue("categoryOptionIdScheme");
    if (scheme != null) schemes.setCategoryOptionIdScheme(scheme);

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
    String ds = text(dvs.get("dataSet"));
    String completionDate = text(dvs.get("completeDate"));
    // keys that are common for all values
    String ou = text(dvs.get("orgUnit"));
    String pe = text(dvs.get("period"));
    JsonValue aoc = dvs.get("attributeOptionCombo");
    String aocId = aoc.isObject() ? null : text(aoc);
    Map<String, String> aocMap = textMap(aoc);
    Boolean dryRun = bool(dvs.get("dryRun"));
    if (dryRun != null) options.setDryRun(dryRun);
    // ID schemes
    String scheme = text(dvs.get("idScheme"));
    if (scheme != null) schemes.setIdScheme(scheme);
    scheme = text(dvs.get("dataSetIdScheme"));
    if (scheme != null) schemes.setDataSetIdScheme(scheme);
    scheme = text(dvs.get("dataElementIdScheme"));
    if (scheme != null) schemes.setDataElementIdScheme(scheme);
    scheme = text(dvs.get("orgUnitIdScheme"));
    if (scheme != null) schemes.setOrgUnitIdScheme(scheme);
    scheme = text(dvs.get("categoryOptionComboIdScheme"));
    if (scheme != null) schemes.setCategoryOptionComboIdScheme(scheme);
    scheme = text(dvs.get("attributeOptionComboIdScheme"));
    if (scheme != null) schemes.setAttributeOptionComboIdScheme(scheme);
    scheme = text(dvs.get("categoryIdScheme"));
    if (scheme != null) schemes.setCategoryIdScheme(scheme);
    scheme = text(dvs.get("categoryOptionIdScheme"));
    if (scheme != null) schemes.setCategoryOptionIdScheme(scheme);

    // deletion scope
    DataEntryGroup.Input.Scope deletion = null;
    JsonValue del = dvs.get("deletion");
    if (!del.isUndefined() && !del.isNull()) {
      JsonObject scope = object(del);
      deletion =
          new DataEntryGroup.Input.Scope(
              textList(scope.get("orgUnits")),
              textList(scope.get("periods")),
              elements(scope.get("elements")).stream()
                  .map(DataEntryInput::object)
                  .map(
                      e ->
                          new DataEntryGroup.Input.Scope.Element(
                              requiredText(e.get("dataElement")),
                              text(e.get("categoryOptionCombo")),
                              text(e.get("attributeOptionCombo"))))
                  .toList());
    }

    // values...
    List<DataEntryValue.Input> values = new ArrayList<>();
    for (JsonValue e : elements(dvs.get("dataValues"))) { // lazy, no copy of the array
      JsonObject dv = object(e);
      JsonValue coc = dv.get("categoryOptionCombo");
      values.add(
          new DataEntryValue.Input(
              text(dv.get("dataElement")),
              text(dv.get("orgUnit")),
              coc.isObject() ? null : text(coc),
              textMap(coc),
              text(dv.get("attributeOptionCombo")),
              null,
              null,
              text(dv.get("period")),
              text(dv.get("value")),
              text(dv.get("comment")),
              bool(firstDefined(dv, "followUp", "followup")),
              bool(dv.get("deleted"))));
    }
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(schemes);
    return List.of(
        new DataEntryGroup.Input(
            ids, ds, completionDate, null, ou, pe, aocId, aocMap, deletion, values));
  }

  /**
   * Up to 2.42 the JSON input was mapped by Jackson which accepted any JSON scalar for a {@link
   * String} property. Clients depend on this, for example by sending {@code "value": 5} or numeric
   * codes, so numbers and booleans are accepted as their JSON text.
   *
   * @return the text of a JSON scalar or null if the value is undefined or JSON null
   * @throws IllegalArgumentException if the value is an object or array
   */
  @CheckForNull
  private static String text(@Nonnull JsonValue value) {
    if (value.isUndefined() || value.isNull()) return null;
    if (value.isString()) return value.as(JsonString.class).string();
    if (value.isNumber() || value.isBoolean()) return value.toJson();
    throw new IllegalArgumentException(
        "Property `%s` must be a string, number or boolean but was: %s"
            .formatted(value.path(), value.type()));
  }

  @Nonnull
  private static String requiredText(@Nonnull JsonValue value) {
    String text = text(value);
    if (text == null)
      throw new IllegalArgumentException("Property `%s` is required".formatted(value.path()));
    return text;
  }

  /**
   * Accepts what 2.42 (Jackson) accepted for a boolean: JSON booleans, the strings {@code true} and
   * {@code false} (any case), the numbers {@code 1} and {@code 0}, and an empty string as null.
   *
   * @return the boolean value or null if the value is undefined, JSON null or an empty string
   * @throws IllegalArgumentException for any other value
   */
  @CheckForNull
  private static Boolean bool(@Nonnull JsonValue value) {
    if (value.isUndefined() || value.isNull()) return null;
    if (value.isBoolean()) return value.as(JsonBoolean.class).booleanValue();
    if (value.isString()) {
      String str = value.as(JsonString.class).string();
      if (str.isEmpty()) return null;
      if ("true".equalsIgnoreCase(str)) return true;
      if ("false".equalsIgnoreCase(str)) return false;
    }
    if (value.isNumber()) {
      String num = value.toJson();
      if ("1".equals(num)) return true;
      if ("0".equals(num)) return false;
    }
    throw new IllegalArgumentException(
        "Property `%s` must be a boolean but was: %s".formatted(value.path(), value.toJson()));
  }

  /**
   * @return a category to option map if the value is an object, otherwise null
   */
  @CheckForNull
  private static Map<String, String> textMap(@Nonnull JsonValue value) {
    if (!value.isObject()) return null;
    Map<String, String> res = new LinkedHashMap<>();
    for (JsonMixed e : value.asObject().entries()) res.put(e.getKey().toString(), text(e));
    return res;
  }

  @Nonnull
  private static List<String> textList(@Nonnull JsonValue value) {
    return elements(value).stream().map(DataEntryInput::requiredText).toList();
  }

  /**
   * @return the elements of an array (iterated lazily), none if the value is undefined or JSON null
   * @throws IllegalArgumentException if the value is not an array
   */
  @Nonnull
  private static Streamable<? extends JsonValue> elements(@Nonnull JsonValue value) {
    if (value.isUndefined() || value.isNull()) return Streamable.empty();
    if (!value.isArray())
      throw new IllegalArgumentException(
          "Property `%s` must be an array but was: %s".formatted(value.path(), value.type()));
    // SKIP: large arrays are iterated once, an index would only cost memory
    return value.asArray().values(Index.SKIP);
  }

  /**
   * @return the first of the given properties that is defined (not undefined), or the last one
   */
  @Nonnull
  private static JsonValue firstDefined(@Nonnull JsonObject obj, @Nonnull String... names) {
    for (int i = 0; i < names.length - 1; i++) {
      JsonValue value = obj.get(names[i]);
      if (!value.isUndefined()) return value;
    }
    return obj.get(names[names.length - 1]);
  }

  @Nonnull
  private static JsonObject object(@Nonnull JsonValue value) {
    if (!value.isObject())
      throw new IllegalArgumentException(
          "Property `%s` must be an object but was: %s".formatted(value.path(), value.type()));
    return value.asObject();
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
