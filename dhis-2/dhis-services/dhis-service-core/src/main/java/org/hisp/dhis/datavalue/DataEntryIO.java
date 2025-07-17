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
import static org.hisp.dhis.feedback.DataEntrySummary.toConflict;

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.csv.CSV;
import org.hisp.dhis.dxf2.adx.AdxPeriod;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.staxwax.factory.XMLFactory;
import org.hisp.staxwax.reader.XMLReader;
import org.springframework.stereotype.Component;

/**
 * Handles the input and output format and exception transformations for data entry between the
 * controller and the service layer.
 *
 * <p>Transactions are opened and closed by {@link DataEntryService} so that each {@link
 * DataEntryGroup} is handled in its on transaction context to keep the transactions smaller in
 * scope.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@Component
@RequiredArgsConstructor
public class DataEntryIO {

  private final DataEntryService service;

  public ImportSummary importAdx(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing ADX data");
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(options.getIdSchemes());
    return importRaw(progress.runStage(() -> parseAdx(in, ids)), options, progress);
  }

  @Nonnull
  private static List<DataEntryGroup.Input> parseAdx(InputStream in, DataEntryGroup.Ids ids)
      throws IOException {
    XMLReader dvs = XMLFactory.getXMLReader(wrapAndCheckCompressionFormat(in));
    String ns = "urn:ihe:qrph:adx:2015";
    dvs.moveToStartElement("adx", ns);
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
        values.add(
            new DataEntryValue.Input(
                de, null, coc, co, null, null, value, comment, followup, deleted));
      }
      res.add(new DataEntryGroup.Input(ids, ds, null, ou, pe, aoc, aco, values));
    }
    return res;
  }

  public ImportSummary importPdf(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing PDF data");
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(options.getIdSchemes());
    return importRaw(progress.runStage(() -> parsePdf(in, ids)), options, progress);
  }

  @Nonnull
  private static List<DataEntryGroup.Input> parsePdf(InputStream in, DataEntryGroup.Ids ids)
      throws IOException {
    PdfReader dvs = new PdfReader(in);
    AcroFields form = dvs.getAcroFields();
    if (form == null) throw new IllegalArgumentException("PDF has no Acro fields");
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
            new DataEntryValue.Input(de, null, coc, null, null, null, value, null, null, null));
      }
    }
    return List.of(new DataEntryGroup.Input(ids, null, null, ou, pe, null, null, values));
  }

  public ImportSummary importCsv(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing CVS data");
    return importRaw(progress.runStage(() -> parseCsv(in, options)), options, progress);
  }

  @Nonnull
  private static List<DataEntryGroup.Input> parseCsv(InputStream in, ImportOptions options)
      throws IOException {
    // TODO maybe handle firstRowIsHeader=false by specifying a default header?
    List<DataEntryValue.Input> values =
        CSV.of(wrapAndCheckCompressionFormat(in)).as(DataEntryValue.Input.class).list();
    String ds = options.getDataSet();
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(options.getIdSchemes());
    return List.of(new DataEntryGroup.Input(ids, ds, values));
  }

  public ImportSummary importXml(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing CVS data");
    return importRaw(
        progress.runStage(() -> parseXml(in, options, options.getIdSchemes())), options, progress);
  }

  @Nonnull
  private static List<DataEntryGroup.Input> parseXml(
      InputStream in, ImportOptions options, IdSchemes schemes) throws IOException {
    XMLReader dvs = XMLFactory.getXMLReader(wrapAndCheckCompressionFormat(in));
    dvs.moveToStartElement("dataValueSet");
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
              dvs.getAttributeValue("period"),
              dvs.getAttributeValue("value"),
              dvs.getAttributeValue("comment"),
              followUp == null ? null : parseBoolean(followUp),
              deleted == null ? null : parseBoolean(deleted)));
    }
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(schemes);
    return List.of(new DataEntryGroup.Input(ids, ds, null, ou, pe, aoc, null, values));
  }

  public ImportSummary importJson(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing JSON data");
    return importRaw(
        progress.runStage(() -> parseJson(in, options, options.getIdSchemes())), options, progress);
  }

  @Nonnull
  private static List<DataEntryGroup.Input> parseJson(
      InputStream in, ImportOptions options, IdSchemes schemes) throws IOException {
    JsonObject dvs =
        JsonValue.of(new InputStreamReader(wrapAndCheckCompressionFormat(in))).asObject();
    String ds = dvs.getString("dataSet").string();
    // keys that are common for all values
    String ou = dvs.getString("orgUnit").string();
    String pe = dvs.getString("period").string();
    String aoc = dvs.getString("attributeOptionCombo").string();
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
    dvs.get("dataValues")
        .node()
        .elements(false)
        .forEachRemaining(
            node -> {
              JsonObject dv = JsonMixed.of(node);
              values.add(
                  new DataEntryValue.Input(
                      dv.getString("dataElement").string(),
                      dv.getString("orgUnit").string(),
                      dv.getString("categoryOptionCombo").string(),
                      null,
                      dv.getString("attributeOptionCombo").string(),
                      dv.getString("period").string(),
                      dv.getString("value").string(),
                      dv.getString("comment").string(),
                      dv.getBoolean("followUp").bool(),
                      dv.getBoolean("deleted").bool()));
            });
    DataEntryGroup.Ids ids = DataEntryGroup.Ids.of(schemes);
    return List.of(new DataEntryGroup.Input(ids, ds, null, ou, pe, aoc, null, values));
  }

  @Nonnull
  public ImportSummary importRaw(
      List<DataEntryGroup.Input> inputs, ImportOptions options, JobProgress progress) {
    // when parsing fails the input is null, this forces abort because of failed stage before
    progress.nonNullStagePostCondition(inputs);

    try {
      ImportSummary summary = importRawUnsafe(inputs, options, progress);
      summary.setImportOptions(options);
      return summary;
    } catch (BadRequestException | ConflictException ex) {
      ImportSummary summary = new ImportSummary(ImportStatus.ERROR);
      summary.addConflict(toConflict(IntStream.of(-1), ex.getCode(), ex.getArgs()));
      return summary;
    }
  }

  private ImportSummary importRawUnsafe(
      List<DataEntryGroup.Input> inputs, ImportOptions options, JobProgress progress)
      throws BadRequestException, ConflictException {
    List<DataEntryGroup> groups = new ArrayList<>();
    for (DataEntryGroup.Input input : inputs) {
      progress.startingStage("Resolving " + input.describe());
      groups.add(
          progress.runStageAndRethrow(BadRequestException.class, () -> service.decodeGroup(input)));
    }

    List<DataEntryGroup> dsGroups = groups;
    if (options.isGroup()) {
      dsGroups = new ArrayList<>();
      for (DataEntryGroup g : groups) {
        if (g.dataSet() == null) {
          progress.startingStage("Grouping " + g.describe());
          dsGroups.addAll(
              progress.runStageAndRethrow(ConflictException.class, () -> service.splitGroup(g)));
        } else {
          dsGroups.add(g);
        }
      }
    }

    DataEntryGroup.Options opt =
        new DataEntryGroup.Options(options.isDryRun(), options.isAtomic(), options.isForce());

    return importGroups(dsGroups, opt, progress);
  }

  @Nonnull
  private ImportSummary importGroups(
      List<DataEntryGroup> groups, DataEntryGroup.Options options, JobProgress progress) {
    DataEntrySummary summary = new DataEntrySummary(0, 0, List.of());
    List<ImportConflict> conflicts = new ArrayList<>();
    for (DataEntryGroup g : groups) {
      try {
        // further stages happen within the service method...
        summary = summary.add(service.upsertGroup(options, g, progress));
      } catch (ConflictException ex) {
        conflicts.add(
            toConflict(
                g.values().stream().mapToInt(DataEntryValue::index), ex.getCode(), ex.getArgs()));
      }
    }
    ImportSummary res = summary.toImportSummary();
    if (!conflicts.isEmpty()) {
      res.setStatus(ImportStatus.ERROR);
      conflicts.forEach(res::addConflict);
      conflicts.forEach(c -> res.addRejected(c.getIndexes()));
    }
    return res;
  }
}
