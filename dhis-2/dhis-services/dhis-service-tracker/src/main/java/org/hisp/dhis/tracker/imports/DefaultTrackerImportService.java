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

import static org.hisp.dhis.tracker.imports.report.TimingsStats.COMMIT_OPS;
import static org.hisp.dhis.tracker.imports.report.TimingsStats.PREHEAT_OPS;
import static org.hisp.dhis.tracker.imports.report.TimingsStats.PREPROCESS_OPS;
import static org.hisp.dhis.tracker.imports.report.TimingsStats.PROGRAMRULE_OPS;
import static org.hisp.dhis.tracker.imports.report.TimingsStats.TOTAL_OPS;
import static org.hisp.dhis.tracker.imports.report.TimingsStats.VALIDATE_PROGRAMRULE_OPS;
import static org.hisp.dhis.tracker.imports.report.TimingsStats.VALIDATION_OPS;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preprocess.TrackerPreprocessService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.TimingsStats;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.tracker.imports.validation.ValidationResult;
import org.hisp.dhis.tracker.imports.validation.ValidationService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  @Nonnull private final Notifier notifier;

  @Override
  @Transactional
  public ImportReport importTracker(TrackerImportParams params) {
    User user = trackerUserService.getUser(params.getUserId());
    params.setUser(user);

    TimingsStats opsTimer = new TimingsStats();

    startImport(params);

    try {
      TrackerBundle trackerBundle = preHeat(params, opsTimer);

      Map<TrackerType, Integer> bundleSize = calculatePayloadSize(trackerBundle);

      preProcess(opsTimer, trackerBundle);

      ValidationReport validationReport = validate(params, opsTimer, trackerBundle);

      if (exitOnError(validationReport, params)) {
        return buildReportAndNotify(params, validationReport, opsTimer, bundleSize);
      }

      PersistenceReport persistenceReport = commit(params, opsTimer, trackerBundle);

      postCommit(trackerBundle);

      ImportReport importReport =
          ImportReport.withImportCompleted(
              Status.OK, persistenceReport, validationReport, opsTimer.stopTimer(), bundleSize);

      endImport(params, importReport);

      return importReport;
    } catch (Exception e) {
      log.error("Exception thrown during import.", e);

      ImportReport report =
          ImportReport.withError(
              "Exception:" + e.getMessage(), ValidationReport.emptyReport(), opsTimer.stopTimer());

      endImportWithError(params, report, e);

      return report;
    }
  }

  private TrackerBundle preHeat(TrackerImportParams params, TimingsStats opsTimer) {
    TrackerBundle trackerBundle = opsTimer.exec(PREHEAT_OPS, () -> preheatBundle(params));

    notifyOps(params, PREHEAT_OPS, opsTimer);

    return trackerBundle;
  }

  private void preProcess(TimingsStats opsTimer, TrackerBundle trackerBundle) {
    opsTimer.execVoid(PREPROCESS_OPS, () -> preProcessBundle(trackerBundle));
  }

  private ValidationReport validate(
      TrackerImportParams params, TimingsStats opsTimer, TrackerBundle trackerBundle) {
    ValidationResult validationResult =
        opsTimer.exec(VALIDATION_OPS, () -> validateBundle(params, trackerBundle, opsTimer));

    if (!trackerBundle.isSkipRuleEngine() && !params.getImportStrategy().isDelete()) {
      ValidationResult ruleEnginevalidationResult = execRuleEngine(params, opsTimer, trackerBundle);

      return ValidationReport.merge(validationResult, ruleEnginevalidationResult);
    }

    return ValidationReport.fromResult(validationResult);
  }

  private PersistenceReport commit(
      TrackerImportParams params, TimingsStats opsTimer, TrackerBundle trackerBundle) {
    PersistenceReport persistenceReport;
    if (TrackerImportStrategy.DELETE == params.getImportStrategy()) {
      persistenceReport = opsTimer.exec(COMMIT_OPS, () -> deleteBundle(trackerBundle));
    } else {
      persistenceReport = opsTimer.exec(COMMIT_OPS, () -> commitBundle(trackerBundle));
    }

    notifyOps(params, COMMIT_OPS, opsTimer);
    return persistenceReport;
  }

  private void postCommit(TrackerBundle trackerBundle) {
    trackerBundleService.postCommit(trackerBundle);
  }

  protected ValidationResult validateBundle(
      TrackerImportParams params, TrackerBundle bundle, TimingsStats opsTimer) {
    ValidationResult result = validationService.validate(bundle);
    bundle.setTrackedEntities(result.getTrackedEntities());
    bundle.setEnrollments(result.getEnrollments());
    bundle.setEvents(result.getEvents());
    bundle.setRelationships(result.getRelationships());

    notifyOps(params, VALIDATION_OPS, opsTimer);

    return result;
  }

  private ValidationResult execRuleEngine(
      TrackerImportParams params, TimingsStats opsTimer, TrackerBundle bundle) {
    opsTimer.execVoid(PROGRAMRULE_OPS, () -> trackerBundleService.runRuleEngine(bundle));

    notifyOps(params, PROGRAMRULE_OPS, opsTimer);

    ValidationResult result =
        opsTimer.exec(VALIDATE_PROGRAMRULE_OPS, () -> validationService.validateRuleEngine(bundle));
    bundle.setTrackedEntities(result.getTrackedEntities());
    bundle.setEnrollments(result.getEnrollments());
    bundle.setEvents(result.getEvents());
    bundle.setRelationships(result.getRelationships());

    notifyOps(params, VALIDATE_PROGRAMRULE_OPS, opsTimer);

    return result;
  }

  private ImportReport buildReportAndNotify(
      TrackerImportParams params,
      ValidationReport validationReport,
      TimingsStats opsTimer,
      Map<TrackerType, Integer> bundleSize) {
    ImportReport importReport =
        ImportReport.withValidationErrors(
            validationReport,
            opsTimer.stopTimer(),
            bundleSize.values().stream().mapToInt(Integer::intValue).sum());

    endImport(params, importReport);

    return importReport;
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

  protected TrackerBundle preheatBundle(TrackerImportParams params) {
    return trackerBundleService.create(params);
  }

  protected void preProcessBundle(TrackerBundle bundle) {
    trackerPreprocessService.preprocess(bundle);
  }

  protected PersistenceReport commitBundle(TrackerBundle trackerBundle) {
    PersistenceReport persistenceReport = trackerBundleService.commit(trackerBundle);

    if (!trackerBundle.isSkipSideEffects()) {
      List<TrackerSideEffectDataBundle> sideEffectDataBundles =
          Stream.of(TrackerType.ENROLLMENT, TrackerType.EVENT)
              .map(trackerType -> safelyGetSideEffectsDataBundles(persistenceReport, trackerType))
              .flatMap(Collection::stream)
              .collect(Collectors.toList());

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

  private void startImport(TrackerImportParams params) {
    if (null != params.getJobConfiguration()) {
      notifier.notify(params.getJobConfiguration(), params.userStartInfo() + " Import:Start");
    }
  }

  private void notifyOps(TrackerImportParams params, String validationOps, TimingsStats opsTimer) {
    if (null != params.getJobConfiguration()) {
      notifier.update(
          params.getJobConfiguration(),
          NotificationLevel.DEBUG,
          params
              + validationOps
              + " completed in "
              + opsTimer.get(validationOps)
              + " Import:"
              + validationOps);
    }
  }

  private void endImport(TrackerImportParams params, ImportReport importReport) {
    if (null != params.getJobConfiguration()) {
      notifier.update(
          params.getJobConfiguration(),
          params + " finished in " + importReport.getTimingsStats().get(TOTAL_OPS) + " Import:Done",
          true);

      if (params.getJobConfiguration().isInMemoryJob()) {
        notifier.addJobSummary(params.getJobConfiguration(), importReport, ImportReport.class);
      }
    }
  }

  private void endImportWithError(
      TrackerImportParams params, ImportReport importReport, Exception e) {
    if (null != params.getJobConfiguration() && params.getJobConfiguration().isInMemoryJob()) {
      notifier.update(
          params.getJobConfiguration(),
          NotificationLevel.ERROR,
          params + " failed with exception: " + e.getMessage() + " Import:Error",
          true);

      if (params.getJobConfiguration().isInMemoryJob()) {
        notifier.addJobSummary(params.getJobConfiguration(), importReport, ImportReport.class);
      }
    }
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
    if (originalValidationReport != null && TrackerBundleReportMode.WARNINGS == reportMode) {
      validationReport.addWarnings(originalValidationReport.getWarnings());
    } else if (originalValidationReport != null && TrackerBundleReportMode.FULL == reportMode) {
      validationReport.addWarnings(originalValidationReport.getWarnings());
      importReportBuilder.timingsStats(originalImportReport.getTimingsStats());
    }

    importReportBuilder.validationReport(validationReport);

    return importReportBuilder.build();
  }
}
