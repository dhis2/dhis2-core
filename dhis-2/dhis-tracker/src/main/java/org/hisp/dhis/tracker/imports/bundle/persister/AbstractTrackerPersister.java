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
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserUtil;
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

  // One batched read of existing attribute values for every tracked entity touched by this
  // persister run, keyed by tracked-entity uid (resolved by the join so callers need only the
  // payload uids, not pre-resolved DB ids). New tracked entities have no row yet at read time, so
  // the join simply returns nothing for them. Replaces the former per-tracked-entity SELECT.
  private static final String EXISTING_ATTRIBUTE_VALUES_SQL =
      "select teav.trackedentityid, teav.trackedentityattributeid, teav.value"
          + " from trackedentityattributevalue teav"
          + " join trackedentity te on te.trackedentityid = teav.trackedentityid"
          + " where te.uid = any(?)";

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

      // One batched read of existing attribute values for all tracked entities this run touches,
      // keyed by tracked-entity id. Threaded into attribute handling as a mutable per-run map: it
      // is seeded from the DB (so it sees rows an earlier persister flushed on this connection) and
      // updated as TEAVs are staged, so a second occurrence of the same logical TEAV within this
      // run is routed to an UPDATE instead of a duplicate INSERT.
      Map<Long, Map<MetadataIdentifier, TrackedEntityAttributeValue>> existingAttributeValues =
          loadExistingAttributeValues(
              conn, bundle.getPreheat(), trackedEntityUidsForAttributeLoad(dtos));

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
            persistOwnership(bundle, trackerDto, convertedDto, batch);
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
                bundle.getPreheat(),
                trackerDto,
                convertedDto,
                bundle.getUser(),
                changeLogs,
                batch,
                existingAttributeValues);
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
                  batch,
                  existingAttributeValues);
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
            batch.flush(conn);
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
        batch.flush(conn);
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

  /**
   * Assigns a pre-allocated id to a new entity. Implemented by each concrete persister against its
   * statically-known entity type {@code V}; only invoked when {@link #sequenceName()} is non-null.
   */
  protected abstract void assignId(V convertedDto, long id);

  /**
   * Stages a new entity for insertion in the batch. Implemented by each concrete persister against
   * its statically-known entity type {@code V}, so the correct {@link EntityWriteBatch} overload is
   * selected at compile time rather than by runtime type dispatch.
   */
  protected abstract void stageInsert(V convertedDto, EntityWriteBatch batch);

  /**
   * Stages an existing entity for update in the batch. {@link RelationshipPersister} throws, as
   * relationships are never updated -- the persister branch in {@link #persist} ignores update
   * payloads before this is called.
   */
  protected abstract void stageUpdate(V convertedDto, EntityWriteBatch batch);

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

  /**
   * Persists ownership records for the given entity. The only non-trivial implementation
   * (enrollments creating a {@link org.hisp.dhis.tracker.model.TrackedEntityProgramOwner}) stages
   * the owner row into {@code batch} for a single batched multi-row INSERT at flush, alongside the
   * other entity writes. Staging within the per-entity try/catch means a rollback also drops the
   * staged owner, and the {@code trackedentityid + programid} unique-key violation (only reachable
   * via a concurrent import racing on the same key) now surfaces at the batch flush rather than at
   * transaction commit.
   */
  protected abstract void persistOwnership(
      TrackerBundle bundle, T trackerDto, V entity, EntityWriteBatch batch);

  /** Execute the persistence of Data values linked to the entity being processed */
  protected abstract void updateDataValues(
      TrackerPreheat preheat,
      T trackerDto,
      V payloadEntity,
      V currentEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs);

  /**
   * Execute the persistence of Attribute values linked to the entity being processed. {@code
   * existingAttributeValues} is the per-run map of already-known attribute values keyed by tracked
   * entity id (see {@link #persist}); attribute handling reads and mutates it instead of querying
   * the DB per tracked entity.
   */
  protected abstract void updateAttributes(
      TrackerPreheat preheat,
      T trackerDto,
      V hibernateEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs,
      EntityWriteBatch batch,
      Map<Long, Map<MetadataIdentifier, TrackedEntityAttributeValue>> existingAttributeValues);

  /**
   * The uids of the tracked entities whose existing attribute values must be bulk-loaded before the
   * persist loop. Empty by default (persisters that do not write tracked-entity attribute values);
   * overridden by the TrackedEntity and Enrollment persisters.
   */
  protected Set<String> trackedEntityUidsForAttributeLoad(List<T> dtos) {
    return Set.of();
  }

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
    return JdbcBatchSupport.allocateIds(conn, sequenceName, count);
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
      EntityWriteBatch batch,
      Map<Long, Map<MetadataIdentifier, TrackedEntityAttributeValue>> existingAttributeValues) {
    if (payloadAttributes.isEmpty()) {
      return;
    }

    // The per-tracked-entity slice of the run's existing-values map. Seeded by the bulk read in
    // persist() and kept current here: each staged save/update is reflected back so a later
    // occurrence of the same logical TEAV (composite key trackedentityid +
    // trackedentityattributeid)
    // within this run -- e.g. two enrollments under the same TE both carrying the same attribute --
    // is routed to an UPDATE instead of a duplicate INSERT. A brand-new tracked entity simply has
    // no
    // row in the read, so its slice starts empty. The bulk read leaves trackedEntity unset (it has
    // only the id), so bind it to this entity for the writers and change logs.
    Map<MetadataIdentifier, TrackedEntityAttributeValue> attributeValueById =
        existingAttributeValues.computeIfAbsent(trackedEntity.getId(), k -> new HashMap<>());
    attributeValueById.values().forEach(v -> v.setTrackedEntity(trackedEntity));

    payloadAttributes.forEach(
        attribute -> {
          boolean isDelete = StringUtils.isEmpty(attribute.getValue());

          TrackedEntityAttributeValue currentValue =
              attributeValueById.get(attribute.getAttribute());

          boolean isNew = Objects.isNull(currentValue);
          String previousValue = isNew ? null : currentValue.getValue();
          boolean valueChanged = isNew || !Objects.equals(previousValue, attribute.getValue());

          if (isDelete) {
            if (!isNew) {
              delete(preheat, currentValue, trackedEntity, user, changeLogs, batch);

              // Leave the entry in the map: the DELETE is not flushed until the end of
              // the run, so a later occurrence of the same TE+attribute in this run must
              // still see it as existing (matching the pre-batch DB-read behaviour).
            }

            // If the value doesn't exist yet, deleting it is a no-op.
          } else if (valueChanged) {
            TrackedEntityAttributeValue persisted =
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
            attributeValueById.put(attribute.getAttribute(), persisted);
          }
        });
  }

  /**
   * Reads, in a single round-trip, the existing attribute values of every tracked entity in {@code
   * trackedEntityUids}, grouped by tracked-entity id and keyed within each group by the attribute's
   * {@link MetadataIdentifier}. Runs on the transaction-bound connection held by {@link #persist},
   * so it sees rows an earlier persister flushed on the same (uncommitted) transaction. The values
   * are plain {@link TrackedEntityAttributeValue} instances (not Hibernate-managed), matching what
   * the {@code value} property mapping ({@code access="property"}) produces on load, with {@code
   * trackedEntity} left unset (the caller binds it). Attributes are resolved from the preheat by
   * primary key, which is independent of the import's configured idScheme -- the preheat lookup
   * maps are keyed by the idScheme identifier, so resolving by the UID read from the DB would
   * silently miss every existing value under idScheme CODE/NAME/ATTRIBUTE, turning updates into
   * duplicate INSERTs. Existing values for attributes not in the preheat are skipped -- the caller
   * only probes attributes that appear in the payload, and those are always preheated.
   */
  private Map<Long, Map<MetadataIdentifier, TrackedEntityAttributeValue>>
      loadExistingAttributeValues(
          Connection connection, TrackerPreheat preheat, Set<String> trackedEntityUids) {
    Map<Long, Map<MetadataIdentifier, TrackedEntityAttributeValue>> existingByTe = new HashMap<>();
    if (trackedEntityUids.isEmpty()) {
      return existingByTe;
    }
    TrackerIdSchemeParams idSchemes = preheat.getIdSchemes();
    Map<Long, TrackedEntityAttribute> attributesById =
        preheat.getAll(TrackedEntityAttribute.class).stream()
            .collect(Collectors.toMap(TrackedEntityAttribute::getId, a -> a, (a, b) -> a));
    try (PreparedStatement ps = connection.prepareStatement(EXISTING_ATTRIBUTE_VALUES_SQL)) {
      ps.setArray(1, connection.createArrayOf("text", trackedEntityUids.toArray(new String[0])));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          TrackedEntityAttribute attribute =
              attributesById.get(rs.getLong("trackedentityattributeid"));
          if (attribute == null) {
            continue;
          }
          TrackedEntityAttributeValue value =
              new TrackedEntityAttributeValue()
                  .setAttribute(attribute)
                  .setValue(rs.getString("value"));
          existingByTe
              .computeIfAbsent(rs.getLong("trackedentityid"), k -> new HashMap<>())
              .put(idSchemes.toMetadataIdentifier(attribute), value);
        }
      }
    } catch (SQLException e) {
      throw new PersistenceException("Failed to load existing attribute values", e);
    }
    return existingByTe;
  }

  private TrackedEntityAttributeValue saveOrUpdateAttributeValue(
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
            .setUpdatedBy(CurrentUserUtil.getCurrentUsername())
            .setValue(attribute.getValue())
            .setLastUpdated(new Date());

    saveOrUpdate(
        preheat, isNew, trackedEntity, attributeToPersist, previousValue, user, changeLogs, batch);

    return attributeToPersist;
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
        trackedEntityAttributeValue.getValue(),
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
        trackedEntityAttributeValue.getValue(),
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
