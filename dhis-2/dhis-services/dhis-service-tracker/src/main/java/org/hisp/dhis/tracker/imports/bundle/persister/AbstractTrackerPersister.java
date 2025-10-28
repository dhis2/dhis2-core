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
package org.hisp.dhis.tracker.imports.bundle.persister;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.changelog.ChangeLogType.CREATE;
import static org.hisp.dhis.changelog.ChangeLogType.DELETE;
import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.FlushMode;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.job.NotificationTrigger;
import org.hisp.dhis.tracker.imports.job.TrackerNotificationDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractTrackerPersister<T extends TrackerDto, V extends IdentifiableObject>
    implements TrackerPersister<T, V> {
  protected final ReservedValueService reservedValueService;

  protected final TrackedEntityChangeLogService trackedEntityChangeLogService;

  /**
   * Template method that can be used by classes extending this class to execute the persistence
   * flow of Tracker entities
   *
   * @param entityManager a valid EntityManager
   * @param bundle the Bundle to persist
   * @return a {@link TrackerTypeReport}
   */
  @Override
  public TrackerTypeReport persist(EntityManager entityManager, TrackerBundle bundle) {
    //
    // Init the report that will hold the results of the persist operation
    //
    TrackerTypeReport typeReport = new TrackerTypeReport(getType());

    List<TrackerNotificationDataBundle> notificationDataBundles = new ArrayList<>();

    //
    // Extract the entities to persist from the Bundle
    //
    List<T> dtos = getByType(bundle);

    for (T trackerDto : dtos) {

      Entity objectReport = new Entity(getType(), trackerDto.getUid());
      List<NotificationTrigger> triggers =
          determineNotificationTriggers(bundle.getPreheat(), trackerDto);

      try {
        V originalEntity = cloneEntityProperties(bundle.getPreheat(), trackerDto);

        //
        // Convert the TrackerDto into an Hibernate-managed entity
        //
        V convertedDto = convert(bundle, trackerDto);

        //
        // Handle ownership records, if required
        //
        persistOwnership(bundle, trackerDto, convertedDto);

        //
        // Save or update the entity
        //
        if (isNew(bundle, trackerDto)) {
          entityManager.persist(convertedDto);
          updateDataValues(
              entityManager,
              bundle.getPreheat(),
              trackerDto,
              convertedDto,
              originalEntity,
              bundle.getUser());
          typeReport.getStats().incCreated();
          typeReport.addEntity(objectReport);
          updateAttributes(
              entityManager, bundle.getPreheat(), trackerDto, convertedDto, bundle.getUser());
          bundle.addUpdatedTrackedEntities(getUpdatedTrackedEntities(convertedDto));
        } else {
          if (trackerDto.getTrackerType() == TrackerType.RELATIONSHIP) {
            typeReport.getStats().incIgnored();
            // Relationships are not updated. A warning was already added to the report
          } else {
            updateDataValues(
                entityManager,
                bundle.getPreheat(),
                trackerDto,
                convertedDto,
                originalEntity,
                bundle.getUser());
            updateAttributes(
                entityManager, bundle.getPreheat(), trackerDto, convertedDto, bundle.getUser());
            entityManager.merge(convertedDto);
            typeReport.getStats().incUpdated();
            typeReport.addEntity(objectReport);
            bundle.addUpdatedTrackedEntities(getUpdatedTrackedEntities(convertedDto));
          }
        }

        if (!bundle.isSkipSideEffects()) {
          notificationDataBundles.add(handleNotifications(bundle, convertedDto, triggers));
        }

        //
        // Add the entity to the Preheat
        //
        updatePreheat(bundle.getPreheat(), convertedDto);

        if (FlushMode.OBJECT == bundle.getFlushMode()) {
          entityManager.flush();
        }
      } catch (Exception e) {
        final String msg =
            "A Tracker Entity of type '"
                + getType().getName()
                + "' ("
                + trackerDto.getUid()
                + ") failed to persist.";

        if (AtomicMode.ALL.equals(bundle.getAtomicMode())) {
          throw new PersistenceException(msg, e);
        } else {
          // TODO currently we do not keep track of the failed entity
          // in the TrackerObjectReport

          log.warn(msg + "\nThe Import process will process remaining entities.", e);

          typeReport.getStats().incIgnored();
        }
      }
    }

    typeReport.getNotificationDataBundles().addAll(notificationDataBundles);

    return typeReport;
  }

  // // // // // // // //
  // // // // // // // //
  // TEMPLATE METHODS //
  // // // // // // // //
  // // // // // // // //

  /**
   * Get Tracked Entities for enrollments, events or relationships that have been created or updated
   */
  protected abstract Set<UID> getUpdatedTrackedEntities(V entity);

  /** Clones the event properties that may potentially be change logged */
  protected abstract V cloneEntityProperties(TrackerPreheat preheat, T trackerDto);

  /**
   * Converts an object implementing the {@link TrackerDto} interface into the corresponding
   * Hibernate-managed object
   */
  protected abstract V convert(TrackerBundle bundle, T trackerDto);

  /** Persists ownership records for the given entity */
  protected abstract void persistOwnership(TrackerBundle bundle, T trackerDto, V entity);

  /** Execute the persistence of Data values linked to the entity being processed */
  protected abstract void updateDataValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      T trackerDto,
      V payloadEntity,
      V currentEntity,
      UserDetails user);

  /** Execute the persistence of Attribute values linked to the entity being processed */
  protected abstract void updateAttributes(
      EntityManager entityManager,
      TrackerPreheat preheat,
      T trackerDto,
      V hibernateEntity,
      UserDetails user);

  /** Updates the {@link TrackerPreheat} object with the entity that has been persisted */
  protected abstract void updatePreheat(TrackerPreheat preheat, V convertedDto);

  /** TODO add comment */
  protected abstract TrackerNotificationDataBundle handleNotifications(
      TrackerBundle bundle, V entity, List<NotificationTrigger> triggers);

  /**
   * Determines the notification triggers based on the enrollment/event status.
   *
   * @param preheat the enrollment/event fetched from the database
   * @param entity the enrollment/event coming from the request payload
   * @return a list of NotificationTriggers
   */
  protected abstract List<NotificationTrigger> determineNotificationTriggers(
      TrackerPreheat preheat, T entity);

  /** Get the Tracker Type for which the current Persister is responsible for. */
  protected abstract TrackerType getType();

  protected boolean isNew(TrackerBundle bundle, TrackerDto trackerDto) {
    return bundle.getStrategy(trackerDto) == TrackerImportStrategy.CREATE;
  }

  @SuppressWarnings("unchecked")
  protected abstract List<T> getByType(TrackerBundle bundle);

  // // // // // // // //
  // // // // // // // //
  // SHARED METHODS //
  // // // // // // // //
  // // // // // // // //

  protected void assignFileResource(
      EntityManager entityManager, TrackerPreheat preheat, String fileResourceOwner, String fr) {
    assignFileResource(entityManager, preheat, fileResourceOwner, fr, true);
  }

  protected void unassignFileResource(
      EntityManager entityManager, TrackerPreheat preheat, String fileResourceOwner, String fr) {
    assignFileResource(entityManager, preheat, fileResourceOwner, fr, false);
  }

  private void assignFileResource(
      EntityManager entityManager,
      TrackerPreheat preheat,
      String fileResourceOwner,
      String fr,
      boolean isAssign) {
    FileResource fileResource = preheat.get(FileResource.class, fr);

    if (fileResource == null) {
      return;
    }

    fileResource.setAssigned(isAssign);
    fileResource.setFileResourceOwner(fileResourceOwner);
    entityManager.merge(fileResource);
  }

  protected void handleTrackedEntityAttributeValues(
      EntityManager entityManager,
      TrackerPreheat preheat,
      List<Attribute> payloadAttributes,
      TrackedEntity trackedEntity,
      UserDetails user) {
    if (payloadAttributes.isEmpty()) {
      return;
    }

    TrackerIdSchemeParams idSchemes = preheat.getIdSchemes();
    Map<MetadataIdentifier, TrackedEntityAttributeValue> attributeValueById =
        trackedEntity.getTrackedEntityAttributeValues().stream()
            .collect(
                Collectors.toMap(
                    teav -> idSchemes.toMetadataIdentifier(teav.getAttribute()),
                    Function.identity()));

    payloadAttributes.forEach(
        attribute -> {

          // We cannot get the value from attributeToStore because it uses
          // encryption logic, so we need to use the one from payload
          boolean isDelete = StringUtils.isEmpty(attribute.getValue());

          TrackedEntityAttributeValue trackedEntityAttributeValue =
              attributeValueById.get(attribute.getAttribute());

          boolean isUpdated = false;

          boolean isNew = Objects.isNull(trackedEntityAttributeValue);

          if (isDelete && isNew) {
            return;
          }

          if (isDelete) {
            delete(entityManager, preheat, trackedEntityAttributeValue, trackedEntity, user);
          } else {
            if (!isNew) {
              isUpdated = !trackedEntityAttributeValue.getPlainValue().equals(attribute.getValue());
            }

            String previousValue =
                trackedEntityAttributeValue == null
                    ? null
                    : trackedEntityAttributeValue.getPlainValue();
            trackedEntityAttributeValue =
                Optional.ofNullable(trackedEntityAttributeValue)
                    .orElseGet(
                        () ->
                            new TrackedEntityAttributeValue()
                                .setAttribute(
                                    getTrackedEntityAttributeFromPreheat(
                                        preheat, attribute.getAttribute()))
                                .setTrackedEntity(trackedEntity))
                    .setStoredBy(attribute.getStoredBy())
                    .setValue(attribute.getValue());

            saveOrUpdate(
                entityManager,
                preheat,
                isNew,
                trackedEntity,
                trackedEntityAttributeValue,
                previousValue,
                isUpdated,
                user);
          }

          handleReservedValue(trackedEntityAttributeValue);
        });
  }

  private void delete(
      EntityManager entityManager,
      TrackerPreheat preheat,
      TrackedEntityAttributeValue trackedEntityAttributeValue,
      TrackedEntity trackedEntity,
      UserDetails user) {
    if (isFileResource(trackedEntityAttributeValue)) {
      unassignFileResource(
          entityManager, preheat, trackedEntity.getUid(), trackedEntityAttributeValue.getValue());
    }

    entityManager.remove(
        entityManager.contains(trackedEntityAttributeValue)
            ? trackedEntityAttributeValue
            : entityManager.merge(trackedEntityAttributeValue));

    addTrackedEntityChangeLog(
        user.getUsername(),
        trackedEntityAttributeValue,
        trackedEntityAttributeValue.getPlainValue(),
        null,
        trackedEntity,
        DELETE);
  }

  private void saveOrUpdate(
      EntityManager entityManager,
      TrackerPreheat preheat,
      boolean isNew,
      TrackedEntity trackedEntity,
      TrackedEntityAttributeValue trackedEntityAttributeValue,
      String previousValue,
      boolean isUpdated,
      UserDetails user) {
    if (isFileResource(trackedEntityAttributeValue)) {
      assignFileResource(
          entityManager, preheat, trackedEntity.getUid(), trackedEntityAttributeValue.getValue());
    }

    ChangeLogType changeLogType = null;

    if (isNew) {
      entityManager.persist(trackedEntityAttributeValue);
      // In case it's a newly created attribute we'll add it back to TE,
      // so it can end up in preheat
      trackedEntity.getTrackedEntityAttributeValues().add(trackedEntityAttributeValue);
      changeLogType = CREATE;
    } else {
      entityManager.merge(trackedEntityAttributeValue);

      if (isUpdated) {
        changeLogType = UPDATE;
      }
    }

    addTrackedEntityChangeLog(
        user.getUsername(),
        trackedEntityAttributeValue,
        previousValue,
        trackedEntityAttributeValue.getPlainValue(),
        trackedEntity,
        changeLogType);
  }

  private static boolean isFileResource(TrackedEntityAttributeValue trackedEntityAttributeValue) {
    return trackedEntityAttributeValue.getAttribute().getValueType() == ValueType.FILE_RESOURCE;
  }

  private static TrackedEntityAttribute getTrackedEntityAttributeFromPreheat(
      TrackerPreheat preheat, MetadataIdentifier attribute) {
    TrackedEntityAttribute trackedEntityAttribute = preheat.getTrackedEntityAttribute(attribute);

    checkNotNull(
        trackedEntityAttribute,
        "Attribute "
            + attribute.getIdentifierOrAttributeValue()
            + " should never be NULL here if validation is enforced before commit.");

    return trackedEntityAttribute;
  }

  private void handleReservedValue(TrackedEntityAttributeValue attributeValue) {
    if (Boolean.TRUE.equals(attributeValue.getAttribute().isGenerated())
        && attributeValue.getAttribute().getTextPattern() != null) {
      reservedValueService.useReservedValue(
          attributeValue.getAttribute().getTextPattern(), attributeValue.getValue());
    }
  }

  private void addTrackedEntityChangeLog(
      String userName,
      TrackedEntityAttributeValue attributeValue,
      String previousValue,
      String currentValue,
      TrackedEntity trackedEntity,
      ChangeLogType changeLogType) {

    if (changeLogType != null) {
      trackedEntityChangeLogService.addTrackedEntityChangeLog(
          trackedEntity,
          attributeValue.getAttribute(),
          previousValue,
          currentValue,
          changeLogType,
          userName);
    }
  }
}
