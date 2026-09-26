/*
 * Copyright (c) 2004-2026, University of Oslo
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
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.ParamsConverter;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.bundle.persister.CommitService;
import org.hisp.dhis.tracker.imports.bundle.persister.PersistenceException;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerPersister;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.job.TrackerNotificationDataBundle;
import org.hisp.dhis.tracker.imports.notification.NotificationHandlerService;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.imports.preheat.supplier.OptionValueSupplier;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleService;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.AssignAttributeExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.AssignDataValueExecutor;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
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

  public static final int LAST_UPDATED_PARTITION_SIZE = 20_000;

  private final TrackerPreheatService trackerPreheatService;

  private final EntityManager entityManager;

  private final CommitService commitService;

  private final ProgramRuleService programRuleService;

  private final TrackerObjectDeletionService deletionService;

  private final OptionValueSupplier optionValueSupplier;

  private final ObjectMapper mapper;

  private List<NotificationHandlerService> notificationHandlers = new ArrayList<>();

  @Autowired(required = false)
  public void setNotificationHandlers(List<NotificationHandlerService> notificationHandlers) {
    this.notificationHandlers = notificationHandlers;
  }

  @Nonnull
  @Override
  public TrackerBundle create(
      @Nonnull TrackerImportParams params,
      @Nonnull TrackerObjects trackerObjects,
      @Nonnull UserDetails user) {
    TrackerPreheat preheat = trackerPreheatService.preheat(trackerObjects, params.getIdSchemes());
    TrackerBundle trackerBundle = ParamsConverter.convert(params, trackerObjects, user);
    trackerBundle.setPreheat(preheat);

    return trackerBundle;
  }

  @Nonnull
  @Override
  public TrackerBundle runRuleEngine(@Nonnull TrackerBundle trackerBundle) {
    programRuleService.calculateRuleEffects(trackerBundle, trackerBundle.getPreheat());

    optionValueSupplier.preheatAdd(
        collectRuleAssignedValues(trackerBundle), trackerBundle.getPreheat());

    return trackerBundle;
  }

  /**
   * Collects the values {@code ASSIGN} rule actions are going to apply, shaped as a synthetic
   * {@link TrackerObjects} payload the {@link OptionValueSupplier} can resolve option codes from.
   *
   * <p>Rule engine validation rejects unknown option codes based on {@link
   * TrackerPreheat#isValidOptionCode(Long, String)}, but {@code ASSIGN} actions can add or
   * overwrite data values and attributes that were not in the original payload, so the codes they
   * introduce were never resolved during preheat and valid data would be rejected with E1125.
   *
   * <p>The values are already final here: they are the rule engine's evaluated output, captured in
   * the executors {@code calculateRuleEffects} just built, so nothing evaluated later can change
   * them.
   *
   * <p>All assigned values are gathered onto a single synthetic event and enrollment. The supplier
   * only looks at (data element, value) and (attribute, value) pairs, so which or how many real
   * entities the values belong to does not matter.
   */
  private TrackerObjects collectRuleAssignedValues(TrackerBundle bundle) {
    TrackerPreheat preheat = bundle.getPreheat();

    Set<DataValue> assignedDataValues =
        bundle.getEventRuleActionExecutors().values().stream()
            .flatMap(List::stream)
            .filter(AssignDataValueExecutor.class::isInstance)
            .map(AssignDataValueExecutor.class::cast)
            .map(executor -> toDataValue(preheat, executor))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    List<Attribute> assignedAttributes =
        bundle.getEnrollmentRuleActionExecutors().values().stream()
            .flatMap(List::stream)
            .filter(AssignAttributeExecutor.class::isInstance)
            .map(AssignAttributeExecutor.class::cast)
            .map(executor -> toAttribute(preheat, executor))
            .filter(Objects::nonNull)
            .toList();

    return TrackerObjects.builder()
        .events(
            List.of(Event.builder().event(UID.generate()).dataValues(assignedDataValues).build()))
        .enrollments(
            List.of(
                Enrollment.builder()
                    .enrollment(UID.generate())
                    .attributes(assignedAttributes)
                    .build()))
        .build();
  }

  /**
   * Executors only know the target's UID, while the supplier looks metadata up by the payload's id
   * scheme, so the identifier has to be converted the same way the executors themselves do when
   * they apply the value.
   */
  private DataValue toDataValue(TrackerPreheat preheat, AssignDataValueExecutor executor) {
    DataElement dataElement = preheat.getDataElement(executor.getDataElementUid().getValue());
    if (dataElement == null) {
      return null;
    }

    return DataValue.builder()
        .dataElement(preheat.getIdSchemes().toMetadataIdentifier(dataElement))
        .value(executor.getValue())
        .build();
  }

  private Attribute toAttribute(TrackerPreheat preheat, AssignAttributeExecutor executor) {
    TrackedEntityAttribute attribute =
        preheat.getTrackedEntityAttribute(executor.getAttributeUid().getValue());
    if (attribute == null) {
      return null;
    }

    return Attribute.builder()
        .attribute(preheat.getIdSchemes().toMetadataIdentifier(attribute))
        .value(executor.getValue())
        .build();
  }

  @Nonnull
  @Override
  @Transactional
  public CommitResult commit(@Nonnull TrackerBundle bundle) {
    if (TrackerBundleMode.VALIDATE == bundle.getImportMode()) {
      return new CommitResult(PersistenceReport.emptyReport(), List.of());
    }

    List<TrackerPersister.PersistResult> results =
        List.of(
            commitService.getTrackerPersister().persist(entityManager, bundle),
            commitService.getEnrollmentPersister().persist(entityManager, bundle),
            commitService.getEventPersister().persist(entityManager, bundle),
            commitService.getRelationshipPersister().persist(entityManager, bundle));

    Map<TrackerType, TrackerTypeReport> reportMap =
        results.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    r -> r.report().getTrackerType(), TrackerPersister.PersistResult::report));

    List<TrackerNotificationDataBundle> notificationBundles =
        results.stream().flatMap(r -> r.notificationBundles().stream()).toList();

    return new CommitResult(new PersistenceReport(reportMap), notificationBundles);
  }

  @Override
  @Transactional
  public void postCommit(@Nonnull TrackerBundle bundle) {
    updateTrackedEntitiesLastUpdated(bundle);
    updateSingleEventsLastUpdated(bundle);
  }

  private void updateTrackedEntitiesLastUpdated(TrackerBundle bundle) {
    if (bundle.getUpdatedTrackedEntities().isEmpty()) {
      return;
    }

    List<List<UID>> uidsPartitions =
        Lists.partition(
            Lists.newArrayList(bundle.getUpdatedTrackedEntities()), LAST_UPDATED_PARTITION_SIZE);

    try (Session session = entityManager.unwrap(Session.class)) {
      for (List<UID> trackedEntities : uidsPartitions) {
        if (trackedEntities.isEmpty()) {
          continue;
        }
        executeLastUpdatedQuery(
            session,
            "updateTrackedEntitiesLastUpdated",
            "trackedEntities",
            UID.toValueList(trackedEntities),
            bundle.getUser());
      }
    }
  }

  private void updateSingleEventsLastUpdated(TrackerBundle bundle) {
    if (bundle.getUpdatedSingleEvents().isEmpty()) {
      return;
    }

    List<List<UID>> uidsPartitions =
        Lists.partition(
            Lists.newArrayList(bundle.getUpdatedSingleEvents()), LAST_UPDATED_PARTITION_SIZE);

    try (Session session = entityManager.unwrap(Session.class)) {
      for (List<UID> events : uidsPartitions) {
        if (events.isEmpty()) {
          continue;
        }
        executeLastUpdatedQuery(
            session,
            "updateEventsLastUpdated",
            "events",
            UID.toValueList(events),
            bundle.getUser());
      }
    }
  }

  private void executeLastUpdatedQuery(
      Session session,
      String namedQuery,
      String uidsParameter,
      List<String> uids,
      UserDetails user) {
    try {
      UserInfoSnapshot userInfo = UserInfoSnapshot.from(user);
      session
          .getNamedQuery(namedQuery)
          .setParameter(uidsParameter, uids)
          .setParameter("lastUpdated", new Date())
          .setParameter("lastupdatedbyuserinfo", mapper.writeValueAsString(userInfo))
          .executeUpdate();
    } catch (JsonProcessingException e) {
      throw new PersistenceException(e);
    }
  }

  @Override
  public void sendNotifications(@Nonnull List<TrackerNotificationDataBundle> bundles) {
    notificationHandlers.forEach(handler -> handler.handleNotifications(bundles));
  }

  @Nonnull
  @Override
  @Transactional
  public PersistenceReport delete(@Nonnull TrackerBundle bundle)
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
