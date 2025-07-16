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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.csv.CSV;
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

  public ImportSummary importPdf(InputStream in, ImportOptions options, JobProgress progress) {

    progress.startingStage("Deserializing PDF data");
    DataEntryGroup.Input input =
        progress.nonNullStagePostCondition(
            progress.runStage(
                () -> {
                  PdfReader dvs = new PdfReader(in);
                  AcroFields form = dvs.getAcroFields();
                  if (form == null) throw new IllegalArgumentException("PDF has no Acro fields");
                  String ou = form.getField("TXFD_OrgID").trim();
                  String pe = form.getField("TXFD_PeriodID").trim();
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
                          new DataEntryValue.Input(de, null, coc, null, null, value, null, null));
                    }
                  }
                  return new DataEntryGroup.Input(null, null, ou, pe, null, values);
                }));
    return importRaw(input, options, progress);
  }

  public ImportSummary importXml(InputStream in, ImportOptions options, JobProgress progress) {

    progress.startingStage("Deserializing CVS data");
    DataEntryGroup.Input input =
        progress.nonNullStagePostCondition(
            progress.runStage(
                () -> {
                  XMLReader dvs = XMLFactory.getXMLReader(wrapAndCheckCompressionFormat(in));
                  dvs.moveToStartElement("dataValueSet");
                  String ds = dvs.getAttributeValue("dataSet");
                  String ou = dvs.getAttributeValue("orgUnit");
                  String pe = dvs.getAttributeValue("period");
                  String aoc = dvs.getAttributeValue("attributeOptionCombo");
                  String dryRun = dvs.getAttributeValue("dryRun");
                  if (dryRun != null) options.setDryRun(parseBoolean(dryRun));
                  // TODO ID scheme
                  List<DataEntryValue.Input> values = new ArrayList<>();
                  while (dvs.moveToStartElement("dataValue", "dataValueSet")) {
                    String followUp = dvs.getAttributeValue("followUp");
                    values.add(
                        new DataEntryValue.Input(
                            dvs.getAttributeValue("dataElement"),
                            dvs.getAttributeValue("orgUnit"),
                            dvs.getAttributeValue("categoryOptionCombo"),
                            dvs.getAttributeValue("attributeOptionCombo"),
                            dvs.getAttributeValue("period"),
                            dvs.getAttributeValue("value"),
                            dvs.getAttributeValue("comment"),
                            followUp == null ? null : parseBoolean(followUp)));
                  }
                  return new DataEntryGroup.Input(ds, null, ou, pe, aoc, values);
                }));
    return importRaw(input, options, progress);
  }

  public ImportSummary importCsv(InputStream in, ImportOptions options, JobProgress progress) {

    // TODO maybe handle firstRowIsHeader=false by specifying a default header?
    progress.startingStage("Deserializing CVS data");
    List<DataEntryValue.Input> values =
        progress.nonNullStagePostCondition(
            progress.runStage(
                () ->
                    CSV.of(wrapAndCheckCompressionFormat(in))
                        .as(DataEntryValue.Input.class)
                        .list()));
    String ds = options.getDataSet();
    DataEntryGroup.Input input = new DataEntryGroup.Input(ds, null, null, null, null, values);

    return importRaw(input, options, progress);
  }

  public ImportSummary importJson(InputStream in, ImportOptions options, JobProgress progress) {

    progress.startingStage("Deserializing JSON data");
    DataEntryGroup.Input input =
        progress.nonNullStagePostCondition(
            progress.runStage(
                () -> {
                  JsonObject dvs =
                      JsonValue.of(new InputStreamReader(wrapAndCheckCompressionFormat(in)))
                          .asObject();
                  String ds = dvs.getString("dataSet").string();
                  String ou = dvs.getString("orgUnit").string();
                  String pe = dvs.getString("period").string();
                  String aoc = dvs.getString("attributeOptionCombo").string();
                  Boolean dryRun = dvs.getBoolean("dryRun").bool();
                  if (dryRun != null) options.setDryRun(dryRun);
                  // TODO ID scheme
                  List<DataEntryValue.Input> values = new ArrayList<>();
                  // this uses JsonNode API to iterate without indexing
                  // to make the memory footprint smaller
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
                                    dv.getString("attributeOptionCombo").string(),
                                    dv.getString("period").string(),
                                    dv.getString("value").string(),
                                    dv.getString("comment").string(),
                                    dv.getBoolean("followUp").bool()));
                          });
                  return new DataEntryGroup.Input(ds, null, ou, pe, aoc, values);
                }));
    return importRaw(input, options, progress);
  }

  @Nonnull
  public ImportSummary importRaw(
      DataEntryGroup.Input input, ImportOptions options, JobProgress progress) {
    try {
      ImportSummary summary = importRawUnsafe(input, options, progress);
      summary.setImportOptions(options);
      return summary;
    } catch (BadRequestException ex) {
      ImportSummary summary = new ImportSummary(ImportStatus.ERROR);
      ImportConflict c =
          toConflict(IntStream.range(0, input.values().size()), ex.getCode(), ex.getArgs());
      summary.addConflict(c);
      summary.addRejected(c.getIndexes());
      return summary;
    }
  }

  private ImportSummary importRawUnsafe(
      DataEntryGroup.Input input, ImportOptions options, JobProgress progress)
      throws BadRequestException {

    DataEntryGroup.Identifiers identifiers = DataEntryGroup.Identifiers.of(options.getIdSchemes());

    progress.startingStage("Resolving %d data values".formatted(input.values().size()));
    DataEntryGroup group =
        progress.runStageAndRethrow(
            BadRequestException.class, () -> service.decode(input, identifiers));

    DataEntryGroup.Options opt =
        new DataEntryGroup.Options(options.isDryRun(), options.isAtomic(), options.isForce());

    if (!options.isGroup() || group.dataSet() != null)
      return importGroups(List.of(group), opt, progress);

    progress.startingStage(
        "Grouping %d values by target data set".formatted(group.values().size()));
    List<DataEntryGroup> groups =
        progress.nonNullStagePostCondition(
            progress.runStage(
                null,
                res ->
                    "Grouped into %d groups targeting data sets %s"
                        .formatted(
                            res.size(),
                            res.stream()
                                .filter(g -> g.dataSet() != null)
                                .map(g -> g.dataSet().getValue())
                                .collect(Collectors.joining(","))),
                () -> service.groupByDataSet(group)));

    return importGroups(groups, opt, progress);
  }

  @Nonnull
  private ImportSummary importGroups(
      List<DataEntryGroup> groups, DataEntryGroup.Options options, JobProgress progress) {
    DataEntrySummary summary = new DataEntrySummary(0, 0, List.of());
    List<ImportConflict> conflicts = new ArrayList<>();
    for (DataEntryGroup g : groups) {
      try {
        // further stages happen within the service method...
        summary = summary.add(service.upsertDataValueGroup(options, g, progress));
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
