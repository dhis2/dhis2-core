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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.tracker.export.trackedentity.aggregates.AsyncUtils.conditionalAsyncFetch;
import static org.hisp.dhis.tracker.export.trackedentity.aggregates.ThreadPoolManager.getPool;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityIdentifiers;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
@RequiredArgsConstructor
public class TrackedEntityAggregate {
  @Nonnull private final TrackedEntityStore trackedEntityStore;

  @Qualifier("org.hisp.dhis.tracker.trackedentity.aggregates.EnrollmentAggregate")
  @Nonnull
  private final EnrollmentAggregate enrollmentAggregate;

  private final UserService userService;

  @Nonnull private final TrackedEntityAttributeService trackedEntityAttributeService;

  @Nonnull private final CacheProvider cacheProvider;

  private Cache<Context> securityCache;

  @PostConstruct
  protected void init() {
    securityCache = cacheProvider.createSecurityCache();
  }

  /**
   * Fetches a List of {@see TrackedEntity} based on the list of primary keys and search parameters
   *
   * @param identifiers a List of {@see TrackedEntity} primary key and uid tuples
   * @param params an instance of {@see TrackedEntityParams}
   * @return a List of {@see TrackedEntity} objects
   */
  public List<TrackedEntity> find(
      List<TrackedEntityIdentifiers> identifiers,
      TrackedEntityParams params,
      TrackedEntityQueryParams queryParams,
      OrganisationUnitSelectionMode orgUnitMode) {
    if (identifiers.isEmpty()) {
      return Collections.emptyList();
    }
    List<Long> ids = identifiers.stream().map(TrackedEntityIdentifiers::id).toList();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    final Optional<User> user = Optional.ofNullable(currentUser);

    /*
     * Create a context with information which will be used to fetch the
     * entities. Use a superUser context if the user is null.
     */
    Context ctx =
        user.map(
                u ->
                    securityCache.get(u.getUid(), userUID -> Context.builder().build()).toBuilder()
                        .userId(u.getId())
                        .userUid(u.getUid())
                        .superUser(u.isSuper()))
            .orElse(Context.builder().superUser(true))
            .params(params)
            .queryParams(queryParams)
            .build();

    /*
     * Async fetch Enrollments for the given TrackedEntity id (only if
     * isIncludeEnrollments = true)
     */
    final CompletableFuture<Multimap<String, Enrollment>> enrollmentsAsync =
        conditionalAsyncFetch(
            ctx.getParams().isIncludeEnrollments(),
            () -> enrollmentAggregate.findByTrackedEntityIds(identifiers, ctx),
            getPool());

    /*
     * Async fetch all ProgramOwner for the given TrackedEntity id
     */
    final CompletableFuture<Multimap<String, TrackedEntityProgramOwner>> programOwnersAsync =
        conditionalAsyncFetch(
            ctx.getParams().isIncludeProgramOwners(),
            () -> trackedEntityStore.getProgramOwners(ids),
            getPool());

    /*
     * Async Fetch TrackedEntities by id
     */
    final CompletableFuture<Map<String, TrackedEntity>> trackedEntitiesAsync =
        supplyAsync(() -> trackedEntityStore.getTrackedEntities(ids), getPool());

    /*
     * Async fetch TrackedEntity Attributes by TrackedEntity id
     */
    final CompletableFuture<Multimap<String, TrackedEntityAttributeValue>> attributesAsync =
        supplyAsync(() -> trackedEntityStore.getAttributes(ids), getPool());

    /*
     * Async fetch Owned TE mapped to the provided program attributes by
     * TrackedEntity id
     */
    final CompletableFuture<Multimap<String, String>> ownedTeiAsync =
        conditionalAsyncFetch(
            user.isPresent(),
            () -> trackedEntityStore.getOwnedTrackedEntities(ids, ctx, orgUnitMode == ALL),
            getPool());
    /*
     * Execute all queries and merge the results
     */
    return allOf(trackedEntitiesAsync, attributesAsync, enrollmentsAsync, ownedTeiAsync)
        .thenApplyAsync(
            fn -> {
              Map<String, TrackedEntity> trackedEntities = trackedEntitiesAsync.join();

              Multimap<String, TrackedEntityAttributeValue> attributes = attributesAsync.join();
              Multimap<String, Enrollment> enrollments = enrollmentsAsync.join();
              Multimap<String, TrackedEntityProgramOwner> programOwners = programOwnersAsync.join();
              Multimap<String, String> ownedTeis = ownedTeiAsync.join();

              Stream<String> teUidStream = trackedEntities.keySet().parallelStream();

              return teUidStream
                  .map(
                      uid -> {
                        TrackedEntity te = trackedEntities.get(uid);
                        te.setTrackedEntityAttributeValues(
                            filterAttributes(
                                attributes.get(uid),
                                ownedTeis.get(uid),
                                trackedEntityAttributeService
                                    .getTrackedEntityAttributesByTrackedEntityTypes(),
                                trackedEntityAttributeService.getTrackedEntityAttributesByProgram(),
                                ctx));
                        te.setEnrollments(new HashSet<>(enrollments.get(uid)));
                        te.setProgramOwners(new HashSet<>(programOwners.get(uid)));
                        return te;
                      })
                  .toList();
            },
            getPool())
        .join();
  }

  /** Filter attributes based on queryParams, ownership and superuser status */
  private Set<TrackedEntityAttributeValue> filterAttributes(
      Collection<TrackedEntityAttributeValue> attributes,
      Collection<String> programs,
      Set<TrackedEntityAttribute> trackedEntityTypeAttributes,
      Map<Program, Set<TrackedEntityAttribute>> teaByProgram,
      Context ctx) {
    if (attributes.isEmpty()) {
      return Set.of();
    }

    // Add all tet attributes
    Set<String> allowedAttributeUids =
        trackedEntityTypeAttributes.stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());

    if (ctx.getQueryParams().hasEnrolledInTrackerProgram()) {
      allowedAttributeUids.addAll(
          teaByProgram.get(ctx.getQueryParams().getEnrolledInTrackerProgram()).stream()
              .map(IdentifiableObject::getUid)
              .collect(Collectors.toSet()));
    }

    return attributes.stream()
        .filter(av -> allowedAttributeUids.contains(av.getAttribute().getUid()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
