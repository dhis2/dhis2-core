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

import static org.hisp.dhis.commons.util.StreamUtils.wrapAndCheckCompressionFormat;
import static org.hisp.dhis.feedback.DataEntrySummary.toConflict;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
import org.hisp.dhis.scheduling.JobProgress;
import org.springframework.stereotype.Component;

/**
 * Handles the data entry process orchestrating the calls to the {@link DataEntryService} without
 * being transactional. This happens between the controller layer and the service layer.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@Component
@RequiredArgsConstructor
public class DataEntryProcessor {

  private final DataEntryService service;
  private final ObjectMapper jsonMapper;

  public ImportSummary importDataValueSetCsv(
      InputStream in, ImportOptions options, JobProgress progress) {

    // TODO maybe handle firstRowIsHeader=false by specifying a default header?
    progress.startingStage("Deserializing data values");
    List<DataEntryValue.Input> values =
        progress.nonNullStagePostCondition(
            progress.runStage(
                () ->
                    CSV.of(wrapAndCheckCompressionFormat(in))
                        .as(DataEntryValue.Input.class)
                        .list()));
    String ds = options.getDataSet();
    DataEntryGroup.Input input = new DataEntryGroup.Input(ds, null, null, null, null, values);

    return importDataValueSetGroup(input, options, progress);
  }

  public ImportSummary importDataValueSetJson(
      InputStream in, ImportOptions options, JobProgress progress) {

    progress.startingStage("Deserializing data values");
    DataEntryGroup.Input input =
        progress.nonNullStagePostCondition(
            progress.runStage(
                () ->
                    jsonMapper.readValue(
                        wrapAndCheckCompressionFormat(in), DataEntryGroup.Input.class)));

    return importDataValueSetGroup(input, options, progress);
  }

  @Nonnull
  public ImportSummary importDataValueSetGroup(
      DataEntryGroup.Input input, ImportOptions options, JobProgress progress) {
    try {
      ImportSummary summary = importDataValueSet(input, options, progress);
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

  private ImportSummary importDataValueSet(
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
      return importDataValueSet(List.of(group), opt, progress);

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

    return importDataValueSet(groups, opt, progress);
  }

  @Nonnull
  private ImportSummary importDataValueSet(
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
