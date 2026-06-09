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
package org.hisp.dhis.tracker.imports.bundle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.imports.ParamsConverter;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.bundle.persister.CommitService;
import org.hisp.dhis.tracker.imports.bundle.persister.PersistenceException;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerPersister.PersistResult;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.notification.EntityNotifications;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleService;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.user.UserDetails;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@RequiredArgsConstructor
public class DefaultTrackerBundleService implements TrackerBundleService {
  private final TrackerPreheatService trackerPreheatService;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  private final CommitService commitService;

  private final ProgramRuleService programRuleService;

  private final TrackerObjectDeletionService deletionService;

  private final ObjectMapper mapper;

  @Nonnull
  @Override
  public TrackerBundle create(
      @Nonnull TrackerImportParams params,
      @Nonnull TrackerObjects trackerObjects,
      @Nonnull UserDetails user) {
    TrackerPreheat preheat = trackerPreheatService.preheat(trackerObjects, params.getIdSchemes());
    TrackerBundle trackerBundle = ParamsConverter.convert(params, trackerObjects, user, preheat);
    trackerBundle.setPreheat(preheat);

    return trackerBundle;
  }

  @Nonnull
  @Override
  public TrackerBundle runRuleEngine(@Nonnull TrackerBundle trackerBundle) {
    programRuleService.calculateRuleEffects(trackerBundle, trackerBundle.getPreheat());

    return trackerBundle;
  }

  @Nonnull
  @Override
  @Transactional
  public CommitResult commit(@Nonnull TrackerBundle bundle) {
    if (TrackerBundleMode.VALIDATE == bundle.getImportMode()) {
      return new CommitResult(PersistenceReport.emptyReport(), List.of());
    }

    PersistResult trackedEntities =
        commitService.getTrackerPersister().persist(entityManager, bundle);
    PersistResult enrollments =
        commitService.getEnrollmentPersister().persist(entityManager, bundle);
    PersistResult trackerEvents =
        commitService.getTrackerEventPersister().persist(entityManager, bundle);
    PersistResult singleEvents =
        commitService.getSingleEventPersister().persist(entityManager, bundle);
    PersistResult relationships =
        commitService.getRelationshipPersister().persist(entityManager, bundle);

    PersistenceReport report =
        new PersistenceReport(
            trackedEntities.report(),
            enrollments.report(),
            trackerEvents.report(),
            singleEvents.report(),
            relationships.report());

    List<EntityNotifications> notifications =
        Stream.of(trackedEntities, enrollments, trackerEvents, singleEvents, relationships)
            .flatMap(r -> r.notifications().stream())
            .toList();

    return new CommitResult(report, notifications);
  }

  @Override
  @Transactional
  public void postCommit(@Nonnull TrackerBundle bundle) {
    updateTrackedEntitiesLastUpdated(bundle);
  }

  private void updateTrackedEntitiesLastUpdated(TrackerBundle bundle) {
    if (bundle.getUpdatedTrackedEntities().isEmpty()) {
      return;
    }

    String userInfoJson;
    try {
      userInfoJson = mapper.writeValueAsString(UserInfoSnapshot.from(bundle.getUser()));
    } catch (JsonProcessingException e) {
      throw new PersistenceException(e);
    }

    Date lastUpdated = new Date();
    String sql =
        "update trackedentity set lastUpdated = :lastUpdated,"
            + " lastupdatedbyuserinfo = CAST(:lastupdatedbyuserinfo as jsonb)"
            + " where uid in (:trackedEntities)";

    for (List<UID> partition :
        Lists.partition(Lists.newArrayList(bundle.getUpdatedTrackedEntities()), 20000)) {
      if (partition.isEmpty()) {
        continue;
      }
      MapSqlParameterSource params =
          new MapSqlParameterSource()
              .addValue("trackedEntities", UID.toValueList(partition))
              .addValue("lastUpdated", lastUpdated)
              .addValue("lastupdatedbyuserinfo", userInfoJson);
      jdbcTemplate.update(sql, params);
    }
  }

  @Nonnull
  @Override
  @Transactional
  public PersistenceReport delete(@Nonnull TrackerBundle bundle)
      throws ForbiddenException, NotFoundException {
    if (TrackerBundleMode.VALIDATE == bundle.getImportMode()) {
      return PersistenceReport.emptyReport();
    }

    TrackerTypeReport trackedEntitiesReport =
        deletionService.deleteTrackedEntities(
            bundle.getTrackedEntities().stream().map(TrackerDto::getUID).toList());
    TrackerTypeReport enrollmentsReport =
        deletionService.deleteEnrollments(
            bundle.getEnrollments().stream().map(TrackerDto::getUID).toList());
    TrackerTypeReport trackerEventsReport =
        deletionService.deleteTrackerEvents(
            bundle.getTrackerEvents().stream().map(TrackerDto::getUID).toList());
    TrackerTypeReport singleEventsReport =
        deletionService.deleteSingleEvents(
            bundle.getSingleEvents().stream().map(TrackerDto::getUID).toList());
    TrackerTypeReport relationshipsReport =
        deletionService.deleteRelationships(
            bundle.getRelationships().stream().map(TrackerDto::getUID).toList());

    return new PersistenceReport(
        trackedEntitiesReport,
        enrollmentsReport,
        trackerEventsReport,
        singleEventsReport,
        relationshipsReport);
  }
}
