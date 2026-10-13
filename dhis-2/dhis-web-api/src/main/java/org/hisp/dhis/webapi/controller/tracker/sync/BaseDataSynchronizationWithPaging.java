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
package org.hisp.dhis.webapi.controller.tracker.sync;

import static java.lang.String.format;
import static org.hisp.dhis.dxf2.sync.SyncUtils.testServerAvailability;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.webapi.controller.tracker.export.MappingErrors.ensureNoMappingErrors;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.alreadyDeletedOrSucceededUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.blockingFailedItems;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.failedItems;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.failedUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.formatFailedUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.sendTrackerRequest;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.successfullyProcessedUids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.SoftDeletableEntity;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.DataSynchronizationWithPaging;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.FailedItem;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.springframework.web.client.RestTemplate;

/**
 * Base abstract class providing common functionality for data synchronization operations with
 * pagination support. This class handles the core synchronization logic including HTTP
 * communication, progress tracking, error handling, and entity partitioning.
 *
 * <p>Concrete implementations should extend this class and provide entity-specific behavior for
 * data fetching, mapping, and timestamp updates. If an entity can have deleted children — like a
 * tracked entity with a deleted enrollment, or an event with a deleted relationship — override
 * {@link #splitDeletedChildren(List)} to pull those children out of the create/update payload and
 * send them in the same delete request as everything else being deleted. If an entity can have
 * fields that should never leave the local instance — like an attribute or data element flagged
 * {@code skipSynchronization} — override {@link #stripSkipSyncFields(List, List,
 * TrackerSynchronizationContext)} to remove them before sending.
 *
 * <p>This class follows a template method pattern where the abstract methods define the specific
 * behavior that concrete classes must implement.
 *
 * @param <V> The type of entity DTO/View object used for serialization and HTTP communication
 * @param <D> The type of domain entity being synchronized, must extend {@link SoftDeletableEntity}
 * @see DataSynchronizationWithPaging
 * @see TrackerSynchronizationContext
 */
