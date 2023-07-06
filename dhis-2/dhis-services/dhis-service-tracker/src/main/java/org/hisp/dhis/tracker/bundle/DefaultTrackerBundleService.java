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
package org.hisp.dhis.tracker.bundle;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.ParamsConverter;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerProgramRuleService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.persister.CommitService;
import org.hisp.dhis.tracker.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.tracker.sideeffect.SideEffectHandlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@RequiredArgsConstructor
public class DefaultTrackerBundleService implements TrackerBundleService {
  private final TrackerPreheatService trackerPreheatService;

  private final SessionFactory sessionFactory;

  private final CommitService commitService;

  private final TrackerProgramRuleService trackerProgramRuleService;

  private final TrackerObjectDeletionService deletionService;

  private final TrackedEntityInstanceService trackedEntityInstanceService;

  private List<SideEffectHandlerService> sideEffectHandlers = new ArrayList<>();

  @Autowired(required = false)
  public void setSideEffectHandlers(List<SideEffectHandlerService> sideEffectHandlers) {
    this.sideEffectHandlers = sideEffectHandlers;
  }

  @Override
  public TrackerBundle create(TrackerImportParams params) {
    TrackerBundle trackerBundle = ParamsConverter.convert(params);
    TrackerPreheat preheat = trackerPreheatService.preheat(params);
    trackerBundle.setPreheat(preheat);

    return trackerBundle;
  }

  @Override
  public TrackerBundle runRuleEngine(TrackerBundle trackerBundle) {
    List<RuleEffects> ruleEffects = trackerProgramRuleService.calculateRuleEffects(trackerBundle);
    trackerBundle.setRuleEffects(ruleEffects);

    return trackerBundle;
  }

  @Override
  @Transactional
  public TrackerBundleReport commit(TrackerBundle bundle) {
    TrackerBundleReport bundleReport = new TrackerBundleReport();

    if (TrackerBundleMode.VALIDATE == bundle.getImportMode()) {
      return bundleReport;
    }

    Session session = sessionFactory.getCurrentSession();
    Map<TrackerType, TrackerTypeReport> report = bundleReport.getTypeReportMap();
    report.put(
        TrackerType.TRACKED_ENTITY, commitService.getTrackerPersister().persist(session, bundle));
    report.put(
        TrackerType.ENROLLMENT, commitService.getEnrollmentPersister().persist(session, bundle));
    report.put(TrackerType.EVENT, commitService.getEventPersister().persist(session, bundle));
    report.put(
        TrackerType.RELATIONSHIP,
        commitService.getRelationshipPersister().persist(session, bundle));

    return bundleReport;
  }

  @Override
  public void postCommit(TrackerBundle bundle) {
    updateTeisLastUpdated(bundle);
  }

  private void updateTeisLastUpdated(TrackerBundle bundle) {
    Optional.ofNullable(bundle.getUpdatedTeis())
        .filter(ut -> !ut.isEmpty())
        .ifPresent(
            teis ->
                trackedEntityInstanceService.updateTrackedEntityInstanceLastUpdated(
                    teis, new Date()));
  }

  @Override
  public void handleTrackerSideEffects(List<TrackerSideEffectDataBundle> bundles) {
    sideEffectHandlers.forEach(handler -> handler.handleSideEffects(bundles));
  }

  @Override
  @Transactional
  public TrackerBundleReport delete(TrackerBundle bundle) {
    TrackerBundleReport bundleReport = new TrackerBundleReport();

    if (TrackerBundleMode.VALIDATE == bundle.getImportMode()) {
      return bundleReport;
    }

    Map<TrackerType, TrackerTypeReport> report = bundleReport.getTypeReportMap();
    report.put(TrackerType.RELATIONSHIP, deletionService.deleteRelationShips(bundle));
    report.put(TrackerType.EVENT, deletionService.deleteEvents(bundle));
    report.put(TrackerType.ENROLLMENT, deletionService.deleteEnrollments(bundle));
    report.put(TrackerType.TRACKED_ENTITY, deletionService.deleteTrackedEntityInstances(bundle));

    return bundleReport;
  }
}
