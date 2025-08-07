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

import static org.hisp.dhis.feedback.DataEntrySummary.toConflict;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.RecordingJobProgress;
import org.hisp.dhis.setting.SystemSettingsProvider;
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
  private final SystemSettingsProvider settings;

  public ImportSummary importXml(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing XML data");
    return importData(
        progress.runStage(() -> DataEntryInput.fromDfxXml(in, options)), options, progress);
  }

  public ImportSummary importAdx(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing XML data");
    return importData(
        progress.runStage(() -> DataEntryInput.fromAdxXml(in, options)), options, progress);
  }

  public ImportSummary importPdf(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing PDF data");
    return importData(progress.runStage(() -> DataEntryInput.fromPdf(in, options)), options, progress);
  }

  public ImportSummary importCsv(InputStream in) {
    return importCsv(in, new ImportOptions(), RecordingJobProgress.transitory());
  }

  public ImportSummary importCsv(InputStream in, ImportOptions options, JobProgress progress) {
    if (!options.isFirstRowIsHeader())
      throw new UnsupportedOperationException("CSV without header row is no longer supported.");
    progress.startingStage("Deserializing CVS data");
    return importData(progress.runStage(() -> DataEntryInput.fromCsv(in, options)), options, progress);
  }

  public ImportSummary importJson(InputStream in, ImportOptions options, JobProgress progress) {
    progress.startingStage("Deserializing JSON data");
    return importData(
        progress.runStage(() -> DataEntryInput.fromJson(in, options)), options, progress);
  }

  @Nonnull
  public ImportSummary importData(
      @CheckForNull List<DataEntryGroup.Input> inputs,
      @Nonnull ImportOptions options,
      @Nonnull JobProgress progress) {
    // when parsing fails the input is null, this forces abort because of failed stage before
    inputs = progress.nonNullStagePostCondition(inputs);

    try {
      ImportSummary summary = importAutoSplitAndMerge(inputs, options, progress);
      summary.setImportOptions(options);
      return summary;
    } catch (BadRequestException | ConflictException ex) {
      ImportSummary summary = new ImportSummary(ImportStatus.ERROR);
      summary.addConflict(toConflict(IntStream.of(-1), ex.getCode(), ex.getArgs()));
      return summary;
    }
  }

  /**
   * Values not belonging to a single DS must be split into groups per DS but groups belonging to
   * the same DS should be merged into a single group.
   */
  private ImportSummary importAutoSplitAndMerge(
      List<DataEntryGroup.Input> inputs, ImportOptions options, JobProgress progress)
      throws BadRequestException, ConflictException {
    List<DataEntryGroup> groups = new ArrayList<>();
    for (DataEntryGroup.Input input : inputs) {
      progress.startingStage("Decoding group " + input.describe());
      progress.nonNullStagePostCondition(
          groups.add(
              progress.runStageAndRethrow(
                  BadRequestException.class, () -> service.decodeGroup(input))));
    }

    List<DataEntryGroup> splitGroups = groups;
    if (settings.getCurrentSettings().getDataEntryAutoGroup()) {
      splitGroups = new ArrayList<>();
      for (DataEntryGroup g : groups) {
        if (g.dataSet() == null) {
          progress.startingStage("Splitting group " + g.describe());
          splitGroups.addAll(
              progress.nonNullStagePostCondition(
                  progress.runStageAndRethrow(
                      ConflictException.class, () -> service.splitGroup(g))));
        } else {
          splitGroups.add(g);
        }
      }
    }

    List<DataEntryGroup> mergedGroups = splitGroups;
    if (mergedGroups.size() > 1) {
      List<DataEntryGroup> preMerge = splitGroups;
      progress.startingStage("Merging same dataset groups");
      mergedGroups =
          progress.nonNullStagePostCondition(progress.runStage(() -> mergeGroups(preMerge)));
    }

    DataEntryGroup.Options opt =
        new DataEntryGroup.Options(options.isDryRun(), options.isAtomic(), options.isForce());
    return importGroups(mergedGroups, opt, progress, options.getImportStrategy().isDelete());
  }

  /**
   * Merges groups of same dataset for best performance (fewer larger groups are faster) but only
   * merge in-order to maintain overall order of values
   */
  private List<DataEntryGroup> mergeGroups(List<DataEntryGroup> groups) {
    if (groups.size() < 2) return groups;
    List<DataEntryGroup> merged = new ArrayList<>(groups.size()); // assume no merge
    DataEntryGroup g1 = groups.get(0);
    for (int i = 1; i < groups.size(); i++) {
      DataEntryGroup g2 = groups.get(i);
      if (g1.dataSet() != null && g1.dataSet().equals(g2.dataSet())) {
        if (!(g1.values() instanceof ArrayList<DataEntryValue>))
          g1 = new DataEntryGroup(g1.dataSet(), new ArrayList<>(g1.values()));
        g1.values().addAll(g2.values());
      } else {
        merged.add(g1);
        g1 = g2;
      }
    }
    merged.add(g1);
    return merged;
  }

  @Nonnull
  private ImportSummary importGroups(
      List<DataEntryGroup> groups,
      DataEntryGroup.Options options,
      JobProgress progress,
      boolean delete) {
    DataEntrySummary summary = new DataEntrySummary(0, 0, 0, List.of());
    List<ImportConflict> conflicts = new ArrayList<>();
    for (DataEntryGroup g : groups) {
      try {
        // further stages happen within the service method...
        DataEntrySummary res =
            delete
                ? service.deleteGroup(options, g, progress)
                : service.upsertGroup(options, g, progress);
        summary = summary.add(res);
      } catch (ConflictException ex) {
        conflicts.add(
            toConflict(
                g.values().stream().mapToInt(DataEntryValue::index), ex.getCode(), ex.getArgs()));
        if (options.atomic()) return toImportSummary(summary, delete, conflicts);
      }
    }
    return toImportSummary(summary, delete, conflicts);
  }

  @Nonnull
  private static ImportSummary toImportSummary(
      DataEntrySummary summary, boolean delete, List<ImportConflict> conflicts) {
    ImportSummary res = summary.toImportSummary();
    if (delete) {
      ImportCount counts = res.getImportCount();
      counts.setDeleted(counts.getUpdated());
      counts.setUpdated(0);
    }
    if (!conflicts.isEmpty()) {
      res.setStatus(ImportStatus.ERROR);
      conflicts.forEach(res::addConflict);
      conflicts.forEach(c -> res.addRejected(c.getIndexes()));
    }
    return res;
  }
}
