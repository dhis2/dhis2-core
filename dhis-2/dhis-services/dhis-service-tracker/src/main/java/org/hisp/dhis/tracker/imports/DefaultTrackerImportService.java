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
package org.hisp.dhis.tracker.imports;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.job.TrackerNotificationDataBundle;
import org.hisp.dhis.tracker.imports.preprocess.TrackerPreprocessService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.tracker.imports.validation.ValidationResult;
import org.hisp.dhis.tracker.imports.validation.ValidationService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@RequiredArgsConstructor
public class DefaultTrackerImportService implements TrackerImportService {
  @Nonnull private final TrackerBundleService trackerBundleService;

  @Nonnull private final ValidationService validationService;

  @Nonnull private final TrackerPreprocessService trackerPreprocessService;

  private PersistenceReport commit(TrackerImportParams params, TrackerBundle trackerBundle)
      throws ForbiddenException, NotFoundException {
    if (TrackerImportStrategy.DELETE == params.getImportStrategy()) {
      return deleteBundle(trackerBundle);
    } else {
      return commitBundle(trackerBundle);
    }
  }

  @Nonnull
  @Override
  @IndirectTransactional
  public ImportReport importTracker(
      @Nonnull TrackerImportParams params,
      @Nonnull TrackerObjects trackerObjects,
      @Nonnull JobProgress jobProgress) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    jobProgress.startingStage("Running PreHeat");
    TrackerBundle trackerBundle =
        jobProgress.nonNullStagePostCondition(
            jobProgress.runStage(
                () -> trackerBundleService.create(params, trackerObjects, currentUser)));

    jobProgress.startingStage("Calculating Payload Size");
    Map<TrackerType, Integer> bundleSize =
        jobProgress.nonNullStagePostCondition(
            jobProgress.runStage(() -> calculatePayloadSize(trackerBundle)));

    jobProgress.startingStage("Running PreProcess");
    jobProgress.runStage(() -> trackerPreprocessService.preprocess(trackerBundle));

    jobProgress.startingStage("Running Validation");
    ValidationResult validationResult =
        jobProgress.nonNullStagePostCondition(
            jobProgress.runStage(() -> validateBundle(trackerBundle)));

    ValidationReport validationReport = ValidationReport.fromResult(validationResult);

    if (!trackerBundle.isSkipRuleEngine() && !params.getImportStrategy().isDelete()) {
      jobProgress.startingStage("Running Rule Engine");
      jobProgress.runStage(() -> trackerBundleService.runRuleEngine(trackerBundle));

      jobProgress.startingStage("Running Rule Engine Validation");
      ValidationResult result =
          jobProgress.nonNullStagePostCondition(
              jobProgress.runStage(() -> validationService.validateRuleEngine(trackerBundle)));
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
    PersistenceReport persistenceReport =
        jobProgress.nonNullStagePostCondition(
            jobProgress.runStage(() -> commit(params, trackerBundle)));

    jobProgress.startingStage("PostCommit");
    jobProgress.runStage(() -> trackerBundleService.postCommit(trackerBundle));

    return ImportReport.withImportCompleted(
        Status.OK, persistenceReport, validationReport, bundleSize);
  }

  protected ValidationResult validateBundle(TrackerBundle bundle) {
    ValidationResult result = validationService.validate(bundle);
    bundle.setTrackedEntities(result.getTrackedEntities());
    bundle.setEnrollments(result.getEnrollments());
    bundle.setTrackerEvents(
        result.getEvents().stream()
            .filter(TrackerEvent.class::isInstance)
            .map(e -> (TrackerEvent) e)
            .toList());
    bundle.setSingleEvents(
        result.getEvents().stream()
            .filter(SingleEvent.class::isInstance)
            .map(e -> (SingleEvent) e)
            .toList());
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
      List<TrackerNotificationDataBundle> notificationDataBundles =
          Stream.of(TrackerType.ENROLLMENT, TrackerType.EVENT)
              .map(trackerType -> safelyGetNotificationDataBundles(persistenceReport, trackerType))
              .flatMap(Collection::stream)
              .toList();

      trackerBundleService.sendNotifications(notificationDataBundles);
    }

    return persistenceReport;
  }

  private List<TrackerNotificationDataBundle> safelyGetNotificationDataBundles(
      PersistenceReport persistenceReport, TrackerType trackerType) {
    return Optional.ofNullable(persistenceReport)
        .map(PersistenceReport::getTypeReportMap)
        .map(reportMap -> reportMap.get(trackerType))
        .map(TrackerTypeReport::getNotificationDataBundles)
        .orElse(Collections.emptyList());
  }

  protected PersistenceReport deleteBundle(TrackerBundle trackerBundle)
      throws ForbiddenException, NotFoundException {
    return trackerBundleService.delete(trackerBundle);
  }

  /**
   * Clone the TrackerImportReport and filters out validation data based on the provided {@link
   * PersistenceReport}.
   *
   * @return a copy of the current TrackerImportReport
   */
  @Nonnull
  @Override
  public ImportReport buildImportReport(
      @Nonnull ImportReport originalImportReport, @Nonnull TrackerBundleReportMode reportMode) {
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
