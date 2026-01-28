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
import static org.hisp.dhis.tracker.export.trackedentity.aggregates.ThreadPoolManager.getPool;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityFields;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackedEntityAggregate {
  @Nonnull private final TrackedEntityStore trackedEntityStore;

  @Qualifier("org.hisp.dhis.tracker.trackedentity.aggregates.EnrollmentAggregate")
  @Nonnull
  private final EnrollmentAggregate enrollmentAggregate;

  @Nonnull private final TrackedEntityAttributeService trackedEntityAttributeService;

  /**
   * Enriches tracked entities with attributes, enrollments, and program owners. The tracked
   * entities are mutated in place. Program owners are only fetched here when no program filter is
   * specified; when a program is specified, the single program owner is included in the initial
   * query by {@link org.hisp.dhis.tracker.export.trackedentity.JdbcTrackedEntityStore}.
   */
  public void find(
      List<TrackedEntity> trackedEntities,
      TrackedEntityFields fields,
      TrackedEntityQueryParams queryParams) {
    if (trackedEntities.isEmpty()) {
      return;
    }
    Context ctx = new Context(CurrentUserUtil.getCurrentUserDetails(), fields, queryParams);

    List<Long> ids = trackedEntities.stream().map(TrackedEntity::getId).toList();
    final CompletableFuture<Multimap<String, Enrollment>> enrollmentsAsync =
        asyncFetch(
            fields.isIncludesEnrollments(),
            () -> enrollmentAggregate.findByTrackedEntityIds(trackedEntities, ctx));
    final CompletableFuture<Multimap<String, TrackedEntityAttributeValue>> attributesAsync =
        asyncFetch(fields.isIncludesAttributes(), () -> trackedEntityStore.getAttributes(ids));
    // Program owners are only fetched here when no program filter is specified.
    // When a program filter is specified, JdbcTrackedEntityStore includes the program owner.
    final CompletableFuture<Multimap<String, TrackedEntityProgramOwner>> programOwnersAsync =
        asyncFetch(
            fields.isIncludesProgramOwners() && !queryParams.hasEnrolledInTrackerProgram(),
            () -> trackedEntityStore.getProgramOwners(ids));

    // Fetch allowed attributes on the HTTP thread while async tasks are fetching other data
    Set<String> allowedAttributeUids = getAllowedAttributeUids(queryParams);
    // Wait for all async fetches to complete
    allOf(attributesAsync, enrollmentsAsync, programOwnersAsync).join();

    // Merge results on the HTTP thread (futures are complete)
    Multimap<String, TrackedEntityAttributeValue> attributes = attributesAsync.join();
    Multimap<String, Enrollment> enrollments = enrollmentsAsync.join();
    Multimap<String, TrackedEntityProgramOwner> programOwners = programOwnersAsync.join();

    for (TrackedEntity te : trackedEntities) {
      String uid = te.getUid();
      te.setTrackedEntityAttributeValues(
          filterAttributes(allowedAttributeUids, attributes.get(uid)));
      Collection<Enrollment> teEnrollments = enrollments.get(uid);
      te.setEnrollments(teEnrollments.isEmpty() ? Set.of() : Set.copyOf(teEnrollments));
      // Only set program owners if fetched here; otherwise preserve those set by
      // JdbcTrackedEntityStore
      if (!programOwners.isEmpty()) {
        Collection<TrackedEntityProgramOwner> teOwners = programOwners.get(uid);
        te.setProgramOwners(teOwners.isEmpty() ? Set.of() : Set.copyOf(teOwners));
      }
    }
  }

  private static <T> CompletableFuture<Multimap<String, T>> asyncFetch(
      boolean condition, Supplier<Multimap<String, T>> supplier) {
    return condition
        ? supplyAsync(supplier, getPool())
        : CompletableFuture.completedFuture(ArrayListMultimap.create());
  }

  private Set<String> getAllowedAttributeUids(TrackedEntityQueryParams params) {
    Set<String> allowedAttributeUids =
        trackedEntityAttributeService.getTrackedEntityAttributesByTrackedEntityTypes().stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());

    if (params.hasEnrolledInTrackerProgram()) {
      Set<String> teasInProgram =
          trackedEntityAttributeService.getTrackedEntityAttributesInProgram(
              params.getEnrolledInTrackerProgram());
      allowedAttributeUids.addAll(teasInProgram);
    }

    return allowedAttributeUids;
  }

  private Set<TrackedEntityAttributeValue> filterAttributes(
      Set<String> allowedAttributeUids, Collection<TrackedEntityAttributeValue> attributes) {
    if (attributes.isEmpty()) {
      return Set.of();
    }

    return attributes.stream()
        .filter(av -> allowedAttributeUids.contains(av.getAttribute().getUid()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
