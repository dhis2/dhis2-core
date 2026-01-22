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
import static org.hisp.dhis.tracker.export.trackedentity.aggregates.AsyncUtils.conditionalAsyncFetch;
import static org.hisp.dhis.tracker.export.trackedentity.aggregates.ThreadPoolManager.getPool;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityFields;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityIdentifiers;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.user.CurrentUserUtil;
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

  @Nonnull private final TrackedEntityAttributeService trackedEntityAttributeService;

  /**
   * Fetches a List of {@see TrackedEntity} based on the list of primary keys and search parameters
   */
  public List<TrackedEntity> find(
      List<TrackedEntityIdentifiers> identifiers,
      TrackedEntityFields fields,
      TrackedEntityQueryParams queryParams) {
    if (identifiers.isEmpty()) {
      return Collections.emptyList();
    }
    Context ctx = new Context(CurrentUserUtil.getCurrentUserDetails(), fields, queryParams);

    List<Long> ids = identifiers.stream().map(TrackedEntityIdentifiers::id).toList();
    final CompletableFuture<Multimap<String, Enrollment>> enrollmentsAsync =
        conditionalAsyncFetch(
            fields.isIncludesEnrollments(),
            () -> enrollmentAggregate.findByTrackedEntityIds(identifiers, ctx),
            getPool());
    final CompletableFuture<Multimap<String, TrackedEntityProgramOwner>> programOwnersAsync =
        conditionalAsyncFetch(
            fields.isIncludesProgramOwners(),
            () -> trackedEntityStore.getProgramOwners(ids),
            getPool());
    final CompletableFuture<Map<String, TrackedEntity>> trackedEntitiesAsync =
        supplyAsync(() -> trackedEntityStore.getTrackedEntities(ids), getPool());
    final CompletableFuture<Multimap<String, TrackedEntityAttributeValue>> attributesAsync =
        conditionalAsyncFetch(
            fields.isIncludesAttributes(), () -> trackedEntityStore.getAttributes(ids), getPool());

    // Fetch allowed attributes on the HTTP thread while async tasks are fetching other data
    Set<String> allowedAttributeUids = getAllowedAttributeUids(queryParams);
    // Wait for all async fetches to complete
    allOf(trackedEntitiesAsync, attributesAsync, enrollmentsAsync, programOwnersAsync).join();

    // Merge results on the HTTP thread (futures are complete)
    Map<String, TrackedEntity> trackedEntities = trackedEntitiesAsync.join();
    Multimap<String, TrackedEntityAttributeValue> attributes = attributesAsync.join();
    Multimap<String, Enrollment> enrollments = enrollmentsAsync.join();
    Multimap<String, TrackedEntityProgramOwner> programOwners = programOwnersAsync.join();

    return trackedEntities.keySet().stream()
        .map(
            uid -> {
              TrackedEntity te = trackedEntities.get(uid);
              te.setTrackedEntityAttributeValues(
                  filterAttributes(allowedAttributeUids, attributes.get(uid)));
              te.setEnrollments(new HashSet<>(enrollments.get(uid)));
              te.setProgramOwners(new HashSet<>(programOwners.get(uid)));
              return te;
            })
        .toList();
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
