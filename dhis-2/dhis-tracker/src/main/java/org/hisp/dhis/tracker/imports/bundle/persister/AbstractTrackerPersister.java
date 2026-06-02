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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.FlushMode;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.notification.EntityNotifications;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.UserDetails;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractTrackerPersister<T extends TrackerDto, V extends IdentifiableObject>
    implements TrackerPersister<T, V> {
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

  @PersistenceContext private EntityManager entityManager;

  protected final ReservedValueService reservedValueService;

  protected final DataSource dataSource;

  protected final FileResourceStore fileResourceStore;

  protected final ObjectMapper objectMapper;

  /**
   * Template method that can be used by classes extending this class to execute the persistence
   * flow of Tracker entities
   *
   * @param bundle the Bundle to persist
   * @return a {@link TrackerTypeReport}
   */
  @Override
  public PersistResult persist(TrackerBundle bundle) {
    //
    // Init the report that will hold the results of the persist operation
    //
    TrackerTypeReport typeReport = new TrackerTypeReport(getType());

    List<EntityNotifications> notifications = new ArrayList<>();
    ChangeLogAccumulator changeLogs = new ChangeLogAccumulator();
    EntityWriteBatch batch = new EntityWriteBatch(objectMapper);

    //
    // Extract the entities to persist from the Bundle
    //
    List<T> dtos = getByType(bundle);

    Connection conn = DataSourceUtils.getConnection(dataSource);
    try {
      // Pre-allocate primary keys in a single round-trip for entity types that opt in.
      // The cursor advances only on isNew branches inside the loop below.
      long[] preAllocatedIds = preAllocateIds(conn, bundle, dtos);
      int preAllocatedIdsCursor = 0;

      for (T trackerDto : dtos) {

        Entity objectReport = new Entity(getType(), trackerDto.getUID());
        boolean isNewEntity = isNew(bundle, trackerDto);
        // Capture before convert() which mutates the preheat entity's status
        boolean completedInThisImport =
            !bundle.isSkipSideEffects()
                && isBeingCompleted(bundle.getPreheat(), trackerDto, isNewEntity);
        ChangeLogAccumulator.Mark changeLogMark = changeLogs.mark();
        EntityWriteBatch.Mark batchMark = batch.mark();
        try {
          V originalEntity = cloneEntityProperties(bundle.getPreheat(), trackerDto);

          //
          // Convert the TrackerDto into an Hibernate-managed entity
          //
          V convertedDto = convert(bundle, trackerDto);

          //
          // Save or update the entity
          //
          if (isNew(bundle, trackerDto)) {
            if (preAllocatedIds != null) {
              assignId(convertedDto, preAllocatedIds[preAllocatedIdsCursor++]);
            }
            persistOwnership(bundle, trackerDto, convertedDto);
            stageInsert(convertedDto, batch);
            updateDataValues(
                bundle.getPreheat(),
                trackerDto,
                convertedDto,
                originalEntity,
                bundle.getUser(),
                changeLogs);
            typeReport.getStats().incCreated();
            typeReport.addEntity(objectReport);
            updateAttributes(
                bundle.getPreheat(), trackerDto, convertedDto, bundle.getUser(), changeLogs, batch);
            bundle.addUpdatedTrackedEntities(getUpdatedTrackedEntities(convertedDto));
          } else {
            if (trackerDto.getTrackerType() == TrackerType.RELATIONSHIP) {
              typeReport.getStats().incIgnored();
              // Relationships are not updated. A warning was already added to the report
            } else {
              updateDataValues(
                  bundle.getPreheat(),
                  trackerDto,
                  convertedDto,
                  originalEntity,
                  bundle.getUser(),
                  changeLogs);
              updateAttributes(
                  bundle.getPreheat(),
                  trackerDto,
                  convertedDto,
                  bundle.getUser(),
                  changeLogs,
                  batch);
              stageUpdate(convertedDto, batch);
              typeReport.getStats().incUpdated();
              typeReport.addEntity(objectReport);
              bundle.addUpdatedTrackedEntities(getUpdatedTrackedEntities(convertedDto));
            }
          }

          if (!bundle.isSkipSideEffects()) {
            EntityNotifications entityNotifications =
                collectNotifications(bundle, convertedDto, isNewEntity, completedInThisImport);
            if (entityNotifications != null) {
              notifications.add(entityNotifications);
            }
          }

          //
          // Add the entity to the Preheat
          //
          updatePreheat(bundle.getPreheat(), convertedDto);

          if (FlushMode.OBJECT == bundle.getFlushMode()) {
            // Flush entity INSERTs/UPDATEs before changelog INSERTs so FK references
            // (trackedentityid, eventid) exist before changelog rows reference them.
            batch.flush(entityManager, conn);
            entityManager.flush();
            changeLogs.flushAll(conn);
          }
        } catch (Exception e) {
          batch.rollbackTo(batchMark);
          changeLogs.rollbackTo(changeLogMark);

          final String msg =
              "A Tracker Entity of type '"
                  + getType().getName()
                  + "' ("
                  + trackerDto.getUID()
                  + ") failed to persist.";

          if (AtomicMode.ALL.equals(bundle.getAtomicMode())) {
            throw new PersistenceException(msg, e);
          } else {
            // TODO currently we do not keep track of the failed entity
            // in the TrackerObjectReport

            // TODO: if the failure originated from a JDBC flush (changeLogs.flushAll or, from
            // Phase 3 onward, EntityWriteBatch.flush), the underlying PostgreSQL connection is
            // now in an aborted-transaction state. Any subsequent SQL on the same connection
            // will fail with "current transaction is aborted". This means a single JDBC flush
            // failure in non-atomic mode silently cascades and causes all remaining entities to
            // be ignored as well. Consider wrapping each entity's JDBC flush in a savepoint so
            // that a failure can be rolled back to the savepoint and the connection stays usable.
            log.warn(msg + "\nThe Import process will process remaining entities.", e);

            typeReport.getStats().incIgnored();
          }
        }
      }

      if (FlushMode.AUTO == bundle.getFlushMode()) {
        // Flush entity INSERTs/UPDATEs before changelog INSERTs so FK references
        // (trackedentityid, eventid) exist before changelog rows reference them.
        batch.flush(entityManager, conn);
        entityManager.flush();
        changeLogs.flushAll(conn);
      }
    } catch (SQLException e) {
      throw new PersistenceException(e);
    } finally {
      DataSourceUtils.releaseConnection(conn, dataSource);
    }
    return new PersistResult(typeReport, notifications);
  }

  private long[] preAllocateIds(Connection conn, TrackerBundle bundle, List<T> dtos)
      throws SQLException {
    String sequenceName = sequenceName();
    if (sequenceName == null) {
      return null;
    }
    int createCount = 0;
    for (T dto : dtos) {
      if (isNew(bundle, dto)) {
        createCount++;
      }
    }
    if (createCount == 0) {
      return null;
    }
    return allocateIds(conn, sequenceName, createCount);
  }

  private void assignId(V convertedDto, long id) {
    if (convertedDto instanceof TrackedEntity te) {
      te.setId(id);
    } else if (convertedDto instanceof Enrollment e) {
      e.setId(id);
    } else if (convertedDto instanceof TrackerEvent ev) {
      ev.setId(id);
    } else if (convertedDto instanceof SingleEvent sev) {
      sev.setId(id);
    } else if (convertedDto instanceof Relationship r) {
      r.setId(id);
    } else {
      throw new IllegalStateException(
          "Pre-allocated id assignment not implemented for "
              + convertedDto.getClass().getName()
              + " -- sequenceName() returned non-null but assignId does not handle this type.");
    }
  }

  private void stageInsert(V convertedDto, EntityWriteBatch batch) {
    if (convertedDto instanceof TrackedEntity te) {
      batch.stageInsert(te);
    } else if (convertedDto instanceof Enrollment e) {
      batch.stageInsert(e);
    } else if (convertedDto instanceof TrackerEvent e) {
      batch.stageInsert(e);
    } else if (convertedDto instanceof SingleEvent e) {
      batch.stageInsert(e);
    } else if (convertedDto instanceof Relationship r) {
      batch.stageInsert(r);
    } else {
      throw new IllegalArgumentException(
          "Unsupported entity type: " + convertedDto.getClass().getName());
    }
  }

  // Relationships are not updated -- the persister branch above ignores update payloads before
  // this is called -- so no Relationship case is needed here.
  private void stageUpdate(V convertedDto, EntityWriteBatch batch) {
    if (convertedDto instanceof TrackedEntity te) {
      batch.stageUpdate(te);
    } else if (convertedDto instanceof Enrollment e) {
      batch.stageUpdate(e);
    } else if (convertedDto instanceof TrackerEvent e) {
      batch.stageUpdate(e);
    } else if (convertedDto instanceof SingleEvent e) {
      batch.stageUpdate(e);
    } else {
      throw new IllegalArgumentException(
          "Unsupported entity type for update: " + convertedDto.getClass().getName());
    }
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
      TrackerPreheat preheat,
      T trackerDto,
      V payloadEntity,
      V currentEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs);

  /** Execute the persistence of Attribute values linked to the entity being processed */
  protected abstract void updateAttributes(
      TrackerPreheat preheat,
      T trackerDto,
      V hibernateEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs,
      EntityWriteBatch batch);

  /** Updates the {@link TrackerPreheat} object with the entity that has been persisted */
  protected abstract void updatePreheat(TrackerPreheat preheat, V convertedDto);

  protected boolean isNew(TrackerBundle bundle, TrackerDto trackerDto) {
    return bundle.getStrategy(trackerDto) == TrackerImportStrategy.CREATE;
  }

  /**
   * Returns true if the entity is being set to completed status in this import. Must be called
   * before convert() which mutates the preheat entity. Returns false by default.
   */
  protected boolean isBeingCompleted(TrackerPreheat preheat, T trackerDto, boolean isNew) {
    return false;
  }

  /**
   * Collects notifications for the persisted entity. Merges lifecycle notifications (from template
   * trigger matching) and rule engine notifications. Override in persisters that support
   * notifications (enrollments, events). Returns null by default.
   */
  protected EntityNotifications collectNotifications(
      TrackerBundle bundle, V entity, boolean isNew, boolean completedInThisImport) {
    return null;
  }

  /** Returns notification templates that match any of the given triggers. */
  protected static Set<ProgramNotificationTemplate> filterTemplates(
      Set<ProgramNotificationTemplate> templates,
      EnumSet<org.hisp.dhis.program.notification.NotificationTrigger> triggers) {
    if (templates == null || templates.isEmpty() || triggers.isEmpty()) {
      return Set.of();
    }
    return templates.stream()
        .filter(t -> triggers.contains(t.getNotificationTrigger()))
        .collect(Collectors.toSet());
  }

  /**
   * Merges lifecycle templates and rule engine notifications into a single deduplicated set.
   * Lifecycle templates produce immediate notifications (scheduledAt=null).
   */
  protected static Set<Notification> mergeNotifications(
      Set<ProgramNotificationTemplate> lifecycleTemplates,
      List<Notification> ruleEngineNotifications) {
    Set<Notification> notifications = new LinkedHashSet<>(ruleEngineNotifications);
    for (ProgramNotificationTemplate t : lifecycleTemplates) {
      notifications.add(new Notification(UID.of(t.getUid()), null));
    }
    return notifications;
  }

  /** Get the Tracker Type for which the current Persister is responsible for. */
  protected abstract TrackerType getType();

  @SuppressWarnings("unchecked")
  protected abstract List<T> getByType(TrackerBundle bundle);

  /**
   * Returns the PostgreSQL sequence used to allocate primary keys for this entity type, or {@code
   * null} if id allocation should be left to Hibernate. When non-null, {@link #persist} pre-fetches
   * one id per CREATE entity in a single round-trip and assigns the id before staging so the flush
   * path can emit a multi-row INSERT.
   */
  protected abstract String sequenceName();

  /**
   * Fetches {@code count} ids from {@code sequenceName} in a single round-trip. The sequence name
   * is interpolated into the SQL (not a bind parameter) because PostgreSQL's {@code nextval} takes
   * a {@code regclass}, and the value comes from {@link #sequenceName()} — controlled by us, not
   * user input.
   */
  private static long[] allocateIds(Connection conn, String sequenceName, int count)
      throws SQLException {
    long[] ids = new long[count];
    String sql = "select nextval('" + sequenceName + "') from generate_series(1, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, count);
      try (ResultSet rs = ps.executeQuery()) {
        int i = 0;
        while (rs.next()) {
          ids[i++] = rs.getLong(1);
        }
        if (i != count) {
          throw new SQLException(
              "Allocated " + i + " ids from " + sequenceName + ", expected " + count);
        }
      }
    }
    return ids;
  }

  // // // // // // // //
  // // // // // // // //
  // SHARED METHODS //
  // // // // // // // //
  // // // // // // // //

  protected void assignFileResource(TrackerPreheat preheat, String fileResourceOwner, String fr) {
    assignFileResource(preheat, fileResourceOwner, fr, true);
  }

  protected void unassignFileResource(TrackerPreheat preheat, String fileResourceOwner, String fr) {
    assignFileResource(preheat, fileResourceOwner, fr, false);
  }

  private void assignFileResource(
      TrackerPreheat preheat, String fileResourceOwner, String fr, boolean isAssign) {
    FileResource fileResource = preheat.get(FileResource.class, fr);

    if (fileResource == null) {
      return;
    }

    fileResourceStore.updateAssignment(fileResource.getUid(), isAssign, fileResourceOwner);
  }

  protected void handleTrackedEntityAttributeValues(
      TrackerPreheat preheat,
      List<Attribute> payloadAttributes,
      TrackedEntity trackedEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs,
      EntityWriteBatch batch) {
    if (payloadAttributes.isEmpty()) {
      return;
    }

    TrackerIdSchemeParams idSchemes = preheat.getIdSchemes();
    // TODO [Phase 6 / DHIS2-21378]: drop this JPQL query and the batch overlay below once
    // EntityWriteBatch is shared across persisters and writes go through JDBC instead of the
    // EntityManager. At that point the batch itself is the source of truth for "what has already
    // been staged for this TE", and DB rows can be loaded eagerly by the preheat -- no per-call
    // round-trip needed here. Until then: build the lookup from the EntityManager rather than
    // the lazy TE collection. The collection is a Hibernate PersistentSet whose in-memory state
    // diverges from the persistence context once a write to a TE attribute has been deferred
    // (staged in EntityWriteBatch) and flushed by a prior persister -- the same logical TEAV
    // (composite key trackedentityid + trackedentityattributeid) can appear on a TrackedEntity
    // payload and on an Enrollment payload, and querying the EM here sees both DB rows and TEAVs
    // already attached to the session, preventing a duplicate em.persist that would throw
    // EntityExistsException. The batch overlay catches the within-persister case (e.g. two
    // enrollments under the same TE both carrying the same attribute) where the second occurrence
    // would otherwise produce a fresh instance with the same composite key. A TE staged for
    // insert in this batch has no DB row yet -- whether or not its id is set (Phase 4a pre-
    // allocates the id from the sequence) -- so the JPQL would always return empty and the lookup
    // can be skipped.
    Map<MetadataIdentifier, TrackedEntityAttributeValue> attributeValueById =
        batch.isStagedAsInsert(trackedEntity)
            ? new HashMap<>()
            : entityManager
                .createQuery(
                    "select v from TrackedEntityAttributeValue v where v.trackedEntity = :te",
                    TrackedEntityAttributeValue.class)
                .setParameter("te", trackedEntity)
                .getResultList()
                .stream()
                .collect(
                    Collectors.toMap(
                        teav -> idSchemes.toMetadataIdentifier(teav.getAttribute()),
                        Function.identity()));
    batch
        .stagedFor(trackedEntity)
        .forEach(
            teav ->
                attributeValueById.put(idSchemes.toMetadataIdentifier(teav.getAttribute()), teav));

    payloadAttributes.forEach(
        attribute -> {
          // We cannot get the value from attributeToStore because it uses
          // encryption logic, so we need to use the one from payload
          boolean isDelete = StringUtils.isEmpty(attribute.getValue());

          TrackedEntityAttributeValue currentValue =
              attributeValueById.get(attribute.getAttribute());

          boolean isNew = Objects.isNull(currentValue);
          String previousValue = isNew ? null : currentValue.getPlainValue();
          boolean valueChanged = isNew || !Objects.equals(previousValue, attribute.getValue());

          if (isDelete && !isNew) {
            delete(preheat, currentValue, trackedEntity, user, changeLogs, batch);
          } else if (valueChanged) {
            saveOrUpdateAttributeValue(
                preheat,
                trackedEntity,
                attribute,
                currentValue,
                isNew,
                previousValue,
                user,
                changeLogs,
                batch);
          }
        });
  }

  private void saveOrUpdateAttributeValue(
      TrackerPreheat preheat,
      TrackedEntity trackedEntity,
      Attribute attribute,
      TrackedEntityAttributeValue currentValue,
      boolean isNew,
      String previousValue,
      UserDetails user,
      ChangeLogAccumulator changeLogs,
      EntityWriteBatch batch) {
    TrackedEntityAttributeValue attributeToPersist =
        Optional.ofNullable(currentValue)
            .orElseGet(
                () ->
                    new TrackedEntityAttributeValue()
                        .setAttribute(
                            getTrackedEntityAttributeFromPreheat(preheat, attribute.getAttribute()))
                        .setTrackedEntity(trackedEntity))
            .setStoredBy(attribute.getStoredBy())
            .setValue(attribute.getValue())
            .setLastUpdated(new Date());

    saveOrUpdate(
        preheat, isNew, trackedEntity, attributeToPersist, previousValue, user, changeLogs, batch);

    handleReservedValue(attributeToPersist);
  }

  private void delete(
      TrackerPreheat preheat,
      TrackedEntityAttributeValue trackedEntityAttributeValue,
      TrackedEntity trackedEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs,
      EntityWriteBatch batch) {
    if (isFileResource(trackedEntityAttributeValue)) {
      unassignFileResource(preheat, trackedEntity.getUid(), trackedEntityAttributeValue.getValue());
    }

    batch.stageTeavDelete(trackedEntityAttributeValue);

    changeLogs.addTrackedEntityChangeLog(
        trackedEntity,
        trackedEntityAttributeValue.getAttribute(),
        trackedEntityAttributeValue.getPlainValue(),
        null,
        DELETE,
        user.getUsername());
  }

  private void saveOrUpdate(
      TrackerPreheat preheat,
      boolean isNew,
      TrackedEntity trackedEntity,
      TrackedEntityAttributeValue trackedEntityAttributeValue,
      String previousValue,
      UserDetails user,
      ChangeLogAccumulator changeLogs,
      EntityWriteBatch batch) {
    if (isFileResource(trackedEntityAttributeValue)) {
      assignFileResource(preheat, trackedEntity.getUid(), trackedEntityAttributeValue.getValue());
    }

    ChangeLogType changeLogType;
    if (isNew) {
      batch.stageTeavInsert(trackedEntityAttributeValue);
      changeLogType = CREATE;
    } else {
      batch.stageTeavUpdate(trackedEntityAttributeValue);
      changeLogType = UPDATE;
    }

    changeLogs.addTrackedEntityChangeLog(
        trackedEntity,
        trackedEntityAttributeValue.getAttribute(),
        previousValue,
        trackedEntityAttributeValue.getPlainValue(),
        changeLogType,
        user.getUsername());
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

  protected static String formatDate(Date date) {
    return date != null ? DATE_FORMATTER.format(date.toInstant()) : null;
  }

  protected static String formatGeometry(org.locationtech.jts.geom.Geometry geometry) {
    if (geometry == null) {
      return null;
    }
    return java.util.stream.Stream.of(geometry.getCoordinates())
        .map(c -> String.format("(%f, %f)", c.x, c.y))
        .collect(java.util.stream.Collectors.joining(", "));
  }
}
