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
package org.hisp.dhis.tracker.imports.preheat.supplier.strategy;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityStore;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class TrackerEntityStrategyTest {
  @InjectMocks private TrackerEntityStrategy strategy;

  @Mock private TrackedEntityStore trackedEntityStore;

  @Mock private TrackerPreheat preheat;

  @Test
  void verifyStrategyAddRightTeToPreheat() {
    final List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> trackedEntities =
        trackedEntities();
    final TrackerImportParams params =
        TrackerImportParams.builder().trackedEntities(trackedEntities).build();

    final List<String> uids = List.of("TEIA", "TEIB");

    List<List<String>> splitUids = new ArrayList<>();
    splitUids.add(uids);

    TrackedEntity teA = new TrackedEntity();
    teA.setUid("TEIA");
    TrackedEntity teB = new TrackedEntity();
    teB.setUid("TEIB");
    List<TrackedEntity> dbTrackedEntities = List.of(teA, teB);
    when(trackedEntityStore.getIncludingDeleted(uids)).thenReturn(dbTrackedEntities);
    strategy.add(params, splitUids, preheat);

    Mockito.verify(trackedEntityStore).getIncludingDeleted(uids);
    Mockito.verify(preheat).putTrackedEntities(dbTrackedEntities);
  }

  private List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> trackedEntities() {
    return List.of(
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder().trackedEntity("TEIA").build(),
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder().trackedEntity("TEIB").build());
  }
}
