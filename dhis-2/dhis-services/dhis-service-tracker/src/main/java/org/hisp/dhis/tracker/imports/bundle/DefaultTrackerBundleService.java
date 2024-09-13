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
package org.hisp.dhis.tracker.imports.bundle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.ParamsConverter;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.bundle.persister.CommitService;
import org.hisp.dhis.tracker.imports.bundle.persister.PersistenceException;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.job.TrackerNotificationDataBundle;
import org.hisp.dhis.tracker.imports.notification.NotificationHandlerService;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleService;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
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

  private final EntityManager entityManager;

  private final CommitService commitService;

  private final ProgramRuleService programRuleService;

  private final TrackerObjectDeletionService deletionService;

  private final ObjectMapper mapper;

  private List<NotificationHandlerService> notificationHandlers = new ArrayList<>();

  @Autowired(required = false)
  public void setNotificationHandlers(List<NotificationHandlerService> notificationHandlers) {
    this.notificationHandlers = notificationHandlers;
  }

  @Override
  public TrackerBundle create(
      TrackerImportParams params, TrackerObjects trackerObjects, User user) {
    UserInfoSnapshot userInfo = UserInfoSnapshot.from(UserDetails.fromUser(user));

    TrackerPreheat preheat =
        trackerPreheatService.preheat(trackerObjects, params.getIdSchemes(), user);
    preheat.setUserInfo(userInfo);

    TrackerBundle trackerBundle = ParamsConverter.convert(params, trackerObjects, user);
    trackerBundle.setUserInfo(userInfo);
    trackerBundle.setPreheat(preheat);

    return trackerBundle;
  }

  @Override
  public TrackerBundle runRuleEngine(TrackerBundle trackerBundle) {
    programRuleService.calculateRuleEffects(trackerBundle, trackerBundle.getPreheat());

    return trackerBundle;
  }

  @Override
  @Transactional
  public PersistenceReport commit(TrackerBundle bundle) {
    if (TrackerBundleMode.VALIDATE == bundle.getImportMode()) {
      return PersistenceReport.emptyReport();
    }

    Map<TrackerType, TrackerTypeReport> reportMap =
        Map.of(
            TrackerType.TRACKED_ENTITY,
            commitService.getTrackerPersister().persist(entityManager, bundle),
            TrackerType.ENROLLMENT,
            commitService.getEnrollmentPersister().persist(entityManager, bundle),
            TrackerType.EVENT,
            commitService.getEventPersister().persist(entityManager, bundle),
            TrackerType.RELATIONSHIP,
            commitService.getRelationshipPersister().persist(entityManager, bundle));

    return new PersistenceReport(reportMap);
  }

  @Override
  @Transactional
  public void postCommit(TrackerBundle bundle) {
    updateTrackedEntitiesLastUpdated(bundle);
  }

  private void updateTrackedEntitiesLastUpdated(TrackerBundle bundle) {
    Set<String> updatedTrackedEntities = bundle.getUpdatedTrackedEntities();

    if (updatedTrackedEntities.isEmpty()) {
      return;
    }

    List<List<String>> uidsPartitions =
        Lists.partition(Lists.newArrayList(bundle.getUpdatedTrackedEntities()), 20000);

    try (Session session = entityManager.unwrap(Session.class)) {
      for (List<String> trackedEntities : uidsPartitions) {
        if (trackedEntities.isEmpty()) {
          continue;
        }
        executeLastUpdatedQuery(session, trackedEntities, bundle);
      }
    }
  }

  private void executeLastUpdatedQuery(
      Session session, List<String> trackedEntities, TrackerBundle bundle) {
    try {
      session
          .getNamedQuery("updateTrackedEntitiesLastUpdated")
          .setParameter("trackedEntities", trackedEntities)
          .setParameter("lastUpdated", new Date())
          .setParameter("lastupdatedbyuserinfo", mapper.writeValueAsString(bundle.getUserInfo()))
          .executeUpdate();
    } catch (JsonProcessingException e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public void sendNotifications(List<TrackerNotificationDataBundle> bundles) {
    notificationHandlers.forEach(handler -> handler.handleNotifications(bundles));
  }

  @Override
  @Transactional
  public PersistenceReport delete(TrackerBundle bundle)
      throws ForbiddenException, NotFoundException {
    if (TrackerBundleMode.VALIDATE == bundle.getImportMode()) {
      return PersistenceReport.emptyReport();
    }

    Map<TrackerType, TrackerTypeReport> reportMap =
        Map.of(
            TrackerType.RELATIONSHIP,
                deletionService.deleteRelationships(
                    bundle.getRelationships().stream().map(TrackerDto::getUid).toList()),
            TrackerType.EVENT,
                deletionService.deleteEvents(
                    bundle.getEvents().stream().map(TrackerDto::getUid).toList()),
            TrackerType.ENROLLMENT,
                deletionService.deleteEnrollments(
                    bundle.getEnrollments().stream().map(TrackerDto::getUid).toList()),
            TrackerType.TRACKED_ENTITY,
                deletionService.deleteTrackedEntities(
                    bundle.getTrackedEntities().stream().map(TrackerDto::getUid).toList()));

    return new PersistenceReport(reportMap);
  }
}
