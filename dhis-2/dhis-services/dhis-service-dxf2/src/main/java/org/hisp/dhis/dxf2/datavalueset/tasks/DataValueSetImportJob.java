/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.datavalueset.tasks;

import static org.hisp.dhis.commons.util.StreamUtils.wrapAndCheckCompressionFormat;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.csv.CSV;
import org.hisp.dhis.datavalue.DataEntryGroup;
import org.hisp.dhis.datavalue.DataEntryService;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.dxf2.adx.AdxDataService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

/**
 * @author Jan Bernitt
 */
@Component
@RequiredArgsConstructor
public class DataValueSetImportJob implements Job {

  private final FileResourceService fileResourceService;
  private final DataValueSetService dataValueSetService;
  private final AdxDataService adxDataService;
  private final Notifier notifier;
  private final DataEntryService dataEntryService;
  private final ObjectMapper jsonMapper;

  @Override
  public JobType getJobType() {
    return JobType.DATAVALUE_IMPORT;
  }

  @Override
  public void execute(JobConfiguration jobId, JobProgress progress) {
    progress.startingProcess("Data value set import");
    ImportOptions options = (ImportOptions) jobId.getJobParameters();
    progress.startingStage("Loading file resource");
    FileResource data =
        progress.nonNullStagePostCondition(
            progress.runStage(() -> fileResourceService.getFileResource(jobId.getUid())));
    progress.startingStage("Loading file content");
    try (InputStream input =
        progress.runStage(() -> fileResourceService.getFileResourceContent(data))) {
      String contentType = data.getContentType();
      boolean unknownFormat = false;
      ImportSummary summary =
          switch (contentType) {
            case "application/json" -> importDataValueSetJson(input, options, progress);
            case "application/csv" -> importDataValueSetCsv(input, options, progress);
            case "application/pdf" ->
                dataValueSetService.importDataValueSetPdf(input, options, progress);
            case "application/xml" ->
                dataValueSetService.importDataValueSetXml(input, options, progress);
            case "application/adx+xml" -> adxDataService.saveDataValueSet(input, options, progress);
            default -> {
              unknownFormat = true;
              yield null;
            }
          };
      if (summary == null) {
        String error =
            unknownFormat
                ? "Unknown format: " + contentType
                : "Import failed, no summary available";
        progress.failedProcess(error);
        return;
      }

      if (summary.hasConflicts()) {
        for (ImportConflict c : summary.getConflicts())
          progress.addError(c.getErrorCode(), c.getObject(), c.getGroupingKey(), c.getArgs());
      }

      ImportCount count = summary.getImportCount();
      progress.completedProcess(
          "Import complete with status {}, {} created, {} updated, {} deleted, {} ignored",
          summary.getStatus(),
          count.getImported(),
          count.getUpdated(),
          count.getDeleted(),
          count.getIgnored());

      NotificationLevel level = options == null ? INFO : options.getNotificationLevel(INFO);
      notifier.addJobSummary(jobId, level, summary, ImportSummary.class);
    } catch (Exception ex) {
      progress.failedProcess(ex);
      // TODO add error summary
    }
  }

  private ImportSummary importDataValueSetCsv(
      InputStream input, ImportOptions options, JobProgress progress) throws BadRequestException {
    if (options.isGroup())
      return dataValueSetService.importDataValueSetCsv(input, options, progress);

    // TODO maybe handle firstRowIsHeader=false by specifying a default header?
    progress.startingStage("Deserializing data values");
    List<DataEntryValue.Input> values =
        progress.nonNullStagePostCondition(
            progress.runStage(
                () ->
                    CSV.of(wrapAndCheckCompressionFormat(input))
                        .as(DataEntryValue.Input.class)
                        .list()));
    String ds = options.getDataSet();
    DataEntryGroup.Input request = new DataEntryGroup.Input(ds, null, null, null, null, values);

    ImportSummary summary = importDataValues(request, options, progress);
    summary.setImportOptions(options);
    return summary;
  }

  private ImportSummary importDataValueSetJson(
      InputStream in, ImportOptions options, JobProgress progress) throws BadRequestException {
    if (options.isGroup()) return dataValueSetService.importDataValueSetJson(in, options, progress);

    progress.startingStage("Deserializing data values");
    DataEntryGroup.Input input =
        progress.nonNullStagePostCondition(
            progress.runStage(
                () ->
                    jsonMapper.readValue(
                        wrapAndCheckCompressionFormat(in), DataEntryGroup.Input.class)));

    // further stages happen within the service method...
    ImportSummary summary = importDataValues(input, options, progress);
    summary.setImportOptions(options);
    return summary;
  }

  @Nonnull
  private ImportSummary importDataValues(
      DataEntryGroup.Input input, ImportOptions options, JobProgress progress)
      throws BadRequestException {

    DataEntryGroup.Identifiers identifiers = DataEntryGroup.Identifiers.of(options.getIdSchemes());

    progress.startingStage("Resolving %d data values".formatted(input.values().size()));
    DataEntryGroup group =
        progress.runStageAndRethrow(
            BadRequestException.class, () -> dataEntryService.decode(input, identifiers));

    DataEntryGroup.Options opt =
        new DataEntryGroup.Options(options.isDryRun(), options.isAtomic(), options.isForce());

    if (!options.isGroup() || group.dataSet() != null)
      return importDataValues(List.of(group), opt, progress);

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
                () -> dataEntryService.groupByDataSet(group)));

    return importDataValues(groups, opt, progress);
  }

  @Nonnull
  private ImportSummary importDataValues(
      List<DataEntryGroup> groups, DataEntryGroup.Options options, JobProgress progress) {
    DataEntrySummary summary = new DataEntrySummary(0, 0, List.of());
    List<Integer> rejectedNoSummary = new ArrayList<>();
    for (DataEntryGroup g : groups) {
      try {
        // further stages happen within the service method...
        summary = summary.add(dataEntryService.upsertDataValueGroup(options, g, progress));
      } catch (ConflictException ex) {
        rejectedNoSummary.addAll(g.values().stream().map(DataEntryValue::index).toList());
      }
    }
    ImportSummary res = summary.toImportSummary();
    res.getRejectedIndexes().addAll(rejectedNoSummary);
    if (!rejectedNoSummary.isEmpty()) res.setStatus(ImportStatus.ERROR);
    return res;
  }
}
