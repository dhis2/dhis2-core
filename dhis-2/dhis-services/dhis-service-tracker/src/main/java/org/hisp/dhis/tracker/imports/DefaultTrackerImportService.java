/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preprocess.TrackerPreprocessService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.tracker.imports.validation.ValidationResult;
import org.hisp.dhis.tracker.imports.validation.ValidationService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTrackerImportService implements TrackerImportService {
  @Nonnull private final TrackerBundleService trackerBundleService;

  @Nonnull private final ValidationService validationService;

  @Nonnull private final TrackerPreprocessService trackerPreprocessService;

  @Nonnull private final TrackerUserService trackerUserService;

  /* Import is not meant to be annotated with @Transactional.
   * PreHeat and Commit phases are separated transactions, other
   * phases do not need to be in a transaction. */
  @Override
  public ImportReport importTracker(
      TrackerImportParams params, TrackerObjects trackerObjects, JobProgress jobProgress) {
    User user = trackerUserService.getUser(params.getUserId());

    jobProgress.startingStage("Running PreHeat");
    TrackerBundle trackerBundle =
        jobProgress.runStage(() -> trackerBundleService.create(params, trackerObjects, user));

    jobProgress.startingStage("Calculating Payload Size");
    Map<TrackerType, Integer> bundleSize =
        jobProgress.runStage(() -> calculatePayloadSize(trackerBundle));

    jobProgress.startingStage("Running PreProcess");
    jobProgress.runStage(() -> trackerPreprocessService.preprocess(trackerBundle));

    jobProgress.startingStage("Running Validation");
    ValidationResult validationResult = jobProgress.runStage(() -> validateBundle(trackerBundle));

    ValidationReport validationReport = ValidationReport.fromResult(validationResult);

    if (!trackerBundle.isSkipRuleEngine() && !params.getImportStrategy().isDelete()) {
      jobProgress.startingStage("Running Rule Engine");
      jobProgress.runStage(() -> trackerBundleService.runRuleEngine(trackerBundle));

      jobProgress.startingStage("Running Rule Engine Validation");
      ValidationResult result =
          jobProgress.runStage(() -> validationService.validateRuleEngine(trackerBundle));
      trackerBundle.setTrackedEntities(result.getTrackedEntities());
      trackerBundle.setEnrollments(result.getEnrollments());
      trackerBundle.setEvents(result.getEvents());
      trackerBundle.setRelationships(result.getRelationships());

      validationReport = ValidationReport.merge(validationResult, result);
    }

    if (exitOnError(validationReport, params)) {
      return ImportReport.withValidationErrors(
          validationReport, bundleSize.values().stream().mapToInt(Integer::intValue).sum());
    }

    jobProgress.startingStage("Commit Transaction");
    PersistenceReport persistenceReport = jobProgress.runStage(() -> commit(params, trackerBundle));

    jobProgress.startingStage("PostCommit");
    jobProgress.runStage(() -> trackerBundleService.postCommit(trackerBundle));

    return ImportReport.withImportCompleted(
        Status.OK, persistenceReport, validationReport, bundleSize);
  }

  private PersistenceReport commit(TrackerImportParams params, TrackerBundle trackerBundle) {
    if (TrackerImportStrategy.DELETE == params.getImportStrategy()) {
      return deleteBundle(trackerBundle);
    } else {
      return commitBundle(trackerBundle);
    }
  }

  protected ValidationResult validateBundle(TrackerBundle bundle) {
    ValidationResult result = validationService.validate(bundle);
    bundle.setTrackedEntities(result.getTrackedEntities());
    bundle.setEnrollments(result.getEnrollments());
    bundle.setEvents(result.getEvents());
    bundle.setRelationships(result.getRelationships());

    return result;
  }

  private boolean exitOnError(ValidationReport validationReport, TrackerImportParams params) {
    return validationReport.hasErrors() && params.getAtomicMode() == AtomicMode.ALL;
  }

  private Map<TrackerType, Integer> calculatePayloadSize(TrackerBundle bundle) {
    return Map.of(
        TrackerType.TRACKED_ENTITY, bundle.getTrackedEntities().size(),
        TrackerType.ENROLLMENT, bundle.getEnrollments().size(),
        TrackerType.EVENT, bundle.getEvents().size(),
        TrackerType.RELATIONSHIP, bundle.getRelationships().size());
  }

  protected PersistenceReport commitBundle(TrackerBundle trackerBundle) {
    PersistenceReport persistenceReport = trackerBundleService.commit(trackerBundle);

    if (!trackerBundle.isSkipSideEffects()) {
      List<TrackerSideEffectDataBundle> sideEffectDataBundles =
          Stream.of(TrackerType.ENROLLMENT, TrackerType.EVENT)
              .map(trackerType -> safelyGetSideEffectsDataBundles(persistenceReport, trackerType))
              .flatMap(Collection::stream)
              .toList();

      trackerBundleService.handleTrackerSideEffects(sideEffectDataBundles);
    }

    return persistenceReport;
  }

  private List<TrackerSideEffectDataBundle> safelyGetSideEffectsDataBundles(
      PersistenceReport persistenceReport, TrackerType trackerType) {
    return Optional.ofNullable(persistenceReport)
        .map(PersistenceReport::getTypeReportMap)
        .map(reportMap -> reportMap.get(trackerType))
        .map(TrackerTypeReport::getSideEffectDataBundles)
        .orElse(Collections.emptyList());
  }

  protected PersistenceReport deleteBundle(TrackerBundle trackerBundle) {
    return trackerBundleService.delete(trackerBundle);
  }

  /**
   * Clone the TrackerImportReport and filters out validation data based on the provided {@link
   * PersistenceReport}.
   *
   * @return a copy of the current TrackerImportReport
   */
  @Override
  public ImportReport buildImportReport(
      ImportReport originalImportReport, TrackerBundleReportMode reportMode) {
    ImportReport.ImportReportBuilder importReportBuilder =
        ImportReport.builder()
            .status(originalImportReport.getStatus())
            .stats(originalImportReport.getStats())
            .persistenceReport(originalImportReport.getPersistenceReport())
            .message(originalImportReport.getMessage());

    ValidationReport originalValidationReport = originalImportReport.getValidationReport();
    ValidationReport validationReport = ValidationReport.emptyReport();
    if (originalValidationReport != null) {
      validationReport.addErrors(originalValidationReport.getErrors());
    }
    if (originalValidationReport != null
        && (TrackerBundleReportMode.WARNINGS == reportMode
            || TrackerBundleReportMode.FULL == reportMode)) {
      validationReport.addWarnings(originalValidationReport.getWarnings());
    }

    importReportBuilder.validationReport(validationReport);

    return importReportBuilder.build();
  }
}