@Slf4j
abstract class BaseDataSynchronizationWithPaging<V, D extends SoftDeletableEntity>
    implements DataSynchronizationWithPaging {

  /**
   * Synthetic error code used in place of a real one when an entity is excluded from sync only
   * because one of its children failed, not because of any error of its own.
   */
  static final String CHILD_FAILED_ERROR_CODE = "CHILD_FAILED";

  private final RenderService renderService;
  private final RestTemplate restTemplate;
  private final SystemSettingsService systemSettingsService;

  protected BaseDataSynchronizationWithPaging(
      RenderService renderService,
      RestTemplate restTemplate,
      SystemSettingsService systemSettingsService) {
    this.renderService = renderService;
    this.restTemplate = restTemplate;
    this.systemSettingsService = systemSettingsService;
  }

  @Override
  public SynchronizationResult synchronizeData(int pageSize, JobProgress progress) {
    progress.startingProcess(getProcessName());
    SystemSettings settings = systemSettingsService.getCurrentSettings();

    if (!testServerAvailability(settings, restTemplate).isAvailable()) {
      return failProcess(progress, "Remote server unavailable");
    }

    TrackerSynchronizationContext context =
        progress.runStage(
            TrackerSynchronizationContext.emptyContext(null, pageSize),
            ctx ->
                format(
                    "%s changed before %s will not sync",
                    getEntityName(), ctx.getSkipChangedBefore()),
            () -> createContext(pageSize, settings));

    if (context.hasNoObjectsToSynchronize()) {
      return endProcess(progress, "No %s to synchronize".formatted(getEntityName()));
    }

    boolean success = executeSynchronizationWithPaging(context, progress, settings);

    return success
        ? endProcess(progress, "Completed successfully")
        : failProcess(progress, "Page-level synchronization failed");
  }

  public abstract V toMinimalEntity(D entity);

  public abstract String getJsonRootName();

  public abstract String getEntityName();

  public abstract String getProcessName();

  public abstract void updateEntitySyncTimeStamp(List<V> entities, Date syncTime);

  public abstract boolean isDeleted(D entity);

  public abstract V getMappedEntities(
      D ev, TrackerIdSchemeParams idSchemeParam, MappingErrors errors);

  public abstract long countEntitiesForSynchronization(Date skipChangedBefore)
      throws ForbiddenException, BadRequestException;

  /**
   * Fetches the next batch of entities to synchronize. Implementations should always fetch page 1.
   * Once an entity is synced, it stops matching the "needs sync" query, so the list of unsynced
   * entities keeps shrinking as this job runs, and page 1 of that shrinking list is always the next
   * batch to work on. Asking for a later page would skip over entities.
   */
  public abstract List<D> fetchEntitiesForPage(TrackerSynchronizationContext context)
      throws BadRequestException, ForbiddenException, NotFoundException;

  public abstract TrackerSynchronizationContext createContext(int pageSize, SystemSettings settings)
      throws ForbiddenException, BadRequestException;

  public abstract TrackerType getTrackerType();

  public abstract UID getUid(V entity);

  /**
   * Pulls deleted children — like enrollments, events, or relationships — out of {@code
   * activeEntities}, so they don't get sent again as part of a create/update. Returns those deleted
   * children grouped by the JSON key they belong under in the delete request.
   */
  protected NestedDeletion<V> splitDeletedChildren(List<V> activeEntities) {
    return NestedDeletion.none(activeEntities);
  }

  /**
   * Removes any field flagged {@code skipSynchronization} — like an attribute or data element —
   * from {@code activeDtos} before they're sent to the remote instance.
   */
  protected void stripSkipSyncFields(
      List<D> activeDomainEntities, List<V> activeDtos, TrackerSynchronizationContext context) {
    // no-op by default: entities without skip sync fields don't need to override this.
  }

  /**
   * Checks whether one of {@code entity}'s own children shows up in {@code failedChildUidsByType}.
   * If it does, {@code entity} shouldn't be marked as synced yet, even though {@code entity} itself
   * succeeded, because part of what it owns didn't.
   */
  protected boolean hasFailedChild(V entity, Map<TrackerType, Set<UID>> failedChildUidsByType) {
    return false;
  }

  protected record NestedDeletion<V>(
      List<V> activeEntities,
      Map<String, List<?>> deletedPayloadByJsonKey,
      Map<TrackerType, Integer> deletedCountByType,
      Map<UID, Set<UID>> deletedChildUidsByParent) {

    static <V> NestedDeletion<V> none(List<V> activeEntities) {
      return new NestedDeletion<>(activeEntities, Map.of(), Map.of(), Map.of());
    }

    boolean hasDeletions() {
      return deletedCountByType.values().stream().anyMatch(count -> count > 0);
    }
  }

  private record DeleteSyncResult(Set<UID> syncedUids, Set<UID> blockingFailedChildUids) {}

  protected static List<Relationship> stripDeletedRelationships(
      List<Relationship> relationships,
      Map<UID, Relationship> deletedRelationshipsByUid,
      Set<UID> ownedDeletedChildUids) {
    relationships.stream()
        .filter(Relationship::isDeleted)
        .forEach(
            r -> {
              deletedRelationshipsByUid.put(r.getRelationship(), r);
              ownedDeletedChildUids.add(r.getRelationship());
            });
    return relationships.stream().filter(r -> !r.isDeleted()).toList();
  }

  protected static boolean hasFailedRelationship(
      List<Relationship> relationships, Set<UID> failedRelationships) {
    return relationships.stream().anyMatch(r -> failedRelationships.contains(r.getRelationship()));
  }

  protected static Relationship toMinimalRelationship(Relationship relationship) {
    Relationship minimal = new Relationship();
    minimal.setRelationship(relationship.getRelationship());
    return minimal;
  }

  private SynchronizationResult endProcess(JobProgress progress, String message) {
    String fullMessage = format("%s %s", getProcessName(), message);
    progress.completedProcess(fullMessage);
    return SynchronizationResult.success(fullMessage);
  }

  private SynchronizationResult failProcess(JobProgress progress, String reason) {
    String fullMessage = format("%s failed. %s", getProcessName(), reason);
    progress.failedProcess(fullMessage);
    return SynchronizationResult.failure(fullMessage);
  }

  private void synchronizePage(TrackerSynchronizationContext context, SystemSettings settings)
      throws ForbiddenException, BadRequestException, NotFoundException, WebMessageException {
    List<D> entities = fetchEntitiesForPage(context);

    Map<Boolean, List<D>> partitionedEntities = partitionEntitiesByDeletionStatus(entities);
    List<D> deletedEntities = partitionedEntities.get(true);
    List<D> activeEntities = partitionedEntities.get(false);

    syncEntitiesByDeletionStatus(activeEntities, deletedEntities, context, settings);
  }

  private boolean executeSynchronizationWithPaging(
      TrackerSynchronizationContext context, JobProgress progress, SystemSettings settings) {
    final int pages = context.getPages();
    final int pageSize = context.getPageSize();
    final String entityName = getJsonRootName();
    final String remoteUrl = context.getInstance().getUrl();

    String stageDescription =
        format(
            "Found %d %s. Remote: %s. Pages: %d (size %d)",
            context.getObjectsToSynchronize(), entityName, remoteUrl, pages, pageSize);

    progress.startingStage(stageDescription, pages, SKIP_ITEM);

    progress.runStage(
        IntStream.range(1, pages + 1).boxed(),
        page -> format("Syncing page %d (size %d)", page, pageSize),
        page -> synchronizePageSafely(page, context, settings));

    long unprocessed = context.getObjectsToSynchronize() - context.getAttemptedUids().size();
    if (unprocessed > 0) {
      log.info(
          "{}: {} of {} {} were never attempted this run",
          getProcessName(),
          unprocessed,
          context.getObjectsToSynchronize(),
          entityName);
    }

    return !progress.isSkipCurrentStage();
  }

  private void syncEntitiesByDeletionStatus(
      List<D> activeEntities,
      List<D> deletedEntities,
      TrackerSynchronizationContext context,
      SystemSettings settings)
      throws WebMessageException {
    Date syncTime = context.getStartTime();
    SystemInstance instance = context.getInstance();

    TrackerIdSchemeParams idSchemeParam =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.UID).build();
    MappingErrors errors = new MappingErrors(idSchemeParam);

    List<V> activeDtos =
        activeEntities.stream().map(ev -> getMappedEntities(ev, idSchemeParam, errors)).toList();
    ensureNoMappingErrors(errors);

    stripSkipSyncFields(activeEntities, activeDtos, context);

    NestedDeletion<V> nested = splitDeletedChildren(activeDtos);
    List<V> strippedActiveDtos = nested.activeEntities();

    List<V> deletedDtos = deletedEntities.stream().map(this::toMinimalEntity).toList();

    activeDtos.forEach(v -> context.getAttemptedUids().add(getUid(v)));
    deletedDtos.forEach(v -> context.getAttemptedUids().add(getUid(v)));

    DeleteSyncResult deleteResult = syncDeletedIfNeeded(deletedDtos, nested, instance, settings);

    Set<UID> activeSyncCandidateUids =
        strippedActiveDtos.isEmpty()
            ? Set.of()
            : syncActive(strippedActiveDtos, instance, settings);

    Set<UID> blockingFailedChildUids =
        deleteResult == null ? Set.of() : deleteResult.blockingFailedChildUids();
    Set<UID> syncedActiveUids =
        activeSyncCandidateUids.stream()
            .filter(
                uid ->
                    Collections.disjoint(
                        nested.deletedChildUidsByParent().getOrDefault(uid, Set.of()),
                        blockingFailedChildUids))
            .collect(Collectors.toCollection(HashSet::new));
    Set<UID> syncedDeletedUids = deleteResult == null ? Set.of() : deleteResult.syncedUids();

    stampSyncTimestamps(
        deletedDtos, syncedDeletedUids, strippedActiveDtos, syncedActiveUids, syncTime);
  }

  private DeleteSyncResult syncDeletedIfNeeded(
      List<V> deletedTopLevelDtos,
      NestedDeletion<V> nested,
      SystemInstance instance,
      SystemSettings settings) {
    if (deletedTopLevelDtos.isEmpty() && !nested.hasDeletions()) {
      return null;
    }
    return syncDeleted(deletedTopLevelDtos, nested, instance, settings);
  }

  private DeleteSyncResult syncDeleted(
      List<V> deletedTopLevelDtos,
      NestedDeletion<V> nested,
      SystemInstance instance,
      SystemSettings settings) {
    String url = instance.getUrl() + "?importStrategy=" + DELETE + "&async=false&atomicMode=OBJECT";

    Map<String, List<?>> payload = new LinkedHashMap<>();
    payload.put(getJsonRootName(), deletedTopLevelDtos);
    payload.putAll(nested.deletedPayloadByJsonKey());

    ImportReport report =
        sendTrackerRequest(restTemplate, renderService, payload, instance, settings, url);

    // An entity whose own delete came back "already deleted" achieved its goal, so it is treated
    // as synced here too.
    Set<UID> syncedTopLevelUids = alreadyDeletedOrSucceededUids(report, getTrackerType());
    List<FailedItem> failedTopLevelItems = blockingFailedItems(report, getTrackerType());

    Set<UID> blockingFailedChildUids = new HashSet<>();
    StringBuilder childSummary = new StringBuilder();
    for (Map.Entry<TrackerType, Integer> entry : nested.deletedCountByType().entrySet()) {
      TrackerType childType = entry.getKey();
      int total = entry.getValue();
      Set<UID> syncedChild = alreadyDeletedOrSucceededUids(report, childType);
      List<FailedItem> failedChildItems = blockingFailedItems(report, childType);
      failedChildItems.forEach(item -> blockingFailedChildUids.add(item.uid()));
      childSummary.append(
          format(
              ", %s=%d/%d synced%s",
              childType, syncedChild.size(), total, formatFailedUids(failedChildItems)));
    }

    log.info(
        "{} delete sync: {}={}/{} synced{}{}",
        getEntityName(),
        getTrackerType(),
        syncedTopLevelUids.size(),
        deletedTopLevelDtos.size(),
        formatFailedUids(failedTopLevelItems),
        childSummary);

    return new DeleteSyncResult(syncedTopLevelUids, blockingFailedChildUids);
  }

  private Set<UID> syncActive(List<V> entities, SystemInstance instance, SystemSettings settings) {
    String url =
        instance.getUrl()
            + "?importStrategy="
            + CREATE_AND_UPDATE
            + "&async=false&atomicMode=OBJECT";

    ImportReport report =
        sendTrackerRequest(
            restTemplate,
            renderService,
            Map.of(getJsonRootName(), entities),
            instance,
            settings,
            url);

    Set<UID> succeededTopLevelUids = successfullyProcessedUids(report, getTrackerType());
    Set<UID> syncedUids = filterByFailedChildren(succeededTopLevelUids, entities, report);

    log.info(
        "{} create/update sync: {}/{} synced{}",
        getEntityName(),
        syncedUids.size(),
        entities.size(),
        formatFailedUids(explainUnsyncedEntities(succeededTopLevelUids, syncedUids, report)));

    return syncedUids;
  }

  /**
   * Adds the default error code {@link #CHILD_FAILED_ERROR_CODE} to all top entities that are
   * successfully processed but are not synchronized and don't have an error code yet.
   *
   * @return the list of (entity, errorCode) pairs
   */
  List<FailedItem> explainUnsyncedEntities(
      Set<UID> succeededTopLevelUids, Set<UID> syncedUids, ImportReport report) {
    List<FailedItem> failures = new ArrayList<>(failedItems(report, getTrackerType()));
    Set<UID> explainedUids =
        failures.stream().map(FailedItem::uid).collect(Collectors.toCollection(HashSet::new));

    for (UID uid : succeededTopLevelUids) {
      if (!syncedUids.contains(uid) && !explainedUids.contains(uid)) {
        explainedUids.add(uid);
        failures.add(new FailedItem(uid, CHILD_FAILED_ERROR_CODE));
      }
    }

    return failures;
  }

  private Set<UID> filterByFailedChildren(
      Set<UID> succeededTopLevelUids, List<V> candidates, ImportReport report) {
    if (succeededTopLevelUids.isEmpty()) {
      return succeededTopLevelUids;
    }

    Map<TrackerType, Set<UID>> failedByType = new EnumMap<>(TrackerType.class);
    boolean anyFailures = false;
    for (TrackerType type : TrackerType.values()) {
      Set<UID> failed = failedUids(report, type);
      failedByType.put(type, failed);
      anyFailures |= !failed.isEmpty();
    }
    if (!anyFailures) {
      return succeededTopLevelUids;
    }

    return candidates.stream()
        .filter(v -> succeededTopLevelUids.contains(getUid(v)))
        .filter(v -> !hasFailedChild(v, failedByType))
        .map(this::getUid)
        .collect(Collectors.toCollection(HashSet::new));
  }

  private void stampSyncTimestamps(
      List<V> deletedCandidates,
      Set<UID> syncedDeletedUids,
      List<V> activeCandidates,
      Set<UID> syncedActiveUids,
      Date syncTime) {
    List<V> synced = new ArrayList<>();
    synced.addAll(resolveSynced(deletedCandidates, syncedDeletedUids));
    synced.addAll(resolveSynced(activeCandidates, syncedActiveUids));
    if (synced.isEmpty()) {
      return;
    }
    updateEntitySyncTimeStamp(synced, syncTime);
  }

  private List<V> resolveSynced(List<V> candidates, Set<UID> syncedUids) {
    if (syncedUids.isEmpty()) {
      return List.of();
    }
    return candidates.stream().filter(v -> syncedUids.contains(getUid(v))).toList();
  }

  private void synchronizePageSafely(
      int page, TrackerSynchronizationContext context, SystemSettings settings) {
    try {
      synchronizePage(context, settings);
    } catch (Exception ex) {
      log.error("Failed to synchronize page {}", page, ex);
      throw new RuntimeException(
          format("Page %d synchronization failed: %s", page, ex.getMessage()), ex);
    }
  }

  private Map<Boolean, List<D>> partitionEntitiesByDeletionStatus(List<D> entities) {
    return entities.stream().collect(Collectors.partitioningBy(this::isDeleted));
  }
}
