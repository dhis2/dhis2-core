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
package org.hisp.dhis.tracker.preheat.supplier.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class TrackerEntityInstanceStrategyTest {
  @InjectMocks private TrackerEntityInstanceStrategy strategy;

  @Mock private TrackedEntityInstanceStore trackedEntityInstanceStore;

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @Test
  void verifyStrategyFiltersOutNonRootTei() {
    // Create preheat params
    final List<TrackedEntity> trackedEntities =
        rnd.objects(TrackedEntity.class, 2).collect(Collectors.toList());
    final TrackerImportParams params =
        TrackerImportParams.builder().trackedEntities(trackedEntities).build();

    // Preheat
    TrackerPreheat preheat = new TrackerPreheat();

    final List<String> rootUids =
        trackedEntities.stream().map(TrackedEntity::getTrackedEntity).collect(Collectors.toList());
    // Add uid of non-root tei
    rootUids.add("noroottei");

    List<List<String>> uids = new ArrayList<>();
    uids.add(rootUids);

    // when
    strategy.add(params, uids, preheat);
    preheat.createReferenceTree();

    assertTrue(preheat.getReference(trackedEntities.get(0).getTrackedEntity()).isPresent());
    assertTrue(preheat.getReference(trackedEntities.get(1).getTrackedEntity()).isPresent());
    assertFalse(preheat.getReference("noroottei").isPresent());
  }

  @Test
  void verifyStrategyIgnoresPersistedTei() {
    // Create preheat params
    final List<TrackedEntity> trackedEntities =
        rnd.objects(TrackedEntity.class, 2).collect(Collectors.toList());
    final TrackerImportParams params =
        TrackerImportParams.builder().trackedEntities(trackedEntities).build();

    // Preheat
    User user = new User();
    TrackerPreheat preheat = new TrackerPreheat();
    preheat.setUser(user);

    final List<String> rootUids =
        trackedEntities.stream().map(TrackedEntity::getTrackedEntity).collect(Collectors.toList());
    // Add uid of non-root tei
    rootUids.add("noroottei");

    List<List<String>> uids = new ArrayList<>();
    uids.add(rootUids);

    when(trackedEntityInstanceStore.getIncludingDeleted(rootUids))
        .thenReturn(
            Lists.newArrayList(
                new TrackedEntityInstance() {
                  {
                    setUid(trackedEntities.get(0).getTrackedEntity());
                  }
                }));

    // when
    strategy.add(params, uids, preheat);
    preheat.createReferenceTree();

    assertFalse(preheat.getReference(trackedEntities.get(0).getTrackedEntity()).isPresent());
    assertTrue(preheat.getReference(trackedEntities.get(1).getTrackedEntity()).isPresent());
    assertFalse(preheat.getReference("noroottei").isPresent());
  }
}
