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
package org.hisp.dhis.dxf2.events.aggregates;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hisp.dhis.dxf2.events.aggregates.ThreadPoolManager.getPool;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.ProgramOwner;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.store.AclStore;
import org.hisp.dhis.dxf2.events.trackedentity.store.TrackedEntityInstanceStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserServiceTarget;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
@RequiredArgsConstructor
public class TrackedEntityInstanceAggregate extends AbstractAggregate
    implements CurrentUserServiceTarget {
  @NonNull private final TrackedEntityInstanceStore trackedEntityInstanceStore;

  @NonNull private final EnrollmentAggregate enrollmentAggregate;

  @NonNull private CurrentUserService currentUserService;

  @NonNull private final AclStore aclStore;

  @NonNull private final TrackedEntityAttributeService trackedEntityAttributeService;

  @NonNull private final CacheProvider cacheProvider;

  private Cache<Set<TrackedEntityAttribute>> teiAttributesCache;

  private Cache<Map<Program, Set<TrackedEntityAttribute>>> programTeiAttributesCache;

  private Cache<List<String>> userGroupUIDCache;

  private Cache<AggregateContext> securityCache;

  @PostConstruct
  protected void init() {
    teiAttributesCache = cacheProvider.createTeiAttributesCache();
    programTeiAttributesCache = cacheProvider.createProgramTeiAttributesCache();
    userGroupUIDCache = cacheProvider.createUserGroupUIDCache();
    securityCache = cacheProvider.createSecurityCache();
  }

  /**
   * Fetches a List of {@see TrackedEntityInstance} based on the list of primary keys and search
   * parameters
   *
   * @param ids a List of {@see TrackedEntityInstance} Primary Keys
   * @param params an instance of {@see TrackedEntityInstanceParams}
   * @return a List of {@see TrackedEntityInstance} objects
   */
  public List<TrackedEntityInstance> find(
      List<Long> ids,
      TrackedEntityInstanceParams params,
      TrackedEntityInstanceQueryParams queryParams) {
    final Optional<User> user = Optional.ofNullable(currentUserService.getCurrentUser());

    user.ifPresent(
        u -> {
          if (userGroupUIDCache.get(user.get().getUid()).isEmpty()
              && !CollectionUtils.isEmpty(user.get().getGroups())) {
            userGroupUIDCache.put(
                user.get().getUid(),
                user.get().getGroups().stream()
                    .map(BaseIdentifiableObject::getUid)
                    .collect(Collectors.toList()));
          }
        });

    /*
     * Create a context with information which will be used to fetch the
     * entities. Use a superUser context if the user is null.
     */
    AggregateContext ctx =
        user.map(
                u ->
                    securityCache
                        .get(
                            u.getUid(),
                            userUID ->
                                getSecurityContext(
                                    userUID,
                                    userGroupUIDCache.get(userUID).orElse(Lists.newArrayList())))
                        .toBuilder()
                        .userId(u.getId())
                        .userUid(u.getUid())
                        .userGroups(
                            userGroupUIDCache.get(u.getUid()).orElse(Collections.emptyList()))
                        .superUser(u.isSuper()))
            .orElse(
                new AggregateContext.AggregateContextBuilder()
                    .superUser(true)
                    .trackedEntityTypes(Collections.emptyList())
                    .programs(Collections.emptyList())
                    .programStages(Collections.emptyList())
                    .relationshipTypes(Collections.emptyList()))
            .params(params)
            .queryParams(queryParams)
            .build();

    /*
     * Async fetch Relationships for the given TrackedEntityInstance id
     * (only if isIncludeRelationships = true)
     */
    final CompletableFuture<Multimap<String, Relationship>> relationshipsAsync =
        conditionalAsyncFetch(
            ctx.getParams().isIncludeRelationships(),
            () -> trackedEntityInstanceStore.getRelationships(ids),
            getPool());

    /*
     * Async fetch Enrollments for the given TrackedEntityInstance id (only
     * if isIncludeEnrollments = true)
     */
    final CompletableFuture<Multimap<String, Enrollment>> enrollmentsAsync =
        conditionalAsyncFetch(
            ctx.getParams().isIncludeEnrollments(),
            () -> enrollmentAggregate.findByTrackedEntityInstanceIds(ids, ctx),
            getPool());

    /*
     * Async fetch all ProgramOwner for the given TrackedEntityInstance id
     */
    final CompletableFuture<Multimap<String, ProgramOwner>> programOwnersAsync =
        conditionalAsyncFetch(
            ctx.getParams().isIncludeProgramOwners(),
            () -> trackedEntityInstanceStore.getProgramOwners(ids),
            getPool());

    /*
     * Async Fetch TrackedEntityInstances by id
     */
    final CompletableFuture<Map<String, TrackedEntityInstance>> teisAsync =
        supplyAsync(
            () -> trackedEntityInstanceStore.getTrackedEntityInstances(ids, ctx), getPool());

    /*
     * Async fetch TrackedEntityInstance Attributes by TrackedEntityInstance
     * id
     */
    final CompletableFuture<Multimap<String, Attribute>> attributesAsync =
        supplyAsync(() -> trackedEntityInstanceStore.getAttributes(ids), getPool());

    /*
     * Async fetch Owned Tei mapped to the provided program attributes by
     * TrackedEntityInstance id
     */
    final CompletableFuture<Multimap<String, String>> ownedTeiAsync =
        conditionalAsyncFetch(
            user.isPresent(), () -> trackedEntityInstanceStore.getOwnedTeis(ids, ctx), getPool());

    /*
     * Execute all queries and merge the results
     */
    return allOf(teisAsync, attributesAsync, relationshipsAsync, enrollmentsAsync, ownedTeiAsync)
        .thenApplyAsync(
            fn -> {
              Map<String, TrackedEntityInstance> teis = teisAsync.join();

              Multimap<String, Attribute> attributes = attributesAsync.join();
              Multimap<String, Relationship> relationships = relationshipsAsync.join();
              Multimap<String, Enrollment> enrollments = enrollmentsAsync.join();
              Multimap<String, ProgramOwner> programOwners = programOwnersAsync.join();
              Multimap<String, String> ownedTeis = ownedTeiAsync.join();

              Stream<String> teiUidStream = teis.keySet().parallelStream();

              if (user.isPresent() && queryParams.hasProgram()) {
                teiUidStream = teiUidStream.filter(ownedTeis::containsKey);
              }

              return teiUidStream
                  .map(
                      uid -> {
                        TrackedEntityInstance tei = teis.get(uid);
                        tei.setAttributes(
                            filterAttributes(
                                attributes.get(uid),
                                ownedTeis.get(uid),
                                teiAttributesCache.get(
                                    "ALL_ATTRIBUTES",
                                    s ->
                                        trackedEntityAttributeService
                                            .getTrackedEntityAttributesByTrackedEntityTypes()),
                                programTeiAttributesCache.get(
                                    "ATTRIBUTES_BY_PROGRAM",
                                    s ->
                                        trackedEntityAttributeService
                                            .getTrackedEntityAttributesByProgram()),
                                ctx));
                        tei.setRelationships(new ArrayList<>(relationships.get(uid)));
                        tei.setEnrollments(
                            filterEnrollments(enrollments.get(uid), ownedTeis.get(uid), ctx));
                        tei.setProgramOwners(new ArrayList<>(programOwners.get(uid)));
                        return tei;
                      })
                  .collect(Collectors.toList());
            },
            getPool())
        .join();
  }

  @Override
  public void setCurrentUserService(CurrentUserService currentUserService) {
    this.currentUserService = currentUserService;
  }

  /** Filter enrollments based on ownership and super user status. */
  private List<Enrollment> filterEnrollments(
      Collection<Enrollment> enrollments, Collection<String> programs, AggregateContext ctx) {
    List<Enrollment> enrollmentList = new ArrayList<>();

    if (enrollments.isEmpty()) {
      return enrollmentList;
    }

    enrollmentList.addAll(
        enrollments.stream()
            .filter(e -> (programs.contains(e.getProgram()) || ctx.isSuperUser()))
            .collect(Collectors.toList()));

    return enrollmentList;
  }

  /** Filter attributes based on queryParams, ownership and super user status */
  private List<Attribute> filterAttributes(
      Collection<Attribute> attributes,
      Collection<String> programs,
      Set<TrackedEntityAttribute> trackedEntityTypeAttributes,
      Map<Program, Set<TrackedEntityAttribute>> teaByProgram,
      AggregateContext ctx) {
    List<Attribute> attributeList = new ArrayList<>();

    // Nothing to filter from, return empty
    if (attributes.isEmpty()) {
      return attributeList;
    }

    // Add all tet attributes. Conditionally filter out the ones marked for
    // skipSynchronization in case this is a dataSynchronization query
    Set<String> allowedAttributeUids =
        trackedEntityTypeAttributes.stream()
            .filter(
                att ->
                    (!ctx.getParams().isDataSynchronizationQuery()
                        || !att.getSkipSynchronization()))
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet());

    for (Program program : teaByProgram.keySet()) {
      if (programs.contains(program.getUid()) || ctx.isSuperUser()) {
        allowedAttributeUids.addAll(
            teaByProgram.get(program).stream()
                .filter(
                    att ->
                        (!ctx.getParams().isDataSynchronizationQuery()
                            || !att.getSkipSynchronization()))
                .map(BaseIdentifiableObject::getUid)
                .collect(Collectors.toSet()));
      }
    }

    for (Attribute attributeValue : attributes) {
      if (allowedAttributeUids.contains(attributeValue.getAttribute())) {
        attributeList.add(attributeValue);
      }
    }

    return attributeList;
  }

  /**
   * Fetch security related information and add them to the {@see AggregateContext}
   *
   * <p>- all Tracked Entity Instance Types this user has READ access to
   *
   * <p>- all Programs Type this user has READ access to
   *
   * <p>- all Program Stages Type this user has READ access to
   *
   * <p>- all Relationship Types this user has READ access to
   *
   * @param userUID the user uid of a {@see User}
   * @return an instance of {@see AggregateContext} populated with ACL-related info
   */
  private AggregateContext getSecurityContext(String userUID, List<String> userGroupUIDs) {
    final CompletableFuture<List<Long>> getTeiTypes =
        supplyAsync(
            () -> aclStore.getAccessibleTrackedEntityInstanceTypes(userUID, userGroupUIDs),
            getPool());

    final CompletableFuture<List<Long>> getPrograms =
        supplyAsync(() -> aclStore.getAccessiblePrograms(userUID, userGroupUIDs), getPool());

    final CompletableFuture<List<Long>> getProgramStages =
        supplyAsync(() -> aclStore.getAccessibleProgramStages(userUID, userGroupUIDs), getPool());

    final CompletableFuture<List<Long>> getRelationshipTypes =
        supplyAsync(
            () -> aclStore.getAccessibleRelationshipTypes(userUID, userGroupUIDs), getPool());

    return allOf(getTeiTypes, getPrograms, getProgramStages, getRelationshipTypes)
        .thenApplyAsync(
            fn ->
                AggregateContext.builder()
                    .trackedEntityTypes(getTeiTypes.join())
                    .programs(getPrograms.join())
                    .programStages(getProgramStages.join())
                    .relationshipTypes(getRelationshipTypes.join())
                    .build(),
            getPool())
        .join();
  }
}
